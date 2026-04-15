package edu.connexion3a36.Controller;

import javafx.fxml.FXML;
import javafx.scene.layout.HBox;

public class CategorieMenuController {

    @FXML private HBox afficherBtn;
    @FXML private HBox ajouterBtn;

    private DashboardController dashboardController;

    public void setDashboardController(DashboardController controller) {
        this.dashboardController = controller;
    }

    @FXML
    public void initialize() {
        afficherBtn.setOnMouseClicked(e -> dashboardController.navigateTo("afficherCategorie"));
        ajouterBtn.setOnMouseClicked(e -> dashboardController.navigateTo("ajouterCategorie"));
    }
}