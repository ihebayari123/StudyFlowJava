package edu.connexion3a36.utils;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WebcamCaptureUtil {

    private VideoCapture capture;
    private ScheduledExecutorService timer;
    private boolean isRunning = false;

    // Démarre la webcam et affiche le flux dans un ImageView
    public void startCamera(ImageView imageView) {
        AppContext.setActiveWebcam(this); // ← ajoute cette ligne
        // Charger OpenCV
        System.load("C:\\opencv\\build\\java\\x64\\opencv_java490.dll");

        capture = new VideoCapture(0);
        if (!capture.isOpened()) {
            System.out.println("Webcam non détectée !");
            return;
        }

        isRunning = true;
        timer = Executors.newSingleThreadScheduledExecutor();
        timer.scheduleAtFixedRate(() -> {
            Mat frame = new Mat();
            if (capture.read(frame)) {
                WritableImage fxImage = matToWritableImage(frame);
                if (fxImage != null) {
                    Platform.runLater(() -> imageView.setImage(fxImage));
                }
            }
        }, 0, 33, TimeUnit.MILLISECONDS); // ~30 FPS
    }

    // Capture une photo et la sauvegarde
    public String capturePhoto() throws Exception {
        if (capture == null || !capture.isOpened()) {
            throw new Exception("Webcam non disponible");
        }

        Mat frame = new Mat();
        capture.read(frame);

        // Debug — afficher info image
        System.out.println("Channels: " + frame.channels());
        System.out.println("Type: " + frame.type());
        System.out.println("Size: " + frame.size());

        String path = FaceRecognitionUtil.getTempImagePath();
        Imgcodecs.imwrite(path, frame);

        // Debug — vérifier que le fichier existe
        System.out.println("Image sauvegardée : " + path);
        System.out.println("Fichier existe : " + new java.io.File(path).exists());

        return path;
    }

    // Arrête la webcam
    public void stopCamera() {
        isRunning = false;
        if (timer != null && !timer.isShutdown()) {
            timer.shutdown();
        }
        if (capture != null && capture.isOpened()) {
            capture.release();
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    // Convertit Mat OpenCV → WritableImage JavaFX
    private WritableImage matToWritableImage(Mat frame) {
        try {
            MatOfByte buffer = new MatOfByte();
            Imgcodecs.imencode(".png", frame, buffer);
            byte[] bytes = buffer.toArray();
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(bytes));
            return SwingFXUtils.toFXImage(bufferedImage, null);
        } catch (Exception e) {
            return null;
        }
    }
}

