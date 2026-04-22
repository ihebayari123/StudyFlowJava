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

    // ── Ta version (gestion_event) ────────────────────────────────────────────
    @FXML private HBox eventsItem;
    @FXML private HBox sponsorsItem;

    // ── Version main ──────────────────────────────────────────────────────────
    @FXML private HBox categorieItem;
    @FXML private HBox produitItem;
    @FXML private HBox administrationItem;
    @FXML private HBox utilisateursItem;

    // ── Commun ────────────────────────────────────────────────────────────────
    @FXML private HBox progressionItem;
    @FXML private HBox settingsItem;

    // ── Sous-menu Anti-Stress (main) ──────────────────────────────────────────
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
        // ── Commun aux deux versions ──
        homeItem.setOnMouseClicked(e       -> loadView("dashboard"));
        coursItem.setOnMouseClicked(e      -> loadView("cours"));
        chapitresItem.setOnMouseClicked(e  -> loadView("chapitres"));
        quizItem.setOnMouseClicked(e       -> loadView("QuizView"));
        exercicesItem.setOnMouseClicked(e  -> loadView("exercices"));
        settingsItem.setOnMouseClicked(e   -> loadView("settings"));

        // ── Ta version (gestion_event) ──
        if (eventsItem != null)
            eventsItem.setOnMouseClicked(e -> loadView("eventList"));
        if (sponsorsItem != null)
            sponsorsItem.setOnMouseClicked(e -> loadView("sponsor"));

        // ── Version main ──
        if (categorieItem != null)
            categorieItem.setOnMouseClicked(e -> loadView("categorieMenu"));
        if (produitItem != null)
            produitItem.setOnMouseClicked(e -> loadView("produitMenu"));
        if (administrationItem != null)
            administrationItem.setOnMouseClicked(e -> loadView("admin"));
        if (utilisateursItem != null)
            utilisateursItem.setOnMouseClicked(e -> loadView("gestionUtilisateurs"));

        // ── Anti-Stress toggle ──
        progressionItem.setOnMouseClicked(e -> toggleAntiStressMenu());

        // ── Sous-menu Anti-Stress ──
        if (ajouterMedecinItem != null)
            ajouterMedecinItem.setOnMouseClicked(e -> loadView("AjouterMedecin"));
        if (listeMedecinsItem != null)
            listeMedecinsItem.setOnMouseClicked(e -> loadView("AfficherMedecin"));
        if (ajouterConsultationItem != null)
            ajouterConsultationItem.setOnMouseClicked(e -> loadView("AjouterConsultation"));
        if (listeConsultationsItem != null)
            listeConsultationsItem.setOnMouseClicked(e -> loadView("AfficherConsultation"));
        if (antiStresseItem != null)
            antiStresseItem.setOnMouseClicked(e -> loadView("AfficherStressSurvey"));
        if (ajouterScoreEtudiantItem != null)
            ajouterScoreEtudiantItem.setOnMouseClicked(e -> loadView("AjouterStressSurvey"));
        if (ajouterBienEtreItem != null)
            ajouterBienEtreItem.setOnMouseClicked(e -> loadView("AjouterWellBeingScore"));
        if (voirScoreItem != null)
            voirScoreItem.setOnMouseClicked(e -> loadView("AfficherWellBeingScore"));
        if (deconnexionItem != null)
            deconnexionItem.setOnMouseClicked(e -> handleDeconnexion());

        // ── Hover effects ──
        HBox[] allItems = {
                homeItem, coursItem, chapitresItem, quizItem, exercicesItem,
                eventsItem, sponsorsItem,
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
        if (antiStressSubMenu != null) {
            antiStressSubMenu.setVisible(antiStressMenuOpen);
            antiStressSubMenu.setManaged(antiStressMenuOpen);
        }
        if (antiStressArrow != null) {
            antiStressArrow.setText(antiStressMenuOpen ? "▼" : "▶");
        }
    }

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
                System.err.println("Fichier non trouvé: " + resourcePath);
                showErrorView(viewName);
                return;
            }

            System.out.println("Chargement: " + resourcePath);
            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Node view = loader.load();

            Object controller = loader.getController();
            if (controller != null) {
                // Essai générique (main)
                try {
                    controller.getClass()
                            .getMethod("setDashboardController", DashboardController.class)
                            .invoke(controller, this);
                } catch (Exception ignored) {}

                // Essai spécifique CoursController (ta version)
                if (controller instanceof CoursController) {
                    ((CoursController) controller).setDashboardController(this);
                }
            }

            contentArea.getChildren().setAll(view);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur chargement: " + viewName, e);
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
        VBox errorBox = new VBox();
        errorBox.setStyle("-fx-alignment: center; -fx-padding: 40;");
        Label errorLabel = new Label(
                "⚠️ Vue non trouvée: " + viewName + ".fxml\n\n" +
                        "Vérifiez que le fichier existe dans resources/"
        );
        errorLabel.setStyle("-fx-text-fill: #F44336; -fx-font-size: 14;");
        errorBox.getChildren().add(errorLabel);
        contentArea.getChildren().setAll(errorBox);
    }

    private void resetActiveStyles() {
        HBox[] items = {
                homeItem, coursItem, chapitresItem, quizItem,
                exercicesItem, eventsItem, sponsorsItem,
                categorieItem, produitItem, progressionItem,
                administrationItem, settingsItem, utilisateursItem
        };
        for (HBox item : items) {
            if (item != null) {
                item.setStyle(
                        "-fx-background-color: transparent; " +
                                "-fx-background-radius: 8; " +
                                "-fx-padding: 0 12 0 12;"
                );
                for (Node node : item.getChildren()) {
                    if (node instanceof Label) {
                        Label label = (Label) node;
                        String text = label.getText();
                        if (text != null && !text.matches("🏠|📚|📖|❓|✏️|🎉|💼|📊|⚙️|🏷️|🛒|🏛️|👤")) {
                            label.setStyle(
                                    "-fx-font-size: 13; " +
                                            "-fx-text-fill: #757575; " +
                                            "-fx-font-weight: normal;"
                            );
                        }
                    }
                }
            }
        }
    }

    private void setActiveStyle(String viewName) {
        HBox activeItem = null;
        switch (viewName) {
            case "dashboard":           activeItem = homeItem;           break;
            case "cours":               activeItem = coursItem;          break;
            case "chapitres":           activeItem = chapitresItem;      break;
            case "QuizView":            activeItem = quizItem;           break;
            case "exercices":           activeItem = exercicesItem;      break;
            case "eventList":           activeItem = eventsItem;         break;
            case "sponsor":             activeItem = sponsorsItem;       break;
            case "categorieMenu":       activeItem = categorieItem;      break;
            case "produitMenu":         activeItem = produitItem;        break;
            case "admin":               activeItem = administrationItem; break;
            case "gestionUtilisateurs": activeItem = utilisateursItem;   break;
            case "settings":            activeItem = settingsItem;       break;
            default: break;
        }

        if (activeItem != null) {
            activeItem.setStyle(
                    "-fx-background-color: #E8F0FE; " +
                            "-fx-background-radius: 8; " +
                            "-fx-padding: 0 12 0 12;"
            );
            for (Node node : activeItem.getChildren()) {
                if (node instanceof Label) {
                    Label label = (Label) node;
                    String text = label.getText();
                    if (text != null && !text.matches("🏠|📚|📖|❓|✏️|🎉|💼|📊|⚙️|🏷️|🛒|🏛️|👤")) {
                        label.setStyle(
                                "-fx-font-size: 13; " +
                                        "-fx-font-weight: bold; " +
                                        "-fx-text-fill: #2979FF;"
                        );
                    }
                }
            }
        }
    }

    private void addHoverEffect(HBox item) {
        if (item == null) return;
        item.setOnMouseEntered(e -> {
            if (!item.getStyle().contains("#E8F0FE")) {
                item.setStyle(
                        "-fx-background-color: #F5F5F5; " +
                                "-fx-background-radius: 8; " +
                                "-fx-padding: 0 12 0 12; " +
                                "-fx-cursor: hand;"
                );
            }
        });
        item.setOnMouseExited(e -> {
            if (!item.getStyle().contains("#E8F0FE")) {
                item.setStyle(
                        "-fx-background-color: transparent; " +
                                "-fx-background-radius: 8; " +
                                "-fx-padding: 0 12 0 12;"
                );
            }
        });
    }

    public void navigateTo(String viewName) {
        loadView(viewName);
    }

    public void setUtilisateurConnecte(Utilisateur u) {
        this.utilisateurConnecte = u;

        if (u.getRole().equals("ENSEIGNANT")) {
            hideItem(utilisateursItem);
            hideItem(administrationItem);
            hideItem(categorieItem);
            hideItem(produitItem);
            hideItem(settingsItem);
            hideItem(homeItem);
        }
    }

    private void hideItem(HBox item) {
        if (item != null) {
            item.setVisible(false);
            item.setManaged(false);
        }
    }
}