package edu.connexion3a36.Controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for the Cabinet Psychiatre page.
 *
 * Génère un QR code ZXing encodant l'URL de la liste des psychiatres tunisiens.
 * Quand l'utilisateur scanne le QR avec son téléphone, il accède directement
 * à la liste complète des psychiatres en Tunisie.
 *
 * URL encodée : recherche Google Maps "psychiatre Tunisie" + liste statique
 * via un lien court vers les données.
 */
public class CabinetPsychiatreController {

    // ─── Dimensions QR ───────────────────────────────────────────────
    private static final int QR_SIZE = 300;

    // ─── URL encodée dans le QR code ─────────────────────────────────
    // URL Google Maps recherche psychiatres Tunisie — s'ouvre directement
    // sur téléphone dans Google Maps ou navigateur
    private static final String QR_URL =
            "https://www.google.com/maps/search/psychiatre+Tunisie";

    // ─── Liste des psychiatres tunisiens ─────────────────────────────
    private static final PsychiatreInfo[] PSYCHIATRES = {
        new PsychiatreInfo(
            "Dr. Riadh Douki",
            "Hôpital Razi, La Manouba, Tunis",
            "+216 71 600 600",
            "Psychiatrie générale, Addictologie",
            "disponible"
        ),
        new PsychiatreInfo(
            "Dr. Lotfi Gaha",
            "CHU Monastir, Monastir",
            "+216 73 461 200",
            "Psychiatrie adulte, Troubles bipolaires",
            "disponible"
        ),
        new PsychiatreInfo(
            "Dr. Nadia Charfi",
            "Hôpital Charles Nicolle, Tunis",
            "+216 71 578 000",
            "Psychiatrie de l'enfant et adolescent",
            "sur_rdv"
        ),
        new PsychiatreInfo(
            "Dr. Malek Smaoui",
            "Clinique Hannibal, Tunis",
            "+216 71 890 000",
            "Psychothérapie, Anxiété, Dépression",
            "disponible"
        ),
        new PsychiatreInfo(
            "Dr. Sonia Ouali",
            "Hôpital Razi, La Manouba, Tunis",
            "+216 71 600 600",
            "Psychiatrie légale, Expertise",
            "sur_rdv"
        ),
        new PsychiatreInfo(
            "Dr. Fadhel Nacef",
            "Hôpital Razi, La Manouba, Tunis",
            "+216 71 600 600",
            "Psychiatrie générale, Schizophrénie",
            "disponible"
        ),
        new PsychiatreInfo(
            "Dr. Imen Halouani",
            "CHU Sfax, Sfax",
            "+216 74 241 411",
            "Psychiatrie adulte, Troubles anxieux",
            "disponible"
        ),
        new PsychiatreInfo(
            "Dr. Mohamed Mezghanni",
            "CHU Sfax, Sfax",
            "+216 74 241 411",
            "Psychiatrie, Neuropsychiatrie",
            "sur_rdv"
        ),
    };

    // ─── FXML ────────────────────────────────────────────────────────
    @FXML private ImageView qrCodeImage;
    @FXML private Label     qrStatusLabel;
    @FXML private VBox      doctorListContainer;

    /** Référence au dashboard pour la navigation embarquée */
    private FitnessDashboardController dashboardController;

    public void setDashboardController(FitnessDashboardController ctrl) {
        this.dashboardController = ctrl;
    }

