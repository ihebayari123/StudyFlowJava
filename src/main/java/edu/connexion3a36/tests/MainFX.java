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
        //FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fitness_dashboard2.fxml"));
        Parent root = loader.load();
        Scene sc = new Scene(root);
        primaryStage.setTitle("Fitness Dashboard");
        primaryStage.setScene(sc);
        primaryStage.show();
    }
}