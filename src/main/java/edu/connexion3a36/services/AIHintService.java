package edu.connexion3a36.services;

import edu.connexion3a36.entities.Question;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * AIHintService
 * ══════════════
 * Génère un indice intelligent pour une question via Groq (LLaMA-3).
 *
 * L'indice :
 *   - aide l'étudiant à trouver la réponse sans la donner
 *   - est adapté au niveau (facile / moyen / difficile)
 *   - fait maximum 15 mots
 *
 * Utilisation :
 *   AIHintService.HintResult r = AIHintService.generer(question);
 *   String indice = r.indice();   // "AI" ou "LOCAL"
 */
public class AIHintService {

    private static final String GROQ_URL  = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL     = "llama-3.3-70b-versatile";
    private static final int    TIMEOUT_S = 15;

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(TIMEOUT_S))
        .build();

    // ── Résultat ─────────────────────────────────────────────────────────────

    public record HintResult(String indice, String source) {
        public static HintResult ai(String indice)    { return new HintResult(indice, "AI"); }
        public static HintResult local(String indice) { return new HintResult(indice, "LOCAL"); }
    }

    // ── Méthode principale ────────────────────────────────────────────────────

    public static HintResult generer(Question question) {
        if (question == null || question.getTexte() == null || question.getTexte().isBlank())
            return HintResult.local("Relisez attentivement la question.");

        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("[AIHint] GROQ_API_KEY non définie — fallback local");
            return HintResult.local(fallbackLocal(question));
        }

        try {
            return appelGroq(question, apiKey);
        } catch (Exception e) {
            System.err.println("[AIHint] Erreur API : " + e.getMessage());
            return HintResult.local(fallbackLocal(question));
        }
    }

    // ── Prompt ────────────────────────────────────────────────────────────────

    private static String buildPrompt(Question q) {
        String contexteType = switch (nvl(q.getType())) {
            case "choix_multiple" -> String.format(
                "Choix : A=%s, B=%s, C=%s, D=%s.",
                nvl(q.getChoixA()), nvl(q.getChoixB()),
                nvl(q.getChoixC()), nvl(q.getChoixD()));
            case "vrai_faux" ->
                "Bonne réponse : " + (Boolean.TRUE.equals(q.getBonneReponseBool()) ? "VRAI" : "FAUX") + ".";
            case "texte" ->
                "Réponse attendue : «" + nvl(q.getReponseAttendue()) + "».";
            default -> "";
        };

        String niveauConsigne = switch (nvl(q.getNiveau())) {
            case "facile"    -> "L'indice peut être direct, presque une paraphrase.";
            case "moyen"     -> "L'indice doit orienter sans trop dévoiler.";
            case "difficile" -> "L'indice doit être subtil, une simple piste.";
            default          -> "L'indice doit orienter l'étudiant.";
        };

        return """
            Tu es un assistant pédagogique pour StudyFlow.
            
            Question : «%s»
            Niveau : %s
            %s
            
            Génère UN SEUL indice en français qui :
            - Aide l'étudiant SANS donner la réponse directement
            - %s
            - Maximum 15 mots
            - N'utilise pas le mot "réponse" ni la réponse elle-même
            
            Réponds UNIQUEMENT avec l'indice en texte brut, sans guillemets, sans JSON.
            """.formatted(
                q.getTexte(),
                nvl(q.getNiveau()),
                contexteType,
                niveauConsigne);
    }

    // ── Appel Groq ────────────────────────────────────────────────────────────

    private static HintResult appelGroq(Question q, String apiKey) throws Exception {
        String body = "{"
            + "\"model\":\"" + MODEL + "\","
            + "\"messages\":[{\"role\":\"user\",\"content\":" + jsonStr(buildPrompt(q)) + "}],"
            + "\"temperature\":0.5,\"max_tokens\":60}";

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(GROQ_URL))
            .timeout(Duration.ofSeconds(TIMEOUT_S))
            .header("Content-Type",  "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200)
            throw new Exception("HTTP " + resp.statusCode());

        String indice = extraireContenu(resp.body())
            .replaceAll("^\"|\"$|^'|'$", "")
            .replaceAll("\\.{2,}", ".").trim();

        if (indice.isEmpty()) return HintResult.local(fallbackLocal(q));
        if (indice.length() > 200) indice = indice.substring(0, 197) + "…";
        return HintResult.ai(indice);
    }

    // ── Parsing ───────────────────────────────────────────────────────────────

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
                    case 'n' -> { sb.append(' '); i += 2; continue; }
                    case '"' -> { sb.append('"'); i += 2; continue; }
                    case '\\'-> { sb.append('\\'); i += 2; continue; }
                    default  -> { sb.append(c); i++; continue; }
                }
            }
            sb.append(c); i++;
        }
        return sb.toString().trim();
    }

    // ── Fallback ──────────────────────────────────────────────────────────────

    private static String fallbackLocal(Question q) {
        return switch (nvl(q.getType())) {
            case "choix_multiple" -> "Éliminez les choix incorrects un par un.";
            case "vrai_faux"      -> "Pensez à ce que dit le cours sur ce sujet.";
            case "texte"          -> "Relisez le chapitre correspondant du cours.";
            default               -> "Relisez attentivement la question.";
        };
    }

    // ── Utils ─────────────────────────────────────────────────────────────────

    private static String nvl(String s) { return s != null ? s : ""; }

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
