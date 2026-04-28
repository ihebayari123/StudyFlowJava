package edu.connexion3a36.Controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import edu.connexion3a36.services.ChatbotService;

public class ChatbotController {

    @FXML private VBox messagesContainer;
    @FXML private TextField inputField;
    @FXML private ScrollPane scrollPane;

    private final ChatbotService chatbotService = new ChatbotService();

    @FXML
    public void initialize() {
        ajouterMessageBot("Bonjour ! 👋 Je suis votre assistant StudyFlow. Comment puis-je vous aider ?");
    }

    @FXML
    private void envoyerMessage() {
        String texte = inputField.getText().trim();
        if (texte.isEmpty()) return;

        ajouterMessageUtilisateur(texte);
        inputField.clear();

        // Message "typing"
        HBox typingBubble = creerBulleBot("⏳ En train d'écrire...");
        messagesContainer.getChildren().add(typingBubble);
        scrollPane.setVvalue(1.0);

        // Appel API dans un thread séparé
        new Thread(() -> {
            String reponse = chatbotService.envoyerMessage(texte);
            Platform.runLater(() -> {
                messagesContainer.getChildren().remove(typingBubble);
                ajouterMessageBot(reponse);
                scrollPane.setVvalue(1.0);
            });
        }).start();
    }

    private void ajouterMessageUtilisateur(String texte) {
        HBox conteneur = new HBox();
        conteneur.setAlignment(Pos.CENTER_RIGHT);

        Label label = new Label(texte);
        label.setWrapText(true);
        label.setMaxWidth(280);
        label.setStyle(
                "-fx-background-color: #2c3e7a; -fx-text-fill: white;" +
                        "-fx-padding: 10 14; -fx-background-radius: 18 18 4 18;" +
                        "-fx-font-size: 13;"
        );

        HBox.setMargin(label, new Insets(0, 0, 0, 80));
        conteneur.getChildren().add(label);
        messagesContainer.getChildren().add(conteneur);
        scrollPane.setVvalue(1.0);
    }

    private HBox creerBulleBot(String texte) {
        HBox conteneur = new HBox();
        conteneur.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(texte);
        label.setWrapText(true);
        label.setMaxWidth(280);
        label.setStyle(
                "-fx-background-color: white; -fx-text-fill: #333333;" +
                        "-fx-padding: 10 14; -fx-background-radius: 18 18 18 4;" +
                        "-fx-font-size: 13;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0, 0, 1);"
        );

        HBox.setMargin(label, new Insets(0, 80, 0, 0));
        conteneur.getChildren().add(label);
        return conteneur;
    }

    private void ajouterMessageBot(String texte) {
        messagesContainer.getChildren().add(creerBulleBot(texte));
        scrollPane.setVvalue(1.0);
    }
}