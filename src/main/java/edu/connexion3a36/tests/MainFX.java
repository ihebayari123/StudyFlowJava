package edu.connexion3a36.tests;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainFX extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Charge le dashboard principal (studyflow.fxml)
        // Le DashboardController charge QuizView.fxml par défaut dans contentArea
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/studyflow.fxml"));
        Parent root = loader.load();

        Scene sc = new Scene(root, 1280, 720);
        primaryStage.setTitle("StudyFlow — Module Quiz & Questions");
        primaryStage.setScene(sc);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }
}
