package edu.connexion3a36.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

public class OtpService {

    // ═══════════════════════════════════════
    // STOCKAGE OTP EN MÉMOIRE
    // ═══════════════════════════════════════
    private static final Map<String, String>        otpStore      = new HashMap<>();
    private static final Map<String, LocalDateTime> expiryStore   = new HashMap<>();
    private static final int                        OTP_EXPIRY_MIN = 5;

    // ═══════════════════════════════════════
    // CHARGER CONFIG
    // ═══════════════════════════════════════
    private static String API_KEY;
    private static String SECRET_KEY;

    static {
        try (InputStream input = OtpService.class
                .getResourceAsStream("/config.properties")) {
            Properties props = new Properties();
            props.load(input);
            API_KEY    = props.getProperty("MAILJET_API_KEY");
            SECRET_KEY = props.getProperty("MAILJET_SECRET_KEY");
        } catch (IOException e) {
            System.err.println("❌ config.properties introuvable : " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════
    // GÉNÉRER OTP 6 CHIFFRES
    // ═══════════════════════════════════════
    public static String generateOtp() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    // ═══════════════════════════════════════
    // ENVOYER OTP PAR EMAIL
    // ═══════════════════════════════════════
    public static boolean sendOtp(String email, String prenom) {
        String otp = generateOtp();

        // Stocker OTP + expiration
        otpStore.put(email, otp);
        expiryStore.put(email, LocalDateTime.now().plusMinutes(OTP_EXPIRY_MIN));

        // Corps de l'email HTML
        String htmlBody = "<div style='font-family: Arial, sans-serif; max-width: 500px; margin: auto;'>"
                + "<div style='background: #2979FF; padding: 24px; border-radius: 12px 12px 0 0;'>"
                + "<h2 style='color: white; margin: 0;'>🛡️ StudyFlow</h2>"
                + "</div>"
                + "<div style='background: #F5F6FA; padding: 32px; border-radius: 0 0 12px 12px;'>"
                + "<p style='font-size: 16px;'>Bonjour <b>" + prenom + "</b>,</p>"
                + "<p>Voici votre code de vérification :</p>"
                + "<div style='background: white; border-radius: 12px; padding: 24px; "
                + "text-align: center; border: 2px solid #2979FF; margin: 24px 0;'>"
                + "<span style='font-size: 36px; font-weight: bold; "
                + "letter-spacing: 12px; color: #2979FF;'>" + otp + "</span>"
                + "</div>"
                + "<p style='color: #9E9E9E; font-size: 13px;'>"
                + "⏱️ Ce code expire dans <b>5 minutes</b>.</p>"
                + "<p style='color: #9E9E9E; font-size: 13px;'>"
                + "Si vous n'avez pas demandé ce code, ignorez cet email.</p>"
                + "</div></div>";

        // Payload JSON Mailjet
        String payload = "{"
                + "\"Messages\": [{"
                + "\"From\": {\"Email\": \"agrebi.98.oussema@gmail.com\", \"Name\": \"StudyFlow\"},"
                + "\"To\": [{\"Email\": \"" + email + "\", \"Name\": \"" + prenom + "\"}],"
                + "\"Subject\": \"🔐 Votre code de vérification StudyFlow\","
                + "\"HTMLPart\": \"" + htmlBody.replace("\"", "\\\"") + "\""
                + "}]}";

        try {
            // Authentification Base64
            String credentials = Base64.getEncoder()
                    .encodeToString((API_KEY + ":" + SECRET_KEY).getBytes());

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mailjet.com/v3.1/send"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Basic " + credentials)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            System.out.println("Mailjet response : " + response.statusCode());
            return response.statusCode() == 200;

        } catch (Exception e) {
            System.err.println("❌ Erreur envoi email : " + e.getMessage());
            return false;
        }
    }

    // ═══════════════════════════════════════
    // VÉRIFIER OTP
    // ═══════════════════════════════════════
    public static OtpResult verifyOtp(String email, String codeEntre) {
        String otpStocke   = otpStore.get(email);
        LocalDateTime expiry = expiryStore.get(email);

        if (otpStocke == null) {
            return OtpResult.NO_OTP;
        }
        if (LocalDateTime.now().isAfter(expiry)) {
            otpStore.remove(email);
            expiryStore.remove(email);
            return OtpResult.EXPIRED;
        }
        if (!otpStocke.equals(codeEntre)) {
            return OtpResult.WRONG_CODE;
        }

        // ✅ Correct — nettoyer
        otpStore.remove(email);
        expiryStore.remove(email);
        return OtpResult.SUCCESS;
    }

    // ═══════════════════════════════════════
    // ENUM RÉSULTAT
    // ═══════════════════════════════════════
    public enum OtpResult {
        SUCCESS,
        WRONG_CODE,
        EXPIRED,
        NO_OTP
    }
}