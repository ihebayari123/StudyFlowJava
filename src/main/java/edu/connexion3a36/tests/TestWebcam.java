package edu.connexion3a36.tests;

import edu.connexion3a36.utils.WebcamCaptureUtil;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class TestWebcam extends Application {

    private WebcamCaptureUtil webcam = new WebcamCaptureUtil();

    @Override
    public void start(Stage stage) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(640);
        imageView.setFitHeight(480);
        imageView.setPreserveRatio(true);

        StackPane root = new StackPane(imageView);
        Scene scene = new Scene(root, 640, 480);

        stage.setTitle("Test Webcam 🎥");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> webcam.stopCamera());
        stage.show();

        webcam.startCamera(imageView);
    }

    public static void main(String[] args) {
        launch(args);
    }
}