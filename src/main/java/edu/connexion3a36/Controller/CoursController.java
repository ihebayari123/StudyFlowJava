package edu.connexion3a36.Controller;

import javafx.fxml.FXML;
import javafx.scene.layout.HBox;

public class CoursController {

    @FXML private HBox courseJava;
    @FXML private HBox courseSQL;
    @FXML private HBox courseWeb;

    private DashboardController dashboardController;

    @FXML
    public void initialize() {
        System.out.println("CoursController initialisé");
        setupCourseClicks();
    }

    public void setDashboardController(DashboardController controller) {
        this.dashboardController = controller;
    }

    private void setupCourseClicks() {
        if (courseJava != null) {
            courseJava.setOnMouseClicked(event -> {
                System.out.println("Cours Java sélectionné");
                if (dashboardController != null) {
                    dashboardController.navigateTo("chapitres");
                }
            });
        }

        if (courseSQL != null) {
            courseSQL.setOnMouseClicked(event -> {
                System.out.println("Cours SQL sélectionné");
                if (dashboardController != null) {
                    dashboardController.navigateTo("chapitres");
                }
            });
        }

        if (courseWeb != null) {
            courseWeb.setOnMouseClicked(event -> {
                System.out.println("Cours Web sélectionné");
                if (dashboardController != null) {
                    dashboardController.navigateTo("chapitres");
                }
            });
        }
    }
}