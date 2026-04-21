package edu.connexion3a36.controllers;

import edu.connexion3a36.models.Event;
import edu.connexion3a36.services.EventService;
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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.ResourceBundle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import java.io.File;

public class EventController implements Initializable {

    @FXML private TextField titreField;
    @FXML private TextArea  descriptionArea;
    @FXML private ComboBox<String> typeCombo;
    @FXML private DatePicker datePicker;
    @FXML private TextField imageField;
    @FXML private ImageView imagePreview;
    @FXML private TextField userIdField;
    @FXML private Label     messageLabel;

    @FXML private TableView<Event>              eventTable;
    @FXML private TableColumn<Event, Integer>   colId;
    @FXML private TableColumn<Event, String>    colTitre;
    @FXML private TableColumn<Event, String>    colType;
    @FXML private TableColumn<Event, String>    colDate;
    @FXML private TableColumn<Event, String>    colDescription;
    @FXML private TableColumn<Event, Integer>   colUserId;
    @FXML private TableColumn<Event, String>    colImage;

    private final EventService eventService = new EventService();
    private final ObservableList<Event> eventList = FXCollections.observableArrayList();
    private Event eventSelectionne = null;
    private String imageChoisieNom = "";

    // ── Styles réutilisables
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

    private static final String STYLE_AREA_NORMAL =
            "-fx-background-radius: 8; -fx-border-color: #e0e0e0; " +
                    "-fx-border-radius: 8; -fx-font-size: 13px;";

    private static final String STYLE_AREA_ERROR =
            "-fx-background-radius: 8; -fx-border-color: #e74c3c; " +
                    "-fx-border-width: 2; -fx-border-radius: 8; -fx-font-size: 13px;";

    private static final String STYLE_AREA_WARNING =
            "-fx-background-radius: 8; -fx-border-color: #e67e22; " +
                    "-fx-border-width: 2; -fx-border-radius: 8; -fx-font-size: 13px;";

    private static final String STYLE_COMBO_NORMAL =
            "-fx-pref-width: 180; -fx-pref-height: 38;";

    private static final String STYLE_COMBO_ERROR =
            "-fx-pref-width: 180; -fx-pref-height: 38; " +
                    "-fx-border-color: #e74c3c; -fx-border-width: 2; -fx-border-radius: 8;";

    private static final String STYLE_DATE_NORMAL =
            "-fx-pref-width: 180; -fx-pref-height: 38;";

    private static final String STYLE_DATE_ERROR =
            "-fx-pref-width: 180; -fx-pref-height: 38; " +
                    "-fx-border-color: #e74c3c; -fx-border-width: 2; -fx-border-radius: 8;";

    private static final String STYLE_DATE_WARNING =
            "-fx-pref-width: 180; -fx-pref-height: 38; " +
                    "-fx-border-color: #e67e22; -fx-border-width: 2; -fx-border-radius: 8;";

    private static final String STYLE_USERID_NORMAL =
            "-fx-pref-width: 220; -fx-pref-height: 38; -fx-background-radius: 8; " +
                    "-fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-padding: 0 12;";

    private static final String STYLE_USERID_ERROR =
            "-fx-pref-width: 220; -fx-pref-height: 38; -fx-background-radius: 8; " +
                    "-fx-border-color: #e74c3c; -fx-border-width: 2; -fx-border-radius: 8; -fx-padding: 0 12;";

    private static final String STYLE_USERID_WARNING =
            "-fx-pref-width: 220; -fx-pref-height: 38; -fx-background-radius: 8; " +
                    "-fx-border-color: #e67e22; -fx-border-width: 2; -fx-border-radius: 8; -fx-padding: 0 12;";

