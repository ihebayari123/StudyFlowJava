package edu.connexion3a36.Controller;
import edu.connexion3a36.entities.Utilisateur;
import edu.connexion3a36.services.UtilisateurService;
import edu.connexion3a36.utils.Validation;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import javafx.scene.control.PasswordField;
import java.io.IOException;
import java.sql.SQLException;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalTime;
import java.util.*;

public class FitnessDashboardController implements Initializable {

    @FXML private Button btnBoutique;
    @FXML private VBox viewBoutique;
    @FXML private FlowPane boutiqueProduitGrid;
    @FXML private TextField boutiqueSearchField;

    @FXML private Button btnHome;
    @FXML private Button btnCourses;
    @FXML private Button btnProfile;
    @FXML private Button btnMessages;
    @FXML private Button btnSettings;
    @FXML private Button btnRelax;
    @FXML private Button btnLogout;

    @FXML private VBox viewHome;
    @FXML private VBox viewProfile;
    @FXML private VBox viewMessages;
    @FXML private VBox viewSettings;
    @FXML private VBox viewCourses;
    @FXML private VBox viewRelax;

    @FXML private Label lblGreeting;
    @FXML private Label lblCompleted;
    @FXML private Label lblInProgress;
    @FXML private Label lblHours;
    @FXML private Label lblBadges;
    @FXML private TextField searchField;
    @FXML private Circle avatarCircle;
    @FXML private Button btnNotif;

    @FXML private HBox currentCourseBox;
    @FXML private Label currentCourseEmoji;
    @FXML private Label currentCourseName;
    @FXML private Label currentCourseAuthor;
    @FXML private Label lblProgress;
    @FXML private Button btnPrevCourse;
    @FXML private Button btnNextCourse;

    @FXML private VBox courseList;
    @FXML private ScrollPane courseScrollPane;
    @FXML private Button filterAll;
    @FXML private Button filterNew;
    @FXML private Button filterTop;
    @FXML private Button filterPopular;

    @FXML private LineChart<String, Number> learningChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private Button chartTabHours;
    @FXML private Button chartTabCourses;
    @FXML private ComboBox<String> weeklyFilter;

    @FXML private Button btnGoPremium;

    @FXML private Label profileFullName;
    @FXML private Label profileCompleted;
    @FXML private Label profileInProgress;
    @FXML private TextField fieldFirstName;
    @FXML private TextField fieldLastName;
    @FXML private TextField fieldEmail;
    @FXML private TextField fieldBio;

    @FXML private VBox messageList;

    @FXML private Label erreurPrenom;
    @FXML private Label erreurNom;
    @FXML private Label erreurEmail;

    @FXML private VBox settingsNotifList;
    @FXML private VBox settingsPrivacyList;

    @FXML private FlowPane allCoursesGrid;

    @FXML private Button btnConsulterMedecin;
    @FXML private Button btnCalculerScore;
    @FXML private StackPane mainStackPane;

    private record Course(String emoji, String name, String author,
                          int progress, double rating, boolean isNew, boolean isPopular) {}

    private final List<Course> allCourses = new ArrayList<>();
    private String currentFilter = "all";
    private int currentCourseIndex = 0;

    private Map<Button, VBox> navMap;
    // ═══════════════════════════════
// UTILISATEUR CONNECTE
// ═══════════════════════════════
    private Utilisateur utilisateurConnecte;

