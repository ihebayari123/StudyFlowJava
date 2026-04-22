package edu.connexion3a36.utils;

public class AppContext {
    private static WebcamCaptureUtil activeWebcam = null;

    public static void setActiveWebcam(WebcamCaptureUtil webcam) {
        activeWebcam = webcam;
    }

    public static void stopAllCameras() {
        if (activeWebcam != null) {
            activeWebcam.stopCamera();
            activeWebcam = null;
        }
    }
}