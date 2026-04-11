package edu.connexion3a36.Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DashboardController {

    private static final Logger LOGGER = Logger.getLogger(DashboardController.class.getName());

    @FXML private HBox homeItem;
    @FXML private HBox coursItem;
    @FXML private HBox chapitresItem;
    @FXML private HBox quizItem;
    @FXML private HBox exercicesItem;
    @FXML private HBox progressionItem;
    @FXML private HBox settingsItem;

    @FXML private StackPane contentArea;

    @FXML
    public void initialize() {
        System.out.println("DashboardController initialisé");
        setupNavigation();
        loadView("cours");
    }

    private void setupNavigation() {
        homeItem.setOnMouseClicked(event -> loadView("dashboard"));
        coursItem.setOnMouseClicked(event -> loadView("cours"));
        chapitresItem.setOnMouseClicked(event -> loadView("chapitres"));
        quizItem.setOnMouseClicked(event -> loadView("quiz"));
        exercicesItem.setOnMouseClicked(event -> loadView("exercices"));
        progressionItem.setOnMouseClicked(event -> loadView("progression"));
        settingsItem.setOnMouseClicked(event -> loadView("settings"));

        addHoverEffect(homeItem);
        addHoverEffect(coursItem);
        addHoverEffect(chapitresItem);
        addHoverEffect(quizItem);
        addHoverEffect(exercicesItem);
        addHoverEffect(progressionItem);
        addHoverEffect(settingsItem);
    }

    private void loadView(String viewName) {
        try {
            resetActiveStyles();
            setActiveStyle(viewName);

            // Correction du chemin - les ressources sont à la racine
            String resourcePath = "/" + viewName + ".fxml";
            System.out.println("Tentative de chargement: " + resourcePath);

            URL resourceUrl = getClass().getResource(resourcePath);

            if (resourceUrl == null) {
                System.err.println("❌ Fichier non trouvé: " + resourcePath);

                // Afficher tous les fichiers disponibles dans les ressources (debug)
                System.err.println("📁 Vérifiez que ces fichiers existent dans src/main/resources/ :");
                System.err.println("   - cours.fxml");
                System.err.println("   - add_course.fxml");
                System.err.println("   - chapitres.fxml");
                System.err.println("   - quiz.fxml");
                System.err.println("   - exercices.fxml");
                System.err.println("   - progression.fxml");
                System.err.println("   - settings.fxml");

                showErrorView(viewName);
                return;
            }

            System.out.println("✅ Fichier trouvé: " + resourceUrl);
            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Node view = loader.load();
            System.out.println("✅ Vue chargée avec succès: " + viewName);

            // Passer la référence du dashboard au contrôleur si nécessaire
            Object controller = loader.getController();
            if (controller != null) {
                System.out.println("Contrôleur chargé: " + controller.getClass().getSimpleName());
                try {
                    java.lang.reflect.Method method = controller.getClass().getMethod("setDashboardController", DashboardController.class);
                    method.invoke(controller, this);
                    System.out.println("✅ DashboardController injecté");
                } catch (NoSuchMethodException e) {
                    System.out.println("ℹ️ Pas de méthode setDashboardController");
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Erreur injection", e);
                }
            }

            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "❌ Erreur chargement: " + viewName, e);
            showErrorView(viewName);
        }
    }

    private void showErrorView(String viewName) {
        VBox errorBox = new VBox();
        errorBox.setStyle("-fx-alignment: center; -fx-padding: 40; -fx-spacing: 10;");
        Label errorLabel = new Label("⚠️ Vue non trouvée: " + viewName + ".fxml");
        Label infoLabel1 = new Label("Le fichier doit être dans: src/main/resources/");
        Label infoLabel2 = new Label("Vérifiez que le nom du fichier est correct (cours.fxml, chapitres.fxml, etc.)");
        infoLabel1.setStyle("-fx-text-fill: #757575; -fx-font-size: 12;");
        infoLabel2.setStyle("-fx-text-fill: #757575; -fx-font-size: 12;");
        errorLabel.setStyle("-fx-text-fill: #F44336; -fx-font-size: 14;");
        errorBox.getChildren().addAll(errorLabel, infoLabel1, infoLabel2);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(errorBox);
    }

    private void resetActiveStyles() {
        HBox[] items = {homeItem, coursItem, chapitresItem, quizItem, exercicesItem, progressionItem, settingsItem};
        for (HBox item : items) {
            if (item != null) {
                item.setStyle("-fx-background-color: transparent; -fx-background-radius: 8; -fx-padding: 0 12 0 12;");
                for (Node node : item.getChildren()) {
                    if (node instanceof Label) {
                        Label label = (Label) node;
                        String text = label.getText();
                        if (text != null && !text.matches("🏠|📚|📖|❓|✏️|📊|⚙️")) {
                            label.setStyle("-fx-font-size: 13; -fx-text-fill: #757575; -fx-font-weight: normal;");
                        }
                    }
                }
            }
        }
    }

    private void setActiveStyle(String viewName) {
        HBox activeItem = null;
        switch(viewName) {
            case "dashboard": activeItem = homeItem; break;
            case "cours": activeItem = coursItem; break;
            case "chapitres": activeItem = chapitresItem; break;
            case "quiz": activeItem = quizItem; break;
            case "exercices": activeItem = exercicesItem; break;
            case "progression": activeItem = progressionItem; break;
            case "settings": activeItem = settingsItem; break;
            default: break;
        }

        if (activeItem != null) {
            activeItem.setStyle("-fx-background-color: #E8F0FE; -fx-background-radius: 8; -fx-padding: 0 12 0 12;");
            for (Node node : activeItem.getChildren()) {
                if (node instanceof Label) {
                    Label label = (Label) node;
                    String text = label.getText();
                    if (text != null && !text.matches("🏠|📚|📖|❓|✏️|📊|⚙️")) {
                        label.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #2979FF;");
                    }
                }
            }
        }
    }

    private void addHoverEffect(HBox item) {
        if (item == null) return;
        item.setOnMouseEntered(e -> {
            if (!item.getStyle().contains("#E8F0FE")) {
                item.setStyle("-fx-background-color: #F5F5F5; -fx-background-radius: 8; -fx-padding: 0 12 0 12; -fx-cursor: hand;");
            }
        });
        item.setOnMouseExited(e -> {
            if (!item.getStyle().contains("#E8F0FE")) {
                item.setStyle("-fx-background-color: transparent; -fx-background-radius: 8; -fx-padding: 0 12 0 12;");
            }
        });
    }

    public void navigateTo(String viewName) {
        loadView(viewName);
    }
}