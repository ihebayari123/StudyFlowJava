package edu.connexion3a36.services;

import org.json.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;

public class ChatbotService {

    // ✅ Nouveau
    private static final String API_KEY = loadApiKey();

    private static String loadApiKey() {
        try {
            java.util.Properties props = new java.util.Properties();
            props.load(ChatbotService.class
                    .getClassLoader()
                    .getResourceAsStream("config.properties"));
            return props.getProperty("groq.api.key");
        } catch (Exception e) {
            throw new RuntimeException("Clé Groq introuvable dans config.properties");
        }
    }
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    public String envoyerMessage(String messageUtilisateur) {
        try {
            // System prompt StudyFlow
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content",
                    "Tu es l'assistant officiel de StudyFlow, une plateforme d'apprentissage en ligne. " +
                            "Tu peux répondre à ces types de questions : " +
                            "connexion, inscription, compte bloqué, navigation dans les cours, " +
                            "passer un quiz, modification du profil, rôles (étudiant, enseignant, admin). " +
                            "Si l'utilisateur parle de mot de passe oublié, réponds que cette fonctionnalité n'est pas encore disponible. " +
                            "Si la question ne concerne pas StudyFlow, réponds : Je suis spécialisé uniquement pour StudyFlow. " +
                            "Réponds toujours en français, de façon courte et claire. Maximum 3 phrases."
            );

            // Message utilisateur
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", messageUtilisateur);

            // Body complet
            JSONObject body = new JSONObject();
            body.put("model", "llama-3.1-8b-instant");
            body.put("max_tokens", 300);
            body.put("messages", new JSONArray()
                    .put(systemMsg)
                    .put(userMsg)
            );

            // Requête HTTP
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(
                            body.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString());

            // Parser la réponse
            JSONObject json = new JSONObject(response.body());
            return json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

        } catch (Exception e) {
            return "Erreur : " + e.getMessage();
        }
    }
}