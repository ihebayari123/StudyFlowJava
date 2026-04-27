package edu.connexion3a36.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * WhatsAppNotifier
 * ─────────────────
 * Utilitaire Java qui appelle l'API Python (StudyFlow WhatsApp Notifier)
 * pour notifier immédiatement les abonnés WhatsApp lorsqu'un nouveau quiz
 * est inséré en base de données.
 *
 * Utilisation (dans QuizService.addEntity) :
 *   WhatsAppNotifier.notifierNouveauQuiz(quizId);
 *
 * L'API Python doit tourner sur : http://localhost:8000
 * (lancée avec : uvicorn main:app --reload --port 8000)
 */
public class WhatsAppNotifier {

    /** URL de base de l'API Python (modifiable via variable d'environnement). */
    private static final String API_BASE =
        System.getenv("WHATSAPP_API_URL") != null
            ? System.getenv("WHATSAPP_API_URL")
            : "http://localhost:8000";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    /**
     * Notifie tous les abonnés WhatsApp qu'un nouveau quiz a été créé.
     *
     * @param quizId L'ID du quiz qui vient d'être inséré en BDD.
     */
    public static void notifierNouveauQuiz(int quizId) {
        // Appel asynchrone pour ne pas bloquer l'interface Java
        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/quiz/notify/" + quizId))
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

                HttpResponse<String> response = HTTP_CLIENT.send(
                    request, HttpResponse.BodyHandlers.ofString()
                );

                if (response.statusCode() == 200) {
                    System.out.println("[WhatsApp] ✅ Notification envoyée pour quiz #" + quizId);
                } else {
                    System.err.println("[WhatsApp] ⚠️ Réponse API : " + response.statusCode()
                        + " — " + response.body());
                }

            } catch (Exception e) {
                // Ne jamais bloquer Java si l'API Python est arrêtée
                System.err.println("[WhatsApp] ⚠️ API indisponible (quiz #" + quizId
                    + ") : " + e.getMessage());
            }
        }).start();
    }

    /**
     * Vérifie si l'API Python est accessible.
     *
     * @return true si l'API répond correctement.
     */
    public static boolean isApiAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/"))
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build();
            HttpResponse<String> response = HTTP_CLIENT.send(
                request, HttpResponse.BodyHandlers.ofString()
            );
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
