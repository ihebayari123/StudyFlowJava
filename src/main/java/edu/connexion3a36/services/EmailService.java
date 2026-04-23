package edu.connexion3a36.services;

import edu.connexion3a36.entities.CartItem;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class EmailService {

    private static final String FROM_EMAIL;
    private static final String FROM_PASSWORD;

    static {
        try (InputStream input = EmailService.class
                .getClassLoader()
                .getResourceAsStream("config.properties")) {
            Properties props = new Properties();
            props.load(input);
            FROM_EMAIL    = props.getProperty("gmail.email");
            FROM_PASSWORD = props.getProperty("gmail.password");
        } catch (Exception e) {
            throw new RuntimeException("❌ Impossible de charger config.properties : " + e.getMessage());
        }
    }
    public static void envoyerConfirmation(String toEmail, String nomClient,
                                           List<CartItem> items, double total) {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            "smtp.gmail.com");
        props.put("mail.smtp.port",            "587");
        props.put("mail.smtp.ssl.trust",       "smtp.gmail.com");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, FROM_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL, "StudyFlow Shop"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("✅ Confirmation de votre commande — StudyFlow");
            message.setContent(buildEmailHtml(nomClient, items, total), "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("✅ Email envoyé à : " + toEmail);

        } catch (Exception e) {
            System.err.println("❌ Erreur envoi email : " + e.getMessage());
        }
    }

    private static String buildEmailHtml(String nomClient,
                                         List<CartItem> items, double total) {
        StringBuilder rows = new StringBuilder();
        for (CartItem item : items) {
            rows.append("""
                <tr>
                  <td style="padding:10px 16px; border-bottom:1px solid #f0f0f0;">%s</td>
                  <td style="padding:10px 16px; border-bottom:1px solid #f0f0f0;
                             text-align:center;">%d</td>
                  <td style="padding:10px 16px; border-bottom:1px solid #f0f0f0;
                             text-align:right; color:#2979FF; font-weight:bold;">
                             %.2f DT</td>
                </tr>
            """.formatted(
                    item.getProduit().getNom(),
                    item.getQuantite(),
                    item.getSousTotal()
            ));
        }

        return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0; padding:0; background:#f5f5f5;
                         font-family: 'Segoe UI', sans-serif;">

              <table width="100%%" cellpadding="0" cellspacing="0">
                <tr>
                  <td align="center" style="padding: 40px 20px;">

                    <table width="560" cellpadding="0" cellspacing="0"
                           style="background:white; border-radius:20px;
                                  box-shadow:0 4px 20px rgba(0,0,0,0.08);">

                      <!-- HEADER -->
                      <tr>
                        <td style="background: linear-gradient(135deg, #635BFF, #2979FF);
                                   border-radius:20px 20px 0 0; padding:36px 40px;
                                   text-align:center;">
                          <h1 style="color:white; margin:0; font-size:28px;">
                            🎓 StudyFlow
                          </h1>
                          <p style="color:rgba(255,255,255,0.85); margin:8px 0 0 0;
                                    font-size:15px;">
                            Confirmation de commande
                          </p>
                        </td>
                      </tr>

                      <!-- BODY -->
                      <tr>
                        <td style="padding: 36px 40px;">

                          <p style="font-size:16px; color:#333; margin:0 0 8px 0;">
                            Bonjour <strong>%s</strong> 👋
                          </p>
                          <p style="font-size:14px; color:#666; margin:0 0 28px 0;">
                            Merci pour votre achat ! Voici le récapitulatif de votre commande.
                          </p>

                          <!-- TABLE PRODUITS -->
                          <table width="100%%" cellpadding="0" cellspacing="0"
                                 style="border:1px solid #f0f0f0; border-radius:12px;
                                        overflow:hidden;">
                            <thead>
                              <tr style="background:#f8f8f8;">
                                <th style="padding:12px 16px; text-align:left;
                                           font-size:12px; color:#888;
                                           text-transform:uppercase;">Produit</th>
                                <th style="padding:12px 16px; text-align:center;
                                           font-size:12px; color:#888;
                                           text-transform:uppercase;">Qté</th>
                                <th style="padding:12px 16px; text-align:right;
                                           font-size:12px; color:#888;
                                           text-transform:uppercase;">Prix</th>
                              </tr>
                            </thead>
                            <tbody>
                              %s
                            </tbody>
                          </table>

                          <!-- TOTAL -->
                          <table width="100%%" cellpadding="0" cellspacing="0"
                                 style="margin-top:16px;">
                            <tr>
                              <td style="text-align:right; padding:16px;
                                         background:#f0f4ff; border-radius:12px;">
                                <span style="font-size:14px; color:#666;">Total payé : </span>
                                <span style="font-size:22px; font-weight:bold;
                                             color:#2979FF;">%.2f DT</span>
                              </td>
                            </tr>
                          </table>

                          <p style="font-size:13px; color:#aaa; margin:28px 0 0 0;
                                    text-align:center;">
                            🔒 Paiement sécurisé via Stripe
                          </p>
                        </td>
                      </tr>

                      <!-- FOOTER -->
                      <tr>
                        <td style="background:#f8f8f8; border-radius:0 0 20px 20px;
                                   padding:20px 40px; text-align:center;">
                          <p style="font-size:12px; color:#aaa; margin:0;">
                            © 2025 StudyFlow — Tous droits réservés
                          </p>
                        </td>
                      </tr>

                    </table>
                  </td>
                </tr>
              </table>

            </body>
            </html>
        """.formatted(nomClient, rows.toString(), total);
    }
}