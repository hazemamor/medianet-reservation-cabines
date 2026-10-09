package com.medianet.reservation.scheduler;

import com.medianet.reservation.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Purge RGPD : les codes OTP (même hachés) n'ont aucune utilité au-delà de leur
 * fenêtre de validité + une courte marge de sécurité. Ils sont donc supprimés
 * définitivement de la base après app.otp.retention-hours, conformément au
 * principe de minimisation et de limitation de la conservation des données.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PurgeOtpScheduler {

    private final OtpRepository otpRepository;

    @Value("${app.otp.retention-hours}")
    private int retentionHours;

    @Scheduled(cron = "${app.otp.purge-cron}")
    @Transactional
    public void purgerOtpExpires() {
        LocalDateTime seuil = LocalDateTime.now().minusHours(retentionHours);
        otpRepository.deleteByDateExpirationBefore(seuil);
        log.info("Purge RGPD des OTP expirés antérieurs à {} effectuée", seuil);
    }
}
