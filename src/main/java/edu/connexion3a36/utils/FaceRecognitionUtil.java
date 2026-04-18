package edu.connexion3a36.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.file.*;

public class FaceRecognitionUtil {

    private static final String PYTHON_CMD = "python";
    private static final String SCRIPTS_DIR = "python_scripts/";
    private static final String ENCODER_SCRIPT = SCRIPTS_DIR + "face_encoder.py";
    private static final String RECOGNIZER_SCRIPT = SCRIPTS_DIR + "face_recognizer.py";
    private static final String TEMP_IMAGE_PATH = System.getProperty("java.io.tmpdir") + "\\face_capture.png";

    // Encode un visage depuis une image et retourne le vecteur JSON
    public static String encodeFace(String imagePath) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(PYTHON_CMD, ENCODER_SCRIPT, imagePath);
        pb.redirectErrorStream(true);
        pb.directory(new File(System.getProperty("user.dir")));

        Process process = pb.start();
        String output = readProcessOutput(process);
        process.waitFor();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(output);

        if (node.get("success").asBoolean()) {
            return node.get("encoding").toString();
        } else {
            throw new Exception(node.get("error").asText());
        }
    }

    // Compare un visage avec un encoding connu
    public static FaceResult recognizeFace(String imagePath, String knownEncodingJson) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(PYTHON_CMD, RECOGNIZER_SCRIPT, imagePath, knownEncodingJson);
        pb.redirectErrorStream(true);
        pb.directory(new File(System.getProperty("user.dir")));

        Process process = pb.start();
        String output = readProcessOutput(process);
        process.waitFor();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(output);

        FaceResult result = new FaceResult();
        result.success = node.get("success").asBoolean();
        result.match = node.get("match").asBoolean();

        if (node.has("confidence")) {
            result.confidence = node.get("confidence").asDouble();
        }
        if (node.has("error")) {
            result.error = node.get("error").asText();
        }

        return result;
    }

    // Sauvegarde une image BufferedImage en fichier temporaire
    public static String getTempImagePath() {
        return TEMP_IMAGE_PATH;
    }

    private static String readProcessOutput(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
        );
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    // Classe résultat
    public static class FaceResult {
        public boolean success;
        public boolean match;
        public double confidence;
        public String error;
    }
}