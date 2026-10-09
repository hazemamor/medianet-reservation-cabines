package com.medianet.reservation.service;

/**
 * Génère le corps HTML de l'email OTP. Le template correspond à la maquette fournie par le
 * client (charte Medianet : orange #f5a623 / cyan #00b8d4), sans le bandeau "client mail"
 * (avatar/expéditeur/heure) qui n'est qu'une mise en situation visuelle et non un élément
 * à envoyer réellement dans le corps du message.
 */
final class OtpEmailTemplate {

    private OtpEmailTemplate() {
    }

    static String genererHtml(String code) {
        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Code de connexion - Medianet</title>
                </head>
                <body style="margin:0;padding:0;background-color:#edeff3;font-family:'Segoe UI',Arial,sans-serif;-webkit-font-smoothing:antialiased;">
                  <div style="width:100%%;padding:40px 0;background-color:#edeff3;">
                    <div style="max-width:600px;margin:0 auto;">
                      <div style="background-color:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.06);">

                        <div style="display:flex;height:6px;">
                          <div style="flex:1;background-color:#f5a623;"></div>
                          <div style="flex:1;background-color:#00b8d4;"></div>
                        </div>

                        <div style="padding:40px 48px;">

                          <div style="text-align:center;margin-bottom:32px;">
                            <div style="font-size:28px;font-weight:800;letter-spacing:4px;">
                              <span style="color:#f5a623;">MEDIA</span><span style="color:#00b8d4;">NET</span>
                            </div>
                          </div>

                          <p style="font-size:18px;font-weight:700;color:#1f2937;margin:0 0 16px 0;">Bonjour,</p>
                          <p style="font-size:15px;color:#4b5563;line-height:1.6;margin:0 0 32px 0;">
                            Voici votre code de connexion à la plateforme de réservation des cabines :
                          </p>

                          <div style="background-color:#f0fdfa;border-radius:16px;padding:32px;text-align:center;margin-bottom:32px;border:1px solid #e0f2fe;">
                            <div style="display:inline-flex;align-items:center;gap:6px;background-color:#e0f2fe;border-radius:20px;padding:4px 14px;margin-bottom:16px;">
                              <span style="font-size:11px;font-weight:700;color:#0369a1;text-transform:uppercase;letter-spacing:0.5px;">&#128274; Code s&eacute;curis&eacute;</span>
                            </div>
                            <div style="font-family:'Courier New',monospace;font-size:42px;font-weight:700;color:#1f2937;letter-spacing:6px;margin:16px 0;">%s</div>
                            <p style="font-size:13px;font-weight:500;color:#0f766e;margin:0;">Ce code est valable 5 minutes et &agrave; usage unique.</p>
                          </div>

                          <p style="font-size:14px;color:#4b5563;margin:0 0 6px 0;">Cordialement,</p>
                          <p style="font-size:15px;font-weight:700;color:#1f2937;margin:0;">L'&eacute;quipe Medianet</p>

                        </div>

                        <div style="background-color:#f9fafb;padding:24px 48px;text-align:center;border-top:1px solid #e5e7eb;">
                          <p style="font-size:11px;color:#9ca3af;line-height:1.6;margin:0;">
                            &copy; 2026 Medianet Tunisie. Tous droits r&eacute;serv&eacute;s.<br>
                            Vous recevez ce message automatique car une tentative de connexion a &eacute;t&eacute; initi&eacute;e.
                          </p>
                        </div>

                      </div>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(code);
    }
}
