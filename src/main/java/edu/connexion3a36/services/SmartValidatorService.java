package edu.connexion3a36.services;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * SmartValidatorService
 * ──────────────────────
 * Valide SÉMANTIQUEMENT les réponses texte libre via Ollama/Mistral.
 *
 * Même logique que le Symfony SmartValidatorService :
 *   question + bonne_réponse + réponse_étudiant
 *   → { isCorrect, confidence, explanation }
 *
 * Pas de dépendance externe (utilise java.net natif).
 * Fallback automatique si Ollama est indisponible.
 */
public class SmartValidatorService {

    private static final String OLLAMA_URL = "http://localhost:11434/api/chat";
    private static final String MODEL      = "mistral";
    private static final int    TIMEOUT_MS = 30_000;

    // ── Résultat ──────────────────────────────────────────────────────────────

    public record ValidationResult(
            boolean isCorrect,
            int     confidence,
            String  explanation
    ) {}

    // ── Point d'entrée ────────────────────────────────────────────────────────

    public ValidationResult valider(String questionTexte,
                                    String reponseAttendue,
                                    String reponseEtudiant) {

        if (reponseEtudiant == null || reponseEtudiant.isBlank())
            return new ValidationResult(false, 100, "Aucune réponse fournie.");

        // Correspondance exacte → pas besoin d'Ollama
        if (normaliser(reponseEtudiant).equals(normaliser(reponseAttendue)))
            return new ValidationResult(true, 100, "Réponse exacte.");

        try {
            return appellerOllama(questionTexte, reponseAttendue, reponseEtudiant);
        } catch (Exception e) {
            System.err.println("[SmartValidator] Ollama indisponible : " + e.getMessage());
            return fallbackLocal(reponseAttendue, reponseEtudiant);
        }
    }

    // ── Appel HTTP Ollama ─────────────────────────────────────────────────────

    private ValidationResult appellerOllama(String question,
                                            String attendue,
                                            String etudiant) throws Exception {
        String prompt = """
            Tu es un correcteur pédagogique juste et précis.
            
            Question         : %s
            Réponse attendue : %s
            Réponse étudiant : %s
            
            La réponse est correcte si elle exprime la même idée,
            même avec des mots différents.
            
            Réponds UNIQUEMENT avec ce JSON (sans markdown) :
            {"isCorrect": true, "confidence": 85, "explanation": "..."}
            """.formatted(question, attendue, etudiant);

        // Body JSON construit manuellement (sans org.json)
        String body = """
            {
              "model": "%s",
              "stream": false,
              "messages": [{"role": "user", "content": %s}]
            }
            """.formatted(MODEL, jsonString(prompt));

        HttpURLConnection conn = (HttpURLConnection)
                new URL(OLLAMA_URL).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        if (conn.getResponseCode() != 200)
            throw new IOException("HTTP " + conn.getResponseCode());

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }

        return parseResult(sb.toString());
    }

    // ── Parser la réponse Ollama ──────────────────────────────────────────────

    private ValidationResult parseResult(String ollamaJson) {
        try {
            // Extraire le contenu du message
            String content = extraireValeur(ollamaJson, "content");
            if (content == null) throw new Exception("content null");

            // Nettoyer markdown
            content = content.replaceAll("```json|```", "").trim();

            // Extraire le JSON intégré
            int start = content.indexOf('{');
            int end   = content.lastIndexOf('}');
            if (start == -1 || end == -1) throw new Exception("JSON introuvable");
            String json = content.substring(start, end + 1);

            boolean isCorrect  = parseBoolean(json, "isCorrect");
            int     confidence = parseInt(json, "confidence", 70);
            String  expl       = extraireValeur(json, "explanation");
            if (expl == null) expl = "Analyse sémantique effectuée.";

            return new ValidationResult(isCorrect, confidence, expl);

        } catch (Exception e) {
            System.err.println("[SmartValidator] Parsing échoué : " + e.getMessage());
            return new ValidationResult(false, 50, "Analyse IA indisponible.");
        }
    }

    // ── Fallback local ────────────────────────────────────────────────────────

    private ValidationResult fallbackLocal(String attendue, String etudiant) {
        String a = normaliser(attendue);
        String e = normaliser(etudiant);

        if (a.contains(e) || e.contains(a))
            return new ValidationResult(true, 80,
                    "Correspondance partielle (Mistral indisponible).");

        String[] mots = a.split("\\s+");
        long matches = 0;
        for (String m : mots)
            if (m.length() > 3 && e.contains(m)) matches++;

        boolean ok = mots.length > 0 && (double) matches / mots.length >= 0.4;
        return new ValidationResult(ok, 65,
                ok ? "Correspondance par mots-clés (Mistral indisponible)."
                        : "Réponse incorrecte (Mistral indisponible).");
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private String normaliser(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase()
                .replaceAll("[àáâã]","a").replaceAll("[éèêë]","e")
                .replaceAll("[îï]","i").replaceAll("[ôö]","o")
                .replaceAll("[ùûü]","u").replaceAll("[ç]","c")
                .replaceAll("[^a-z0-9\\s]"," ").replaceAll("\\s+"," ").trim();
    }

    /** Encode une String en valeur JSON (avec guillemets et échappements). */
    private String jsonString(String s) {
        return "\"" + s.replace("\\","\\\\")
                .replace("\"","\\\"")
                .replace("\n","\\n")
                .replace("\r","") + "\"";
    }

    /** Extrait la valeur d'une clé simple dans un JSON plat. */
    private String extraireValeur(String json, String key) {
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx == -1) return null;
        int colon = json.indexOf(':', idx + pattern.length());
        if (colon == -1) return null;
        int start = json.indexOf('"', colon + 1);
        if (start == -1) return null;
        int end = json.indexOf('"', start + 1);
        while (end > 0 && json.charAt(end - 1) == '\\')
            end = json.indexOf('"', end + 1);
        if (end == -1) return null;
        return json.substring(start + 1, end).replace("\\\"","\"").replace("\\n","\n");
    }

    private boolean parseBoolean(String json, String key) {
        String p = "\"" + key + "\"";
        int idx = json.indexOf(p);
        if (idx == -1) return false;
        String after = json.substring(json.indexOf(':', idx) + 1).trim();
        return after.startsWith("true");
    }

    private int parseInt(String json, String key, int def) {
        try {
            String p = "\"" + key + "\"";
            int idx = json.indexOf(p);
            if (idx == -1) return def;
            String after = json.substring(json.indexOf(':', idx) + 1).trim();
            StringBuilder nb = new StringBuilder();
            for (char c : after.toCharArray()) {
                if (Character.isDigit(c)) nb.append(c);
                else if (!nb.isEmpty()) break;
            }
            return nb.isEmpty() ? def : Integer.parseInt(nb.toString());
        } catch (Exception e) { return def; }
    }
}