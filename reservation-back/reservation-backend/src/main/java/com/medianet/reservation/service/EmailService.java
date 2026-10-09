package com.medianet.reservation.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void envoyerOtp(String destinataire, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(destinataire);
            helper.setSubject("Medianet - Votre code de connexion");
            helper.setText(OtpEmailTemplate.genererHtml(code), true);
            mailSender.send(message);
        } catch (Exception e) {
            // On log l'erreur sans bloquer le flux (utile en environnement de dev sans SMTP réel)
            log.error("Erreur lors de l'envoi de l'email OTP à {} : {}", destinataire, e.getMessage());
        }
    }
}
