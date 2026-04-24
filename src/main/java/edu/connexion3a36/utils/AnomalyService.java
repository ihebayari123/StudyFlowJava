package edu.connexion3a36.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class AnomalyService {

    private static final String PYTHON_PATH =
            System.getProperty("os.name").toLowerCase().contains("win")
                    ? "python"
                    : "python3";
    private static final String SCRIPT_PATH =
            System.getProperty("user.dir") +
                    "\\python_scripts\\anomaly_detection.py";

    public static List<AnomalyResult> detectAnomalies() {
        List<AnomalyResult> results = new ArrayList<>();
        try {
            // Lancer le script Python
            ProcessBuilder pb = new ProcessBuilder(PYTHON_PATH, SCRIPT_PATH);
            pb.environment().put("PYTHONIOENCODING", "utf-8");
            pb.redirectErrorStream(false);
            Process process = pb.start();

            // Lire la sortie
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8)
            );
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            // Lire les erreurs éventuelles
            BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), "UTF-8")
            );
            StringBuilder errors = new StringBuilder();
            while ((line = errorReader.readLine()) != null) {
                errors.append(line);
            }

            process.waitFor();

            String json = output.toString().trim();

            if (json.isEmpty()) {
                System.err.println("Script Python — aucune sortie. Erreurs : " + errors);
                return results;
            }

            if (json.startsWith("{\"error\"")) {
                System.err.println("Erreur Python : " + json);
                return results;
            }

            // Parser le JSON avec Jackson
            ObjectMapper mapper = new ObjectMapper();
            results = mapper.readValue(json,
                    new TypeReference<List<AnomalyResult>>() {});

        } catch (Exception e) {
            System.err.println("Erreur AnomalyService : " + e.getMessage());
            e.printStackTrace();
        }
        return results;
    }
    public static void main(String[] args) {
        List<AnomalyResult> results = detectAnomalies();
        System.out.println("Nombre d'utilisateurs analysés : " + results.size());
        for (AnomalyResult r : results) {
            System.out.println(r.getNiveauEmoji() + " | " +
                    r.getNom() + " " + r.getPrenom() +
                    " | Score : " + r.getScore() +
                    " | " + r.getRaisonsFormatees());
        }
    }
}