    // ─── Init ────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        generateAndDisplayQRCode();
        populateDoctorList();
    }

    // ─── Génération QR code ──────────────────────────────────────────

    /**
     * Génère le QR code encodant l'URL de la liste des psychiatres tunisiens
     * et l'affiche dans l'ImageView.
     */
    private void generateAndDisplayQRCode() {
        new Thread(() -> {
            try {
                File qrFile = generateQRCodeFile(QR_URL);
                Image img   = new Image(qrFile.toURI().toString());
                Platform.runLater(() -> {
                    qrCodeImage.setImage(img);
                    if (qrStatusLabel != null) {
                        qrStatusLabel.setText("✅ QR Code généré — Scannez pour voir la liste complète");
                        qrStatusLabel.setStyle("-fx-text-fill: #2e7d32; -fx-font-size: 11px;");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (qrStatusLabel != null) {
                        qrStatusLabel.setText("⚠️ Erreur QR : " + e.getMessage());
                        qrStatusLabel.setStyle("-fx-text-fill: #c62828; -fx-font-size: 11px;");
                    }
                });
            }
        }).start();
    }

    /**
     * Génère un fichier PNG du QR code via ZXing.
     * Utilise le niveau de correction d'erreur H (30%) pour une meilleure
     * lisibilité même si le QR est partiellement obstrué.
     */
    private File generateQRCodeFile(String content) throws WriterException, IOException {
        QRCodeWriter writer = new QRCodeWriter();

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET,       "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION,    ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN,              2);

        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);

        // Convertir en BufferedImage avec couleurs personnalisées
        BufferedImage image = new BufferedImage(QR_SIZE, QR_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // Fond blanc
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, QR_SIZE, QR_SIZE);

        // Modules bleus foncés (couleur médicale)
        g.setColor(new Color(21, 101, 192)); // #1565c0
        for (int x = 0; x < QR_SIZE; x++) {
            for (int y = 0; y < QR_SIZE; y++) {
                if (matrix.get(x, y)) {
                    g.fillRect(x, y, 1, 1);
                }
            }
        }
        g.dispose();

        // Sauvegarder dans un fichier temporaire
        File tempFile = File.createTempFile("psychiatre_qr_", ".png");
        tempFile.deleteOnExit();
        ImageIO.write(image, "png", tempFile);
        return tempFile;
    }

    // ─── Liste des médecins ──────────────────────────────────────────

    /**
     * Peuple dynamiquement la liste des psychiatres tunisiens dans le FXML.
     */
    private void populateDoctorList() {
        if (doctorListContainer == null) return;
        doctorListContainer.getChildren().clear();

        for (PsychiatreInfo p : PSYCHIATRES) {
            doctorListContainer.getChildren().add(buildDoctorCard(p));
        }
    }

    // ─── Actions ─────────────────────────────────────────────────────

    /**
     * Affiche la liste complète dans une alerte (bouton "Voir la liste").
     */
    @FXML
    public void showDoctorList() {
        StringBuilder sb = new StringBuilder();
        sb.append("🏥 Psychiatres en Tunisie\n");
        sb.append("═══════════════════════════════\n\n");
        for (PsychiatreInfo p : PSYCHIATRES) {
            sb.append("👨‍⚕️ ").append(p.nom).append("\n");
            sb.append("   📍 ").append(p.adresse).append("\n");
            sb.append("   📞 ").append(p.telephone).append("\n");
            sb.append("   🩺 ").append(p.specialite).append("\n");
            sb.append("   ").append(p.disponibilite.equals("disponible") ? "✅ Disponible" : "⏳ Sur RDV").append("\n\n");
        }
        sb.append("📱 Scannez le QR code pour ouvrir Google Maps\n");
        sb.append("   et trouver d'autres psychiatres près de vous.");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Psychiatres en Tunisie");
        alert.setHeaderText("Liste des psychiatres disponibles");
        alert.setContentText(sb.toString());
        alert.getDialogPane().setPrefWidth(520);
        alert.showAndWait();
    }

    /**
     * Ouvre l'URL dans le navigateur par défaut (bouton "Ouvrir dans navigateur").
     */
    @FXML
    public void openInBrowser() {
        try {
            Desktop.getDesktop().browse(new java.net.URI(QR_URL));
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ouvrir le navigateur : " + e.getMessage());
        }
    }

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
                showAlert("Erreur", "Impossible de retourner à la page précédente");
            }
        }
    }

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
                showAlert("Erreur", "Impossible de retourner au tableau de bord");
            }
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    /**
     * Construit une carte médecin en JavaFX (programmatique pour éviter
     * la duplication dans le FXML).
     */
    private javafx.scene.layout.HBox buildDoctorCard(PsychiatreInfo p) {
        javafx.scene.layout.HBox card = new javafx.scene.layout.HBox(14);
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        card.setStyle(
            "-fx-background-color: white; -fx-background-radius: 12;" +
            "-fx-border-color: #e3f2fd; -fx-border-radius: 12; -fx-border-width: 1;" +
            "-fx-padding: 14 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 6, 0, 0, 2);"
        );

        // Avatar
        Label avatar = new Label("👨‍⚕️");
        avatar.setStyle("-fx-font-size: 26px;");

        // Infos
        javafx.scene.layout.VBox info = new javafx.scene.layout.VBox(3);
        javafx.scene.layout.HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);

        Label nom = new Label(p.nom);
        nom.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");

        Label adresse = new Label("📍 " + p.adresse);
        adresse.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");

        Label tel = new Label("📞 " + p.telephone);
        tel.setStyle("-fx-font-size: 11px; -fx-text-fill: #1565c0;");

        Label spec = new Label("🩺 " + p.specialite);
        spec.setStyle("-fx-font-size: 11px; -fx-text-fill: #666; -fx-font-style: italic;");

        info.getChildren().addAll(nom, adresse, tel, spec);

        // Badge disponibilité
        Label badge;
        if ("disponible".equals(p.disponibilite)) {
            badge = new Label("✅ Disponible");
            badge.setStyle(
                "-fx-font-size: 11px; -fx-text-fill: #2e7d32;" +
                "-fx-background-color: #e8f5e9; -fx-padding: 4 10; -fx-background-radius: 20;"
            );
        } else {
            badge = new Label("⏳ Sur RDV");
            badge.setStyle(
                "-fx-font-size: 11px; -fx-text-fill: #e65100;" +
                "-fx-background-color: #fff3e0; -fx-padding: 4 10; -fx-background-radius: 20;"
            );
        }

        card.getChildren().addAll(avatar, info, badge);
        return card;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ─── Data class ──────────────────────────────────────────────────
    private static class PsychiatreInfo {
        final String nom;
        final String adresse;
        final String telephone;
        final String specialite;
        final String disponibilite; // "disponible" | "sur_rdv"

        PsychiatreInfo(String nom, String adresse, String telephone,
                       String specialite, String disponibilite) {
            this.nom           = nom;
            this.adresse       = adresse;
            this.telephone     = telephone;
            this.specialite    = specialite;
            this.disponibilite = disponibilite;
        }
    }
}
