package com.medianet.reservation.service;

import com.medianet.reservation.dto.ChatbotResponse;
import com.medianet.reservation.dto.RecommendationResponse;
import com.medianet.reservation.entity.Cabine;
import com.medianet.reservation.entity.Equipement;
import com.medianet.reservation.entity.enums.EtatCabine;
import com.medianet.reservation.entity.enums.StatutReservation;
import com.medianet.reservation.exception.BusinessException;
import com.medianet.reservation.repository.CabineRepository;
import com.medianet.reservation.repository.EquipementRepository;
import com.medianet.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Chatbot "maison" (sans appel a un LLM externe) : il repond aux questions frequentes des
 * employes/admins en s'appuyant DIRECTEMENT sur les donnees stockees en base (cabines,
 * equipements, reservations) — et sur le moteur de recommandation deja en place pour les
 * demandes du type "quelle cabine pour X personnes ?". Fonctionne par reconnaissance de
 * mots-cles / motifs (intentions) plutot que par un veritable modele de langage.
 *
 * <p>V2 : corrige des erreurs de reponse de la V1 (une cabine explicitement mentionnee dans
 * la question etait ignoree pour les demandes de disponibilite/reservations, une cabine
 * pouvait apparaitre "disponible" a une heure hors de ses horaires d'ouverture, et de courts
 * numeros de cabine pouvaient matcher par erreur un fragment d'un autre mot) et elargit la
 * couverture des questions comprises.</p>
 */
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final CabineRepository cabineRepository;
    private final ReservationRepository reservationRepository;
    private final EquipementRepository equipementRepository;
    private final RecommendationService recommendationService;

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMAT_HEURE = DateTimeFormatter.ofPattern("HH:mm");

    private static final Pattern PATTERN_DATE_NUM =
            Pattern.compile("\\b(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{2,4}))?\\b");
    private static final Pattern PATTERN_HEURE =
            Pattern.compile("\\b(\\d{1,2})\\s*[h:]\\s*(\\d{2})?\\b");
    private static final Pattern PATTERN_NB_PERSONNES =
            Pattern.compile("\\b(\\d{1,3})\\s*(?:personne|personnes|participant|participants|places?)\\b");

    // Mots-cles indiquant une demande de STATUT ("cette cabine est-elle prise ?"), a distinguer
    // du VERBE "reserver" (intention de reservation, que le chatbot ne traite pas lui-meme).
    private static final String[] MOTS_STATUT_RESERVE = {
            "reservee", "reservees", "reserves", "reservation de", "occupee", "occupees",
            "occupe", "occupes", "prise", "pris", "indisponible", "indisponibles"
    };
    private static final String[] MOTS_INTENTION_RESERVER = {
            "comment reserver", "je veux reserver", "je voudrais reserver", "j aimerais reserver",
            "puis je reserver", "peux tu reserver", "reserve moi", "faire une reservation"
    };

    public ChatbotResponse repondre(String messageBrut) {
        String message = normaliser(messageBrut);

        if (contientUn(message, "bonjour", "salut", "hello", "bonsoir", "coucou")) {
            return new ChatbotResponse(
                    "Bonjour ! Je peux vous renseigner sur les cabines, leurs equipements, leurs "
                            + "disponibilites ou les creneaux deja reserves. Par exemple : "
                            + "\"quelles cabines sont libres demain ?\"",
                    List.of("Cabines libres aujourd'hui", "Equipements de la cabine C5", "Liste des equipements")
            );
        }

        if (contientUn(message, "merci")) {
            return new ChatbotResponse("Avec plaisir ! N'hesitez pas si vous avez une autre question.");
        }

        if (contientUn(message, MOTS_INTENTION_RESERVER)) {
            return new ChatbotResponse(
                    "Je ne peux pas creer de reservation moi-meme pour le moment. Rendez-vous sur la page "
                            + "\"Cabines\" pour reserver un creneau, ou, si vous etes administrateur, depuis "
                            + "la grille \"Disponibilite\" en cliquant sur une case libre. "
                            + "Je peux en revanche vous dire quelles cabines sont libres si ca vous aide !"
            );
        }

        Optional<Cabine> cabineMentionnee = trouverCabineMentionnee(message);
        Optional<LocalDate> dateMentionnee = extraireDate(message);
        Optional<LocalTime[]> creneauMentionne = extraireCreneau(message);

        // Intention : recommandation ("quelle cabine pour 6 personnes ?")
        Optional<Integer> nbPersonnes = extraireNombrePersonnes(message);
        if (nbPersonnes.isPresent() && contientUn(message, "recommand", "conseil", "quelle cabine",
                "quel cabine", "cabine pour", "suggere", "suggestion")) {
            return reponseRecommandation(nbPersonnes.get());
        }

        // Intention : équipements (d'une cabine précise, sinon la liste complète)
        if (contientUn(message, "equipement")) {
            if (cabineMentionnee.isPresent()) {
                return reponseEquipementsCabine(cabineMentionnee.get());
            }
            return reponseListeEquipements();
        }

        // Intention : statistiques globales sur les cabines
        if (contientUn(message, "combien de cabines", "nombre de cabines")) {
            return reponseNombreCabines();
        }
        if (contientUn(message, "maintenance")) {
            return reponseCabinesEnMaintenance();
        }

        // Intention : statut "réservée / occupée" — filtrée sur la cabine mentionnée si présente
        if (contientUn(message, MOTS_STATUT_RESERVE)) {
            LocalDate date = dateMentionnee.orElse(LocalDate.now());
            if (cabineMentionnee.isPresent()) {
                return reponseReservationsCabine(cabineMentionnee.get(), date);
            }
            return reponseCabinesReserveesADate(date);
        }

        // Intention : disponibilité — filtrée sur la cabine mentionnée si présente
        if (contientUn(message, "dispo", "disponib", "libre", "libres")) {
            LocalDate date = dateMentionnee.orElse(LocalDate.now());
            if (cabineMentionnee.isPresent()) {
                return reponseDisponibiliteCabine(cabineMentionnee.get(), date, creneauMentionne);
            }
            return reponseCabinesDisponibles(date, creneauMentionne);
        }

        // Intention : détail d'une cabine précise (capacité, localisation, équipements, horaires...)
        if (cabineMentionnee.isPresent()) {
            return reponseDetailCabine(cabineMentionnee.get());
        }

        return new ChatbotResponse(
                "Je n'ai pas bien compris. Vous pouvez me demander par exemple : "
                        + "\"quelles cabines sont libres aujourd'hui / demain / le 20/07 ?\", "
                        + "\"la cabine C5 est-elle reservee demain ?\", "
                        + "\"quels equipements a la cabine C5 ?\", \"liste des equipements\" ou "
                        + "\"quelle cabine pour 6 personnes ?\".",
                List.of("Cabines libres aujourd'hui", "Cabines reservees demain", "Liste des equipements")
        );
    }

    // ---------------------------------------------------------------------
    // Construction des réponses
    // ---------------------------------------------------------------------

    private ChatbotResponse reponseCabinesDisponibles(LocalDate date, Optional<LocalTime[]> creneau) {
        List<Cabine> cabinesActives = cabineRepository.findAll().stream()
                .filter(c -> c.getEtat() != EtatCabine.MAINTENANCE)
                .toList();

        List<String> libres = cabinesActives.stream()
                .filter(c -> estLibre(c, date, creneau))
                .map(c -> c.getNom() + " (" + c.getNumero() + ", capacite " + c.getCapacite() + ")")
                .toList();

        String periode = decrirePeriode(creneau);

        if (libres.isEmpty()) {
            return new ChatbotResponse("Aucune cabine n'est libre le " + date.format(FORMAT_DATE) + periode + ".");
        }

        String texte = "Le " + date.format(FORMAT_DATE) + periode + ", "
                + libres.size() + " cabine(s) disponible(s) : " + String.join(", ", libres) + ".";
        return new ChatbotResponse(texte);
    }

    private ChatbotResponse reponseDisponibiliteCabine(Cabine cabine, LocalDate date, Optional<LocalTime[]> creneau) {
        String periode = decrirePeriode(creneau);

        if (cabine.getEtat() == EtatCabine.MAINTENANCE) {
            return new ChatbotResponse("La cabine " + cabine.getNom() + " (" + cabine.getNumero()
                    + ") est actuellement en maintenance, elle n'est pas reservable.");
        }

        if (creneau.isPresent() && horsHorairesOuverture(cabine, creneau.get()[0], creneau.get()[1])) {
            return new ChatbotResponse("La cabine " + cabine.getNom() + " (" + cabine.getNumero()
                    + ") n'est ouverte que de " + cabine.getHeureDisponibiliteDebut().format(FORMAT_HEURE)
                    + " a " + cabine.getHeureDisponibiliteFin().format(FORMAT_HEURE)
                    + ", le creneau demande" + periode + " est en dehors de ses horaires.");
        }

        boolean libre = estLibre(cabine, date, creneau);
        String texte = "La cabine " + cabine.getNom() + " (" + cabine.getNumero() + ") est "
                + (libre ? "libre" : "deja reservee") + " le " + date.format(FORMAT_DATE) + periode + ".";
        return new ChatbotResponse(texte);
    }

    /** true si la cabine n'a aucune reservation en conflit avec le creneau demande (ou ses horaires par defaut). */
    private boolean estLibre(Cabine cabine, LocalDate date, Optional<LocalTime[]> creneau) {
        LocalTime debut = creneau.map(c -> c[0]).orElse(cabine.getHeureDisponibiliteDebut());
        LocalTime fin = creneau.map(c -> c[1]).orElse(cabine.getHeureDisponibiliteFin());
        // Une cabine dont le creneau demande deborde de ses horaires d'ouverture n'est jamais "disponible"
        // sur ce creneau, meme si aucune reservation n'existe (elle est simplement fermee a ce moment-la).
        if (creneau.isPresent() && horsHorairesOuverture(cabine, debut, fin)) {
            return false;
        }
        LocalDateTime dateDebut = LocalDateTime.of(date, debut);
        LocalDateTime dateFin = LocalDateTime.of(date, fin);
        return !reservationRepository.existsConflict(cabine.getId(), dateDebut, dateFin, StatutReservation.ANNULEE);
    }

    private boolean horsHorairesOuverture(Cabine cabine, LocalTime debut, LocalTime fin) {
        return debut.isBefore(cabine.getHeureDisponibiliteDebut()) || fin.isAfter(cabine.getHeureDisponibiliteFin());
    }

    private String decrirePeriode(Optional<LocalTime[]> creneau) {
        return creneau.map(c -> " entre " + c[0].format(FORMAT_HEURE) + " et " + c[1].format(FORMAT_HEURE)).orElse("");
    }

    private ChatbotResponse reponseCabinesReserveesADate(LocalDate date) {
        List<String> lignes = reservationRepository.findAllByOrderByDateDebutDesc().stream()
                .filter(r -> r.getStatut() != StatutReservation.ANNULEE)
                .filter(r -> r.getDateDebut().toLocalDate().equals(date))
                .sorted((a, b) -> a.getDateDebut().compareTo(b.getDateDebut()))
                .map(r -> "- " + r.getCabine().getNom() + " (" + r.getCabine().getNumero() + ") de "
                        + r.getDateDebut().format(FORMAT_HEURE) + " a " + r.getDateFin().format(FORMAT_HEURE))
                .toList();

        if (lignes.isEmpty()) {
            return new ChatbotResponse("Aucune reservation n'est enregistree le " + date.format(FORMAT_DATE) + ".");
        }

        return new ChatbotResponse("Reservations du " + date.format(FORMAT_DATE) + " :\n" + String.join("\n", lignes));
    }

    private ChatbotResponse reponseReservationsCabine(Cabine cabine, LocalDate date) {
        List<String> lignes = reservationRepository.findByCabineAndStatutNot(cabine, StatutReservation.ANNULEE)
                .stream()
                .filter(r -> r.getDateDebut().toLocalDate().equals(date))
                .sorted((a, b) -> a.getDateDebut().compareTo(b.getDateDebut()))
                .map(r -> "- de " + r.getDateDebut().format(FORMAT_HEURE) + " a " + r.getDateFin().format(FORMAT_HEURE))
                .toList();

        if (lignes.isEmpty()) {
            return new ChatbotResponse("La cabine " + cabine.getNom() + " (" + cabine.getNumero()
                    + ") n'a aucune reservation le " + date.format(FORMAT_DATE) + " : elle est entierement libre.");
        }

        return new ChatbotResponse("Reservations de la cabine " + cabine.getNom() + " (" + cabine.getNumero()
                + ") le " + date.format(FORMAT_DATE) + " :\n" + String.join("\n", lignes));
    }

    private ChatbotResponse reponseEquipementsCabine(Cabine cabine) {
        if (cabine.getEquipements().isEmpty()) {
            return new ChatbotResponse("La cabine " + cabine.getNom() + " (" + cabine.getNumero()
                    + ") n'a aucun equipement renseigne.");
        }
        String equipements = cabine.getEquipements().stream()
                .map(Equipement::getNom)
                .sorted()
                .collect(Collectors.joining(", "));
        return new ChatbotResponse("La cabine " + cabine.getNom() + " (" + cabine.getNumero()
                + ") dispose de : " + equipements + ".");
    }

    private ChatbotResponse reponseListeEquipements() {
        List<Equipement> equipements = equipementRepository.findAll();
        if (equipements.isEmpty()) {
            return new ChatbotResponse("Aucun equipement n'est encore enregistre sur la plateforme.");
        }
        String liste = equipements.stream().map(Equipement::getNom).sorted().collect(Collectors.joining(", "));
        return new ChatbotResponse("Les equipements disponibles sur la plateforme sont : " + liste + ".");
    }

    private ChatbotResponse reponseDetailCabine(Cabine cabine) {
        String equipements = cabine.getEquipements().isEmpty()
                ? "aucun equipement renseigne"
                : cabine.getEquipements().stream().map(Equipement::getNom).sorted().collect(Collectors.joining(", "));

        String etat = switch (cabine.getEtat()) {
            case MAINTENANCE -> "actuellement en maintenance";
            default -> "en service";
        };

        String texte = "Cabine " + cabine.getNom() + " (" + cabine.getNumero() + ") - " + etat + ". "
                + "Capacite : " + cabine.getCapacite() + " personne(s). "
                + "Localisation : " + cabine.getLocalisation() + ". "
                + "Horaires : " + cabine.getHeureDisponibiliteDebut().format(FORMAT_HEURE) + " a "
                + cabine.getHeureDisponibiliteFin().format(FORMAT_HEURE) + ". "
                + "Equipements : " + equipements + ".";
        return new ChatbotResponse(texte);
    }

    private ChatbotResponse reponseNombreCabines() {
        List<Cabine> toutes = cabineRepository.findAll();
        long disponibles = toutes.stream().filter(c -> c.getEtat() == EtatCabine.DISPONIBLE).count();
        long maintenance = toutes.stream().filter(c -> c.getEtat() == EtatCabine.MAINTENANCE).count();
        return new ChatbotResponse("La plateforme compte " + toutes.size() + " cabine(s) au total : "
                + disponibles + " en service et " + maintenance + " en maintenance.");
    }

    private ChatbotResponse reponseCabinesEnMaintenance() {
        List<String> noms = cabineRepository.findByEtat(EtatCabine.MAINTENANCE).stream()
                .map(c -> c.getNom() + " (" + c.getNumero() + ")")
                .toList();
        if (noms.isEmpty()) {
            return new ChatbotResponse("Aucune cabine n'est actuellement en maintenance.");
        }
        return new ChatbotResponse("Cabine(s) actuellement en maintenance : " + String.join(", ", noms) + ".");
    }

    private ChatbotResponse reponseRecommandation(int nombrePersonnes) {
        try {
            RecommendationResponse reco = recommendationService.recommander(nombrePersonnes, List.of(), null, null);
            return new ChatbotResponse("Pour " + nombrePersonnes + " personne(s), je recommande la cabine "
                    + reco.cabineNom() + " (score " + Math.round(reco.score()) + "%). " + reco.justification());
        } catch (BusinessException e) {
            return new ChatbotResponse("Aucune cabine disponible ne convient actuellement pour "
                    + nombrePersonnes + " personne(s).");
        }
    }

    // ---------------------------------------------------------------------
    // Analyse du message (mots-clés, date, créneau, cabine mentionnée)
    // ---------------------------------------------------------------------

    private boolean contientUn(String message, String... motsCles) {
        for (String mot : motsCles) {
            if (message.contains(mot)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Cherche une cabine mentionnee par son numero ou son nom, en verifiant les frontieres de
     * mot (\b...\b) : sans ca, un numero court comme "C5" pouvait matcher a tort un fragment
     * d'un autre mot ou d'une heure dans le message (faux positif de la V1).
     */
    private Optional<Cabine> trouverCabineMentionnee(String message) {
        return cabineRepository.findAll().stream()
                .filter(c -> contientMot(message, normaliser(c.getNumero()))
                        || contientMot(message, normaliser(c.getNom())))
                .findFirst();
    }

    private boolean contientMot(String message, String mot) {
        if (mot.isBlank()) {
            return false;
        }
        return Pattern.compile("\\b" + Pattern.quote(mot) + "\\b").matcher(message).find();
    }

    private Optional<LocalDate> extraireDate(String message) {
        if (contientUn(message, "aujourd hui", "auj")) {
            return Optional.of(LocalDate.now());
        }
        if (contientUn(message, "apres demain")) {
            return Optional.of(LocalDate.now().plusDays(2));
        }
        if (contientUn(message, "demain")) {
            return Optional.of(LocalDate.now().plusDays(1));
        }

        for (DayOfWeek jour : DayOfWeek.values()) {
            String nomJour = normaliser(jour.getDisplayName(TextStyle.FULL, Locale.FRENCH));
            if (contientMot(message, nomJour)) {
                LocalDate date = LocalDate.now();
                while (date.getDayOfWeek() != jour) {
                    date = date.plusDays(1);
                }
                return Optional.of(date);
            }
        }

        Matcher matcher = PATTERN_DATE_NUM.matcher(message);
        if (matcher.find()) {
            int jour = Integer.parseInt(matcher.group(1));
            int moisNum = Integer.parseInt(matcher.group(2));
            int annee = LocalDate.now().getYear();
            if (matcher.group(3) != null) {
                String brut = matcher.group(3);
                annee = brut.length() == 2 ? 2000 + Integer.parseInt(brut) : Integer.parseInt(brut);
            }
            try {
                return Optional.of(LocalDate.of(annee, moisNum, jour));
            } catch (Exception ignored) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    /** Extrait un creneau "de Xh a Yh" si deux heures valides sont mentionnees ; sinon vide. */
    private Optional<LocalTime[]> extraireCreneau(String message) {
        Matcher matcher = PATTERN_HEURE.matcher(message);
        LocalTime premiere = null;
        LocalTime seconde = null;
        while (matcher.find()) {
            int heure = Integer.parseInt(matcher.group(1));
            int minute = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
            if (heure > 23 || minute > 59) {
                continue;
            }
            LocalTime valeur = LocalTime.of(heure, minute);
            if (premiere == null) {
                premiere = valeur;
            } else {
                seconde = valeur;
                break;
            }
        }
        if (premiere != null && seconde != null && seconde.isAfter(premiere)) {
            return Optional.of(new LocalTime[]{premiere, seconde});
        }
        if (premiere != null) {
            // une seule heure mentionnee : on considere un creneau d'une heure a partir de celle-ci
            return Optional.of(new LocalTime[]{premiere, premiere.plusHours(1)});
        }
        return Optional.empty();
    }

    private Optional<Integer> extraireNombrePersonnes(String message) {
        Matcher matcher = PATTERN_NB_PERSONNES.matcher(message);
        if (matcher.find()) {
            try {
                return Optional.of(Integer.parseInt(matcher.group(1)));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /** Passe en minuscules et retire les accents, pour une comparaison robuste des mots-clés. */
    private String normaliser(String texte) {
        String sansAccents = Normalizer.normalize(texte.toLowerCase(Locale.FRENCH), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sansAccents.replace("'", " ");
    }
}
