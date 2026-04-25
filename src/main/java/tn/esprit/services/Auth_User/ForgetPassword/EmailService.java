package tn.esprit.services.Auth_User.ForgetPassword;

import tn.esprit.config.EmailConfig;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class EmailService {

    /**
     * Send the password reset email.
     * Contains the token embedded directly — user pastes it in the app.
     *
     * Why token not link: JavaFX is a desktop app, not a web app.
     * There is no URL to click. Instead we send the token and the user
     * copies it into the reset form in the app — same security level.
     */
    private Session buildMailSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth",                "true");
        props.put("mail.smtp.starttls.enable",     "true");
        props.put("mail.smtp.starttls.required",   "true");  // ← add this
        props.put("mail.smtp.host",                EmailConfig.SMTP_HOST);
        props.put("mail.smtp.port",                String.valueOf(EmailConfig.SMTP_PORT));
        props.put("mail.smtp.ssl.trust",           EmailConfig.SMTP_HOST);
        props.put("mail.smtp.ssl.protocols",       "TLSv1.2"); // ← add this
        props.put("mail.debug",                    "false");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                        EmailConfig.FROM_EMAIL,
                        EmailConfig.APP_PASSWORD
                );
            }
        });
    }
    public void sendPasswordResetEmail(String toEmail,
                                       String username,
                                       String token,
                                       int expiryMinutes) throws Exception {

        Session session = buildMailSession();

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(
                EmailConfig.FROM_EMAIL, EmailConfig.FROM_NAME));
        message.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(toEmail)
        );
        message.setSubject("🔑 Réinitialisation de votre mot de passe EcoTrip");

        // HTML email body — clean, matches EcoTrip branding
        String html = buildEmailHtml(username, token, expiryMinutes);

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(html, "text/html; charset=UTF-8");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(htmlPart);
        message.setContent(multipart);

        Transport.send(message);
    }

    private String buildEmailHtml(String username,
                                  String token,
                                  int expiryMinutes) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <style>
                body { font-family: 'Segoe UI', Arial, sans-serif;
                       background: #f8fafc; margin: 0; padding: 0; }
                .wrap { max-width: 480px; margin: 40px auto;
                        background: white; border-radius: 16px;
                        box-shadow: 0 4px 24px rgba(0,0,0,0.08);
                        overflow: hidden; }
                .header { background: linear-gradient(135deg, #0d3d18, #1a5f2a);
                          padding: 32px 40px; text-align: center; }
                .header h1 { color: white; font-size: 22px;
                             margin: 0 0 6px 0; }
                .header p  { color: rgba(255,255,255,0.8);
                             font-size: 14px; margin: 0; }
                .body { padding: 36px 40px; }
                .greeting { font-size: 16px; color: #0f172a;
                            margin-bottom: 16px; }
                .msg { font-size: 14px; color: #475569;
                       line-height: 1.6; margin-bottom: 28px; }
                .token-box { background: #f0fdf4;
                             border: 2px dashed #22c55e;
                             border-radius: 12px;
                             padding: 20px;
                             text-align: center;
                             margin-bottom: 24px; }
                .token-label { font-size: 12px; color: #64748b;
                               font-weight: 600; letter-spacing: 0.05em;
                               margin-bottom: 10px; }
                .token-value { font-size: 15px; font-weight: bold;
                               color: #0d3d18; letter-spacing: 2px;
                               word-break: break-all; }
                .expiry { font-size: 13px; color: #f59e0b;
                          text-align: center; margin-bottom: 24px; }
                .steps { background: #f8fafc; border-radius: 10px;
                         padding: 16px 20px; margin-bottom: 24px; }
                .steps p { font-size: 13px; color: #374151;
                           margin: 4px 0; }
                .footer { text-align: center; padding: 20px 40px;
                          border-top: 1px solid #f1f5f9;
                          font-size: 12px; color: #94a3b8; }
              </style>
            </head>
            <body>
              <div class="wrap">
                <div class="header">
                  <h1>🌿 EcoTrip</h1>
                  <p>Réinitialisation du mot de passe</p>
                </div>
                <div class="body">
                  <p class="greeting">Bonjour <strong>%s</strong>,</p>
                  <p class="msg">
                    Vous avez demandé à réinitialiser votre mot de passe.
                    Copiez le code ci-dessous et collez-le dans l'application EcoTrip.
                  </p>

                  <div class="token-box">
                    <div class="token-label">VOTRE CODE DE RÉINITIALISATION</div>
                    <div class="token-value">%s</div>
                  </div>

                  <p class="expiry">⏰ Ce code expire dans <strong>%d minutes</strong>.</p>

                  <div class="steps">
                    <p>📱 <strong>Comment l'utiliser :</strong></p>
                    <p>1. Ouvrez l'application EcoTrip</p>
                    <p>2. Sur la page de connexion → "Mot de passe oublié ?"</p>
                    <p>3. Entrez votre email et collez ce code</p>
                    <p>4. Choisissez votre nouveau mot de passe</p>
                  </div>

                  <p class="msg" style="font-size:13px; color:#94a3b8;">
                    Si vous n'avez pas demandé cette réinitialisation,
                    ignorez cet email. Votre mot de passe restera inchangé.
                  </p>
                </div>
                <div class="footer">
                  © 2025 EcoTrip — Cet email a été envoyé automatiquement.
                </div>
              </div>
            </body>
            </html>
            """.formatted(username, token, expiryMinutes);
    }
}