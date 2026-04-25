package edu.connexion3a36.services;

/**
 * SmartValidator
 * ──────────────
 * Façade statique pour la validation sémantique des réponses texte libre.
 *
 * Utilisée par :
 *   - SmartValidatorTest  (tests unitaires)
 *   - UserQuizController  (correction quiz étudiant)
 *
 * Délègue l'appel Ollama/Mistral à SmartValidatorService.
 * Expose un Result immuable avec :
 *   correct()      → boolean
 *   explication()  → String
 *   source()       → String  ("exact" | "ollama" | "fallback")
 */
public final class SmartValidator {

    private SmartValidator() {}   // Classe utilitaire — pas d'instanciation

    // ── Résultat public ───────────────────────────────────────────────────────

    public record Result(
            boolean correct,
            String  explication,
            String  source
    ) {}

    // ── Point d'entrée statique ───────────────────────────────────────────────

    /**
     * Valide sémantiquement la réponse d'un étudiant.
     *
     * @param reponseAttendue  La bonne réponse de référence
     * @param reponseEtudiant  La réponse fournie par l'étudiant
     * @return Result avec correct, explication et source
     */
    public static Result valider(String reponseAttendue, String reponseEtudiant) {
        return valider(null, reponseAttendue, reponseEtudiant);
    }

    /**
     * Valide sémantiquement avec contexte de la question.
     *
     * @param questionTexte    Le texte de la question (peut être null)
     * @param reponseAttendue  La bonne réponse de référence
     * @param reponseEtudiant  La réponse fournie par l'étudiant
     * @return Result avec correct, explication et source
     */
    public static Result valider(String questionTexte,
                                  String reponseAttendue,
                                  String reponseEtudiant) {

        // Cas vide → faux immédiat
        if (reponseEtudiant == null || reponseEtudiant.isBlank())
            return new Result(false, "Aucune réponse fournie.", "exact");

        // Correspondance exacte (après normalisation)
        if (normaliser(reponseEtudiant).equals(normaliser(reponseAttendue)))
            return new Result(true, "Réponse exacte.", "exact");

        // Délégation à SmartValidatorService (appel Ollama)
        try {
            SmartValidatorService svc = new SmartValidatorService();
            SmartValidatorService.ValidationResult vr = svc.valider(
                    questionTexte != null ? questionTexte : "",
                    reponseAttendue,
                    reponseEtudiant
            );
            String source = detecterSource(vr.explanation());
            return new Result(vr.isCorrect(), vr.explanation(), source);

        } catch (Exception e) {
            // Fallback local si SmartValidatorService lève une exception inattendue
            return fallbackLocal(reponseAttendue, reponseEtudiant);
        }
    }

    // ── Fallback local ────────────────────────────────────────────────────────

    private static Result fallbackLocal(String attendue, String etudiant) {
        String a = normaliser(attendue);
        String e = normaliser(etudiant);

        if (a.contains(e) || e.contains(a))
            return new Result(true,
                    "Correspondance partielle (Mistral indisponible).",
                    "fallback");

        String[] mots = a.split("\\s+");
        long matches = 0;
        for (String m : mots)
            if (m.length() > 3 && e.contains(m)) matches++;

        boolean ok = mots.length > 0 && (double) matches / mots.length >= 0.4;
        return new Result(ok,
                ok  ? "Correspondance par mots-clés (Mistral indisponible)."
                    : "Réponse incorrecte (Mistral indisponible).",
                "fallback");
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private static String normaliser(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase()
                .replaceAll("[àáâã]", "a").replaceAll("[éèêë]", "e")
                .replaceAll("[îï]", "i").replaceAll("[ôö]", "o")
                .replaceAll("[ùûü]", "u").replaceAll("[ç]", "c")
                .replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
    }

    private static String detecterSource(String explication) {
        if (explication == null) return "ollama";
        String low = explication.toLowerCase();
        if (low.contains("indisponible")) return "fallback";
        if (low.contains("exacte"))       return "exact";
        return "ollama";
    }
}
