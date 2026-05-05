package edu.connexion3a36.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;


public class AnthropicAIService {

    private static final String API_URL    = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL      = "llama-3.3-70b-versatile";
    private static final int    MAX_TOKENS = 1024;
    private static final int    TIMEOUT    = 30;

    private final String     apiKey;
    private final HttpClient http;

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static AnthropicAIService instance;

    public static AnthropicAIService getInstance() {
        if (instance == null) instance = new AnthropicAIService();
        return instance;
    }

    private AnthropicAIService() {
        // Accept any of these variable names — whichever the user set
        String k = "";
        if (k == null || k.isBlank()) k = System.getenv("ANTHROPIC_API_KEY");
        if (k == null || k.isBlank()) k = System.getProperty("GROQ_API_KEY", "");
        if (k == null || k.isBlank()) k = System.getProperty("ANTHROPIC_API_KEY", "");
        this.apiKey = (k != null) ? k : "";
        this.http   = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT))
                .build();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FEATURE 1 — Course description + learning objectives
    // ═════════════════════════════════════════════════════════════════════════

    public CourseDescriptionResult generateCourseDescription(String courseTitle,
                                                             String targetAudience) {
        String audienceLine = (targetAudience != null && !targetAudience.isBlank())
                ? "\nPublic cible : " + targetAudience : "";

        String prompt =
                "Titre du cours : \"" + courseTitle + "\"" + audienceLine + "\n\n" +
                        "Génère une description professionnelle du cours et 5 objectifs d'apprentissage clairs.\n" +
                        "Réponds EXACTEMENT dans ce format (sans markdown, sans astérisques) :\n\n" +
                        "DESCRIPTION:\n" +
                        "[2-3 paragraphes de description engageante]\n\n" +
                        "OBJECTIFS:\n" +
                        "- [objectif 1 commençant par un verbe d'action]\n" +
                        "- [objectif 2]\n" +
                        "- [objectif 3]\n" +
                        "- [objectif 4]\n" +
                        "- [objectif 5]";

        String system =
                "Tu es un expert en ingénierie pédagogique. " +
                        "Écris des descriptions de cours concises, engageantes et motivantes pour les apprenants. " +
                        "Sois spécifique et pratique. Réponds toujours en français.";

        String raw = callApi(system, prompt);
        return parseCourseDescription(raw);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FEATURE 2 — Course outline (chapter list)
    // ═════════════════════════════════════════════════════════════════════════

    public List<ChapterSuggestion> generateCourseOutline(String topic, int chapterCount) {
        String prompt =
                "Génère exactement " + chapterCount + " chapitres pour un cours sur : \"" + topic + "\"\n\n" +
                        "Réponds UNIQUEMENT avec un tableau JSON valide, sans markdown ni explication :\n" +
                        "[{\"titre\":\"...\",\"description\":\"courte description en 1 phrase\"}]";

        String system =
                "Tu es un expert en conception pédagogique. " +
                        "Génère uniquement un tableau JSON valide avec les clés \"titre\" et \"description\". " +
                        "Aucun markdown, aucun texte avant ou après le JSON. Réponds en français.";

        String raw = callApi(system, prompt);
        return parseCourseOutline(raw);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FEATURE 3 — Chapter difficulty tagger
    // ═════════════════════════════════════════════════════════════════════════

    public DifficultyResult analyzeChapterDifficulty(String chapterTitle,
                                                     String chapterContent) {
        String prompt =
                "Analyse ce chapitre et suggère un niveau de difficulté.\n\n" +
                        "Titre : " + chapterTitle + "\n" +
                        "Contenu : " + chapterContent + "\n\n" +
                        "Réponds UNIQUEMENT avec du JSON valide (sans markdown) :\n" +
                        "{\"niveau\":\"Débutant|Intermédiaire|Avancé\"," +
                        "\"confiance\":\"Haute|Moyenne|Faible\"," +
                        "\"justification\":\"une phrase d'explication\"}";

        String system =
                "Tu es un expert en analyse de curriculum. " +
                        "Évalue la difficulté selon les prérequis, la complexité et la charge cognitive. " +
                        "Réponds uniquement avec du JSON valide en français.";

        String raw = callApi(system, prompt);
        return parseDifficulty(raw);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HTTP  —  Groq uses the OpenAI-compatible chat completions format
    // ═════════════════════════════════════════════════════════════════════════

    private String callApi(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Clé API manquante.\n" +
                            "Vérifiez que ANTHROPIC_API_KEY=gsk_... est bien défini.\n" +
                            "Arrêtez complètement l'app (bouton Stop rouge) et relancez.\n" +
                            "Sinon recréez la variable : ANTHROPIC_API_KEY = gsk_..."
            );
        }

        // Build OpenAI-compatible request body manually (no external lib)
        String body = "{"
                + "\"model\":"      + jsonStr(MODEL) + ","
                + "\"max_tokens\":" + MAX_TOKENS     + ","
                + "\"messages\":["
                +   "{\"role\":\"system\",\"content\":" + jsonStr(systemPrompt) + "},"
                +   "{\"role\":\"user\",\"content\":"   + jsonStr(userPrompt)   + "}"
                + "]"
                + "}";

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(TIMEOUT))
                    .header("Content-Type",  "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200)
                throw new RuntimeException(
                        "Erreur API Groq " + resp.statusCode() + " : " + resp.body());

            return extractResponseText(resp.body());

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Erreur appel API : " + e.getMessage(), e);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ZERO-DEPENDENCY JSON HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    /** Serialize a Java String into a JSON string literal with proper escaping. */
    private static String jsonStr(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if      (c == '"')  sb.append("\\\"");
            else if (c == '\\') sb.append("\\\\");
            else if (c == '\n') sb.append("\\n");
            else if (c == '\r') sb.append("\\r");
            else if (c == '\t') sb.append("\\t");
            else if (c < 0x20)  sb.append(String.format("\\u%04x", (int) c));
            else                sb.append(c);
        }
        return sb.append('"').toString();
    }

    /**
     * Extract the assistant message text from a Groq/OpenAI response:
     * {"choices":[{"message":{"role":"assistant","content":"TEXT HERE"}}]}
     */
    private static String extractResponseText(String json) {
        // Find "content": inside choices → message
        int choicesIdx = json.indexOf("\"choices\"");
        if (choicesIdx < 0)
            throw new RuntimeException("Réponse inattendue (pas de choices) : " + json);

        int contentIdx = json.indexOf("\"content\":", choicesIdx);
        if (contentIdx < 0)
            throw new RuntimeException("Réponse inattendue (pas de content) : " + json);

        int q1 = json.indexOf('"', contentIdx + 10) + 1;
        return readJsonString(json, q1);
    }

    /**
     * Read a JSON-encoded string starting at {@code start}
     * (the index of the first char AFTER the opening quote).
     */
    private static String readJsonString(String json, int start) {
        StringBuilder sb = new StringBuilder();
        int i = start;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char esc = json.charAt(++i);
                switch (esc) {
                    case '"'  -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/'  -> sb.append('/');
                    case 'n'  -> sb.append('\n');
                    case 'r'  -> sb.append('\r');
                    case 't'  -> sb.append('\t');
                    case 'u'  -> {
                        sb.append((char) Integer.parseInt(json.substring(i + 1, i + 5), 16));
                        i += 4;
                    }
                    default   -> sb.append(esc);
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
            i++;
        }
        return sb.toString();
    }

    /** Extract the value of a string key from a flat JSON object snippet. */
    private static String extractVal(String json, String key) {
        String tag = "\"" + key + "\"";
        int idx = json.indexOf(tag);
        if (idx < 0) return "";
        int colon  = json.indexOf(':', idx + tag.length());
        if (colon < 0) return "";
        int quote1 = json.indexOf('"', colon + 1);
        if (quote1 < 0) return "";
        return readJsonString(json, quote1 + 1);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PARSERS
    // ═════════════════════════════════════════════════════════════════════════

    private CourseDescriptionResult parseCourseDescription(String raw) {
        String description = "";
        List<String> objectives = new ArrayList<>();
        try {
            String[] parts = raw.split("(?i)OBJECTIFS:");
            if (parts.length >= 2) {
                description = parts[0].replaceAll("(?i)DESCRIPTION:", "").trim();
                for (String line : parts[1].trim().split("\n")) {
                    String t = line.replaceAll("^[-•*\\d.)]+\\s*", "").trim();
                    if (!t.isEmpty()) objectives.add(t);
                }
            } else {
                description = raw.replaceAll("(?i)DESCRIPTION:", "").trim();
            }
        } catch (Exception e) {
            description = raw;
        }
        return new CourseDescriptionResult(description, objectives);
    }

    private List<ChapterSuggestion> parseCourseOutline(String raw) {
        List<ChapterSuggestion> result = new ArrayList<>();
        try {
            String clean = raw.replaceAll("(?s)```[a-z]*", "").replace("```", "").trim();
            int start = clean.indexOf('[');
            int end   = clean.lastIndexOf(']');
            if (start < 0 || end < 0) return result;

            String inner   = clean.substring(start + 1, end).trim();
            String[] objects = inner.split("\\},\\s*\\{");
            for (String obj : objects) {
                obj = obj.replaceAll("^\\{", "").replaceAll("\\}$", "").trim();
                String titre = extractVal(obj, "titre");
                if (titre.isEmpty()) titre = extractVal(obj, "title");
                String desc  = extractVal(obj, "description");
                if (!titre.isEmpty()) result.add(new ChapterSuggestion(titre, desc));
            }
        } catch (Exception e) {
            System.err.println("Erreur parsing outline : " + e.getMessage());
        }
        return result;
    }

    private DifficultyResult parseDifficulty(String raw) {
        try {
            String clean = raw.replaceAll("(?s)```[a-z]*", "").replace("```", "").trim();
            int s = clean.indexOf('{');
            int e = clean.lastIndexOf('}');
            if (s < 0 || e < 0) return new DifficultyResult("Intermédiaire", "Faible", raw);
            clean = clean.substring(s, e + 1);

            String niveau    = extractVal(clean, "niveau");
            String confiance = extractVal(clean, "confiance");
            String justif    = extractVal(clean, "justification");
            if (niveau.isEmpty()) niveau = "Intermédiaire";
            return new DifficultyResult(niveau, confiance, justif);
        } catch (Exception ex) {
            return new DifficultyResult("Intermédiaire", "Faible", "Analyse indisponible.");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // RESULT CLASSES
    // ═════════════════════════════════════════════════════════════════════════

    public static class CourseDescriptionResult {
        private final String       description;
        private final List<String> objectives;

        public CourseDescriptionResult(String description, List<String> objectives) {
            this.description = description;
            this.objectives  = objectives;
        }

        public String       getDescription()  { return description; }
        public List<String> getObjectives()   { return objectives;  }

        public String getFullText() {
            if (objectives.isEmpty()) return description;
            StringBuilder sb = new StringBuilder(description);
            sb.append("\n\nObjectifs d'apprentissage :\n");
            for (String o : objectives) sb.append("• ").append(o).append("\n");
            return sb.toString().trim();
        }
    }

    public static class ChapterSuggestion {
        private final String titre;
        private final String description;

        public ChapterSuggestion(String titre, String description) {
            this.titre       = titre;
            this.description = description;
        }

        public String getTitre()       { return titre;       }
        public String getDescription() { return description; }

        @Override public String toString() { return titre; }
    }

    public static class DifficultyResult {
        private final String niveau;
        private final String confiance;
        private final String justification;

        public DifficultyResult(String niveau, String confiance, String justification) {
            this.niveau        = niveau;
            this.confiance     = confiance;
            this.justification = justification;
        }

        public String getNiveau()        { return niveau;        }
        public String getConfiance()     { return confiance;     }
        public String getJustification() { return justification; }

        public String getBadgeColor() {
            return switch (niveau) {
                case "Débutant"      -> "#4CAF50";
                case "Intermédiaire" -> "#FF9800";
                case "Avancé"        -> "#F44336";
                default              -> "#9E9E9E";
            };
        }

        public String getIcon() {
            return switch (niveau) {
                case "Débutant"      -> "🟢";
                case "Intermédiaire" -> "🟡";
                case "Avancé"        -> "🔴";
                default              -> "⚪";
            };
        }
    }
}