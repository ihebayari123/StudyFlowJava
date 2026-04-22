package edu.connexion3a36.tests;

public class TestOpenCV {
    public static void main(String[] args) {
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
        System.out.println("OpenCV version : " + org.opencv.core.Core.VERSION);
    }
}