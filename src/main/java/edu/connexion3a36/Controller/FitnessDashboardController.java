package edu.connexion3a36.Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.IOException;

public class FitnessDashboardController {

    // === Navigation Views ===
    @FXML private VBox viewHome;
    @FXML private VBox viewProfile;
    @FXML private VBox viewMessages;
    @FXML private VBox viewSettings;
    @FXML private VBox viewCourses;

    // === Sidebar Buttons ===
    @FXML private Button btnHome;
    @FXML private Button btnCourses;
    @FXML private Button btnProfile;
    @FXML private Button btnMessages;
    @FXML private Button btnSettings;
    @FXML private Button btnLogout;

    // === Home View Elements ===
    @FXML private Label lblGreeting;
    @FXML private Label lblCompleted;
    @FXML private Label lblInProgress;
    @FXML private Label lblHours;
    @FXML private Label lblBadges;
    @FXML private TextField searchField;
    @FXML private Button btnNotif;
    @FXML private Circle avatarCircle;

    // Current course
    @FXML private HBox currentCourseBox;
    @FXML private Label currentCourseEmoji;
    @FXML private Label currentCourseName;
    @FXML private Label currentCourseAuthor;
    @FXML private Label lblProgress;
    @FXML private Button btnPrevCourse;
    @FXML private Button btnNextCourse;

    // Course filters
    @FXML private Button filterAll;
    @FXML private Button filterNew;
    @FXML private Button filterTop;
    @FXML private Button filterPopular;
    @FXML private ScrollPane courseScrollPane;
    @FXML private VBox courseList;

    // Chart
    @FXML private LineChart<String, Number> learningChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private Button chartTabHours;
    @FXML private Button chartTabCourses;
    @FXML private ComboBox<String> weeklyFilter;
    @FXML private Button btnGoPremium;

    // Profile View Elements
    @FXML private Label profileFullName;
    @FXML private Label profileCompleted;
    @FXML private Label profileInProgress;
    @FXML private TextField fieldFirstName;
    @FXML private TextField fieldLastName;
    @FXML private TextField fieldEmail;
    @FXML private TextField fieldBio;

    // Messages View
    @FXML private VBox messageList;

    // Settings View
    @FXML private VBox settingsNotifList;
    @FXML private VBox settingsPrivacyList;

    // All Courses View
    @FXML private FlowPane allCoursesGrid;

    private String currentView = "home";
    private int currentCourseIndex = 0;
    private String currentChartTab = "hours";
    private String currentPeriod = "Semaine";

    // Sample data
    private final Course[] courses = {
            new Course("🇪🇸", "Espagnol B2", "par Alejandro Velazquez", 83, "En cours"),
            new Course("🇫🇷", "Français Avancé", "par Marie Dubois", 45, "En cours"),
            new Course("🇩🇪", "Allemand A1", "par Hans Schmidt", 12, "Nouveau"),
            new Course("🇮🇹", "Italien Culturel", "par Giovanni Rossi", 90, "Populaire"),
            new Course("🇯🇵", "Japonais Débutant", "par Yuki Tanaka", 30, "Top noté"),
            new Course("🇬🇧", "Business English", "par John Smith", 67, "Populaire")
    };

    private final Message[] messages = {
            new Message("👨‍🏫", "Alejandro Velazquez", "Nouveau contenu ajouté !", "Il y a 2h"),
            new Message("⭐", "Support Premium", "Votre abonnement expire dans 5 jours", "Hier"),
            new Message("🎓", "Communauté", "Nouveau badge débloqué : Assidu", "Mercredi"),
            new Message("📢", "Administration", "Maintenance prévue dimanche", "Lundi")
    };

    @FXML
    public void initialize() {
        // Initialize combo box
        weeklyFilter.getItems().addAll("Semaine", "Mois", "Année");
        weeklyFilter.setValue("Semaine");

        // Initialize default view
        showView("home");
        updateActiveNavButton(btnHome);

        // Load all data
        loadStats();
        loadCourses();
        loadCurrentCourse();
        loadMessages();
        loadSettings();
        loadChart();
        loadAllCoursesGrid();
    }

    // === Navigation Methods ===

    @FXML
    private void handleNav(javafx.event.ActionEvent event) {
        Button source = (Button) event.getSource();
        String viewName = (String) source.getUserData();
        showView(viewName);
        updateActiveNavButton(source);
    }

