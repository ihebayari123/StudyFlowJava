package edu.connexion3a36.utils;

public class Validation {

    // ═══════════════════════════════
    // REGEX
    // ═══════════════════════════════
    private static final String REGEX_NOM        = "^[a-zA-ZÀ-ÿ\\s]{2,50}$";
    private static final String REGEX_EMAIL      = "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$";
    private static final String REGEX_MOT_PASSE  = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*]).{8,}$";

    // ═══════════════════════════════
    // NOM / PRENOM
    // ═══════════════════════════════
    public static boolean validerNom(String nom) {
        return nom != null && !nom.trim().isEmpty() && nom.matches(REGEX_NOM);
    }

    public static String messageNom(String nom) {
        if (nom == null || nom.trim().isEmpty())
            return "❌ Le nom est obligatoire.";
        if (!nom.matches(REGEX_NOM))
            return "❌ Lettres uniquement, 2 à 50 caractères.";
        return "";
    }

    // ═══════════════════════════════
    // EMAIL
    // ═══════════════════════════════
    public static boolean validerEmail(String email) {
        return email != null && !email.trim().isEmpty() && email.matches(REGEX_EMAIL);
    }

    public static String messageEmail(String email) {
        if (email == null || email.trim().isEmpty())
            return "❌ L'email est obligatoire.";
        if (!email.matches(REGEX_EMAIL))
            return "❌ Format invalide. Ex: nom@domaine.com";
        return "";
    }

    // ═══════════════════════════════
    // MOT DE PASSE
    // ═══════════════════════════════
    public static boolean validerMotDePasse(String mdp) {
        return mdp != null && !mdp.trim().isEmpty() && mdp.matches(REGEX_MOT_PASSE);
    }

    public static String messageMotDePasse(String mdp) {
        if (mdp == null || mdp.trim().isEmpty())
            return "❌ Le mot de passe est obligatoire.";
        if (mdp.length() < 8)
            return "❌ Minimum 8 caractères.";
        if (!mdp.matches(".*[A-Z].*"))
            return "❌ Au moins 1 lettre majuscule.";
        if (!mdp.matches(".*[0-9].*"))
            return "❌ Au moins 1 chiffre.";
        if (!mdp.matches(".*[!@#$%^&*].*"))
            return "❌ Au moins 1 caractère spécial (!@#$%^&*).";
        return "";
    }

    // ═══════════════════════════════
    // ROLE
    // ═══════════════════════════════
    public static boolean validerRole(String role) {
        return role != null && !role.trim().isEmpty();
    }

    public static String messageRole(String role) {
        if (role == null || role.trim().isEmpty())
            return "❌ Veuillez sélectionner un rôle.";
        return "";
    }

    // ═══════════════════════════════
    // STATUT
    // ═══════════════════════════════
    public static boolean validerStatut(String statut) {
        return statut != null && !statut.trim().isEmpty();
    }

    public static String messageStatut(String statut) {
        if (statut == null || statut.trim().isEmpty())
            return "❌ Veuillez sélectionner un statut.";
        return "";
    }

    // ═══════════════════════════════
    // VALIDATION GLOBALE
    // ═══════════════════════════════
    public static boolean validerTout(String nom, String prenom, String email,
                                      String motDePasse, String role, String statut) {
        return validerNom(nom)
                && validerNom(prenom)
                && validerEmail(email)
                && validerMotDePasse(motDePasse)
                && validerRole(role)
                && validerStatut(statut);
    }
}