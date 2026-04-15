package edu.connexion3a36.controllers;

import edu.connexion3a36.models.Sponsor;
import edu.connexion3a36.services.SponsorService;
import edu.connexion3a36.services.EventService;
import edu.connexion3a36.models.Event;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class SponsorController implements Initializable {

    // ══════════════════════════════════════════════════════
    //  CHAMPS FXML
    // ══════════════════════════════════════════════════════

    @FXML private TextField      nomSponsorField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField      montantField;
    @FXML private ComboBox<String> eventCombo;
    @FXML private Label          messageLabel;

    @FXML private TableView<Sponsor>             sponsorTable;
    @FXML private TableColumn<Sponsor, Integer>  colId;
    @FXML private TableColumn<Sponsor, String>   colNom;
    @FXML private TableColumn<Sponsor, String>   colType;
    @FXML private TableColumn<Sponsor, Integer>  colMontant;
    @FXML private TableColumn<Sponsor, String>   colEvent;

    // ══════════════════════════════════════════════════════
    //  SERVICES ET DONNÉES
    // ══════════════════════════════════════════════════════

    private final SponsorService sponsorService = new SponsorService();
    private final EventService   eventService   = new EventService();
    private final ObservableList<Sponsor> sponsorList = FXCollections.observableArrayList();
    private final ObservableList<Event>   eventList   = FXCollections.observableArrayList();
    private Sponsor sponsorSelectionne = null;

    // ══════════════════════════════════════════════════════
    //  STYLES
    // ══════════════════════════════════════════════════════

    private static final String STYLE_FIELD_NORMAL =
            "-fx-pref-height: 38; -fx-background-radius: 8; " +
                    "-fx-border-color: #e0e0e0; -fx-border-radius: 8; " +
                    "-fx-padding: 0 12; -fx-font-size: 13px;";

    private static final String STYLE_FIELD_ERROR =
            "-fx-pref-height: 38; -fx-background-radius: 8; " +
                    "-fx-border-color: #e74c3c; -fx-border-width: 2; " +
                    "-fx-border-radius: 8; -fx-padding: 0 12; -fx-font-size: 13px;";

    private static final String STYLE_FIELD_WARNING =
            "-fx-pref-height: 38; -fx-background-radius: 8; " +
                    "-fx-border-color: #e67e22; -fx-border-width: 2; " +
                    "-fx-border-radius: 8; -fx-padding: 0 12; -fx-font-size: 13px;";

    private static final String STYLE_COMBO_NORMAL =
            "-fx-pref-width: 200; -fx-pref-height: 38;";

    private static final String STYLE_COMBO_ERROR =
            "-fx-pref-width: 200; -fx-pref-height: 38; " +
                    "-fx-border-color: #e74c3c; -fx-border-width: 2; -fx-border-radius: 8;";

    // ══════════════════════════════════════════════════════
    //  INITIALIZE
    // ══════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // ── Types de sponsors disponibles
        typeCombo.setItems(FXCollections.observableArrayList(
                "Or", "Argent", "Bronze", "Platine", "Partenaire"
        ));

        // ── Charger les événements dans le ComboBox
        chargerEvenementsDansCombo();

        // ── Configurer les colonnes du tableau
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nomSponsor"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montant"));

        // Colonne Event : afficher le titre de l'événement au lieu de l'id
        colEvent.setCellValueFactory(cellData -> {
            int eventId = cellData.getValue().getEventTitreId();
            for (Event e : eventList) {
                if (e.getId() == eventId) {
                    return new SimpleStringProperty(e.getTitre());
                }
            }
            return new SimpleStringProperty("Événement #" + eventId);
        });

        // ── Style colonne ID
        colId.setCellFactory(col -> new TableCell<Sponsor, Integer>() {
            @Override
            protected void updateItem(Integer id, boolean empty) {
                super.updateItem(id, empty);
                if (empty || id == null) { setText(null); }
                else {
                    setText(String.valueOf(id));
                    setStyle("-fx-alignment: CENTER; -fx-font-weight: bold; -fx-text-fill: #2979FF;");
                }
            }
        });

        // ── Style colonne Nom
        colNom.setCellFactory(col -> new TableCell<Sponsor, String>() {
            @Override
            protected void updateItem(String nom, boolean empty) {
                super.updateItem(nom, empty);
                if (empty || nom == null) { setText(null); }
                else {
                    setText(nom);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #1a1a2e; -fx-font-size: 13px;");
                }
            }
        });

        // ── Style colonne Type avec badge coloré
        colType.setCellFactory(col -> new TableCell<Sponsor, String>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) {
                    setText(null); setGraphic(null);
                } else {
                    String color;
                    switch (type.toLowerCase()) {
                        case "or":        color = "#f39c12"; break;
                        case "argent":    color = "#95a5a6"; break;
                        case "bronze":    color = "#e67e22"; break;
                        case "platine":   color = "#2979FF"; break;
                        default:          color = "#27ae60"; break;
                    }
                    Label badge = new Label(type.toUpperCase());
                    badge.setStyle(
                            "-fx-background-color: " + color + "22;" +
                                    "-fx-text-fill: " + color + ";" +
                                    "-fx-font-size: 10px; -fx-font-weight: bold;" +
                                    "-fx-padding: 3 10; -fx-background-radius: 10;"
                    );
                    setGraphic(badge);
                    setText(null);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });

        // ── Style colonne Montant
        colMontant.setCellFactory(col -> new TableCell<Sponsor, Integer>() {
            @Override
            protected void updateItem(Integer montant, boolean empty) {
                super.updateItem(montant, empty);
                if (empty || montant == null) { setText(null); }
                else {
                    setText("💰 " + montant + " DT");
                    setStyle("-fx-alignment: CENTER; -fx-text-fill: #27ae60; " +
                            "-fx-font-weight: bold; -fx-font-size: 12px;");
                }
            }
        });

        // ── Style colonne Événement
        colEvent.setCellFactory(col -> new TableCell<Sponsor, String>() {
            @Override
            protected void updateItem(String titre, boolean empty) {
                super.updateItem(titre, empty);
                if (empty || titre == null) { setText(null); }
                else {
                    setText("🎉 " + titre);
                    setStyle("-fx-text-fill: #555; -fx-font-size: 12px;");
                }
            }
        });

        // ── Lignes alternées
        sponsorTable.setRowFactory(tv -> {
            TableRow<Sponsor> row = new TableRow<>();
            row.setPrefHeight(55);
            row.styleProperty().bind(
                    Bindings.when(
                                    Bindings.createBooleanBinding(
                                            () -> row.getIndex() % 2 == 0,
                                            row.indexProperty()
                                    ))
                            .then("-fx-background-color: white;")
                            .otherwise("-fx-background-color: #f8faff;")
            );
            row.setOnMouseEntered(e -> {
                if (!row.isSelected())
                    row.setStyle("-fx-background-color: #EEF4FF;");
            });
            row.setOnMouseExited(e -> {
                if (!row.isSelected()) {
                    row.styleProperty().bind(
                            Bindings.when(
                                            Bindings.createBooleanBinding(
                                                    () -> row.getIndex() % 2 == 0,
                                                    row.indexProperty()
                                            ))
                                    .then("-fx-background-color: white;")
                                    .otherwise("-fx-background-color: #f8faff;")
                    );
                }
            });
            return row;
        });

        // ── Clic sur une ligne → remplir le formulaire
        sponsorTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) remplirFormulaire(newVal);
                }
        );

        chargerSponsors();
    }

    // ══════════════════════════════════════════════════════
    //  CHARGER ÉVÉNEMENTS DANS LE COMBOBOX
    // ══════════════════════════════════════════════════════

    private void chargerEvenementsDansCombo() {
        try {
            List<Event> liste = eventService.recupererTous();
            eventList.setAll(liste);

            // On affiche les titres dans le ComboBox
            ObservableList<String> titres = FXCollections.observableArrayList();
            for (Event e : liste) {
                titres.add(e.getId() + " - " + e.getTitre());
            }
            eventCombo.setItems(titres);

        } catch (SQLException e) {
            afficherErreur("❌ Erreur chargement événements : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    //  CHARGER SPONSORS
    // ══════════════════════════════════════════════════════

    private void chargerSponsors() {
        try {
            List<Sponsor> liste = sponsorService.recupererTous();
            sponsorList.setAll(liste);
            sponsorTable.setItems(sponsorList);
        } catch (SQLException e) {
            afficherErreur("❌ Erreur chargement : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    //  AJOUTER
    // ══════════════════════════════════════════════════════

    @FXML
    private void handleAjouter() {
        if (!validerFormulaire()) return;

        String nom = nomSponsorField.getText().trim();

        // Vérification unicité du nom
        for (Sponsor s : sponsorList) {
            if (s.getNomSponsor().equalsIgnoreCase(nom)) {
                nomSponsorField.setStyle(STYLE_FIELD_WARNING);
                afficherErreur("⚠️ Un sponsor avec ce nom existe déjà !");
                return;
            }
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation d'ajout");
        confirm.setHeaderText(null);
        confirm.setContentText("Voulez-vous vraiment ajouter le sponsor « " + nom + " » ?");
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                Sponsor sponsor = new Sponsor(
                        nom,
                        typeCombo.getValue(),
                        Integer.parseInt(montantField.getText().trim()),
                        getEventIdSelectionne()
                );
                try {
                    sponsorService.ajouter(sponsor);
                    afficherSucces("✅ Sponsor ajouté avec succès !");
                    viderFormulaire();
                    chargerSponsors();
                } catch (SQLException ex) {
                    afficherErreur("❌ Erreur ajout : " + ex.getMessage());
                }
            }
        });
    }

    // ══════════════════════════════════════════════════════
    //  MODIFIER
    // ══════════════════════════════════════════════════════

    @FXML
    private void handleModifier() {
        if (sponsorSelectionne == null) {
            afficherErreur("⚠️ Sélectionne un sponsor dans le tableau d'abord !");
            return;
        }
        if (!validerFormulaire()) return;

        String nom = nomSponsorField.getText().trim();

        // Vérification unicité (exclure le sponsor en cours)
        for (Sponsor s : sponsorList) {
            if (s.getNomSponsor().equalsIgnoreCase(nom) &&
                    s.getId() != sponsorSelectionne.getId()) {
                nomSponsorField.setStyle(STYLE_FIELD_WARNING);
                afficherErreur("⚠️ Un autre sponsor avec ce nom existe déjà !");
                return;
            }
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de modification");
        confirm.setHeaderText(null);
        confirm.setContentText("Voulez-vous vraiment modifier « " +
                sponsorSelectionne.getNomSponsor() + " » ?");
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                sponsorSelectionne.setNomSponsor(nom);
                sponsorSelectionne.setType(typeCombo.getValue());
                sponsorSelectionne.setMontant(Integer.parseInt(montantField.getText().trim()));
                sponsorSelectionne.setEventTitreId(getEventIdSelectionne());

                try {
                    sponsorService.modifier(sponsorSelectionne);
                    afficherSucces("✅ Sponsor modifié avec succès !");
                    viderFormulaire();
                    chargerSponsors();
                } catch (SQLException ex) {
                    afficherErreur("❌ Erreur modification : " + ex.getMessage());
                }
            }
        });
    }

    // ══════════════════════════════════════════════════════
    //  SUPPRIMER
    // ══════════════════════════════════════════════════════

    @FXML
    private void handleSupprimer() {
        if (sponsorSelectionne == null) {
            afficherErreur("⚠️ Sélectionne un sponsor dans le tableau d'abord !");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de suppression");
        confirm.setHeaderText(null);
        confirm.setContentText("Voulez-vous vraiment supprimer « " +
                sponsorSelectionne.getNomSponsor() + " » ?\nCette action est irréversible.");
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    sponsorService.supprimer(sponsorSelectionne.getId());
                    afficherSucces("✅ Sponsor supprimé !");
                    viderFormulaire();
                    chargerSponsors();
                } catch (SQLException e) {
                    afficherErreur("❌ Erreur suppression : " + e.getMessage());
                }
            }
        });
    }

    // ══════════════════════════════════════════════════════
    //  VIDER
    // ══════════════════════════════════════════════════════

    @FXML
    private void handleVider() {
        viderFormulaire();
    }

    // ══════════════════════════════════════════════════════
    //  RETOUR
    // ══════════════════════════════════════════════════════

    @FXML
    private void handleRetour() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/sponsorList.fxml"));
            Node page = loader.load();
            StackPane contentArea = (StackPane) nomSponsorField.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(page);
            }
        } catch (Exception e) {
            System.err.println("Erreur retour : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    //  REMPLIR FORMULAIRE
    // ══════════════════════════════════════════════════════

    private void remplirFormulaire(Sponsor s) {
        sponsorSelectionne = s;
        nomSponsorField.setText(s.getNomSponsor());
        typeCombo.setValue(s.getType());
        montantField.setText(String.valueOf(s.getMontant()));
        messageLabel.setText("");
        resetStyles();

        // Sélectionner l'événement correspondant dans le ComboBox
        for (String item : eventCombo.getItems()) {
            if (item.startsWith(s.getEventTitreId() + " - ")) {
                eventCombo.setValue(item);
                break;
            }
        }
    }

    // ══════════════════════════════════════════════════════
    //  VIDER FORMULAIRE
    // ══════════════════════════════════════════════════════

    private void viderFormulaire() {
        sponsorSelectionne = null;
        nomSponsorField.clear();
        typeCombo.setValue(null);
        montantField.clear();
        eventCombo.setValue(null);
        messageLabel.setText("");
        sponsorTable.getSelectionModel().clearSelection();
        resetStyles();
    }

    // ══════════════════════════════════════════════════════
    //  RESET STYLES
    // ══════════════════════════════════════════════════════

    private void resetStyles() {
        nomSponsorField.setStyle(STYLE_FIELD_NORMAL);
        montantField.setStyle(STYLE_FIELD_NORMAL);
        typeCombo.setStyle(STYLE_COMBO_NORMAL);
        eventCombo.setStyle(STYLE_COMBO_NORMAL);
    }

    // ══════════════════════════════════════════════════════
    //  RÉCUPÉRER L'ID DE L'ÉVÉNEMENT SÉLECTIONNÉ
    // ══════════════════════════════════════════════════════

    private int getEventIdSelectionne() {
        String selected = eventCombo.getValue();
        if (selected != null && selected.contains(" - ")) {
            return Integer.parseInt(selected.split(" - ")[0].trim());
        }
        return 0;
    }

    // ══════════════════════════════════════════════════════
    //  VALIDATION COMPLÈTE
    // ══════════════════════════════════════════════════════

    private boolean validerFormulaire() {
        boolean valide = true;
        StringBuilder erreurs = new StringBuilder();

        resetStyles();

        // ── 1. Nom sponsor obligatoire
        if (nomSponsorField.getText().trim().isEmpty()) {
            nomSponsorField.setStyle(STYLE_FIELD_ERROR);
            erreurs.append("• Le nom du sponsor est obligatoire\n");
            valide = false;
        } else {
            // ── 2. Nom commence par une majuscule
            if (!Character.isUpperCase(nomSponsorField.getText().trim().charAt(0))) {
                nomSponsorField.setStyle(STYLE_FIELD_WARNING);
                erreurs.append("• Le nom doit commencer par une majuscule\n");
                valide = false;
            }
            // ── 3. Nom au moins 3 caractères
            if (nomSponsorField.getText().trim().length() < 3) {
                nomSponsorField.setStyle(STYLE_FIELD_WARNING);
                erreurs.append("• Le nom doit contenir au moins 3 caractères\n");
                valide = false;
            }
        }

        // ── 4. Type obligatoire
        if (typeCombo.getValue() == null) {
            typeCombo.setStyle(STYLE_COMBO_ERROR);
            erreurs.append("• Le type est obligatoire\n");
            valide = false;
        }

        // ── 5. Montant obligatoire
        if (montantField.getText().trim().isEmpty()) {
            montantField.setStyle(STYLE_FIELD_ERROR);
            erreurs.append("• Le montant est obligatoire\n");
            valide = false;
        } else {
            try {
                int montant = Integer.parseInt(montantField.getText().trim());
                // ── 6. Montant doit être positif
                if (montant <= 0) {
                    montantField.setStyle(STYLE_FIELD_WARNING);
                    erreurs.append("• Le montant doit être un nombre positif\n");
                    valide = false;
                }
            } catch (NumberFormatException e) {
                montantField.setStyle(STYLE_FIELD_WARNING);
                erreurs.append("• Le montant doit être un nombre entier\n");
                valide = false;
            }
        }

        // ── 7. Événement obligatoire
        if (eventCombo.getValue() == null) {
            eventCombo.setStyle(STYLE_COMBO_ERROR);
            erreurs.append("• Vous devez choisir un événement\n");
            valide = false;
        }

        if (!valide) {
            afficherErreur("⚠️ Veuillez corriger :\n" + erreurs.toString().trim());
        }

        return valide;
    }

    // ══════════════════════════════════════════════════════
    //  MESSAGES
    // ══════════════════════════════════════════════════════

    private void afficherErreur(String msg) {
        messageLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px; -fx-font-weight: bold;");
        messageLabel.setText(msg);
    }

    private void afficherSucces(String msg) {
        messageLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 13px; -fx-font-weight: bold;");
        messageLabel.setText(msg);
    }
}