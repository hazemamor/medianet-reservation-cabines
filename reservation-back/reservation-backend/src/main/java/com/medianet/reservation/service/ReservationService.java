package com.medianet.reservation.service;

import com.medianet.reservation.dto.CreneauOccupeDto;
import com.medianet.reservation.dto.ReservationAdminCreationRequest;
import com.medianet.reservation.dto.ReservationAdminResponse;
import com.medianet.reservation.dto.ReservationRequest;
import com.medianet.reservation.dto.ReservationResponse;
import com.medianet.reservation.entity.Cabine;
import com.medianet.reservation.entity.Reservation;
import com.medianet.reservation.entity.Utilisateur;
import com.medianet.reservation.dto.UtilisateurResumeDto;
import com.medianet.reservation.entity.enums.EtatCabine;
import com.medianet.reservation.entity.enums.Role;
import com.medianet.reservation.entity.enums.StatutReservation;
import com.medianet.reservation.exception.BusinessException;
import com.medianet.reservation.exception.ConflitReservationException;
import com.medianet.reservation.exception.ResourceNotFoundException;
import com.medianet.reservation.repository.CabineRepository;
import com.medianet.reservation.repository.ReservationRepository;
import com.medianet.reservation.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Gère la réservation automatique (6.5), aucune validation administrateur requise :
 * vérification de disponibilité, contrôle des conflits, création, mise à jour immédiate.
 */
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final CabineRepository cabineRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Transactional
    public ReservationResponse reserver(String emailUtilisateur, ReservationRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(emailUtilisateur)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        Reservation reservation = creerReservation(utilisateur, request.cabineId(), request.dateDebut(), request.dateFin());
        return toDto(reservation);
    }

    /**
     * Réservation créée par un administrateur au nom d'un employé (ex: depuis la grille de
     * disponibilité, en cliquant sur une case libre). Mêmes règles métier que reserver() ci-dessus
     * (créneau valide, cabine pas en maintenance, dans les heures d'ouverture, sans conflit),
     * mais l'employé bénéficiaire est choisi par l'admin plutôt que déduit du token courant.
     */
    @Transactional
    public ReservationAdminResponse reserverPourEmploye(ReservationAdminCreationRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findById(request.utilisateurId())
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable : " + request.utilisateurId()));

        Reservation reservation = creerReservation(utilisateur, request.cabineId(), request.dateDebut(), request.dateFin());
        return toAdminDto(reservation);
    }

    /** Logique commune de création de réservation, partagée entre l'employé et l'admin. */
    private Reservation creerReservation(Utilisateur utilisateur, Long cabineId, LocalDateTime dateDebut, LocalDateTime dateFin) {
        if (!dateFin.isAfter(dateDebut)) {
            throw new BusinessException("La date de fin doit être postérieure à la date de début");
        }

        if (!dateDebut.toLocalDate().equals(dateFin.toLocalDate())) {
            throw new BusinessException("Une réservation doit se dérouler sur un seul et même jour");
        }

        // Verrou pessimiste : la ligne "cabine" est verrouillée jusqu'à la fin de la transaction.
        // Si deux requêtes concurrentes tentent de réserver la même cabine au même moment,
        // la seconde attend le commit/rollback de la première avant de relire l'état réel,
        // ce qui élimine la fenêtre de "check-then-act" (race condition classique).
        Cabine cabine = cabineRepository.findByIdForUpdate(cabineId)
                .orElseThrow(() -> new ResourceNotFoundException("Cabine introuvable : " + cabineId));

        if (cabine.getEtat() == EtatCabine.MAINTENANCE) {
            throw new BusinessException("Cette cabine est actuellement en maintenance");
        }

        if (dateDebut.toLocalTime().isBefore(cabine.getHeureDisponibiliteDebut())
                || dateFin.toLocalTime().isAfter(cabine.getHeureDisponibiliteFin())) {
            throw new BusinessException("Cette cabine n'est disponible qu'entre "
                    + cabine.getHeureDisponibiliteDebut() + " et " + cabine.getHeureDisponibiliteFin());
        }

        boolean conflit = reservationRepository.existsConflict(cabine.getId(), dateDebut, dateFin,
                StatutReservation.ANNULEE);
        if (conflit) {
            throw new ConflitReservationException("La cabine est déjà réservée sur ce créneau");
        }

        Reservation reservation = Reservation.builder()
                .dateReservation(LocalDateTime.now())
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .statut(StatutReservation.EN_ATTENTE)
                .presenceConfirmee(false)
                .utilisateur(utilisateur)
                .cabine(cabine)
                .build();

        return reservationRepository.save(reservation);
    }

    @Transactional
    public ReservationResponse confirmerPresence(String emailUtilisateur, Long reservationId) {
        Reservation reservation = getReservationDeUtilisateur(emailUtilisateur, reservationId);

        if (reservation.getStatut() == StatutReservation.ANNULEE) {
            throw new BusinessException("Cette réservation a déjà été annulée");
        }

        reservation.setPresenceConfirmee(true);
        reservation.setStatut(StatutReservation.CONFIRMEE);
        return toDto(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse annuler(String emailUtilisateur, Long reservationId) {
        Reservation reservation = getReservationDeUtilisateur(emailUtilisateur, reservationId);
        verifierAnnulable(reservation);
        reservation.setStatut(StatutReservation.ANNULEE);
        return toDto(reservationRepository.save(reservation));
    }

    /**
     * Annulation par un administrateur : contrairement a annuler(), ne verifie pas que
     * la reservation appartient a l'appelant (5.2 : l'administrateur gere les ressources
     * mais peut intervenir sur n'importe quelle reservation en cas de besoin).
     */
    @Transactional
    public ReservationAdminResponse annulerParAdmin(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable : " + reservationId));
        verifierAnnulable(reservation);
        reservation.setStatut(StatutReservation.ANNULEE);
        return toAdminDto(reservationRepository.save(reservation));
    }

    /**
     * Une reservation deja passee (dateDebut <= maintenant) ou deja annulee ne peut plus
     * etre annulee, ni par l'employe, ni par l'administrateur.
     */
    private void verifierAnnulable(Reservation reservation) {
        if (reservation.getStatut() == StatutReservation.ANNULEE) {
            throw new BusinessException("Cette réservation a déjà été annulée");
        }
        if (!reservation.getDateDebut().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Impossible d'annuler une réservation déjà passée ou en cours");
        }
    }

    public List<ReservationResponse> historique(String emailUtilisateur) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(emailUtilisateur)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        return reservationRepository.findByUtilisateurOrderByDateDebutDesc(utilisateur)
                .stream().map(this::toDto).toList();
    }

    /** Liste complete de toutes les reservations, tous employes confondus (vue admin). */
    public List<ReservationAdminResponse> listerToutesAdmin() {
        return reservationRepository.findAllByOrderByDateDebutDesc()
                .stream().map(this::toAdminDto).toList();
    }

    /** Liste des employes (role EMPLOYE), utilisee par l'admin pour reserver une cabine en leur nom. */
    public List<UtilisateurResumeDto> listerEmployes() {
        return utilisateurRepository.findByRoleOrderByNomAscPrenomAsc(Role.EMPLOYE)
                .stream()
                .map(u -> new UtilisateurResumeDto(u.getId(), u.getNom(), u.getPrenom(), u.getEmail()))
                .toList();
    }

    /**
     * Creneaux deja reserves (non annules) d'une cabine pour un jour donne, sans identite des
     * employes : utilise par la page de reservation employe pour afficher les plages libres et
     * faciliter le choix d'un creneau, avant meme de tenter la reservation.
     */
    public List<CreneauOccupeDto> creneauxOccupes(Long cabineId, LocalDate date) {
        Cabine cabine = cabineRepository.findById(cabineId)
                .orElseThrow(() -> new ResourceNotFoundException("Cabine introuvable : " + cabineId));

        return reservationRepository.findByCabineAndStatutNot(cabine, StatutReservation.ANNULEE).stream()
                .filter(r -> r.getDateDebut().toLocalDate().equals(date))
                .sorted((a, b) -> a.getDateDebut().compareTo(b.getDateDebut()))
                .map(r -> new CreneauOccupeDto(r.getDateDebut().toLocalTime(), r.getDateFin().toLocalTime()))
                .toList();
    }

    private Reservation getReservationDeUtilisateur(String email, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable : " + reservationId));

        if (!reservation.getUtilisateur().getEmail().equalsIgnoreCase(email)) {
            throw new BusinessException("Cette réservation n'appartient pas à l'utilisateur courant");
        }
        return reservation;
    }

    private ReservationResponse toDto(Reservation r) {
        return new ReservationResponse(
                r.getId(),
                r.getCabine().getId(),
                r.getCabine().getNom(),
                r.getCabine().getNumero(),
                r.getDateReservation(),
                r.getDateDebut(),
                r.getDateFin(),
                r.getStatut(),
                r.getPresenceConfirmee()
        );
    }

    private ReservationAdminResponse toAdminDto(Reservation r) {
        return new ReservationAdminResponse(
                r.getId(),
                r.getCabine().getId(),
                r.getCabine().getNom(),
                r.getCabine().getNumero(),
                r.getUtilisateur().getEmail(),
                r.getUtilisateur().getNom(),
                r.getUtilisateur().getPrenom(),
                r.getDateReservation(),
                r.getDateDebut(),
                r.getDateFin(),
                r.getStatut(),
                r.getPresenceConfirmee()
        );
    }
}