    @FXML
    private void handleLogout(javafx.event.ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de se déconnecter");
        }
    }

    @FXML
    private void handleSearch(KeyEvent event) {
        String searchText = searchField.getText().toLowerCase();
        if (searchText.isEmpty()) {
            loadCourses();
        } else {
            filterCoursesBySearch(searchText);
        }
    }

    @FXML
    private void handleNotif(javafx.event.ActionEvent event) {
        showAlert("Notifications", "🔔 Aucune nouvelle notification");
    }

    @FXML
    private void goToProfile(MouseEvent event) {
        showView("profile");
        updateActiveNavButton(btnProfile);
    }

    @FXML
    private void goHome(javafx.event.ActionEvent event) {
        showView("home");
        updateActiveNavButton(btnHome);
    }

    @FXML
    private void handleContinue(javafx.event.ActionEvent event) {
        showAlert("Continuer", "▶️ Reprise du cours : " + currentCourseName.getText());
    }

    @FXML
    private void handlePrevCourse(javafx.event.ActionEvent event) {
        currentCourseIndex--;
        if (currentCourseIndex < 0) {
            currentCourseIndex = courses.length - 1;
        }
        updateCurrentCourseDisplay();
    }

    @FXML
    private void handleNextCourse(javafx.event.ActionEvent event) {
        currentCourseIndex++;
        if (currentCourseIndex >= courses.length) {
            currentCourseIndex = 0;
        }
        updateCurrentCourseDisplay();
    }

    @FXML
    private void handleFilter(javafx.event.ActionEvent event) {
        Button source = (Button) event.getSource();
        String filter = (String) source.getUserData();

        resetFilterButtons();
        source.setStyle("-fx-background-color: transparent; -fx-text-fill: #111; " +
                "-fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 4 0; " +
                "-fx-border-color: transparent transparent #111 transparent; " +
                "-fx-border-width: 0 0 2 0;");

        filterCourses(filter);
    }

    @FXML
    private void handleChartTab(javafx.event.ActionEvent event) {
        Button source = (Button) event.getSource();
        currentChartTab = (String) source.getUserData();

        chartTabHours.setStyle("-fx-background-color: transparent; -fx-font-size: 12px; " +
                "-fx-text-fill: #aaa; -fx-cursor: hand;");
        chartTabCourses.setStyle("-fx-background-color: transparent; -fx-font-size: 12px; " +
                "-fx-text-fill: #aaa; -fx-cursor: hand;");

        source.setStyle("-fx-background-color: transparent; -fx-font-weight: bold; " +
                "-fx-font-size: 12px; -fx-text-fill: #111; -fx-cursor: hand; " +
                "-fx-border-color: transparent transparent #111 transparent; " +
                "-fx-border-width: 0 0 2 0;");

        updateChart(currentChartTab);
    }

    @FXML
    private void handlePeriodChange(javafx.event.ActionEvent event) {
        currentPeriod = weeklyFilter.getValue();
        updateChart(currentChartTab);
    }

    @FXML
    private void handlePremium(javafx.event.ActionEvent event) {
        showAlert("Premium", "⭐ Passer à l'offre Premium - 9,99€/mois");
    }

    @FXML
    private void handleSaveProfile(javafx.event.ActionEvent event) {
        String firstName = fieldFirstName.getText().trim();
        String lastName = fieldLastName.getText().trim();

        if (firstName.isEmpty() || lastName.isEmpty()) {
            showAlert("Erreur", "Veuillez remplir le prénom et le nom");
            return;
        }

        profileFullName.setText(firstName + " " + lastName);
        lblGreeting.setText("Bonjour, " + firstName + " !");
        showAlert("Succès", "✅ Profil mis à jour avec succès !");
    }

    @FXML
    private void handleChangePassword(javafx.event.ActionEvent event) {
        showAlert("Mot de passe", "🔒 Fonctionnalité à venir");
    }

    @FXML
    private void handleDeleteAccount(javafx.event.ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer le compte");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer votre compte ? Cette action est irréversible.");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                showAlert("Compte supprimé", "Votre compte a été supprimé");
            }
        });
    }

    // === Helper Methods ===

    private void showView(String viewName) {
        currentView = viewName;

        viewHome.setVisible(false);
        viewHome.setManaged(false);
        viewProfile.setVisible(false);
        viewProfile.setManaged(false);
        viewMessages.setVisible(false);
        viewMessages.setManaged(false);
        viewSettings.setVisible(false);
        viewSettings.setManaged(false);
        viewCourses.setVisible(false);
        viewCourses.setManaged(false);

        switch (viewName) {
            case "home":
                viewHome.setVisible(true);
                viewHome.setManaged(true);
                break;
            case "profile":
                viewProfile.setVisible(true);
                viewProfile.setManaged(true);
                break;
            case "messages":
                viewMessages.setVisible(true);
                viewMessages.setManaged(true);
                break;
            case "settings":
                viewSettings.setVisible(true);
                viewSettings.setManaged(true);
                break;
            case "courses":
                viewCourses.setVisible(true);
                viewCourses.setManaged(true);
                break;
        }
    }

    private void updateActiveNavButton(Button activeButton) {
        Button[] buttons = {btnHome, btnCourses, btnProfile, btnMessages, btnSettings};

        for (Button btn : buttons) {
            if (btn == activeButton) {
                btn.setStyle("-fx-font-size: 20px; -fx-background-color: #333333; " +
                        "-fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 10;");
            } else {
                btn.setStyle("-fx-font-size: 20px; -fx-background-color: transparent; " +
                        "-fx-text-fill: #888888; -fx-cursor: hand;");
            }
        }
    }

    private void resetFilterButtons() {
        Button[] filters = {filterAll, filterNew, filterTop, filterPopular};
        for (Button btn : filters) {
            if (btn != null) {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #888; " +
                        "-fx-cursor: hand; -fx-padding: 4 0;");
            }
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void updateCurrentCourseDisplay() {
        Course course = courses[currentCourseIndex];
        currentCourseEmoji.setText(course.emoji);
        currentCourseName.setText(course.name);
        currentCourseAuthor.setText(course.author);
        lblProgress.setText(course.progress + "%");
    }

    // === Card Creation Methods ===

    private HBox createCourseCard(Course course) {
        HBox card = new HBox();
        card.setSpacing(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefHeight(70);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-border-color: #eeeeee; -fx-border-radius: 12; -fx-padding: 10 14 10 14;");
        card.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(card, Priority.ALWAYS);

        Label emojiLabel = new Label(course.emoji);
        emojiLabel.setStyle("-fx-font-size: 28px;");

        VBox infoBox = new VBox(4);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label nameLabel = new Label(course.name);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label authorLabel = new Label(course.author);
        authorLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");

        infoBox.getChildren().addAll(nameLabel, authorLabel);

        Label progressLabel = new Label(course.progress + "%");
        progressLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; " +
                "-fx-background-color: #f0f0f0; -fx-padding: 4 12; -fx-background-radius: 20;");

        Button continueBtn = new Button("Continuer");
        continueBtn.setStyle("-fx-background-color: #111111; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-font-size: 11px; -fx-padding: 6 14; -fx-cursor: hand;");
        continueBtn.setOnAction(e -> showAlert("Continuer", "Reprise du cours : " + course.name));

        card.getChildren().addAll(emojiLabel, infoBox, progressLabel, continueBtn);
        return card;
    }

    private HBox createMessageCard(Message msg) {
        HBox card = new HBox();
        card.setSpacing(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-border-color: #eeeeee; -fx-border-radius: 12; -fx-padding: 12 16 12 16;");
        card.setMaxWidth(Double.MAX_VALUE);

        Label avatarLabel = new Label(msg.avatar);
        avatarLabel.setStyle("-fx-font-size: 32px;");

        VBox contentBox = new VBox(4);
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        Label senderLabel = new Label(msg.sender);
        senderLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label contentLabel = new Label(msg.content);
        contentLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");

        contentBox.getChildren().addAll(senderLabel, contentLabel);

        Label timeLabel = new Label(msg.time);
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #aaa;");

        card.getChildren().addAll(avatarLabel, contentBox, timeLabel);
        return card;
    }

    // === Data Loading Methods ===

    private void loadStats() {
        lblCompleted.setText("11");
        lblInProgress.setText("4");
        lblHours.setText("47h");
        lblBadges.setText("🏆 12");
        profileCompleted.setText("11");
        profileInProgress.setText("4");
    }

    private void loadCourses() {
        courseList.getChildren().clear();
        for (Course course : courses) {
            courseList.getChildren().add(createCourseCard(course));
        }
    }

    private void loadCurrentCourse() {
        currentCourseIndex = 0;
        updateCurrentCourseDisplay();
    }

    private void loadMessages() {
        messageList.getChildren().clear();
        for (Message msg : messages) {
            messageList.getChildren().add(createMessageCard(msg));
        }
    }

    private void loadSettings() {
        settingsNotifList.getChildren().clear();
        settingsPrivacyList.getChildren().clear();

        // Notification settings
        String[] notifSettings = {"Notifications push", "Notifications email", "Rappels de cours"};
        boolean[] notifStates = {true, true, false};

        for (int i = 0; i < notifSettings.length; i++) {
            HBox row = createSettingRow(notifSettings[i], notifStates[i]);
            settingsNotifList.getChildren().add(row);
        }

        // Privacy settings
        String[] privacySettings = {"Profil public", "Partager la progression", "Visible dans les recherches"};
        boolean[] privacyStates = {false, true, true};

        for (int i = 0; i < privacySettings.length; i++) {
            HBox row = createSettingRow(privacySettings[i], privacyStates[i]);
            settingsPrivacyList.getChildren().add(row);
        }
    }

    private HBox createSettingRow(String labelText, boolean defaultValue) {
        HBox row = new HBox();
        row.setSpacing(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPrefHeight(44);
        row.setStyle("-fx-border-color: transparent transparent #f0f0f0 transparent; -fx-border-width: 0 0 1 0");

        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 13px");
        HBox.setHgrow(label, Priority.ALWAYS);

        CheckBox checkBox = new CheckBox();
        checkBox.setSelected(defaultValue);

        row.getChildren().addAll(label, checkBox);
        return row;
    }

    private void loadChart() {
        updateChart("hours");
    }

    private void updateChart(String tab) {
        learningChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        if (tab.equals("hours")) {
            series.setName("Heures d'apprentissage");
            if (currentPeriod.equals("Semaine")) {
                series.getData().add(new XYChart.Data<>("Lun", 2.5));
                series.getData().add(new XYChart.Data<>("Mar", 3.0));
                series.getData().add(new XYChart.Data<>("Mer", 1.5));
                series.getData().add(new XYChart.Data<>("Jeu", 4.0));
                series.getData().add(new XYChart.Data<>("Ven", 2.0));
                series.getData().add(new XYChart.Data<>("Sam", 1.0));
                series.getData().add(new XYChart.Data<>("Dim", 0.5));
            } else if (currentPeriod.equals("Mois")) {
                series.getData().add(new XYChart.Data<>("Sem 1", 12.5));
                series.getData().add(new XYChart.Data<>("Sem 2", 15.0));
                series.getData().add(new XYChart.Data<>("Sem 3", 10.5));
                series.getData().add(new XYChart.Data<>("Sem 4", 18.0));
            } else {
                series.getData().add(new XYChart.Data<>("Jan", 45));
                series.getData().add(new XYChart.Data<>("Fév", 52));
                series.getData().add(new XYChart.Data<>("Mar", 48));
                series.getData().add(new XYChart.Data<>("Avr", 60));
            }
        } else {
            series.setName("Cours complétés");
            if (currentPeriod.equals("Semaine")) {
                series.getData().add(new XYChart.Data<>("Lun", 1));
                series.getData().add(new XYChart.Data<>("Mar", 2));
                series.getData().add(new XYChart.Data<>("Mer", 0));
                series.getData().add(new XYChart.Data<>("Jeu", 1));
                series.getData().add(new XYChart.Data<>("Ven", 3));
                series.getData().add(new XYChart.Data<>("Sam", 0));
                series.getData().add(new XYChart.Data<>("Dim", 0));
            } else if (currentPeriod.equals("Mois")) {
                series.getData().add(new XYChart.Data<>("Sem 1", 4));
                series.getData().add(new XYChart.Data<>("Sem 2", 6));
                series.getData().add(new XYChart.Data<>("Sem 3", 3));
                series.getData().add(new XYChart.Data<>("Sem 4", 7));
            } else {
                series.getData().add(new XYChart.Data<>("Jan", 12));
                series.getData().add(new XYChart.Data<>("Fév", 15));
                series.getData().add(new XYChart.Data<>("Mar", 18));
                series.getData().add(new XYChart.Data<>("Avr", 22));
            }
        }

        learningChart.getData().add(series);
    }

    private void filterCourses(String filter) {
        courseList.getChildren().clear();
        for (Course course : courses) {
            boolean shouldShow = false;
            switch (filter) {
                case "all":
                    shouldShow = true;
                    break;
                case "new":
                    shouldShow = course.tag.equals("Nouveau");
                    break;
                case "top":
                    shouldShow = course.tag.equals("Top noté");
                    break;
                case "popular":
                    shouldShow = course.tag.equals("Populaire");
                    break;
            }
            if (shouldShow) {
                courseList.getChildren().add(createCourseCard(course));
            }
        }
    }

    private void filterCoursesBySearch(String searchText) {
        courseList.getChildren().clear();
        for (Course course : courses) {
            if (course.name.toLowerCase().contains(searchText) ||
                    course.author.toLowerCase().contains(searchText)) {
                courseList.getChildren().add(createCourseCard(course));
            }
        }
    }

    private void loadAllCoursesGrid() {
        allCoursesGrid.getChildren().clear();
        for (Course course : courses) {
            allCoursesGrid.getChildren().add(createCourseCard(course));
        }
    }

    // === Inner Classes ===

    private static class Course {
        String emoji, name, author, tag;
        int progress;

        Course(String emoji, String name, String author, int progress, String tag) {
            this.emoji = emoji;
            this.name = name;
            this.author = author;
            this.progress = progress;
            this.tag = tag;
        }
    }

    private static class Message {
        String avatar, sender, content, time;

        Message(String avatar, String sender, String content, String time) {
            this.avatar = avatar;
            this.sender = sender;
            this.content = content;
            this.time = time;
        }
    }
}