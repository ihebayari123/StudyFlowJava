package edu.connexion3a36.utils;

import java.util.regex.Pattern;

public class ValidationUtils {

    // Validation du titre (3-100 caractères, lettres, chiffres, espaces)
    public static boolean isValidTitre(String titre) {
        if (titre == null || titre.trim().isEmpty()) return false;
        String trimmed = titre.trim();
        return trimmed.length() >= 3 && trimmed.length() <= 100;
    }

    // Validation de la description (10-500 caractères)
    public static boolean isValidDescription(String description) {
        if (description == null || description.trim().isEmpty()) return false;
        String trimmed = description.trim();
        return trimmed.length() >= 10 && trimmed.length() <= 500;
    }

    // Validation de l'URL de l'image
    public static boolean isValidImageUrl(String url) {
        if (url == null || url.trim().isEmpty()) return true; // Optionnel
        String urlPattern = "^(https?://|file:/).*\\.(jpg|jpeg|png|gif|webp|svg)(\\?.*)?$";
        return Pattern.compile(urlPattern, Pattern.CASE_INSENSITIVE).matcher(url.trim()).matches();
    }

    // Validation de l'email
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) return false;
        String emailPattern = "^[A-Za-z0-9+_.-]+@(.+)$";
        return Pattern.compile(emailPattern).matcher(email.trim()).matches();
    }

    // Validation du mot de passe (min 6 caractères)
    public static boolean isValidPassword(String password) {
        if (password == null) return false;
        return password.length() >= 6;
    }

    // Validation du nom (2-50 caractères, lettres seulement)
    public static boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        String trimmed = name.trim();
        return trimmed.length() >= 2 && trimmed.length() <= 50 && trimmed.matches("^[a-zA-ZÀ-ÿ\\s-]+$");
    }

    // Nettoyer le texte (trim et enlever caractères spéciaux)
    public static String sanitizeText(String text) {
        if (text == null) return "";
        return text.trim().replaceAll("[<>\"'&]", "");
    }

    // Capitaliser la première lettre
    public static String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) return "";
        text = text.trim().toLowerCase();
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }
}