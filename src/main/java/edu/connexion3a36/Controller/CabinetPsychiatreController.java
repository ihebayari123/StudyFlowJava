package edu.connexion3a36.Controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for the Cabinet Psychiatre page
 * Displays QR code with doctor/cabinet list and navigation options
 */
public class CabinetPsychiatreController {

    // QR code dimensions
    private static final int QR_WIDTH = 250;
    private static final int QR_HEIGHT = 250;

    // Path for temporary QR code image
    private static final String QR_TEMP_FILE = "qrcode_temp.png";

    // List of doctors/cabinets
    private static final List<String> DOCTOR_LIST = Arrays.asList(
            "1. Dr. Martin (Paris) - 01 42 87 65 43",
            "2. Dr. Bernard (Lyon) - 04 78 92 15 67",
            "3. Dr. Petit (Marseille) - 04 91 76 54 32",
            "4. Cabinet Santé Mentale (Toulouse) - 05 61 22 77 89",
            "5. Dr. Durand (Nice) - 04 93 88 55 21"
    );

    @FXML
    private ImageView qrCodeImage;

    /** Référence au dashboard pour la navigation embarquée */
    private FitnessDashboardController dashboardController;

    public void setDashboardController(FitnessDashboardController ctrl) {
        this.dashboardController = ctrl;
    }

    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        // Generate and display the QR code
        loadQRCode();
    }

    /**
     * Generate and display a real QR code using ZXing library
     */
    private void loadQRCode() {
        try {
            // Create the data for the QR code
            StringBuilder qrData = new StringBuilder("Cabinet Psychiatre - Liste médecins:\n");
            for (String doctor : DOCTOR_LIST) {
                qrData.append(doctor).append("\n");
            }
            qrData.append("\nConsultation sur rendez-vous");

            // Generate QR code image
            boolean success = generateQRCodeImage(qrData.toString());
            if (success) {
                // Load the generated image
                Image image = new Image(new File(QR_TEMP_FILE).toURI().toString());
                qrCodeImage.setImage(image);
            } else {
                showAlert("Erreur", "Impossible de générer le code QR");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de la génération du code QR: " + e.getMessage());
        }
    }

    /**
     * Generate a QR code image using ZXing library
     * @param content The content to encode in the QR code
     * @return true if successful, false otherwise
     */
    private boolean generateQRCodeImage(String content) {
        // First clean up old temp file
        File tempFile = new File(QR_TEMP_FILE);
        if (tempFile.exists()) {
            tempFile.delete();
        }

        try {
            // Create QR code writer
            QRCodeWriter qrCodeWriter = new QRCodeWriter();

            // Set encoding hints
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            // Generate the bit matrix
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, QR_WIDTH, QR_HEIGHT, hints);

            // Convert to buffered image
            BufferedImage bufferedImage = new BufferedImage(QR_WIDTH, QR_HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = bufferedImage.createGraphics();

            // Set background to white
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, QR_WIDTH, QR_HEIGHT);

            // Set quality
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw the QR code (black modules)
            graphics.setColor(Color.BLACK);
            for (int x = 0; x < QR_WIDTH; x++) {
                for (int y = 0; y < QR_HEIGHT; y++) {
                    if (bitMatrix.get(x, y)) {
                        graphics.fillRect(x, y, 1, 1);
                    }
                }
            }

            graphics.dispose();

            // Save to file
            ImageIO.write(bufferedImage, "png", tempFile);

            return true;

        } catch (WriterException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Go back to the stress options page
     */
    @FXML
    public void goBack(ActionEvent event) {
        if (dashboardController != null) {
            dashboardController.goToStressOptions(event);
        } else {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/stress_options.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Erreur", "Impossible de retourner à la page précédente");
            }
        }
    }

    /**
     * Go back to the main fitness dashboard
     */
    @FXML
    public void goToDashboard(ActionEvent event) {
        if (dashboardController != null) {
            dashboardController.goToRelax(event);
        } else {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fitness_dashboard2.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Erreur", "Impossible de retourner au tableau de bord");
            }
        }
    }

    /**
     * Show the doctor list in a dialog when QR code is scanned
     */
    @FXML
    public void showDoctorList() {
        StringBuilder message = new StringBuilder();
        for (String doctor : DOCTOR_LIST) {
            message.append(doctor).append("\n");
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Liste des Médecins");
        alert.setHeaderText("Cabinets de Psychiatrie Disponibles");
        alert.setContentText(message.toString());
        alert.showAndWait();
    }

    /**
     * Show error alert
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}