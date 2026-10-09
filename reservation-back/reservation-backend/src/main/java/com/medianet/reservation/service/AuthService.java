package com.medianet.reservation.service;

import com.medianet.reservation.dto.AuthResponse;
import com.medianet.reservation.entity.Otp;
import com.medianet.reservation.entity.Utilisateur;
import com.medianet.reservation.entity.enums.Role;
import com.medianet.reservation.exception.BusinessException;
import com.medianet.reservation.repository.OtpRepository;
import com.medianet.reservation.repository.UtilisateurRepository;
import com.medianet.reservation.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final OtpRepository otpRepository;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.otp.validity-minutes}")
    private int otpValidityMinutes;

    @Value("${app.otp.max-attempts}")
    private int otpMaxAttempts;

    @Value("${app.otp.lock-minutes}")
    private int otpLockMinutes;

    @Value("${app.google.client-id:}")
    private String googleClientId;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Étape 1 : l'utilisateur saisit son email -> génération et envoi de l'OTP.
     * Si l'utilisateur n'existe pas encore, il est créé automatiquement en tant qu'EMPLOYE.
     * Le code n'est jamais stocké en clair : seul son hash BCrypt est persisté.
     */
    @Transactional
    public void demanderOtp(String email) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseGet(() -> creerUtilisateurParDefaut(email));

        verifierNonBloque(utilisateur);

        String code = genererCodeOtp();
        // TEMPORAIRE - a retirer avant toute mise en production : permet de tester
        // /api/auth/verify-otp sans dependre de la reception effective de l'email.
        log.debug("=== OTP genere pour {} : {} ===", email, code);
        String codeHash = passwordEncoder.encode(code);

        Otp otp = Otp.builder()
                .codeHash(codeHash)
                .dateExpiration(LocalDateTime.now().plusMinutes(otpValidityMinutes))
                .utilise(false)
                .tentatives(0)
                .utilisateur(utilisateur)
                .build();

        otpRepository.save(otp);
        // Le code en clair ne quitte jamais la mémoire du serveur, hormis via l'email envoyé au propriétaire du compte.
        emailService.envoyerOtp(email, code);
    }

    /**
     * Étape 2 : vérification du code OTP saisi -> génération du JWT.
     */
    @Transactional
    public AuthResponse verifierOtp(String email, String code) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable", HttpStatus.NOT_FOUND));

        verifierNonBloque(utilisateur);

        Otp otp = otpRepository.findTopByUtilisateurOrderByIdDesc(utilisateur)
                .orElseThrow(() -> new BusinessException("Aucun code OTP demandé pour cet utilisateur"));

        if (Boolean.TRUE.equals(otp.getUtilise())) {
            throw new BusinessException("Ce code OTP a déjà été utilisé");
        }

        if (otp.getDateExpiration().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Le code OTP a expiré, veuillez en redemander un");
        }

        if (!passwordEncoder.matches(code, otp.getCodeHash())) {
            otp.setTentatives(otp.getTentatives() + 1);

            if (otp.getTentatives() >= otpMaxAttempts) {
                // Blocage temporaire du compte après plusieurs tentatives erronées (6.1.2).
                utilisateur.setBloqueJusqua(LocalDateTime.now().plusMinutes(otpLockMinutes));
                utilisateurRepository.save(utilisateur);
                otpRepository.save(otp);
                throw new BusinessException(
                        "Nombre maximal de tentatives atteint, compte bloqué pendant " + otpLockMinutes + " minutes",
                        HttpStatus.TOO_MANY_REQUESTS);
            }

            otpRepository.save(otp);
            throw new BusinessException("Code OTP invalide");
        }

        otp.setUtilise(true);
        otpRepository.save(otp);

        String token = jwtUtil.generateToken(utilisateur.getEmail(), Map.of(
                "role", utilisateur.getRole().name(),
                "userId", utilisateur.getId()
        ));

        return AuthResponse.of(token, utilisateur.getId(), utilisateur.getEmail(), utilisateur.getRole().name());
    }

    /**
     * Connexion via Google Sign-In : le frontend obtient un idToken via Google
     * Identity Services, on le verifie aupres des serveurs Google (audience =
     * notre Client ID, email verifie), puis on cree/retrouve l'utilisateur et on
     * emet notre propre JWT exactement comme pour le flux OTP. L'authentification
     * Google remplace uniquement la saisie du code OTP, pas le reste du systeme.
     */
    @Transactional
    public AuthResponse connexionAvecGoogle(String idToken) {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new BusinessException("Connexion Google non configurée côté serveur (app.google.client-id manquant)");
        }

        Map<String, Object> infos = verifierTokenGoogle(idToken);

        Object audience = infos.get("aud");
        if (audience == null || !googleClientId.equals(audience.toString())) {
            throw new BusinessException("Token Google invalide (audience incorrecte)", HttpStatus.UNAUTHORIZED);
        }

        boolean emailVerifie = "true".equals(String.valueOf(infos.get("email_verified")));
        if (!emailVerifie) {
            throw new BusinessException("L'adresse email Google associée n'est pas vérifiée", HttpStatus.UNAUTHORIZED);
        }

        String email = String.valueOf(infos.get("email"));
        if (email == null || email.isBlank() || "null".equals(email)) {
            throw new BusinessException("Impossible de récupérer l'email depuis le compte Google", HttpStatus.UNAUTHORIZED);
        }

        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseGet(() -> creerUtilisateurParDefaut(email));

        verifierNonBloque(utilisateur);

        String token = jwtUtil.generateToken(utilisateur.getEmail(), Map.of(
                "role", utilisateur.getRole().name(),
                "userId", utilisateur.getId()
        ));

        return AuthResponse.of(token, utilisateur.getId(), utilisateur.getEmail(), utilisateur.getRole().name());
    }

    /**
     * Verifie le idToken aupres du endpoint officiel Google tokeninfo (simple,
     * sans dependance supplementaire). Google verifie deja la signature JWT et
     * l'expiration ; on verifie nous-memes l'audience (notre Client ID) et que
     * l'email est confirme.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> verifierTokenGoogle(String idToken) {
        try {
            Map<String, Object> reponse = WebClient.create()
                    .get()
                    .uri("https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            if (reponse == null) {
                throw new BusinessException("Réponse vide de Google lors de la vérification du token", HttpStatus.UNAUTHORIZED);
            }
            return reponse;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Token Google invalide ou expiré", HttpStatus.UNAUTHORIZED);
        }
    }

    private void verifierNonBloque(Utilisateur utilisateur) {
        LocalDateTime bloqueJusqua = utilisateur.getBloqueJusqua();
        if (bloqueJusqua != null) {
            if (bloqueJusqua.isAfter(LocalDateTime.now())) {
                throw new BusinessException(
                        "Compte temporairement bloqué suite à plusieurs échecs, réessayez après " + bloqueJusqua,
                        HttpStatus.TOO_MANY_REQUESTS);
            } else {
                // La période de blocage est passée : on la réinitialise silencieusement.
                utilisateur.setBloqueJusqua(null);
                utilisateurRepository.save(utilisateur);
            }
        }
    }

    private Utilisateur creerUtilisateurParDefaut(String email) {
        Utilisateur nouveau = Utilisateur.builder()
                .email(email)
                .nom("")
                .prenom("")
                .role(Role.EMPLOYE)
                .build();
        return utilisateurRepository.save(nouveau);
    }

    private String genererCodeOtp() {
        int code = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(code);
    }
}
