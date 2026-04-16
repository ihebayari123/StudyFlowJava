package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Utilisateur;
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

    private Utilisateur utilisateurConnecte;

    // ── Sidebar items principaux ──────────────────────────────────────────────
    @FXML private HBox homeItem;
    @FXML private HBox coursItem;
    @FXML private HBox chapitresItem;
    @FXML private HBox quizItem;
    @FXML private HBox exercicesItem;
    @FXML private HBox categorieItem;
    @FXML private HBox produitItem;
    @FXML private HBox progressionItem;
    @FXML private HBox administrationItem;
    @FXML private HBox settingsItem;
    @FXML private HBox utilisateursItem;

    // ── Sous-menu Anti-Stress ─────────────────────────────────────────────────
    @FXML private VBox antiStressSubMenu;
    @FXML private Label antiStressArrow;

    @FXML private HBox ajouterMedecinItem;
    @FXML private HBox listeMedecinsItem;
    @FXML private HBox ajouterConsultationItem;
    @FXML private HBox listeConsultationsItem;
    @FXML private HBox antiStresseItem;
    @FXML private HBox ajouterScoreEtudiantItem;
    @FXML private HBox ajouterBienEtreItem;
    @FXML private HBox voirScoreItem;
    @FXML private HBox deconnexionItem;

    @FXML private StackPane contentArea;

    private boolean antiStressMenuOpen = false;

    @FXML
    public void initialize() {
        System.out.println("DashboardController initialisé");
        setupNavigation();
        loadView("cours");
    }

    private void setupNavigation() {

        homeItem.setOnMouseClicked(e -> loadView("dashboard"));
        coursItem.setOnMouseClicked(e -> loadView("cours"));
        chapitresItem.setOnMouseClicked(e -> loadView("chapitres"));
        quizItem.setOnMouseClicked(e -> loadView("QuizView"));
        exercicesItem.setOnMouseClicked(e -> loadView("exercices"));
        categorieItem.setOnMouseClicked(e -> loadView("categorieMenu"));
        produitItem.setOnMouseClicked(e -> loadView("produitMenu"));
        administrationItem.setOnMouseClicked(e -> loadView("admin"));
        settingsItem.setOnMouseClicked(e -> loadView("settings"));
        utilisateursItem.setOnMouseClicked(e -> loadView("gestionUtilisateurs"));

        // Toggle Anti-Stress
        progressionItem.setOnMouseClicked(e -> toggleAntiStressMenu());

        // Sous-menu Anti-Stress
        ajouterMedecinItem.setOnMouseClicked(e -> loadView("AjouterMedecin"));
        listeMedecinsItem.setOnMouseClicked(e -> loadView("AfficherMedecin"));
        ajouterConsultationItem.setOnMouseClicked(e -> loadView("AjouterConsultation"));
        listeConsultationsItem.setOnMouseClicked(e -> loadView("AfficherConsultation"));
        antiStresseItem.setOnMouseClicked(e -> loadView("AfficherStressSurvey"));
        ajouterScoreEtudiantItem.setOnMouseClicked(e -> loadView("AjouterStressSurvey"));
        ajouterBienEtreItem.setOnMouseClicked(e -> loadView("AjouterWellBeingScore"));
        voirScoreItem.setOnMouseClicked(e -> loadView("AfficherWellBeingScore"));
        deconnexionItem.setOnMouseClicked(e -> handleDeconnexion());

        HBox[] allItems = {
                homeItem, coursItem, chapitresItem, quizItem, exercicesItem,
                categorieItem, produitItem, progressionItem,
                administrationItem, settingsItem, utilisateursItem,
                ajouterMedecinItem, listeMedecinsItem, ajouterConsultationItem,
                listeConsultationsItem, antiStresseItem,
                ajouterScoreEtudiantItem, ajouterBienEtreItem, voirScoreItem
        };

        for (HBox item : allItems) {
            if (item != null) addHoverEffect(item);
        }
    }

    private void toggleAntiStressMenu() {
        antiStressMenuOpen = !antiStressMenuOpen;

        antiStressSubMenu.setVisible(antiStressMenuOpen);
        antiStressSubMenu.setManaged(antiStressMenuOpen);

        if (antiStressArrow != null) {
            antiStressArrow.setText(antiStressMenuOpen ? "▼" : "▶");
        }
    }

    private void loadView(String viewName) {
        try {
            resetActiveStyles();
            setActiveStyle(viewName);

            if (viewName.equals("studyflow")) return;

            String resourcePath = "/" + viewName + ".fxml";
            URL resourceUrl = getClass().getResource(resourcePath);

            if (resourceUrl == null) {
                showErrorView(viewName);
                return;
            }

            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Node view = loader.load();

            Object controller = loader.getController();
            if (controller != null) {
                try {
                    controller.getClass()
                            .getMethod("setDashboardController", DashboardController.class)
                            .invoke(controller, this);
                } catch (Exception ignored) {}
            }

            contentArea.getChildren().setAll(view);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur chargement", e);
            showErrorView(viewName);
        }
    }

    private void handleDeconnexion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur déconnexion", e);
        }
    }

    private void showErrorView(String viewName) {
        VBox box = new VBox();
        box.setStyle("-fx-alignment: center; -fx-padding: 40;");
        Label label = new Label("Vue non trouvée : " + viewName);
        box.getChildren().add(label);
        contentArea.getChildren().setAll(box);
    }

    private void resetActiveStyles() {
        HBox[] items = {homeItem, coursItem, chapitresItem, quizItem,
                exercicesItem, categorieItem, produitItem,
                progressionItem, administrationItem, settingsItem, utilisateursItem};

        for (HBox item : items) {
            if (item != null)
                item.setStyle("-fx-background-color: transparent;");
        }
    }

    private void setActiveStyle(String viewName) {
        // simple version
    }

    private void addHoverEffect(HBox item) {
        if (item == null) return;

        item.setOnMouseEntered(e ->
                item.setStyle("-fx-background-color: #F5F5F5;"));

        item.setOnMouseExited(e ->
                item.setStyle("-fx-background-color: transparent;"));
    }

    public void navigateTo(String viewName) {
        loadView(viewName);
    }

    public void setUtilisateurConnecte(Utilisateur u) {
        this.utilisateurConnecte = u;

        // ═══════════════════════════════
        // MASQUER SELON LE RÔLE
        // ═══════════════════════════════
        if (u.getRole().equals("ENSEIGNANT")) {
            // Cacher tout ce qui n'est pas pour l'enseignant
            utilisateursItem.setVisible(false);
            utilisateursItem.setManaged(false);

            administrationItem.setVisible(false);
            administrationItem.setManaged(false);

            categorieItem.setVisible(false);
            categorieItem.setManaged(false);

            produitItem.setVisible(false);
            produitItem.setManaged(false);

            settingsItem.setVisible(false);
            settingsItem.setManaged(false);

            homeItem.setVisible(false);
            homeItem.setManaged(false);
        }
    }
}