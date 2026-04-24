package edu.connexion3a36.services;

import edu.connexion3a36.models.Event;
import java.net.URI;
import java.net.http.*;
import java.util.List;
import java.util.function.Consumer;
import org.json.*;

public class RecommendationService {

    private static final String API_KEY = "AIzaSyDU6HfltaHabPIF_0gUrzdsxNE02jRLT6U";
    private static final String URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + API_KEY;

    // Surcharge sans callback (appelée depuis d'autres endroits)
    public String recommander(List<Event> events) throws Exception {
        return recommander(events, null);
    }

    // Méthode principale avec callback de statut
    public String recommander(List<Event> events, Consumer<String> statusCallback) throws Exception {
        try {
            return appellerGemini(events, statusCallback);
        } catch (Exception e) {
            // Si l'API échoue, générer localement
            if (statusCallback != null) {
                statusCallback.accept("⚠️ API indisponible, génération locale...");
            }
            return genererLocalement(events);
        }
    }

    private String appellerGemini(List<Event> events, Consumer<String> statusCallback) throws Exception {
        if (statusCallback != null) statusCallback.accept("⏳ Connexion à Gemini AI...");

        StringBuilder prompt = new StringBuilder();
        prompt.append("Voici des événements disponibles :\n\n");
        for (Event e : events) {
            prompt.append("- ").append(e.getTitre())
                    .append(" (").append(e.getType()).append(") : ")
                    .append(e.getDescription()).append("\n");
        }
        prompt.append("\nRecommande les 2 meilleurs en expliquant pourquoi. Réponds en français.");

        JSONObject body = new JSONObject();
        body.put("contents", new JSONArray()
                .put(new JSONObject()
                        .put("parts", new JSONArray()
                                .put(new JSONObject()
                                        .put("text", prompt.toString())))));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Réponse Gemini : " + response.body());

        JSONObject json = new JSONObject(response.body());
        if (json.has("error")) {
            throw new Exception(json.getJSONObject("error").getString("message"));
        }

        return json.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text");
    }

    private String genererLocalement(List<Event> events) {
        StringBuilder result = new StringBuilder();
        result.append("🤖 Analyse IA des événements\n");
        result.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        int count = Math.min(2, events.size());
        result.append("✨ Top ").append(count).append(" événements recommandés :\n\n");

        for (int i = 0; i < count; i++) {
            Event e = events.get(i);
            result.append("🏆 ").append(i + 1).append(". ").append(e.getTitre()).append("\n");
            result.append("   📌 Catégorie : ").append(e.getType()).append("\n");
            result.append("   📝 ").append(e.getDescription()).append("\n");
            result.append("   💡 Recommandé car cet événement offre une expérience")
                    .append(" enrichissante dans le domaine ").append(e.getType()).append(".\n\n");
        }

        result.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        result.append("📊 Analyse basée sur ").append(events.size()).append(" événements.");
        return result.toString();
    }
}