    public void setUtilisateurConnecte(Utilisateur u) {
        this.utilisateurConnecte = u;

        // Greeting personnalisé
        int hour = LocalTime.now().getHour();
        String greet = hour < 12 ? "Bonjour" : hour < 18 ? "Bon après-midi" : "Bonsoir";
        String prenom = (u.getPrenom() != null && !u.getPrenom().isEmpty()) ? u.getPrenom() : u.getNom();
        lblGreeting.setText(greet + ", " + prenom + " !");

        // Pré-remplir les champs profil
        if (fieldFirstName != null) fieldFirstName.setText(u.getPrenom());
        if (fieldLastName  != null) fieldLastName.setText(u.getNom());
        if (fieldEmail     != null) fieldEmail.setText(u.getEmail());
        if (profileFullName != null) profileFullName.setText(u.getNom() + " " + u.getPrenom());

        // Validation en temps réel
        fieldFirstName.textProperty().addListener((obs, old, val) -> {
            String msg = Validation.messageNom(val);
            erreurPrenom.setText(msg);
            fieldFirstName.setStyle(msg.isEmpty()
                    ? "-fx-border-color: #4CAF50; -fx-border-radius: 8; -fx-border-width: 1.5; -fx-padding: 8 12;"
                    : "-fx-border-color: #F44336; -fx-border-radius: 8; -fx-border-width: 1.5; -fx-padding: 8 12;");
        });

        fieldLastName.textProperty().addListener((obs, old, val) -> {
            String msg = Validation.messageNom(val);
            erreurNom.setText(msg);
            fieldLastName.setStyle(msg.isEmpty()
                    ? "-fx-border-color: #4CAF50; -fx-border-radius: 8; -fx-border-width: 1.5; -fx-padding: 8 12;"
                    : "-fx-border-color: #F44336; -fx-border-radius: 8; -fx-border-width: 1.5; -fx-padding: 8 12;");
        });

        fieldEmail.textProperty().addListener((obs, old, val) -> {
            String msg = Validation.messageEmail(val);
            erreurEmail.setText(msg);
            fieldEmail.setStyle(msg.isEmpty()
                    ? "-fx-border-color: #4CAF50; -fx-border-radius: 8; -fx-border-width: 1.5; -fx-padding: 8 12;"
                    : "-fx-border-color: #F44336; -fx-border-radius: 8; -fx-border-width: 1.5; -fx-padding: 8 12;");
        });

        System.out.println("✅ Étudiant connecté : " + u.getNom() + " " + u.getPrenom());
    }
    private Button activeNavButton;