    // ══════════════════════════════════════════════════════
    //  INITIALIZE
    // ══════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        typeCombo.setItems(FXCollections.observableArrayList(
                "education", "sport", "culture", "technologie", "autre"
        ));

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));

        colDate.setCellValueFactory(cellData -> {
            LocalDateTime dt = cellData.getValue().getDateCreation();
            String formatted = (dt != null) ? dt.toLocalDate().toString() : "";
            return new SimpleStringProperty(formatted);
        });

        colId.setCellFactory(col -> new TableCell<Event, Integer>() {
            @Override
            protected void updateItem(Integer id, boolean empty) {
                super.updateItem(id, empty);
                if (empty || id == null) {
                    setText(null);
                } else {
                    setText(String.valueOf(id));
                    setStyle("-fx-alignment: CENTER; -fx-font-weight: bold; -fx-text-fill: #2979FF;");
                }
            }
        });

        colType.setCellFactory(col -> new TableCell<Event, String>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String color;
                    switch (type.toLowerCase()) {
                        case "education":   color = "#2979FF"; break;
                        case "sport":       color = "#27ae60"; break;
                        case "culture":     color = "#9b59b6"; break;
                        case "technologie": color = "#e67e22"; break;
                        default:            color = "#95a5a6"; break;
                    }
                    Label badge = new Label(type.toUpperCase());
                    badge.setStyle(
                            "-fx-background-color: " + color + "22;" +
                                    "-fx-text-fill: " + color + ";" +
                                    "-fx-font-size: 10px;" +
                                    "-fx-font-weight: bold;" +
                                    "-fx-padding: 3 10;" +
                                    "-fx-background-radius: 10;"
                    );
                    setGraphic(badge);
                    setText(null);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });

        colDate.setCellFactory(col -> new TableCell<Event, String>() {
            @Override
            protected void updateItem(String date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null || date.isEmpty()) {
                    setText(null);
                } else {
                    setText("📅 " + date);
                    setStyle("-fx-alignment: CENTER; -fx-text-fill: #555; -fx-font-size: 12px;");
                }
            }
        });

        colTitre.setCellFactory(col -> new TableCell<Event, String>() {
            @Override
            protected void updateItem(String titre, boolean empty) {
                super.updateItem(titre, empty);
                if (empty || titre == null) {
                    setText(null);
                } else {
                    setText(titre);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #1a1a2e; -fx-font-size: 13px;");
                }
            }
        });

        colDescription.setCellFactory(col -> new TableCell<Event, String>() {
            @Override
            protected void updateItem(String desc, boolean empty) {
                super.updateItem(desc, empty);
                if (empty || desc == null) {
                    setText(null);
                } else {
                    setText(desc.length() > 50 ? desc.substring(0, 50) + "..." : desc);
                    setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
                    setTooltip(new Tooltip(desc));
                }
            }
        });

        colUserId.setCellFactory(col -> new TableCell<Event, Integer>() {
            @Override
            protected void updateItem(Integer userId, boolean empty) {
                super.updateItem(userId, empty);
                if (empty || userId == null) {
                    setText(null);
                } else {
                    setText("👤 " + userId);
                    setStyle("-fx-alignment: CENTER; -fx-text-fill: #888; -fx-font-size: 12px;");
                }
            }
        });

        colImage.setCellValueFactory(new PropertyValueFactory<>("image"));
        colImage.setCellFactory(col -> new TableCell<Event, String>() {
            private final ImageView iv = new ImageView();
            {
                iv.setFitWidth(55);
                iv.setFitHeight(42);
                iv.setPreserveRatio(false);
                Rectangle clip = new Rectangle(55, 42);
                clip.setArcWidth(8);
                clip.setArcHeight(8);
                iv.setClip(clip);
            }
            @Override
            protected void updateItem(String nomImage, boolean empty) {
                super.updateItem(nomImage, empty);
                setStyle("-fx-alignment: CENTER;");
                if (empty || nomImage == null || nomImage.isEmpty()) {
                    setGraphic(null);
                    setText(null);
                } else {
                    File f = new File("src/main/resources/images/" + nomImage);
                    if (f.exists()) {
                        iv.setImage(new Image(f.toURI().toString()));
                        setGraphic(iv);
                        setText(null);
                    } else {
                        setGraphic(null);
                        setText("🖼");
                        setStyle("-fx-alignment: CENTER; -fx-font-size: 20px;");
                    }
                }
            }
        });

        eventTable.setRowFactory(tv -> {
            TableRow<Event> row = new TableRow<>();
            row.setPrefHeight(65);
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

        eventTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) remplirFormulaire(newVal);
                }
        );

        chargerEvents();
    }

    // ══════════════════════════════════════════════════════
    //  CHARGEMENT
    // ══════════════════════════════════════════════════════

    private void chargerEvents() {
        try {
            List<Event> liste = eventService.recupererTous();
            eventList.setAll(liste);
            eventTable.setItems(eventList);
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

        // Vérification unicité du titre
        String nouveauTitre = titreField.getText().trim();
        for (Event e : eventList) {
            if (e.getTitre().equalsIgnoreCase(nouveauTitre)) {
                titreField.setStyle(STYLE_FIELD_WARNING);
                afficherErreur("⚠️ Un événement avec ce titre existe déjà !");
                return;
            }
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation d'ajout");
        confirm.setHeaderText(null);
        confirm.setContentText("Voulez-vous vraiment ajouter l'événement « " + nouveauTitre + " » ?");
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                Event event = new Event(
                        titreField.getText().trim(),
                        descriptionArea.getText().trim(),
                        LocalDateTime.of(datePicker.getValue(), LocalTime.NOON),
                        typeCombo.getValue(),
                        imageChoisieNom,
                        Integer.parseInt(userIdField.getText().trim())
                );
                try {
                    eventService.ajouter(event);
                    afficherSucces("✅ Événement ajouté avec succès !");
                    viderFormulaire();
                    chargerEvents();
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
        if (eventSelectionne == null) {
            afficherErreur("⚠️ Sélectionne un événement dans le tableau d'abord !");
            return;
        }
        if (!validerFormulaire()) return;

        // Vérification unicité du titre (exclure l'événement en cours)
        String nouveauTitre = titreField.getText().trim();
        for (Event e : eventList) {
            if (e.getTitre().equalsIgnoreCase(nouveauTitre) && e.getId() != eventSelectionne.getId()) {
                titreField.setStyle(STYLE_FIELD_WARNING);
                afficherErreur("⚠️ Un autre événement avec ce titre existe déjà !");
                return;
            }
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de modification");
        confirm.setHeaderText(null);
        confirm.setContentText("Voulez-vous vraiment modifier l'événement « " + eventSelectionne.getTitre() + " » ?");
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                eventSelectionne.setTitre(titreField.getText().trim());
                eventSelectionne.setDescription(descriptionArea.getText().trim());
                eventSelectionne.setDateCreation(LocalDateTime.of(datePicker.getValue(), LocalTime.NOON));
                eventSelectionne.setType(typeCombo.getValue());
                eventSelectionne.setImage(imageChoisieNom.isEmpty() ? eventSelectionne.getImage() : imageChoisieNom);
                eventSelectionne.setUserId(Integer.parseInt(userIdField.getText().trim()));

                try {
                    eventService.modifier(eventSelectionne);
                    afficherSucces("✅ Événement modifié avec succès !");
                    viderFormulaire();
                    chargerEvents();
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
        if (eventSelectionne == null) {
            afficherErreur("⚠️ Sélectionne un événement dans le tableau d'abord !");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de suppression");
        confirm.setHeaderText(null);
        confirm.setContentText("Voulez-vous vraiment supprimer « " + eventSelectionne.getTitre() + " » ?\nCette action est irréversible.");
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    eventService.supprimer(eventSelectionne.getId());
                    afficherSucces("✅ Événement supprimé !");
                    viderFormulaire();
                    chargerEvents();
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
    //  CHOISIR IMAGE
    // ══════════════════════════════════════════════════════

    @FXML
    private void handleChoisirImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File fichier = fileChooser.showOpenDialog(imageField.getScene().getWindow());
        if (fichier != null) {
            imageChoisieNom = fichier.getName();
            imageField.setText(imageChoisieNom);
            imagePreview.setImage(new Image(fichier.toURI().toString()));
            try {
                File dest = new File("src/main/resources/images/" + fichier.getName());
                dest.getParentFile().mkdirs();
                Files.copy(fichier.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                afficherErreur("⚠️ Impossible de copier l'image : " + e.getMessage());
            }
        }
    }

    // ══════════════════════════════════════════════════════
    //  RETOUR
    // ══════════════════════════════════════════════════════

    @FXML
    private void handleRetour() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/eventList.fxml"));
            Node page = loader.load();
            StackPane contentArea = (StackPane) titreField.getScene().lookup("#contentArea");
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

    private void remplirFormulaire(Event e) {
        eventSelectionne = e;
        titreField.setText(e.getTitre());
        descriptionArea.setText(e.getDescription());
        typeCombo.setValue(e.getType());
        if (e.getDateCreation() != null)
            datePicker.setValue(e.getDateCreation().toLocalDate());
        imageField.setText(e.getImage());
        userIdField.setText(String.valueOf(e.getUserId()));
        messageLabel.setText("");
        // Reset styles quand on sélectionne une ligne
        resetStyles();
        File f = new File("src/main/resources/images/" + e.getImage());
        if (f.exists()) {
            imagePreview.setImage(new Image(f.toURI().toString()));
        } else {
            imagePreview.setImage(null);
        }
    }

    // ══════════════════════════════════════════════════════
    //  VIDER FORMULAIRE
    // ══════════════════════════════════════════════════════

    private void viderFormulaire() {
        eventSelectionne = null;
        titreField.clear();
        descriptionArea.clear();
        typeCombo.setValue(null);
        datePicker.setValue(null);
        imageField.clear();
        imagePreview.setImage(null);
        imageChoisieNom = "";
        userIdField.clear();
        messageLabel.setText("");
        eventTable.getSelectionModel().clearSelection();
        // Reset toutes les bordures
        resetStyles();
    }

    // ══════════════════════════════════════════════════════
    //  RESET STYLES
    // ══════════════════════════════════════════════════════

    private void resetStyles() {
        titreField.setStyle(STYLE_FIELD_NORMAL);
        descriptionArea.setStyle(STYLE_AREA_NORMAL);
        typeCombo.setStyle(STYLE_COMBO_NORMAL);
        datePicker.setStyle(STYLE_DATE_NORMAL);
        userIdField.setStyle(STYLE_USERID_NORMAL);
    }

    // ══════════════════════════════════════════════════════
    //  VALIDATION COMPLÈTE
    // ══════════════════════════════════════════════════════

    private boolean validerFormulaire() {
        boolean valide = true;
        StringBuilder erreurs = new StringBuilder();

        // Reset styles avant validation
        resetStyles();

        // ── 1. Titre obligatoire
        if (titreField.getText().trim().isEmpty()) {
            titreField.setStyle(STYLE_FIELD_ERROR);
            erreurs.append("• Le titre est obligatoire\n");
            valide = false;
        } else {
            // ── 2. Titre commence par une majuscule
            if (!Character.isUpperCase(titreField.getText().trim().charAt(0))) {
                titreField.setStyle(STYLE_FIELD_WARNING);
                erreurs.append("• Le titre doit commencer par une majuscule\n");
                valide = false;
            }
        }

        // ── 3. Description obligatoire
        if (descriptionArea.getText().trim().isEmpty()) {
            descriptionArea.setStyle(STYLE_AREA_ERROR);
            erreurs.append("• La description est obligatoire\n");
            valide = false;
        } else {
            // ── 4. Description au moins 5 mots
            String[] mots = descriptionArea.getText().trim().split("\\s+");
            if (mots.length < 5) {
                descriptionArea.setStyle(STYLE_AREA_WARNING);
                erreurs.append("• La description doit contenir au moins 5 mots (actuellement : " + mots.length + ")\n");
                valide = false;
            }
        }

        // ── 5. Type obligatoire
        if (typeCombo.getValue() == null) {
            typeCombo.setStyle(STYLE_COMBO_ERROR);
            erreurs.append("• Le type est obligatoire\n");
            valide = false;
        }

        // ── 6. Date obligatoire
        if (datePicker.getValue() == null) {
            datePicker.setStyle(STYLE_DATE_ERROR);
            erreurs.append("• La date est obligatoire\n");
            valide = false;
        } else {
            // ── 7. Date >= aujourd'hui
            if (datePicker.getValue().isBefore(LocalDate.now())) {
                datePicker.setStyle(STYLE_DATE_WARNING);
                erreurs.append("• La date doit être aujourd'hui ou dans le futur\n");
                valide = false;
            }
        }

        // ── 8. User ID obligatoire
        if (userIdField.getText().trim().isEmpty()) {
            userIdField.setStyle(STYLE_USERID_ERROR);
            erreurs.append("• Le User ID est obligatoire\n");
            valide = false;
        } else {
            // ── 9. User ID doit être un entier
            try {
                Integer.parseInt(userIdField.getText().trim());
            } catch (NumberFormatException e) {
                userIdField.setStyle(STYLE_USERID_WARNING);
                erreurs.append("• Le User ID doit être un nombre entier\n");
                valide = false;
            }
        }

        // Afficher toutes les erreurs en une seule fois
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