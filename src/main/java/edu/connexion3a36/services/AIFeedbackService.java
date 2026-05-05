package edu.connexion3a36.services;

import edu.connexion3a36.entities.Question;
import edu.connexion3a36.entities.Quiz;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * AIFeedbackService
 * ══════════════════
 * Génère un rapport de feedback intelligent après un quiz,
 * en utilisant l'API Groq (LLaMA-3).
 */
public class AIFeedbackService {

    private static final String GROQ_URL  = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL     = "llama-3.3-70b-versatile";
    private static final int    TIMEOUT_S = 20;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(TIMEOUT_S))
            .build();

    // ── Résultat ─────────────────────────────────────────────────────────────

    public record FeedbackResult(
            String mention,
            String encouragement,
            String pointsForts,
            String pointsFaibles,
            String conseil,
            String source
    ) {
        public static FeedbackResult local(int score, int total) {
            double pct = total > 0 ? (score * 100.0 / total) : 0;
            String mention, encouragement, pointsForts, pointsFaibles, conseil;

            if (pct >= 80) {
                mention       = "Excellent";
                encouragement = "Très bon résultat ! Continuez sur cette lancée.";
                pointsForts   = "Bonne maîtrise générale des questions.";
                pointsFaibles = "Quelques questions méritent encore attention.";
                conseil       = "Révisez les questions incorrectes pour atteindre la perfection.";
            } else if (pct >= 60) {
                mention       = "Bien";
                encouragement = "Bon travail ! Vous progressez.";
                pointsForts   = "La moitié des questions est maîtrisée.";
                pointsFaibles = "Des lacunes persistent sur certains types de questions.";
                conseil       = "Relisez le cours sur les points difficiles avant de repasser.";
            } else if (pct >= 40) {
                mention       = "Passable";
                encouragement = "Un effort supplémentaire et vous y êtes !";
                pointsForts   = "Quelques bonnes réponses à conserver.";
                pointsFaibles = "Plusieurs notions fondamentales à retravailler.";
                conseil       = "Recommencez le quiz après avoir relu le cours en entier.";
            } else {
                mention       = "À revoir";
                encouragement = "Ne vous découragez pas, chaque essai est un progrès !";
                pointsForts   = "Vous avez participé, c'est le premier pas.";
                pointsFaibles = "La majorité des notions sont à revoir.";
                conseil       = "Reprenez le cours depuis le début et retentez le quiz.";
            }
            return new FeedbackResult(mention, encouragement, pointsForts, pointsFaibles, conseil, "LOCAL");
        }
    }

    // ── Méthode principale ────────────────────────────────────────────────────

    public static FeedbackResult generer(
            Quiz quiz,
            List<Question> questions,
            Map<Integer, Boolean> reponsesMap,
            int score,
            int total) {

        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("[AIFeedback] GROQ_API_KEY non définie — fallback local");
            return FeedbackResult.local(score, total);
        }

        try {
            return appelGroq(quiz, questions, reponsesMap, score, total, apiKey);
        } catch (Exception e) {
            System.err.println("[AIFeedback] Erreur API : " + e.getMessage());
            return FeedbackResult.local(score, total);
        }
    }

    // ── Construction du prompt ────────────────────────────────────────────────

    private static String buildPrompt(
            Quiz quiz,
            List<Question> questions,
            Map<Integer, Boolean> reponsesMap,
            int score,
            int total) {

        double pct = total > 0 ? Math.round(score * 100.0 / total) : 0;

        long nbChoix   = questions.stream().filter(q -> "choix_multiple".equals(q.getType())).count();
        long nbVF      = questions.stream().filter(q -> "vrai_faux".equals(q.getType())).count();
        long nbTexte   = questions.stream().filter(q -> "texte".equals(q.getType())).count();
        long nbFacile  = questions.stream().filter(q -> "facile".equals(q.getNiveau())).count();
        long nbMoyen   = questions.stream().filter(q -> "moyen".equals(q.getNiveau())).count();
        long nbDiff    = questions.stream().filter(q -> "difficile".equals(q.getNiveau())).count();

        long errChoix  = questions.stream()
                .filter(q -> "choix_multiple".equals(q.getType()) && Boolean.FALSE.equals(reponsesMap.get(q.getId())))
                .count();
        long errVF     = questions.stream()
                .filter(q -> "vrai_faux".equals(q.getType()) && Boolean.FALSE.equals(reponsesMap.get(q.getId())))
                .count();
        long errTexte  = questions.stream()
                .filter(q -> "texte".equals(q.getType()) && Boolean.FALSE.equals(reponsesMap.get(q.getId())))
                .count();
        long errFacile = questions.stream()
                .filter(q -> "facile".equals(q.getNiveau()) && Boolean.FALSE.equals(reponsesMap.get(q.getId())))
                .count();
        long errDiff   = questions.stream()
                .filter(q -> "difficile".equals(q.getNiveau()) && Boolean.FALSE.equals(reponsesMap.get(q.getId())))
                .count();

        return """
            Tu es un tuteur pédagogique expert et bienveillant pour la plateforme StudyFlow.
            
            Un étudiant vient de terminer le quiz "%s" avec ce résultat :
            - Score : %d/%d (%d%%)
            - Questions par type : choix_multiple=%d, vrai_faux=%d, texte=%d
            - Questions par niveau : facile=%d, moyen=%d, difficile=%d
            - Erreurs par type : choix_multiple=%d, vrai_faux=%d, texte=%d
            - Erreurs sur les questions faciles : %d | difficiles : %d
            
            Génère un feedback personnalisé, motivant et concret. Réponds UNIQUEMENT avec ce JSON (rien d'autre) :
            {
              "mention": "Excellent|Bien|Passable|À revoir",
              "encouragement": "phrase courte et motivante (max 15 mots)",
              "pointsForts": "ce que l'étudiant maîtrise (max 20 mots)",
              "pointsFaibles": "ce qui nécessite révision (max 20 mots)",
              "conseil": "action concrète et précise pour progresser (max 25 mots)"
            }
            
            Règles : JSON brut uniquement, pas de markdown, texte en français, ton chaleureux.
            """.formatted(
                quiz.getTitre(), score, total, (int) pct,
                nbChoix, nbVF, nbTexte,
                nbFacile, nbMoyen, nbDiff,
                errChoix, errVF, errTexte,
                errFacile, errDiff
        );
    }

    // ── Appel Groq ────────────────────────────────────────────────────────────

    private static FeedbackResult appelGroq(
            Quiz quiz, List<Question> questions,
            Map<Integer, Boolean> reponsesMap,
            int score, int total, String apiKey)
            throws IOException, InterruptedException {

        String prompt = buildPrompt(quiz, questions, reponsesMap, score, total);

        String body = "{"
                + "\"model\":\"" + MODEL + "\","
                + "\"messages\":[{\"role\":\"user\",\"content\":" + jsonStr(prompt) + "}],"
                + "\"temperature\":0.3,"
                + "\"max_tokens\":200"
                + "}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .timeout(Duration.ofSeconds(TIMEOUT_S))
                .header("Content-Type",  "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() != 200)
            throw new IOException("HTTP " + resp.statusCode() + " : " + resp.body());

        System.out.println("[AIFeedback] raw response: " + resp.body());
        return parseGroqResponse(resp.body(), score, total);
    }

    // ── Parsing — même logique robuste que AIHintService ─────────────────────

    /**
     * Extrait le contenu du champ "content" en parcourant caractère par caractère
     * (identique à AIHintService.extraireContenu) puis parse le JSON imbriqué.
     */
    private static FeedbackResult parseGroqResponse(String body, int score, int total) {
        try {
            String content = extraireContenu(body);
            System.out.println("[AIFeedback] content extrait: " + content);

            if (content.isBlank()) {
                System.err.println("[AIFeedback] content vide après extraction");
                return FeedbackResult.local(score, total);
            }

            // Nettoyer les éventuels backticks markdown
            content = content.replaceAll("(?s)```json|```", "").trim();

            // Trouver le JSON entre { et }
            int start = content.indexOf('{');
            int end   = content.lastIndexOf('}');
            if (start == -1 || end == -1 || end <= start) {
                System.err.println("[AIFeedback] JSON introuvable dans: " + content);
                return FeedbackResult.local(score, total);
            }
            String json = content.substring(start, end + 1);

            return new FeedbackResult(
                    extractStr(json, "mention",       "Bien"),
                    extractStr(json, "encouragement", "Bon travail !"),
                    extractStr(json, "pointsForts",   "Bonne participation."),
                    extractStr(json, "pointsFaibles", "Quelques notions à revoir."),
                    extractStr(json, "conseil",        "Relisez le cours et retentez."),
                    "AI"
            );
        } catch (Exception e) {
            System.err.println("[AIFeedback] Parsing échoué : " + e.getMessage());
            return FeedbackResult.local(score, total);
        }
    }

    /**
     * Parcours char-by-char identique à AIHintService.extraireContenu.
     * Gère correctement les séquences d'échappement \n \\" \\\\ etc.
     */
    private static String extraireContenu(String body) {
        int ci = body.indexOf("\"content\":\"");
        if (ci == -1) return "";
        int i = ci + "\"content\":\"".length();
        StringBuilder sb = new StringBuilder();
        while (i < body.length()) {
            char c = body.charAt(i);
            if (c == '"' && (i == 0 || body.charAt(i - 1) != '\\')) break;
            if (c == '\\' && i + 1 < body.length()) {
                char nx = body.charAt(i + 1);
                switch (nx) {
                    case 'n'  -> { sb.append('\n'); i += 2; continue; }
                    case '"'  -> { sb.append('"');  i += 2; continue; }
                    case '\\' -> { sb.append('\\'); i += 2; continue; }
                    case 'r'  -> { sb.append(' ');  i += 2; continue; }
                    case 't'  -> { sb.append(' ');  i += 2; continue; }
                    default   -> { sb.append(c);    i++;    continue; }
                }
            }
            sb.append(c);
            i++;
        }
        return sb.toString().trim();
    }

    // ── Extraction d'un champ JSON string ────────────────────────────────────

    private static String extractStr(String json, String key, String def) {
        String marker = "\"" + key + "\":\"";
        int i = json.indexOf(marker);
        if (i == -1) return def;
        int s = i + marker.length();
        // Parcours char-by-char pour ignorer les \" échappés
        StringBuilder sb = new StringBuilder();
        int j = s;
        while (j < json.length()) {
            char c = json.charAt(j);
            if (c == '\\' && j + 1 < json.length()) {
                char nx = json.charAt(j + 1);
                if (nx == '"') { sb.append('"'); j += 2; continue; }
                if (nx == '\\') { sb.append('\\'); j += 2; continue; }
                if (nx == 'n') { sb.append(' '); j += 2; continue; }
                sb.append(c); j++; continue;
            }
            if (c == '"') break;
            sb.append(c);
            j++;
        }
        String result = sb.toString().trim();
        return result.isEmpty() ? def : result;
    }

    // ── Utils ─────────────────────────────────────────────────────────────────

    private static String jsonStr(String s) {
        return "\"" + s.replace("\\","\\\\").replace("\"","\\\"")
                .replace("\n","\\n").replace("\r","\\r") + "\"";
    }

    private static String resolveApiKey() {
        String k = System.getenv("GROQ_API_KEY");
        if (k != null && !k.isBlank()) return k.trim();
        k = System.getProperty("GROQ_API_KEY");
        if (k != null && !k.isBlank()) return k.trim();
        for (String p : new String[]{".env", "ai/.env", "../ai/.env"}) {
            try {
                java.io.File f = new java.io.File(p);
                if (!f.exists()) continue;
                java.util.Scanner sc = new java.util.Scanner(f);
                while (sc.hasNextLine()) {
                    String ln = sc.nextLine().trim();
                    if (ln.startsWith("GROQ_API_KEY=")) {
                        sc.close();
                        return ln.substring("GROQ_API_KEY=".length()).trim();
                    }
                }
                sc.close();
            } catch (Exception ignored) {}
        }
        return null;
    }
}