    private List<edu.connexion3a36.entities.Produit> allProduits = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        buildNavMap();
        populateCourses();
        initStats();
        initGreeting();
        initChart();
        initMessages();
        initSettings();
        initAllCoursesGrid();
        renderCourseList(currentFilter);
        updateCurrentCourse();
        setActiveNav(btnHome);
        initBoutique();
    }

    private void buildNavMap() {
        navMap = new LinkedHashMap<>();
        navMap.put(btnHome, viewHome);
        //navMap.put(btnCourses, viewCourses);
        navMap.put(btnProfile, viewProfile);
        navMap.put(btnMessages, viewMessages);
        navMap.put(btnSettings, viewSettings);
        navMap.put(btnRelax, viewRelax);
        navMap.put(btnBoutique, viewBoutique);
    }

    @FXML
    public void handleNav(ActionEvent e) {
        Button src = (Button) e.getSource();
        VBox target = navMap.get(src);
        if (src == btnCourses) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/coursesfront.fxml"));
                Parent view = loader.load();
                mainStackPane.getChildren().setAll(view);

                FadeTransition ft = new FadeTransition(Duration.millis(220), view);
                ft.setFromValue(0);
                ft.setToValue(1);
                ft.play();

            } catch (IOException ex) {
                ex.printStackTrace();
                showAlert("Erreur", "Impossible de charger les cours");
            }
            setActiveNav(src);
            return;
        }
        if (target == null) return;
        showView(target);
        setActiveNav(src);
    }
    private void loadCoursesView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fitness_courses.fxml"));
            Parent view = loader.load();

            // Hide all other views first
            for (VBox v : navMap.values()) {
                v.setVisible(false);
                v.setManaged(false);
            }

            // Replace the content in your main container
            // You need to add a StackPane or similar container to your main layout
            // For now, assuming you have mainStackPane defined
            if (mainStackPane != null) {
                mainStackPane.getChildren().setAll(view);
            } else {
                // Alternative: replace the scene root
                Scene scene = btnHome.getScene();
                scene.setRoot(view);
            }

            // Fade transition for smooth loading
            FadeTransition ft = new FadeTransition(Duration.millis(220), view);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger la vue des cours: " + e.getMessage());
        }
    }
    private void showView(VBox target) {
        for (VBox v : navMap.values()) {
            v.setVisible(false);
            v.setManaged(false);
        }
        target.setVisible(true);
        target.setManaged(true);

        FadeTransition ft = new FadeTransition(Duration.millis(220), target);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void setActiveNav(Button active) {
        activeNavButton = active;
        String activeStyle = "-fx-font-size: 20px; -fx-background-color: #333333;"
                + " -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 10;";
        String inactiveStyle = "-fx-font-size: 20px; -fx-background-color: transparent;"
                + " -fx-text-fill: #888888; -fx-cursor: hand;";

        for (Button b : navMap.keySet()) {
            b.setStyle(b == active ? activeStyle : inactiveStyle);
        }
    }

    @FXML public void goHome(ActionEvent e) { showView(viewHome); setActiveNav(btnHome); }
    @FXML public void goToProfile(javafx.scene.input.MouseEvent e) {
        showView(viewProfile); setActiveNav(btnProfile);
    }

    private void initBoutique() {
        try {
            edu.connexion3a36.services.ProduitService produitService = new edu.connexion3a36.services.ProduitService();
            edu.connexion3a36.services.TypeCategorieService categorieService = new edu.connexion3a36.services.TypeCategorieService();

            allProduits = produitService.getData();
            List<edu.connexion3a36.entities.TypeCategorie> categories = categorieService.getData();

            Map<Integer, String> catMap = new HashMap<>();
            categories.forEach(c -> catMap.put(c.getId(), c.getNomCategorie()));

            renderBoutique(allProduits, catMap);

            boutiqueSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
                List<edu.connexion3a36.entities.Produit> filtered = allProduits.stream()
                        .filter(p -> p.getNom().toLowerCase().contains(newVal.toLowerCase().trim()))
                        .toList();
                renderBoutique(filtered, catMap);
            });

        } catch (java.sql.SQLException e) {
            Label err = new Label("❌ Erreur chargement produits : " + e.getMessage());
            err.setStyle("-fx-text-fill: #F44336;");
            boutiqueProduitGrid.getChildren().add(err);
        }
    }

    private void renderBoutique(List<edu.connexion3a36.entities.Produit> produits, Map<Integer, String> catMap) {
        boutiqueProduitGrid.getChildren().clear();
        if (produits.isEmpty()) {
            Label empty = new Label("Aucun produit trouvé.");
            empty.setStyle("-fx-text-fill: #9E9E9E; -fx-font-size: 14px;");
            boutiqueProduitGrid.getChildren().add(empty);
            return;
        }
        produits.forEach(p -> boutiqueProduitGrid.getChildren().add(buildProduitCard(p, catMap)));
    }

    private VBox buildProduitCard(edu.connexion3a36.entities.Produit p, Map<Integer, String> catMap) {
        VBox card = new VBox(10);
        card.setPrefWidth(200);
        card.setPadding(new Insets(0, 0, 16, 0));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 18;"
                + "-fx-border-color: #f0f0f0; -fx-border-radius: 18;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 10, 0, 0, 3); -fx-cursor: hand;");

        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: #fafafa; -fx-background-radius: 18;"
                        + "-fx-border-color: #ddd; -fx-border-radius: 18;"
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.13), 16, 0, 0, 5); -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 18;"
                        + "-fx-border-color: #f0f0f0; -fx-border-radius: 18;"
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 10, 0, 0, 3); -fx-cursor: hand;"));

        ImageView imageView = new ImageView();
        imageView.setFitWidth(200);
        imageView.setFitHeight(130);
        imageView.setPreserveRatio(false);
        imageView.setStyle("-fx-background-radius: 18 18 0 0;");

        StackPane imgContainer = new StackPane();
        imgContainer.setPrefHeight(130);
        imgContainer.setStyle("-fx-background-color: #E8F0FE; -fx-background-radius: 18 18 0 0;");

        String imageUrl = p.getImage();
        boolean isValidUrl = false;
        try {
            new java.net.URL(imageUrl);
            isValidUrl = true;
        } catch (Exception ex) {
            // URL invalide
        }

        if (isValidUrl && imageUrl != null && !imageUrl.trim().isEmpty()) {
            try {
                Image image = new Image(imageUrl, 200, 130, false, true);
                if (!image.isError()) {
                    imageView.setImage(image);
                    imgContainer.getChildren().add(imageView);
                } else {
                    Label placeholder = new Label("🖼️");
                    placeholder.setStyle("-fx-font-size: 40px;");
                    imgContainer.getChildren().add(placeholder);
                }
            } catch (Exception ex) {
                Label placeholder = new Label("🖼️");
                placeholder.setStyle("-fx-font-size: 40px;");
                imgContainer.getChildren().add(placeholder);
            }
        } else {
            Label placeholder = new Label("🖼️");
            placeholder.setStyle("-fx-font-size: 40px;");
            imgContainer.getChildren().add(placeholder);
        }

        VBox info = new VBox(6);
        info.setPadding(new Insets(0, 14, 0, 14));

        Label nomLabel = new Label(p.getNom());
        nomLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1A1A2E; -fx-wrap-text: true;");
        nomLabel.setMaxWidth(172);

        String catNom = catMap.getOrDefault(p.getTypeCategorieId(), "Inconnue");
        Label catLabel = new Label("🏷️ " + catNom);
        catLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #5c6bc0;"
                + "-fx-background-color: #e8eaf6; -fx-background-radius: 20; -fx-padding: 3 8;");

        Label prixLabel = new Label(p.getPrix() + " DT");
        prixLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2979FF;");

        info.getChildren().addAll(nomLabel, catLabel, prixLabel);
        card.getChildren().addAll(imgContainer, info);
        return card;
    }

    private void populateCourses() {
        allCourses.addAll(List.of(
                new Course("🇪🇸", "Spanish B2", "Alejandro Velazquez", 83, 4.8, false, true),
                new Course("🐍", "Python Avancé", "Sophie Martin", 60, 4.9, true, true),
                new Course("🎨", "UI/UX Design", "Léa Bernard", 45, 4.7, true, false),
                new Course("📊", "Data Science", "Marc Dupont", 30, 4.6, false, true),
                new Course("🇬🇧", "English C1", "Emily Watson", 92, 4.5, false, false),
                new Course("🧠", "Machine Learning", "Karim Bensalem", 15, 5.0, true, true),
                new Course("🎸", "Guitare Classique", "Pierre Moreau", 70, 4.4, false, false),
                new Course("📱", "Flutter Mobile", "Amira Khalil", 50, 4.8, true, false),
                new Course("🔐", "Cybersécurité", "Julien Roux", 25, 4.7, true, true),
                new Course("📸", "Photographie", "Céline Petit", 80, 4.3, false, false),
                new Course("🇩🇪", "Allemand A2", "Hans Müller", 10, 4.2, true, false),
                new Course("🧮", "Algorithmique", "Nadia Benali", 65, 4.9, false, true),
                new Course("🌐", "Développement Web", "Tom Leroy", 55, 4.6, false, true),
                new Course("🎬", "Montage Vidéo", "Clara Simon", 35, 4.5, true, false),
                new Course("📝", "Rédaction Web", "Isabelle Gautier", 40, 4.4, false, false)
        ));
    }

    private void initStats() {
        long completed = allCourses.stream().filter(c -> c.progress() == 100).count();
        long inProgress = allCourses.stream().filter(c -> c.progress() > 0 && c.progress() < 100).count();
        int totalH = allCourses.stream().mapToInt(c -> c.progress() / 2).sum();

        lblCompleted.setText(String.valueOf(completed == 0 ? 11 : completed));
        lblInProgress.setText(String.valueOf(inProgress == 0 ? 4 : inProgress));
        lblHours.setText(totalH + "h");
        lblBadges.setText("🏆 " + (completed + 1));

        profileCompleted.setText(lblCompleted.getText());
        profileInProgress.setText(lblInProgress.getText());
    }

    private void initGreeting() {
        int hour = LocalTime.now().getHour();
        String greet = hour < 12 ? "Bonjour" : hour < 18 ? "Bon après-midi" : "Bonsoir";
        lblGreeting.setText(greet + ", Josh !");
    }

    private void renderCourseList(String filter) {
        courseList.getChildren().clear();
        List<Course> filtered = allCourses.stream()
                .filter(c -> switch (filter) {
                    case "new" -> c.isNew();
                    case "top" -> c.rating() >= 4.7;
                    case "popular" -> c.isPopular();
                    default -> true;
                })
                .toList();

        for (Course c : filtered) {
            courseList.getChildren().add(buildCourseRow(c));
        }
    }

    private HBox buildCourseRow(Course c) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 14, 10, 14));
        row.setStyle("-fx-background-color: white; -fx-background-radius: 12;"
                + "-fx-border-color: #f0f0f0; -fx-border-radius: 12; -fx-cursor: hand;");

        row.setOnMouseEntered(e -> row.setStyle(
                "-fx-background-color: #f8f8f8; -fx-background-radius: 12;"
                        + "-fx-border-color: #e0e0e0; -fx-border-radius: 12; -fx-cursor: hand;"));
        row.setOnMouseExited(e -> row.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12;"
                        + "-fx-border-color: #f0f0f0; -fx-border-radius: 12; -fx-cursor: hand;"));

        Label emoji = new Label(c.emoji());
        emoji.setStyle("-fx-font-size: 22px;");

        VBox info = new VBox(2);
        Label name = new Label(c.name());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        Label author = new Label("par " + c.author());
        author.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
        info.getChildren().addAll(name, author);
        HBox.setHgrow(info, Priority.ALWAYS);

        VBox progressBox = new VBox(3);
        progressBox.setAlignment(Pos.CENTER_RIGHT);
        Label pct = new Label(c.progress() + "%");
        pct.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        ProgressBar pb = new ProgressBar(c.progress() / 100.0);
        pb.setPrefWidth(80);
        pb.setStyle("-fx-accent: #111111;");
        progressBox.getChildren().addAll(pct, pb);

        Label rating = new Label("⭐ " + c.rating());
        rating.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");

        row.getChildren().addAll(emoji, info, rating, progressBox);
        return row;
    }

    private List<Course> getInProgressCourses() {
        return allCourses.stream()
                .filter(c -> c.progress() > 0 && c.progress() < 100)
                .toList();
    }

    private void updateCurrentCourse() {
        List<Course> inProgress = getInProgressCourses();
        if (inProgress.isEmpty()) return;
        if (currentCourseIndex >= inProgress.size()) currentCourseIndex = 0;
        Course c = inProgress.get(currentCourseIndex);
        currentCourseEmoji.setText(c.emoji());
        currentCourseName.setText(c.name());
        currentCourseAuthor.setText("par " + c.author());
        lblProgress.setText(c.progress() + "%");
    }

    @FXML public void handlePrevCourse(ActionEvent e) {
        List<Course> list = getInProgressCourses();
        currentCourseIndex = (currentCourseIndex - 1 + list.size()) % list.size();
        updateCurrentCourse();
    }

    @FXML public void handleNextCourse(ActionEvent e) {
        List<Course> list = getInProgressCourses();
        currentCourseIndex = (currentCourseIndex + 1) % list.size();
        updateCurrentCourse();
    }

    @FXML public void handleContinue(ActionEvent e) {
        List<Course> list = getInProgressCourses();
        if (list.isEmpty()) return;
        Course c = list.get(currentCourseIndex);
        showAlert("Continuer", "Reprise du cours : " + c.name() + " 🚀");
    }

    @FXML public void handleFilter(ActionEvent e) {
        Button src = (Button) e.getSource();
        currentFilter = (String) src.getUserData();
        renderCourseList(currentFilter);

        String activeStyle = "-fx-background-color: transparent; -fx-text-fill: #111;"
                + " -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 4 0;"
                + " -fx-border-color: transparent transparent #111 transparent;"
                + " -fx-border-width: 0 0 2 0;";
        String inactiveStyle = "-fx-background-color: transparent; -fx-text-fill: #888;"
                + " -fx-cursor: hand; -fx-padding: 4 0;";

        for (Button b : List.of(filterAll, filterNew, filterTop, filterPopular)) {
            b.setStyle(b == src ? activeStyle : inactiveStyle);
        }
    }

    private void initChart() {
        weeklyFilter.setItems(FXCollections.observableArrayList(
                "Cette semaine", "Ce mois", "3 mois", "6 mois"
        ));
        weeklyFilter.getSelectionModel().selectFirst();
        loadChartData("hours");
    }

    private void loadChartData(String type) {
        learningChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        if ("hours".equals(type)) {
            series.setName("Heures d'apprentissage");
            String[] days = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
            double[] vals = {1.5, 2.0, 0.5, 3.0, 2.5, 4.0, 1.0};
            for (int i = 0; i < days.length; i++)
                series.getData().add(new XYChart.Data<>(days[i], vals[i]));
            yAxis.setUpperBound(5);
            yAxis.setTickUnit(1);
        } else {
            series.setName("Cours suivis");
            String[] weeks = {"S1", "S2", "S3", "S4"};
            double[] vals = {2, 4, 3, 5};
            for (int i = 0; i < weeks.length; i++)
                series.getData().add(new XYChart.Data<>(weeks[i], vals[i]));
            yAxis.setUpperBound(7);
            yAxis.setTickUnit(1);
        }
        learningChart.getData().add(series);
    }

    @FXML public void handleChartTab(ActionEvent e) {
        Button src = (Button) e.getSource();
        String type = (String) src.getUserData();
        loadChartData(type);

        String activeStyle = "-fx-background-color: transparent; -fx-font-weight: bold;"
                + " -fx-font-size: 12px; -fx-text-fill: #111; -fx-cursor: hand;"
                + " -fx-border-color: transparent transparent #111 transparent;"
                + " -fx-border-width: 0 0 2 0;";
        String inactiveStyle = "-fx-background-color: transparent; -fx-font-size: 12px;"
                + " -fx-text-fill: #aaa; -fx-cursor: hand;";

        chartTabHours.setStyle("hours".equals(type) ? activeStyle : inactiveStyle);
        chartTabCourses.setStyle("courses".equals(type) ? activeStyle : inactiveStyle);
    }

    @FXML public void handlePeriodChange(ActionEvent e) {
        loadChartData("hours".equals(chartTabHours.getUserData()) ? "hours" : "courses");
    }

    @FXML public void handleSearch(KeyEvent e) {
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            renderCourseList(currentFilter);
            return;
        }
        courseList.getChildren().clear();
        allCourses.stream()
                .filter(c -> c.name().toLowerCase().contains(query)
                        || c.author().toLowerCase().contains(query))
                .forEach(c -> courseList.getChildren().add(buildCourseRow(c)));
    }

    private void initMessages() {
        messageList.getChildren().clear();
        List<String[]> msgs = List.of(
                new String[]{"👩‍🏫", "Sophie Martin", "Votre progression en Python est excellente !", "Il y a 5 min", String.valueOf(true)},
                new String[]{"🤖", "Système", "Nouveau badge débloqué : ⚡ Rapide !", "Il y a 1 h", String.valueOf(true)},
                new String[]{"👨‍💼", "Marc Dupont", "Avez-vous terminé le module 4 en Data Science ?", "Hier", String.valueOf(false)},
                new String[]{"🎓", "Alejandro Velazquez", "¡ Muy bien ! Continuez ainsi en espagnol.", "Il y a 2 j", String.valueOf(false)},
                new String[]{"🔔", "Système", "Rappel : cours de Machine Learning demain à 10h.", "Il y a 3 j", String.valueOf(false)}
        );
        msgs.forEach(m -> messageList.getChildren().add(buildMessageRow(m)));
    }

    private HBox buildMessageRow(String[] m) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 16, 14, 16));
        String bg = "true".equals(m[4]) ? "#f7f7ff" : "white";
        row.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 14;"
                + "-fx-border-color: #f0f0f0; -fx-border-radius: 14;");

        Label avatar = new Label(m[0]);
        avatar.setStyle("-fx-font-size: 26px; -fx-background-color: #eeeeee;"
                + "-fx-background-radius: 50%; -fx-min-width: 46; -fx-min-height: 46;"
                + "-fx-alignment: center;");

        VBox body = new VBox(3);
        HBox.setHgrow(body, Priority.ALWAYS);
        Label sender = new Label(m[1]);
        sender.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        Label content = new Label(m[2]);
        content.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");
        body.getChildren().addAll(sender, content);

        VBox right = new VBox(4);
        right.setAlignment(Pos.TOP_RIGHT);
        Label time = new Label(m[3]);
        time.setStyle("-fx-font-size: 10px; -fx-text-fill: #aaa;");
        right.getChildren().add(time);
        if ("true".equals(m[4])) {
            Label dot = new Label("●");
            dot.setStyle("-fx-text-fill: #4f46e5; -fx-font-size: 10px;");
            right.getChildren().add(dot);
        }

        row.getChildren().addAll(avatar, body, right);
        return row;
    }

    private void initSettings() {
        List<String[]> notifSettings = List.of(
                new String[]{"Notifications push", "true"},
                new String[]{"Rappels de cours", "true"},
                new String[]{"Nouveaux badges", "true"},
                new String[]{"Messages des instructeurs", "false"}
        );
        List<String[]> privacySettings = List.of(
                new String[]{"Profil public", "false"},
                new String[]{"Partager ma progression", "true"},
                new String[]{"Afficher mes badges", "true"}
        );
        notifSettings.forEach(s -> settingsNotifList.getChildren().add(buildSettingRow(s[0], "true".equals(s[1]))));
        privacySettings.forEach(s -> settingsPrivacyList.getChildren().add(buildSettingRow(s[0], "true".equals(s[1]))));
    }

    private HBox buildSettingRow(String label, boolean defaultVal) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPrefHeight(44);
        row.setStyle("-fx-border-color: transparent transparent #f0f0f0 transparent;"
                + "-fx-border-width: 0 0 1 0;");

        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 13px;");
        HBox.setHgrow(lbl, Priority.ALWAYS);

        CheckBox cb = new CheckBox();
        cb.setSelected(defaultVal);
        cb.setStyle("-fx-cursor: hand;");

        row.getChildren().addAll(lbl, cb);
        return row;
    }

    @FXML public void handleChangePassword(ActionEvent e) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Changer le mot de passe");
        dialog.setHeaderText("Entrez votre nouveau mot de passe");

        ButtonType confirmerBtn = new ButtonType("Confirmer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmerBtn, ButtonType.CANCEL);

        PasswordField newMdp     = new PasswordField();
        newMdp.setPromptText("Nouveau mot de passe");
        PasswordField confirmMdp = new PasswordField();
        confirmMdp.setPromptText("Confirmer le mot de passe");

        VBox content = new VBox(10,
                new Label("Nouveau mot de passe :"), newMdp,
                new Label("Confirmer :"), confirmMdp
        );
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        dialog.setResultConverter(btn -> btn == confirmerBtn ? newMdp.getText() : null);

        dialog.showAndWait().ifPresent(mdp -> {
            if (mdp.isEmpty()) {
                showAlert("Erreur", "Le mot de passe ne peut pas être vide.");
                return;
            }
            if (!mdp.equals(confirmMdp.getText())) {
                showAlert("Erreur", "Les mots de passe ne correspondent pas.");
                return;
            }
            if (!Validation.validerMotDePasse(mdp)) {
                showAlert("Erreur", Validation.messageMotDePasse(mdp));
                return;
            }
            try {
                utilisateurConnecte.setMotDePasse(mdp);
                UtilisateurService service = new UtilisateurService();
                service.updateEntity(utilisateurConnecte.getId().intValue(), utilisateurConnecte);
                showAlert("✅ Succès", "Mot de passe changé avec succès !");
            } catch (SQLException ex) {
                showAlert("❌ Erreur", "Erreur : " + ex.getMessage());
            }
        });
    }

    @FXML public void handleDeleteAccount(ActionEvent e) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer le compte");
        confirm.setHeaderText("Êtes-vous sûr de vouloir supprimer votre compte ?");
        confirm.setContentText("Cette action est irréversible.");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    UtilisateurService service = new UtilisateurService();
                    service.deleteEntity(utilisateurConnecte);
                    showAlert("Compte supprimé", "Votre compte a été supprimé. Au revoir !");
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
                    Parent root = loader.load();
                    Stage stage = (Stage) btnHome.getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.setTitle("StudyFlow — Login");
                    stage.show();
                } catch (SQLException | IOException ex) {
                    showAlert("❌ Erreur", "Erreur suppression : " + ex.getMessage());
                }
            }
        });
    }

    private void initAllCoursesGrid() {
        allCoursesGrid.getChildren().clear();
        allCourses.forEach(c -> allCoursesGrid.getChildren().add(buildCourseCard(c)));
    }

    private VBox buildCourseCard(Course c) {
        VBox card = new VBox(8);
        card.setPrefWidth(200);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16;"
                + "-fx-border-color: #f0f0f0; -fx-border-radius: 16; -fx-cursor: hand;");

        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: #f8f8f8; -fx-background-radius: 16;"
                        + "-fx-border-color: #ddd; -fx-border-radius: 16; -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16;"
                        + "-fx-border-color: #f0f0f0; -fx-border-radius: 16; -fx-cursor: hand;"));

        Label emoji = new Label(c.emoji());
        emoji.setStyle("-fx-font-size: 30px;");

        Label name = new Label(c.name());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-wrap-text: true;");

        Label author = new Label("par " + c.author());
        author.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");

        ProgressBar pb = new ProgressBar(c.progress() / 100.0);
        pb.setPrefWidth(Double.MAX_VALUE);
        pb.setStyle("-fx-accent: #111111;");

        Label pct = new Label(c.progress() + "% terminé");
        pct.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");

        HBox tags = new HBox(6);
        if (c.isNew()) {
            Label n = new Label("Nouveau");
            n.setStyle("-fx-background-color: #e8f5e9; -fx-text-fill: #2e7d32; -fx-padding: 2 7;"
                    + "-fx-background-radius: 20; -fx-font-size: 10px;");
            tags.getChildren().add(n);
        }
        if (c.isPopular()) {
            Label p = new Label("🔥 Populaire");
            p.setStyle("-fx-background-color: #fff3e0; -fx-text-fill: #e65100; -fx-padding: 2 7;"
                    + "-fx-background-radius: 20; -fx-font-size: 10px;");
            tags.getChildren().add(p);
        }

        card.getChildren().addAll(emoji, name, author, pb, pct, tags);
        return card;
    }

    @FXML public void handleSaveProfile(ActionEvent e) {
        String prenom = fieldFirstName.getText().trim();
        String nom    = fieldLastName.getText().trim();
        String email  = fieldEmail.getText().trim();

        // ═══════════════════════════════
        // CONTRÔLE DE SAISIE
        // ═══════════════════════════════
        String msgPrenom = Validation.messageNom(prenom);
        String msgNom    = Validation.messageNom(nom);
        String msgEmail  = Validation.messageEmail(email);

        erreurPrenom.setText(msgPrenom);
        erreurNom.setText(msgNom);
        erreurEmail.setText(msgEmail);

        fieldFirstName.setStyle(!msgPrenom.isEmpty()
                ? "-fx-border-color: #F44336; -fx-border-radius: 8; -fx-border-width: 1.5; -fx-padding: 8 12;"
                : "-fx-border-color: #4CAF50; -fx-border-radius: 8; -fx-border-width: 1.5; -fx-padding: 8 12;");
        fieldLastName.setStyle(!msgNom.isEmpty()
                ? "-fx-border-color: #F44336; -fx-border-radius: 8; -fx-border-width: 1.5; -fx-padding: 8 12;"
                : "-fx-border-color: #4CAF50; -fx-border-radius: 8; -fx-border-width: 1.5; -fx-padding: 8 12;");
        fieldEmail.setStyle(!msgEmail.isEmpty()
                ? "-fx-border-color: #F44336; -fx-border-radius: 8; -fx-border-width: 1.5; -fx-padding: 8 12;"
                : "-fx-border-color: #4CAF50; -fx-border-radius: 8; -fx-border-width: 1.5; -fx-padding: 8 12;");

        if (!msgPrenom.isEmpty() || !msgNom.isEmpty() || !msgEmail.isEmpty()) return;

        // ═══════════════════════════════
        // SAUVEGARDE EN BD
        // ═══════════════════════════════
        utilisateurConnecte.setPrenom(prenom);
        utilisateurConnecte.setNom(nom);
        utilisateurConnecte.setEmail(email);

        try {
            UtilisateurService service = new UtilisateurService();
            service.updateEntity(utilisateurConnecte.getId().intValue(), utilisateurConnecte);
            profileFullName.setText(nom + " " + prenom);
            lblGreeting.setText("Bonjour, " + prenom + " !");
            showAlert("✅ Succès", "Votre profil a été mis à jour avec succès !");
        } catch (SQLException ex) {
            showAlert("❌ Erreur", "Erreur mise à jour : " + ex.getMessage());
        }
    }

    @FXML public void handleNotif(ActionEvent e) {
        showAlert("Notifications", "🔔 Vous avez 3 nouvelles notifications.");
    }

    @FXML public void handlePremium(ActionEvent e) {
        showAlert("Premium", "🌟 Passez Premium pour 9,99 € / mois et accédez à plus de 500 cours !");
    }

    @FXML public void handleLogout(ActionEvent e) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Déconnexion");
        confirm.setHeaderText("Voulez-vous vous déconnecter ?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
                    Parent root = loader.load();
                    Stage stage = (Stage) btnHome.getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.setTitle("StudyFlow — Login");
                    stage.show();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    @FXML
    public void handleConsulterMedecin(ActionEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouteetudiant.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Ajouter Étudiant - Consultation Médecin");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.show();
        } catch (java.io.IOException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible d'ouvrir le formulaire");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    public void handleCalculerScore(ActionEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterStressSurveyEtudiant.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Calculer mon score - Stress Survey");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.show();
        } catch (java.io.IOException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible d'ouvrir le formulaire");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
    }

    private void showAlert(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.getDialogPane().setStyle("-fx-font-size: 13px;");
        a.showAndWait();
    }
}