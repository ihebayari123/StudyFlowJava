package edu.connexion3a36.Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DashboardController {

    private static final Logger LOGGER = Logger.getLogger(DashboardController.class.getName());

    // ── Sidebar items principaux ──────────────────────────────────────────────
    @FXML private HBox homeItem;
    @FXML private HBox coursItem;
    @FXML private HBox chapitresItem;
    @FXML private HBox quizItem;
    @FXML private HBox exercicesItem;
    @FXML private HBox progressionItem;        // bouton "Anti-Stress" (toggle)
    @FXML private HBox administrationItem;
    @FXML private HBox settingsItem;

    // ── Sous-menu Anti-Stress ─────────────────────────────────────────────────
    @FXML private VBox  antiStressSubMenu;     // VBox caché par défaut
    @FXML private Label antiStressArrow;       // flèche ▶ / ▼

    @FXML private HBox ajouterMedecinItem;
    @FXML private HBox listeMedecinsItem;
    @FXML private HBox ajouterConsultationItem;
    @FXML private HBox listeConsultationsItem;
    @FXML private HBox antiStresseItem;
    @FXML private HBox ajouterScoreEtudiantItem;
    @FXML private HBox ajouterBienEtreItem;
    @FXML private HBox voirScoreItem;
    @FXML private HBox deconnexionItem;

    // ── Zone de contenu ───────────────────────────────────────────────────────
    @FXML private StackPane contentArea;

    // ── État du sous-menu ─────────────────────────────────────────────────────
    private boolean antiStressMenuOpen = false;

    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        setupNavigation();
        loadView("cours");   // vue par défaut
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Navigation
    // ─────────────────────────────────────────────────────────────────────────
    private void setupNavigation() {

        // ── Menu principal ────────────────────────────────────────────────────
        homeItem.setOnMouseClicked(e           -> loadView("dashboard"));
        coursItem.setOnMouseClicked(e          -> loadView("cours"));
        chapitresItem.setOnMouseClicked(e      -> loadView("chapitres"));
        quizItem.setOnMouseClicked(e           -> loadView("quiz"));
        exercicesItem.setOnMouseClicked(e      -> loadView("exercices"));
        administrationItem.setOnMouseClicked(e -> loadView("admin"));
        settingsItem.setOnMouseClicked(e       -> loadView("settings"));

        // Anti-Stress → toggle le sous-menu uniquement
        progressionItem.setOnMouseClicked(e    -> toggleAntiStressMenu());

        // ── Sous-menu Anti-Stress (CORRIGÉ) ───────────────────────────────────
        ajouterMedecinItem.setOnMouseClicked(e       -> loadView("AjouterMedecin"));        // ➕ AjouterMedecin.fxml
        listeMedecinsItem.setOnMouseClicked(e        -> loadView("AfficherMedecin"));       // 👥 AfficherMedecin.fxml
        ajouterConsultationItem.setOnMouseClicked(e  -> loadView("AjouterConsultation"));   // 📅 AjouterConsultation.fxml
        listeConsultationsItem.setOnMouseClicked(e   -> loadView("AfficherConsultation"));  // 📋 AfficherConsultation.fxml
        antiStresseItem.setOnMouseClicked(e          -> loadView("AfficherStressSurvey"));  // 📱 AfficherStressSurvey.fxml
        ajouterScoreEtudiantItem.setOnMouseClicked(e -> loadView("AjouterStressSurvey"));   // 📝 AjouterStressSurvey.fxml
        ajouterBienEtreItem.setOnMouseClicked(e      -> loadView("AjouterWellBeingScore")); // 📊 AjouterWellBeingScore.fxml
        voirScoreItem.setOnMouseClicked(e            -> loadView("AfficherWellBeingScore"));// 📈 AfficherWellBeingScore.fxml
        deconnexionItem.setOnMouseClicked(e          -> handleDeconnexion());               // 🔓 Déconnexion

        // ── Hover effects ─────────────────────────────────────────────────────
        HBox[] allItems = {
                homeItem, coursItem, chapitresItem, quizItem, exercicesItem,
                progressionItem, administrationItem, settingsItem,
                ajouterMedecinItem, listeMedecinsItem, ajouterConsultationItem,
                listeConsultationsItem, antiStresseItem, ajouterScoreEtudiantItem,
                ajouterBienEtreItem, voirScoreItem, deconnexionItem
        };
        for (HBox item : allItems) {
            if (item != null) addHoverEffect(item);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Toggle sous-menu Anti-Stress
    // ─────────────────────────────────────────────────────────────────────────
    private void toggleAntiStressMenu() {
        antiStressMenuOpen = !antiStressMenuOpen;

        antiStressSubMenu.setVisible(antiStressMenuOpen);
        antiStressSubMenu.setManaged(antiStressMenuOpen);

        if (antiStressArrow != null) {
            antiStressArrow.setText(antiStressMenuOpen ? "▼" : "▶");
        }

        if (progressionItem != null) {
            progressionItem.setStyle(antiStressMenuOpen
                    ? "-fx-background-color: #E8F0FE; -fx-background-radius: 8; -fx-padding: 0 12 0 12; -fx-cursor: hand;"
                    : "-fx-background-color: transparent; -fx-background-radius: 8; -fx-padding: 0 12 0 12;"
            );
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Chargement d'une vue dans contentArea
    // ─────────────────────────────────────────────────────────────────────────
    private void loadView(String viewName) {
        try {
            resetActiveStyles();
            setActiveStyle(viewName);

            if (viewName.equals("studyflow")) {
                System.out.println("Ignorer studyflow.fxml");
                return;
            }

            String resourcePath = "/" + viewName + ".fxml";
            URL resourceUrl = getClass().getResource(resourcePath);

            if (resourceUrl == null) {
                System.err.println("Fichier non trouvé : " + resourcePath);
                showErrorView(viewName);
                return;
            }

            System.out.println("Chargement : " + resourcePath);
            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Node view = loader.load();

            Object controller = loader.getController();
            if (controller instanceof CoursController) {
                ((CoursController) controller).setDashboardController(this);
            }

            contentArea.getChildren().setAll(view);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur chargement : " + viewName, e);
            showErrorView(viewName);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Déconnexion → retour à Login
    // ─────────────────────────────────────────────────────────────────────────
    private void handleDeconnexion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur déconnexion", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Vue d'erreur inline
    // ─────────────────────────────────────────────────────────────────────────
    private void showErrorView(String viewName) {
        VBox errorBox = new VBox();
        errorBox.setStyle("-fx-alignment: center; -fx-padding: 40;");
        Label errorLabel = new Label(
                "⚠️ Vue non trouvée : " + viewName + ".fxml\n\nVérifiez que le fichier existe dans resources/"
        );
        errorLabel.setStyle("-fx-text-fill: #F44336; -fx-font-size: 14;");
        errorBox.getChildren().add(errorLabel);
        contentArea.getChildren().setAll(errorBox);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Styles actif / inactif (CORRIGÉ)
    // ─────────────────────────────────────────────────────────────────────────
    private void resetActiveStyles() {
        if (homeItem != null) {
            HBox[] mainItems = {
                    homeItem, coursItem, chapitresItem, quizItem, exercicesItem,
                    progressionItem, administrationItem, settingsItem
            };
            for (HBox item : mainItems) {
                if (item == null) continue;
                item.setStyle("-fx-background-color: transparent; -fx-background-radius: 8; -fx-padding: 0 12 0 12;");
                for (Node node : item.getChildren()) {
                    if (node instanceof Label label) {
                        String txt = label.getText();
                        if (txt != null && !txt.matches("🏠|📚|📖|❓|✏️|📊|🏛️|⚙️|▶|▼")) {
                            label.setStyle("-fx-font-size: 13; -fx-text-fill: #757575; -fx-font-weight: normal;");
                        }
                    }
                }
            }
        }

        // Réinitialiser les sous-items
        HBox[] subItems = {
                ajouterMedecinItem, listeMedecinsItem, ajouterConsultationItem,
                listeConsultationsItem, antiStresseItem, ajouterScoreEtudiantItem,
                ajouterBienEtreItem, voirScoreItem
        };
        for (HBox item : subItems) {
            if (item != null)
                item.setStyle("-fx-background-color: transparent; -fx-background-radius: 6; -fx-padding: 0 8 0 8;");
        }
    }

    private void setActiveStyle(String viewName) {
        HBox activeItem = switch (viewName) {
            // Menu principal
            case "dashboard"              -> homeItem;
            case "cours"                  -> coursItem;
            case "chapitres"              -> chapitresItem;
            case "quiz"                   -> quizItem;
            case "exercices"              -> exercicesItem;
            case "admin"                  -> administrationItem;
            case "settings"               -> settingsItem;
            // Sous-items Anti-Stress (CORRIGÉ)
            case "AjouterMedecin"         -> ajouterMedecinItem;
            case "AfficherMedecin"        -> listeMedecinsItem;
            case "AjouterConsultation"    -> ajouterConsultationItem;
            case "AfficherConsultation"   -> listeConsultationsItem;
            case "AfficherStressSurvey"   -> antiStresseItem;
            case "AjouterStressSurvey"    -> ajouterScoreEtudiantItem;
            case "AjouterWellBeingScore"  -> ajouterBienEtreItem;
            case "AfficherWellBeingScore" -> voirScoreItem;
            default                       -> null;
        };

        if (activeItem == null) return;

        boolean isMainItem = (activeItem == homeItem || activeItem == coursItem ||
                activeItem == chapitresItem || activeItem == quizItem ||
                activeItem == exercicesItem || activeItem == administrationItem ||
                activeItem == settingsItem);

        if (isMainItem) {
            activeItem.setStyle("-fx-background-color: #E8F0FE; -fx-background-radius: 8; -fx-padding: 0 12 0 12;");
            for (Node node : activeItem.getChildren()) {
                if (node instanceof Label label) {
                    String txt = label.getText();
                    if (txt != null && !txt.matches("🏠|📚|📖|❓|✏️|📊|🏛️|⚙️")) {
                        label.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #2979FF;");
                    }
                }
            }
        } else {
            // Style actif pour les sous-items
            activeItem.setStyle("-fx-background-color: #E8F0FE; -fx-background-radius: 6; -fx-padding: 0 8 0 8;");
            for (Node node : activeItem.getChildren()) {
                if (node instanceof Label label) {
                    String txt = label.getText();
                    if (txt != null && !txt.matches("➕|👥|📅|📋|📱|📝|📊|📈")) {
                        label.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #2979FF;");
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Hover effect générique
    // ─────────────────────────────────────────────────────────────────────────
    private void addHoverEffect(HBox item) {
        if (item == null) return;
        item.setOnMouseEntered(e -> {
            if (!item.getStyle().contains("#E8F0FE"))
                item.setStyle(item.getStyle() + "-fx-background-color: #F5F5F5;");
        });
        item.setOnMouseExited(e -> {
            if (!item.getStyle().contains("#E8F0FE"))
                item.setStyle(item.getStyle().replace("-fx-background-color: #F5F5F5;", ""));
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // API publique pour autres contrôleurs
    // ─────────────────────────────────────────────────────────────────────────
    public void navigateTo(String viewName) {
        loadView(viewName);
    }
}