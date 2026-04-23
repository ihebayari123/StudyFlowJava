package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.CartItem;
import edu.connexion3a36.entities.Utilisateur;
import edu.connexion3a36.services.CartService;
import edu.connexion3a36.services.EmailService;
import edu.connexion3a36.services.StripeService;
import edu.connexion3a36.services.UtilisateurService;
import edu.connexion3a36.utils.Validation;
import edu.connexion3a36.models.Event;
import edu.connexion3a36.models.Sponsor;
import edu.connexion3a36.services.EventService;
import edu.connexion3a36.services.SponsorService;

import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class FitnessDashboardController implements Initializable {

    // =========================================================================
    // EVENTS PERSISTENCE (from branch)
    // =========================================================================
    private static final String DATA_DIR     = System.getProperty("user.home") + "/.connexion3a36/";
    private static final String FAV_FILE     = DATA_DIR + "favorites.txt";
    private static final String COMMENT_FILE = DATA_DIR + "comments.txt";

    private final Set<Integer>              favorites = new HashSet<>();
    private final Map<Integer, List<String>> comments  = new HashMap<>();

    // =========================================================================
    // FXML INJECTIONS — SIDEBAR
    // =========================================================================
    @FXML private Button btnHome, btnCourses, btnProfile, btnMessages,
            btnSettings, btnBoutique, btnRelax, btnQuiz,
            btnEvents, btnLogout;

    // =========================================================================
    // FXML INJECTIONS — VIEWS
    // =========================================================================
    @FXML private VBox viewHome, viewProfile, viewMessages, viewSettings,
            viewCourses, viewRelax, viewBoutique, viewEvents;
    @FXML private StackPane contentArea;
    @FXML private StackPane mainStackPane;

    // =========================================================================
    // FXML — HOME
    // =========================================================================
    @FXML private Label      lblGreeting, lblCompleted, lblInProgress, lblHours, lblBadges;
    @FXML private TextField  searchField;
    @FXML private Circle     avatarCircle;
    @FXML private Button     btnNotif;

    @FXML private HBox  currentCourseBox;
    @FXML private Label currentCourseEmoji, currentCourseName, currentCourseAuthor, lblProgress;
    @FXML private Button btnPrevCourse, btnNextCourse;

    @FXML private VBox       courseList;
    @FXML private ScrollPane courseScrollPane;
    @FXML private Button     filterAll, filterNew, filterTop, filterPopular;

    @FXML private LineChart<String, Number> learningChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis   yAxis;
    @FXML private Button       chartTabHours, chartTabCourses;
    @FXML private ComboBox<String> weeklyFilter;
    @FXML private Button btnGoPremium;

    // =========================================================================
    // FXML — PROFILE
    // =========================================================================
    @FXML private Label     profileFullName, profileCompleted, profileInProgress;
    @FXML private TextField fieldFirstName, fieldLastName, fieldEmail, fieldBio;
    @FXML private Label     erreurPrenom, erreurNom, erreurEmail;

    // =========================================================================
    // FXML — MESSAGES / SETTINGS / COURSES
    // =========================================================================
    @FXML private VBox     messageList;
    @FXML private VBox     settingsNotifList, settingsPrivacyList;
    @FXML private FlowPane allCoursesGrid;

    // =========================================================================
    // FXML — BOUTIQUE & CART
    // =========================================================================
    @FXML private FlowPane  boutiqueProduitGrid;
    @FXML private TextField boutiqueSearchField;
    @FXML private Label cartBadge;
    @FXML private VBox viewCart;
    @FXML private VBox cartItemList;
    @FXML private Label cartTotalLabel;
    @FXML private VBox viewCheckout;
    @FXML private TextField checkoutNom, checkoutEmail, checkoutAdresse;
    @FXML private Label checkoutTotalLabel;

    // =========================================================================
    // FXML — RELAX
    // =========================================================================
    @FXML private Button btnConsulterMedecin;
    @FXML private Button btnCalculerScore;
    @FXML private Button btnCatchStress;
    @FXML private Button btnFitness;
    @FXML private Button btnNutrition;
    @FXML private Button btnEnvoyerReclamation;
    @FXML private Button btnVip;

    @FXML private VBox viewMedecin;
    @FXML private VBox viewStress;
    @FXML private VBox viewCatchStress;
    @FXML private VBox viewFitness;
    @FXML private VBox viewNutrition;
    @FXML private VBox viewChatbot;
    @FXML private VBox viewCabinet;
    @FXML private VBox viewRespiration;
    @FXML private VBox viewAPropos;
    @FXML private VBox viewVip;
    @FXML private VBox viewSleep;
    @FXML private VBox viewReclamation;

    @FXML private StackPane medecinArea;
    @FXML private StackPane stressArea;
    @FXML private StackPane catchStressArea;
    @FXML private StackPane fitnessArea;
    @FXML private StackPane nutritionArea;
    @FXML private StackPane chatbotArea;
    @FXML private StackPane cabinetArea;
    @FXML private StackPane respirationArea;
    @FXML private StackPane aProposArea;
    @FXML private StackPane vipArea;
    @FXML private StackPane sleepArea;

    @FXML private Label lblMedecinTitle;
    @FXML private TextArea reclamationTextArea;
    @FXML private Button btnEnvoyer;

    // =========================================================================
    // FXML — EVENTS
    // =========================================================================
    @FXML private TextField        eventSearchField;
    @FXML private FlowPane         eventCardsContainer;
    @FXML private ComboBox<String> eventTypeFilter, eventDateSort;
    @FXML private Label            eventCounterLabel, filterStatusLabel;

    // =========================================================================
    // DATA MODELS
    // =========================================================================
    private record Course(String emoji, String name, String author,
                          int progress, double rating, boolean isNew, boolean isPopular) {}

    private final List<Course> allCourses = new ArrayList<>();
    private String currentFilter = "all";
    private int currentCourseIndex = 0;

    private final EventService eventService = new EventService();
    private final SponsorService sponsorService = new SponsorService();

    private List<Event> tousLesEvents;
    private List<Event> eventsFiltres;
    private String currentTypeFilter = "all";
    private boolean showFavoritesOnly = false;

    private List<edu.connexion3a36.entities.Produit> allProduits = new ArrayList<>();

    private static final Map<String, String> TYPE_COLORS = new LinkedHashMap<>() {{
        put("FORMATION", "#4f8ef7");
        put("ÉDUCATION", "#4f8ef7");
        put("EDUCATION", "#4f8ef7");
        put("CONFÉRENCE", "#a855f7");
        put("CONFERENCE", "#a855f7");
        put("WORKSHOP", "#f59e0b");
        put("SPORT", "#22c55e");
        put("SPORTIF", "#22c55e");
        put("CULTURE", "#ec4899");
        put("TECHNOLOGIE", "#06b6d4");
        put("AUTRE", "#f43f5e");
    }};

    // =========================================================================
    // STATE
    // =========================================================================
    private Utilisateur utilisateurConnecte;
    private Button activeNavButton;
    private Map<Button, VBox> navMap;
    private List<Region> allViews;

    // =========================================================================
    // SETTER
    // =========================================================================
    public void setUtilisateurConnecte(Utilisateur u) {
        this.utilisateurConnecte = u;

        int hour = LocalTime.now().getHour();
        String greet = hour < 12 ? "Bonjour" : hour < 18 ? "Bon après-midi" : "Bonsoir";
        String prenom = (u.getPrenom() != null && !u.getPrenom().isEmpty()) ? u.getPrenom() : u.getNom();
        lblGreeting.setText(greet + ", " + prenom + " !");

        if (fieldFirstName != null) fieldFirstName.setText(u.getPrenom());
        if (fieldLastName != null) fieldLastName.setText(u.getNom());
        if (fieldEmail != null) fieldEmail.setText(u.getEmail());
        if (profileFullName != null) profileFullName.setText(u.getNom() + " " + u.getPrenom());

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

    // =========================================================================
    // INITIALIZE
    // =========================================================================
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
        loadPersistentData();
        initializeEventFilters();
    }

    // =========================================================================
    // NAVIGATION
    // =========================================================================
    private void buildNavMap() {
        navMap = new LinkedHashMap<>();
        navMap.put(btnHome, viewHome);
        navMap.put(btnCourses, viewCourses);
        navMap.put(btnProfile, viewProfile);
        navMap.put(btnMessages, viewMessages);
        navMap.put(btnSettings, viewSettings);
        navMap.put(btnRelax, viewRelax);
        navMap.put(btnBoutique, viewBoutique);

        allViews = new ArrayList<>(navMap.values());
        if (viewMedecin != null) allViews.add(viewMedecin);
        if (viewStress != null) allViews.add(viewStress);
        if (viewCatchStress != null) allViews.add(viewCatchStress);
        if (viewFitness != null) allViews.add(viewFitness);
        if (viewNutrition != null) allViews.add(viewNutrition);
        if (viewChatbot != null) allViews.add(viewChatbot);
        if (viewCabinet != null) allViews.add(viewCabinet);
        if (viewRespiration != null) allViews.add(viewRespiration);
        if (viewAPropos != null) allViews.add(viewAPropos);
        if (viewVip != null) allViews.add(viewVip);
        if (viewSleep != null) allViews.add(viewSleep);
        if (viewReclamation != null) allViews.add(viewReclamation);
        if (contentArea != null) allViews.add(contentArea);
        if (viewEvents != null) allViews.add(viewEvents);
        if (viewCart != null) allViews.add(viewCart);
        if (viewCheckout != null) allViews.add(viewCheckout);
    }

    @FXML
    public void handleNav(ActionEvent e) {
        Button src = (Button) e.getSource();

        if (src == btnQuiz) {
            handleQuizNav();
            return;
        }
        if (src == btnEvents) {
            showEventsView();
            return;
        }

        VBox target = navMap.get(src);
        if (target == null) return;
        showView(target);
        setActiveNav(src);
    }

    private void showView(Region target) {
        for (Region v : allViews) {
            if (v != null) {
                v.setVisible(false);
                v.setManaged(false);
            }
        }
        target.setVisible(true);
        target.setManaged(true);

        FadeTransition ft = new FadeTransition(Duration.millis(220), target);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void showEventsView() {
        for (Region v : allViews) {
            if (v != null) {
                v.setVisible(false);
                v.setManaged(false);
            }
        }

        if (viewEvents != null) {
            viewEvents.setVisible(true);
            viewEvents.setManaged(true);
        }
        setActiveNav(btnEvents);

        FadeTransition ft = new FadeTransition(Duration.millis(220), viewEvents);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        chargerEvents();
    }

    private void setActiveNav(Button active) {
        activeNavButton = active;
        String activeStyle = "-fx-font-size: 20px; -fx-background-color: #333333;"
                + " -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 10;";
        String inactiveStyle = "-fx-font-size: 20px; -fx-background-color: transparent;"
                + " -fx-text-fill: #888888; -fx-cursor: hand;";

        for (Button b : navMap.keySet()) b.setStyle(b == active ? activeStyle : inactiveStyle);
        if (btnQuiz != null) btnQuiz.setStyle(btnQuiz == active ? activeStyle : inactiveStyle);
        if (btnEvents != null) btnEvents.setStyle(btnEvents == active ? activeStyle : inactiveStyle);
    }

    private void handleQuizNav() {
        for (Region v : allViews) {
            if (v != null) {
                v.setVisible(false);
                v.setManaged(false);
            }
        }

        if (contentArea != null) {
            contentArea.setVisible(true);
            contentArea.setManaged(true);
        }
        setActiveNav(btnQuiz);

        if (contentArea != null && contentArea.getChildren().isEmpty()) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/UserHomeView.fxml"));
                Node vue = loader.load();
                UserHomeController ctrl = loader.getController();
                ctrl.setContentArea(contentArea);
                contentArea.getChildren().setAll(vue);
            } catch (IOException ex) {
                showAlert("❌ Erreur", "Impossible de charger le module Quiz : " + ex.getMessage());
            }
        }

        if (contentArea != null) {
            FadeTransition ft = new FadeTransition(Duration.millis(220), contentArea);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        }
    }

    @FXML
    public void goHome(ActionEvent e) {
        showView(viewHome);
        setActiveNav(btnHome);
    }

    @FXML
    public void goToProfile(javafx.scene.input.MouseEvent e) {
        showView(viewProfile);
        setActiveNav(btnProfile);
    }

    @FXML
    public void goToRelax(ActionEvent e) {
        showView(viewRelax);
        setActiveNav(btnRelax);
    }

    public void showViewMedecin() {
        showView(viewMedecin);
        setActiveNav(btnRelax);
    }

    @FXML
    public void goToMedecin(ActionEvent e) {
        showView(viewMedecin);
        setActiveNav(btnRelax);
    }

    public void handleOpenChatbot() {
        if (chatbotArea.getChildren().isEmpty()) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/happiness_chatbot.fxml"));
                Node vue = loader.load();
                HappinessChatbotController ctrl = loader.getController();
                ctrl.setDashboardController(this);
                chatbotArea.getChildren().setAll(vue);
            } catch (IOException ex) {
                Label err = new Label("❌ Impossible de charger le chatbot : " + ex.getMessage());
                err.setStyle("-fx-text-fill: #e24b4a; -fx-font-size: 13px; -fx-padding: 24;");
                chatbotArea.getChildren().setAll(err);
            }
        }
        showView(viewChatbot);
        setActiveNav(btnRelax);
    }

    public void handleOpenCabinet() {
        if (cabinetArea.getChildren().isEmpty()) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/cabinet_psychiatre.fxml"));
                Node vue = loader.load();
                CabinetPsychiatreController ctrl = loader.getController();
                ctrl.setDashboardController(this);
                cabinetArea.getChildren().setAll(vue);
            } catch (IOException ex) {
                Label err = new Label("❌ Impossible de charger le cabinet : " + ex.getMessage());
                err.setStyle("-fx-text-fill: #e24b4a; -fx-font-size: 13px; -fx-padding: 24;");
                cabinetArea.getChildren().setAll(err);
            }
        }
        showView(viewCabinet);
        setActiveNav(btnRelax);
    }

    public void handleOpenRespiration() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/respiration.fxml"));
            Node vue = loader.load();
            RespirationController ctrl = loader.getController();
            ctrl.setDashboardController(this);
            respirationArea.getChildren().setAll(vue);
        } catch (IOException ex) {
            Label err = new Label("❌ Impossible de charger l'exercice : " + ex.getMessage());
            err.setStyle("-fx-text-fill: #e24b4a; -fx-font-size: 13px; -fx-padding: 24;");
            respirationArea.getChildren().setAll(err);
        }
        showView(viewRespiration);
        setActiveNav(btnRelax);
    }

    public void handleOpenAPropos() {
        if (aProposArea.getChildren().isEmpty()) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/a_propos.fxml"));
                Node vue = loader.load();
                AProposController ctrl = loader.getController();
                ctrl.setDashboardController(this);
                aProposArea.getChildren().setAll(vue);
            } catch (IOException ex) {
                Label err = new Label("❌ Impossible de charger la page : " + ex.getMessage());
                err.setStyle("-fx-text-fill: #e24b4a; -fx-font-size: 13px; -fx-padding: 24;");
                aProposArea.getChildren().setAll(err);
            }
        }
        showView(viewAPropos);
        setActiveNav(btnRelax);
    }

    @FXML
    public void goToStressOptions(ActionEvent e) {
        showView(viewMedecin);
        setActiveNav(btnRelax);
    }

    // =========================================================================
    // VIP
    // =========================================================================
    @FXML
    public void handleVip(ActionEvent e) {
        if (vipArea.getChildren().isEmpty()) {
            VipPaymentController ctrl = new VipPaymentController();
            ctrl.setDashboardController(this);
            vipArea.getChildren().setAll(ctrl.buildUI());
        }
        showView(viewVip);
        setActiveNav(btnRelax);
    }

    public void handleOpenSleep() {
        if (!VipPaymentController.isVipActive()) {
            showView(viewVip);
            setActiveNav(btnRelax);
            return;
        }
        SleepChatbotController ctrl = new SleepChatbotController();
        ctrl.setDashboardController(this);
        sleepArea.getChildren().setAll(ctrl.buildUI());
        showView(viewSleep);
        setActiveNav(btnRelax);
    }

    @FXML
    public void goToVip(ActionEvent e) {
        showView(viewVip);
        setActiveNav(btnRelax);
    }

    // =========================================================================
    // RÉCLAMATION
    // =========================================================================
    @FXML
    public void handleEnvoyerReclamation(ActionEvent e) {
        showView(viewReclamation);
        setActiveNav(btnRelax);
    }

    @FXML
    public void handleSendReclamation(ActionEvent e) {
        String message = reclamationTextArea.getText();
        if (message == null || message.trim().isEmpty()) {
            showAlert("Veuillez écrire une réclamation.");
            return;
        }
        if (message.length() > 160) {
            showAlert("La réclamation doit être courte (max 160 caractères). Veuillez la résumer.");
            return;
        }

        try {
            String accountSid = "aaa";
            String authToken = "bb";
            String toNumber = "+21652176756";
            String messagingSid = "MG132a322f96e964c43e508bba78e7bb84";
            String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";

            ProcessBuilder pb = new ProcessBuilder(
                    "curl", "-s", "-w", "\n%{http_code}",
                    url,
                    "-X", "POST",
                    "--data-urlencode", "To=" + toNumber,
                    "--data-urlencode", "MessagingServiceSid=" + messagingSid,
                    "--data-urlencode", "Body=" + message,
                    "-u", accountSid + ":" + authToken
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String response = new String(process.getInputStream().readAllBytes());
            process.waitFor();

            String[] lines = response.trim().split("\n");
            String httpCode = lines[lines.length - 1].trim();

            if (httpCode.startsWith("2")) {
                showAlert("✅ Réclamation envoyée avec succès !");
                reclamationTextArea.clear();
            } else {
                String body = lines.length > 1 ? lines[0] : response;
                showAlert("❌ Erreur lors de l'envoi (HTTP " + httpCode + ").\n" + body);
            }
        } catch (Exception ex) {
            showAlert("Erreur : " + ex.getMessage());
        }
    }

    // =========================================================================
    // RELAXATION
    // =========================================================================
    @FXML
    public void handleConsulterMedecin(ActionEvent e) {
        loadConsultationForm();
        showView(viewMedecin);
        setActiveNav(btnRelax);
    }

    public void loadConsultationForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouteetudiant.fxml"));
            Node vue = loader.load();
            AjouterConsultationEtudiantController ctrl = loader.getController();
            ctrl.setDashboardController(this);
            medecinArea.getChildren().setAll(vue);
            if (lblMedecinTitle != null) lblMedecinTitle.setText("🩺  Consulter un Médecin");
        } catch (IOException ex) {
            Label err = new Label("❌ Impossible de charger le formulaire : " + ex.getMessage());
            err.setStyle("-fx-text-fill: #e24b4a; -fx-font-size: 13px; -fx-padding: 24;");
            medecinArea.getChildren().setAll(err);
        }
    }

    public void handleFormSubmitSuccess() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText("✅ Consultation enregistrée avec succès !");
        alert.showAndWait();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/stress_options.fxml"));
            Node vue = loader.load();
            StressOptionsController ctrl = loader.getController();
            ctrl.setDashboardController(this);
            medecinArea.getChildren().setAll(vue);
            if (lblMedecinTitle != null) lblMedecinTitle.setText("🧘  Options Anti-Stress");
        } catch (IOException ex) {
            Label err = new Label("❌ Impossible de charger les options : " + ex.getMessage());
            err.setStyle("-fx-text-fill: #e24b4a; -fx-font-size: 13px; -fx-padding: 24;");
            medecinArea.getChildren().setAll(err);
        }

        showView(viewMedecin);
        setActiveNav(btnRelax);
    }

    @FXML
    public void handleCalculerScore(ActionEvent e) {
        if (stressArea.getChildren().isEmpty()) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterStressSurveyEtudiant.fxml"));
                Node vue = loader.load();
                stressArea.getChildren().setAll(vue);
            } catch (IOException ex) {
                Label err = new Label("❌ Impossible de charger le formulaire : " + ex.getMessage());
                err.setStyle("-fx-text-fill: #e24b4a; -fx-font-size: 13px; -fx-padding: 24;");
                stressArea.getChildren().setAll(err);
            }
        }
        showView(viewStress);
        setActiveNav(btnRelax);
    }

    @FXML
    public void handleCatchStress(ActionEvent e) {
        if (catchStressArea.getChildren().isEmpty()) {
            VBox catchStressContent = new VBox(20);
            catchStressContent.setPadding(new Insets(30));
            catchStressContent.setStyle("-fx-background-color: #f5f5f5;");

            Label title = new Label("🎯 Catch the Stress - Exercices Anti-Stress");
            title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #c62828;");

            Label subtitle = new Label("Libérez vos tensions avec ces exercices simples et efficaces");
            subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #666; -fx-font-style: italic;");

            VBox timerBox = new VBox(15);
            timerBox.setAlignment(Pos.CENTER);
            timerBox.setStyle("-fx-background-color: linear-gradient(to bottom, #ffebee, #ffcdd2); " +
                    "-fx-background-radius: 15; -fx-padding: 25; -fx-border-color: #ef5350; " +
                    "-fx-border-width: 2; -fx-border-radius: 15;");

            Label timerLabel = new Label("00:00");
            timerLabel.setStyle("-fx-font-size: 48px; -fx-font-weight: bold; -fx-text-fill: #c62828;");

            Label motivationLabel = new Label("💪 Prêt à commencer ? Choisissez un exercice !");
            motivationLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #d32f2f; -fx-font-weight: bold; " +
                    "-fx-wrap-text: true; -fx-text-alignment: center;");
            motivationLabel.setMaxWidth(500);

            HBox timerControls = new HBox(15);
            timerControls.setAlignment(Pos.CENTER);

            Button startTimerBtn = new Button("▶ Démarrer");
            startTimerBtn.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; " +
                    "-fx-font-size: 14px; -fx-padding: 10 25; -fx-background-radius: 20; -fx-cursor: hand;");

            Button pauseTimerBtn = new Button("⏸ Pause");
            pauseTimerBtn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; " +
                    "-fx-font-size: 14px; -fx-padding: 10 25; -fx-background-radius: 20; -fx-cursor: hand;");
            pauseTimerBtn.setDisable(true);

            Button resetTimerBtn = new Button("⏹ Reset");
            resetTimerBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; " +
                    "-fx-font-size: 14px; -fx-padding: 10 25; -fx-background-radius: 20; -fx-cursor: hand;");
            resetTimerBtn.setDisable(true);

            timerControls.getChildren().addAll(startTimerBtn, pauseTimerBtn, resetTimerBtn);
            timerBox.getChildren().addAll(timerLabel, motivationLabel, timerControls);

            String[] motivationMessages = {
                    "💪 Excellent ! Continue comme ça !",
                    "🔥 Tu es en feu ! Ne lâche rien !",
                    "⭐ Bravo ! Chaque seconde compte !",
                    "🎯 Parfait ! Tu gères ton stress !",
                    "✨ Superbe effort ! Tu es incroyable !",
                    "🌟 Continue ! Tu es sur la bonne voie !",
                    "💯 Fantastique ! Tu es un champion !",
                    "🚀 Incroyable ! Tu dépasses tes limites !"
            };

            final int[] seconds = {0};
            final boolean[] isRunning = {false};
            final Timeline[] timeline = {null};

            startTimerBtn.setOnAction(ev -> {
                if (!isRunning[0]) {
                    isRunning[0] = true;
                    startTimerBtn.setDisable(true);
                    pauseTimerBtn.setDisable(false);
                    resetTimerBtn.setDisable(false);

                    timeline[0] = new Timeline(
                            new KeyFrame(Duration.seconds(1), event -> {
                                seconds[0]++;
                                int mins = seconds[0] / 60;
                                int secs = seconds[0] % 60;
                                timerLabel.setText(String.format("%02d:%02d", mins, secs));

                                if (seconds[0] % 10 == 0) {
                                    int index = (seconds[0] / 10) % motivationMessages.length;
                                    motivationLabel.setText(motivationMessages[index]);
                                }
                            })
                    );
                    timeline[0].setCycleCount(Timeline.INDEFINITE);
                    timeline[0].play();
                }
            });

            pauseTimerBtn.setOnAction(ev -> {
                if (isRunning[0] && timeline[0] != null) {
                    timeline[0].pause();
                    isRunning[0] = false;
                    startTimerBtn.setDisable(false);
                    pauseTimerBtn.setDisable(true);
                    motivationLabel.setText("⏸ Pause - Reprends quand tu es prêt !");
                }
            });

            resetTimerBtn.setOnAction(ev -> {
                if (timeline[0] != null) {
                    timeline[0].stop();
                }
                seconds[0] = 0;
                isRunning[0] = false;
                timerLabel.setText("00:00");
                startTimerBtn.setDisable(false);
                pauseTimerBtn.setDisable(true);
                resetTimerBtn.setDisable(true);
                motivationLabel.setText("💪 Prêt à recommencer ? Tu peux le faire !");
            });

            Label exercisesTitle = new Label("📋 Exercices Anti-Stress Simples");
            exercisesTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #c62828; -fx-padding: 20 0 10 0;");

            ScrollPane exercisesScroll = new ScrollPane();
            exercisesScroll.setFitToWidth(true);
            exercisesScroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

            VBox exercisesList = new VBox(15);

            VBox ex1 = createExerciseCard(
                    "1️⃣ Étirements du Cou",
                    "Durée: 2-3 minutes",
                    new String[]{
                            "• Asseyez-vous confortablement, dos droit",
                            "• Inclinez lentement la tête vers la droite (10 sec)",
                            "• Revenez au centre, puis inclinez vers la gauche (10 sec)",
                            "• Inclinez la tête vers l'avant, menton vers la poitrine (10 sec)",
                            "• Faites 3 rotations complètes douces dans chaque sens",
                            "• Respirez profondément pendant l'exercice"
                    },
                    "#e3f2fd",
                    "#1976d2"
            );

            VBox ex2 = createExerciseCard(
                    "2️⃣ Respiration Profonde 4-7-8",
                    "Durée: 3-4 minutes",
                    new String[]{
                            "• Asseyez-vous confortablement, fermez les yeux",
                            "• Inspirez par le nez pendant 4 secondes",
                            "• Retenez votre souffle pendant 7 secondes",
                            "• Expirez lentement par la bouche pendant 8 secondes",
                            "• Répétez ce cycle 4 fois minimum",
                            "• Sentez votre corps se détendre à chaque expiration"
                    },
                    "#f3e5f5",
                    "#7b1fa2"
            );

            VBox ex3 = createExerciseCard(
                    "3️⃣ Étirements des Épaules",
                    "Durée: 2-3 minutes",
                    new String[]{
                            "• Debout ou assis, dos droit",
                            "• Levez les épaules vers les oreilles (5 sec)",
                            "• Relâchez brusquement - répétez 5 fois",
                            "• Faites 10 rotations d'épaules vers l'arrière",
                            "• Faites 10 rotations d'épaules vers l'avant",
                            "• Croisez les bras et étirez le haut du dos (15 sec)"
                    },
                    "#e8f5e9",
                    "#388e3c"
            );

            VBox ex4 = createExerciseCard(
                    "4️⃣ Étirements des Poignets et Mains",
                    "Durée: 2 minutes",
                    new String[]{
                            "• Tendez le bras droit devant vous, paume vers le haut",
                            "• Avec la main gauche, tirez doucement les doigts vers vous (15 sec)",
                            "• Répétez avec la paume vers le bas (15 sec)",
                            "• Changez de bras et répétez",
                            "• Faites 10 rotations de poignets dans chaque sens",
                            "• Serrez les poings fort puis ouvrez grand les mains (10 fois)"
                    },
                    "#fff3e0",
                    "#f57c00"
            );

            VBox ex5 = createExerciseCard(
                    "5️⃣ Marche Active sur Place",
                    "Durée: 3-5 minutes",
                    new String[]{
                            "• Debout, dos droit, regardez devant vous",
                            "• Marchez sur place en levant bien les genoux",
                            "• Balancez les bras naturellement",
                            "• Augmentez progressivement le rythme",
                            "• Ajoutez des montées de genoux plus hautes",
                            "• Ralentissez progressivement les 30 dernières secondes"
                    },
                    "#fce4ec",
                    "#c2185b"
            );

            VBox ex6 = createExerciseCard(
                    "6️⃣ Étirements du Dos (Chat-Vache)",
                    "Durée: 2-3 minutes",
                    new String[]{
                            "• À quatre pattes, mains sous les épaules, genoux sous les hanches",
                            "• Inspirez: creusez le dos, levez la tête (position vache)",
                            "• Expirez: arrondissez le dos, rentrez le menton (position chat)",
                            "• Alternez lentement 10 fois",
                            "• Sentez chaque vertèbre bouger",
                            "• Terminez en position neutre, respirez profondément"
                    },
                    "#e0f2f1",
                    "#00796b"
            );

            exercisesList.getChildren().addAll(ex1, ex2, ex3, ex4, ex5, ex6);
            exercisesScroll.setContent(exercisesList);
            exercisesScroll.setPrefHeight(400);

            VBox tipsBox = new VBox(10);
            tipsBox.setStyle("-fx-background-color: #fff8e1; -fx-background-radius: 10; -fx-padding: 20; " +
                    "-fx-border-color: #fbc02d; -fx-border-width: 2; -fx-border-radius: 10;");

            Label tipsTitle = new Label("💡 Conseils pour Maximiser les Bienfaits");
            tipsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #f57f17;");

            Label tips = new Label(
                    "✓ Pratiquez ces exercices 2-3 fois par jour\n" +
                            "✓ Choisissez un endroit calme et confortable\n" +
                            "✓ Portez des vêtements amples\n" +
                            "✓ Ne forcez jamais - l'étirement doit être agréable\n" +
                            "✓ Respirez profondément pendant chaque exercice\n" +
                            "✓ Hydratez-vous avant et après\n" +
                            "✓ Soyez régulier pour des résultats durables"
            );
            tips.setStyle("-fx-font-size: 13px; -fx-text-fill: #555; -fx-line-spacing: 5;");

            tipsBox.getChildren().addAll(tipsTitle, tips);

            Button backBtn = new Button("← Retour aux Options Anti-Stress");
            backBtn.setStyle("-fx-background-color: #c62828; -fx-text-fill: white; -fx-font-size: 14px; " +
                    "-fx-padding: 12 30; -fx-background-radius: 10; -fx-cursor: hand;");
            backBtn.setOnAction(ev -> goToRelax(ev));

            catchStressContent.getChildren().addAll(
                    title, subtitle, timerBox, exercisesTitle, exercisesScroll, tipsBox, backBtn
            );

            catchStressArea.getChildren().setAll(catchStressContent);
        }
        showView(viewCatchStress);
        setActiveNav(btnRelax);
    }

    private VBox createExerciseCard(String title, String duration, String[] steps,
                                    String bgColor, String accentColor) {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 12; " +
                "-fx-padding: 20; -fx-border-color: " + accentColor + "; " +
                "-fx-border-width: 2; -fx-border-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2);");

        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + accentColor + ";");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label durationLabel = new Label(duration);
        durationLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + accentColor + "; " +
                "-fx-background-color: white; -fx-padding: 5 12; -fx-background-radius: 15;");

        header.getChildren().addAll(titleLabel, spacer, durationLabel);

        VBox stepsBox = new VBox(6);
        for (String step : steps) {
            Label stepLabel = new Label(step);
            stepLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #333; -fx-wrap-text: true;");
            stepLabel.setMaxWidth(Double.MAX_VALUE);
            stepsBox.getChildren().add(stepLabel);
        }

        card.getChildren().addAll(header, stepsBox);
        return card;
    }

    @FXML
    public void handleFitness(ActionEvent e) {
        if (fitnessArea.getChildren().isEmpty()) {
            VBox fitnessContent = new VBox(20);
            fitnessContent.setPadding(new Insets(20));
            fitnessContent.setStyle("-fx-background-color: #f5f5f5;");

            Label title = new Label("💪 Exercices Avances");
            title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #0277bd;");

            Label durationLabel = new Label("Duree de l'exercice (minutes):");
            durationLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #555;");

            Slider durationSlider = new Slider(5, 60, 15);
            durationSlider.setStyle("-fxPref-width: 300;");
            Label durationValue = new Label("15 minutes");
            durationValue.setStyle("-fx-font-size: 14px; -fx-text-fill: #0277bd;");
            durationSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                durationValue.setText(newVal.intValue() + " minutes");
            });

            HBox exerciseButtons = new HBox(15);
            exerciseButtons.setAlignment(Pos.CENTER);

            Button btnWarmup = new Button("Echauffement");
            btnWarmup.setStyle("-fx-background-color: #4fc3f7; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 15 25; -fx-background-radius: 10;");

            Button btnCardio = new Button("Cardio");
            btnCardio.setStyle("-fx-background-color: #039be5; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 15 25; -fx-background-radius: 10;");

            Button btnStrength = new Button("Force");
            btnStrength.setStyle("-fx-background-color: #0277bd; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 15 25; -fx-background-radius: 10;");

            Button btnStretch = new Button("Etirements");
            btnStretch.setStyle("-fx-background-color: #01579b; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 15 25; -fx-background-radius: 10;");

            exerciseButtons.getChildren().addAll(btnWarmup, btnCardio, btnStrength, btnStretch);

            Label advancedLabel = new Label("Exercices Avances:");
            advancedLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0277bd; -fx-padding: 20 0 10 0;");

            HBox advancedExercises = new HBox(15);
            advancedExercises.setAlignment(Pos.CENTER);

            Button btnYoga = new Button("Yoga 3D");
            btnYoga.setStyle("-fx-background-color: #7c4dff; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 15 25; -fx-background-radius: 10;");

            Button btnPilates = new Button("Pilates");
            btnPilates.setStyle("-fx-background-color: #651fff; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 15 25; -fx-background-radius: 10;");

            Button btnHIIT = new Button("HIIT");
            btnHIIT.setStyle("-fx-background-color: #536dfe; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 15 25; -fx-background-radius: 10;");

            Button btnMeditation = new Button("Meditation");
            btnMeditation.setStyle("-fx-background-color: #3d5afe; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 15 25; -fx-background-radius: 10;");

            advancedExercises.getChildren().addAll(btnYoga, btnPilates, btnHIIT, btnMeditation);

            Button backBtn = new Button("← Retour");
            backBtn.setStyle("-fx-background-color: #0277bd; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 12 30; -fx-background-radius: 10; -fx-cursor: hand;");
            backBtn.setOnAction(ev -> goToRelax(ev));

            Label infoLabel = new Label("Selectnez un type d'exercice et ajustez la duree selon vos besoins.");
            infoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #888; -fx-padding: 10 0 0 0;");

            fitnessContent.getChildren().addAll(title, durationLabel, durationSlider, durationValue, exerciseButtons, advancedLabel, advancedExercises, backBtn, infoLabel);
            fitnessArea.getChildren().setAll(fitnessContent);
        }
        showView(viewFitness);
        setActiveNav(btnRelax);
    }

    @FXML
    public void handleNutrition(ActionEvent e) {
        if (nutritionArea.getChildren().isEmpty()) {
            VBox nutritionContent = new VBox(20);
            nutritionContent.setPadding(new Insets(20));
            nutritionContent.setStyle("-fx-background-color: #f5f5f5;");

            Label title = new Label("🥗 Nutrition et Bien-Etre");
            title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2e7d32;");

            Label foodLabel = new Label("Aliments pour booster votre energie:");
            foodLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #555;");

            FlowPane foodGrid = new FlowPane(15, 15);
            foodGrid.setPrefWrapLength(500);

            String[][] foods = {
                    {"Banane", "89 cal", "#fff176"},
                    {"Avocat", "160 cal", "#a5d6a7"},
                    {"Noix", "185 cal", "#d7ccc8"},
                    {"Miel", "64 cal", "#ffe082"},
                    {"Chocolat Noir", "170 cal", "#5d4037"},
                    {"The Vert", "2 cal", "#c8e6c9"},
                    {"Blueberries", "57 cal", "#90caf9"},
                    {"Saumon", "208 cal", "#ef9a9a"},
                    {"Eggs", "78 cal", "#fff59d"},
                    {"Legumes Verts", "35 cal", "#a5d6a7"}
            };

            for (String[] food : foods) {
                VBox foodCard = new VBox(5);
                foodCard.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 15; -fx-min-width: 100;");
                Label foodName = new Label(food[0]);
                foodName.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
                Label foodCals = new Label(food[1] + " cal");
                foodCals.setStyle("-fx-font-size: 12px; -fx-text-fill: #2e7d32;");
                foodCard.getChildren().addAll(foodName, foodCals);
                foodGrid.getChildren().add(foodCard);
            }

            Label calcTitle = new Label("Calculateur de Calories");
            calcTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2e7d32; -fx-padding: 20 0 10 0;");

            HBox calcRow = new HBox(15);
            calcRow.setAlignment(Pos.CENTER);
            Label weightLabel = new Label("Poids (g):");
            weightLabel.setStyle("-fx-font-size: 14px;");
            TextField weightField = new TextField("100");
            weightField.setStyle("-fx-pref-width: 80; -fx-padding: 5;");
            Label calsPerGramLabel = new Label("Calories pour 100g:");
            calsPerGramLabel.setStyle("-fx-font-size: 14px;");
            TextField calsField = new TextField("89");
            calsField.setStyle("-fx-pref-width: 80; -fx-padding: 5;");
            Button calcBtn = new Button("Calculer");
            calcBtn.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-padding: 8 20; -fx-background-radius: 8;");
            Label resultLabel = new Label("Resultat: 89 cal");
            resultLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2e7d32;");
            calcBtn.setOnAction(ev -> {
                try {
                    double weight = Double.parseDouble(weightField.getText());
                    double calsPer100 = Double.parseDouble(calsField.getText());
                    double totalCals = (weight * calsPer100) / 100;
                    resultLabel.setText("Resultat: " + String.format("%.0f", totalCals) + " cal");
                } catch (NumberFormatException ex) {
                    resultLabel.setText("Erreur: Entrez des nombres");
                }
            });
            calcRow.getChildren().addAll(weightLabel, weightField, calsPerGramLabel, calsField, calcBtn, resultLabel);

            Label joyLabel = new Label("Aliments pour ameliorer l'humeur:");
            joyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #555; -fx-padding: 20 0 10 0;");

            FlowPane joyGrid = new FlowPane(15, 15);
            joyGrid.setPrefWrapLength(500);
            String[][] joyFoods = {
                    {"Chocolat", "170 cal", "🍫"},
                    {"Fraises", "32 cal", "🍓"},
                    {"Mangue", "60 cal", "🥭"},
                    {"The Matcha", "2 cal", "🍵"}
            };
            for (String[] joy : joyFoods) {
                HBox joyCard = new HBox(10);
                joyCard.setStyle("-fx-background-color: #fff8e1; -fx-background-radius: 10; -fx-padding: 15;");
                Label emoji = new Label(joy[2]);
                emoji.setStyle("-fx-font-size: 24px;");
                VBox joyInfo = new VBox(2);
                Label joyName = new Label(joy[0]);
                joyName.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
                Label joyCals = new Label(joy[1] + " cal - Bonheur!");
                joyCals.setStyle("-fx-font-size: 11px; -fx-text-fill: #f57f17;");
                joyInfo.getChildren().addAll(joyName, joyCals);
                joyCard.getChildren().addAll(emoji, joyInfo);
                joyGrid.getChildren().add(joyCard);
            }

            nutritionContent.getChildren().addAll(title, foodLabel, foodGrid, calcTitle, calcRow, joyLabel, joyGrid);

            Button backBtn = new Button("← Retour");
            backBtn.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 12 30; -fx-background-radius: 10; -fx-cursor: hand;");
            backBtn.setOnAction(ev -> goToRelax(ev));
            nutritionContent.getChildren().add(backBtn);
            nutritionArea.getChildren().setAll(nutritionContent);
        }
        showView(viewNutrition);
        setActiveNav(btnRelax);
    }

    // =========================================================================
    // BOUTIQUE
    // =========================================================================
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
        } catch (SQLException e) {
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

        StackPane imgContainer = new StackPane();
        imgContainer.setPrefHeight(130);
        imgContainer.setStyle("-fx-background-color: #E8F0FE; -fx-background-radius: 18 18 0 0;");

        String imageUrl = p.getImage();
        boolean isValidUrl = false;
        try {
            new java.net.URL(imageUrl);
            isValidUrl = true;
        } catch (Exception ignored) {
        }

        if (isValidUrl && imageUrl != null && !imageUrl.trim().isEmpty()) {
            try {
                Image image = new Image(imageUrl, 200, 130, false, true);
                if (!image.isError()) {
                    imageView.setImage(image);
                    imgContainer.getChildren().add(imageView);
                } else {
                    imgContainer.getChildren().add(placeholderLabel("🖼️"));
                }
            } catch (Exception ex) {
                imgContainer.getChildren().add(placeholderLabel("🖼️"));
            }
        } else {
            imgContainer.getChildren().add(placeholderLabel("🖼️"));
        }

        VBox info = new VBox(6);
        info.setPadding(new Insets(0, 14, 0, 14));
        Label nomLabel = new Label(p.getNom());
        nomLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1A1A2E; -fx-wrap-text: true;");
        nomLabel.setMaxWidth(172);
        String catNom = catMap.getOrDefault(p.getTypeCategorieId(), "Inconnue");
        Label catLabel = new Label("🏷️ " + catNom);
        catLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #5c6bc0; -fx-background-color: #e8eaf6; -fx-background-radius: 20; -fx-padding: 3 8;");
        Label prixLabel = new Label(p.getPrix() + " DT");
        prixLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2979FF;");

        Button btnAjouter = new Button("＋ Ajouter");
        String styleNormal = "-fx-background-color: #2979FF; -fx-text-fill: white;"
                + "-fx-background-radius: 10; -fx-font-size: 12px;"
                + "-fx-font-weight: bold; -fx-padding: 8 0; -fx-cursor: hand;";
        String styleAdded = "-fx-background-color: #4CAF50; -fx-text-fill: white;"
                + "-fx-background-radius: 10; -fx-font-size: 12px;"
                + "-fx-font-weight: bold; -fx-padding: 8 0; -fx-cursor: hand;";

        btnAjouter.setStyle(styleNormal);
        btnAjouter.setMaxWidth(Double.MAX_VALUE);

        btnAjouter.setOnAction(ev -> {
            CartService.getInstance().ajouterProduit(p);
            updateCartBadge();

            btnAjouter.setText("✔ Ajouté !");
            btnAjouter.setStyle(styleAdded);

            PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
            pause.setOnFinished(e -> {
                btnAjouter.setText("＋ Ajouter");
                btnAjouter.setStyle(styleNormal);
            });
            pause.play();
        });

        info.getChildren().addAll(nomLabel, catLabel, prixLabel, btnAjouter);
        card.getChildren().addAll(imgContainer, info);
        return card;
    }

    private void updateCartBadge() {
        int n = CartService.getInstance().getNombreArticles();
        if (cartBadge != null) {
            cartBadge.setText(String.valueOf(n));
            cartBadge.setVisible(n > 0);
        }
    }

    @FXML
    public void handleOpenCart(javafx.scene.input.MouseEvent e) {
        showView(viewCart);
        renderCart();
        setActiveNav(btnBoutique);
    }

    @FXML
    public void handleOpenBoutique(ActionEvent e) {
        showView(viewBoutique);
        setActiveNav(btnBoutique);
    }

    @FXML
    public void handleViderCart(ActionEvent e) {
        CartService.getInstance().vider();
        updateCartBadge();
        renderCart();
    }

    private void renderCart() {
        cartItemList.getChildren().clear();
        List<CartItem> items = CartService.getInstance().getItems();

        if (items.isEmpty()) {
            Label empty = new Label("🛒 Votre panier est vide.");
            empty.setStyle("-fx-font-size: 15px; -fx-text-fill: #888; -fx-padding: 20;");
            cartItemList.getChildren().add(empty);
        } else {
            items.forEach(item -> cartItemList.getChildren().add(buildCartRow(item)));
        }
        cartTotalLabel.setText(String.format("Total : %.2f DT", CartService.getInstance().getTotal()));
    }

    private HBox buildCartRow(CartItem item) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14));
        row.setStyle("-fx-background-color: white; -fx-background-radius: 14;" +
                "-fx-border-color: #f0f0f0; -fx-border-radius: 14;");

        Label nom = new Label(item.getProduit().getNom());
        nom.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        HBox.setHgrow(nom, Priority.ALWAYS);

        Label qte = new Label("x" + item.getQuantite());
        qte.setStyle("-fx-font-size: 13px; -fx-text-fill: #666;");

        Label prix = new Label(String.format("%.2f DT", item.getSousTotal()));
        prix.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2979FF;");

        Button suppr = new Button("✕");
        suppr.setStyle("-fx-background-color: #fff0f0; -fx-text-fill: #e24b4a;" +
                "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 4 8;");
        suppr.setOnAction(e -> {
            CartService.getInstance().supprimerItem(item);
            updateCartBadge();
            renderCart();
        });

        row.getChildren().addAll(nom, qte, prix, suppr);
        return row;
    }

    @FXML
    public void handlePayer(ActionEvent e) {
        if (CartService.getInstance().getItems().isEmpty()) {
            showAlert("Panier vide", "Ajoutez des produits avant de payer.");
            return;
        }
        checkoutTotalLabel.setText(String.format("Total : %.2f DT", CartService.getInstance().getTotal()));
        showView(viewCheckout);
    }

    @FXML
    public void handleBackToCart(ActionEvent e) {
        showView(viewCart);
        renderCart();
    }

    @FXML
    public void handleConfirmerPaiement(ActionEvent e) {
        String nom = checkoutNom.getText().trim();
        String email = checkoutEmail.getText().trim();
        String adresse = checkoutAdresse.getText().trim();

        if (nom.isEmpty() || email.isEmpty() || adresse.isEmpty()) {
            showAlert("Champs manquants", "Veuillez remplir tous les champs.");
            return;
        }
        if (!email.contains("@")) {
            showAlert("Email invalide", "Veuillez entrer un email valide.");
            return;
        }

        List<CartItem> items = CartService.getInstance().getItems();
        double total = CartService.getInstance().getTotal();

        try {
            String stripeUrl = StripeService.creerSession(email, items);
            java.awt.Desktop.getDesktop().browse(new java.net.URI(stripeUrl));

            new Thread(() -> {
                EmailService.envoyerConfirmation(email, nom, items, total);
            }).start();

            CartService.getInstance().vider();
            updateCartBadge();

            showAlert("✅ Commande confirmée !",
                    "Redirection vers Stripe...\nUn email de confirmation sera envoyé à : " + email);

            showView(viewBoutique);

        } catch (Exception ex) {
            showAlert("❌ Erreur", "Erreur : " + ex.getMessage());
        }
    }

    private Label placeholderLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 40px;");
        return l;
    }

    // =========================================================================
    // COURSES
    // =========================================================================
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
        allCourses.stream()
                .filter(c -> switch (filter) {
                    case "new" -> c.isNew();
                    case "top" -> c.rating() >= 4.7;
                    case "popular" -> c.isPopular();
                    default -> true;
                })
                .forEach(c -> courseList.getChildren().add(buildCourseRow(c)));
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
        return allCourses.stream().filter(c -> c.progress() > 0 && c.progress() < 100).toList();
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

    @FXML
    public void handlePrevCourse(ActionEvent e) {
        List<Course> list = getInProgressCourses();
        currentCourseIndex = (currentCourseIndex - 1 + list.size()) % list.size();
        updateCurrentCourse();
    }

    @FXML
    public void handleNextCourse(ActionEvent e) {
        List<Course> list = getInProgressCourses();
        currentCourseIndex = (currentCourseIndex + 1) % list.size();
        updateCurrentCourse();
    }

    @FXML
    public void handleContinue(ActionEvent e) {
        List<Course> list = getInProgressCourses();
        if (list.isEmpty()) return;
        showAlert("Continuer", "Reprise du cours : " + list.get(currentCourseIndex).name() + " 🚀");
    }

    @FXML
    public void handleFilter(ActionEvent e) {
        Button src = (Button) e.getSource();
        currentFilter = (String) src.getUserData();
        renderCourseList(currentFilter);

        String activeStyle = "-fx-background-color: transparent; -fx-text-fill: #111;"
                + " -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 4 0;"
                + " -fx-border-color: transparent transparent #111 transparent; -fx-border-width: 0 0 2 0;";
        String inactiveStyle = "-fx-background-color: transparent; -fx-text-fill: #888;"
                + " -fx-cursor: hand; -fx-padding: 4 0;";
        for (Button b : List.of(filterAll, filterNew, filterTop, filterPopular))
            b.setStyle(b == src ? activeStyle : inactiveStyle);
    }

    // =========================================================================
    // CHART
    // =========================================================================
    private void initChart() {
        weeklyFilter.setItems(FXCollections.observableArrayList("Cette semaine", "Ce mois", "3 mois", "6 mois"));
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

    @FXML
    public void handleChartTab(ActionEvent e) {
        Button src = (Button) e.getSource();
        String type = (String) src.getUserData();
        loadChartData(type);
        String activeStyle = "-fx-background-color: transparent; -fx-font-weight: bold;"
                + " -fx-font-size: 12px; -fx-text-fill: #111; -fx-cursor: hand;"
                + " -fx-border-color: transparent transparent #111 transparent; -fx-border-width: 0 0 2 0;";
        String inactiveStyle = "-fx-background-color: transparent; -fx-font-size: 12px;"
                + " -fx-text-fill: #aaa; -fx-cursor: hand;";
        chartTabHours.setStyle("hours".equals(type) ? activeStyle : inactiveStyle);
        chartTabCourses.setStyle("courses".equals(type) ? activeStyle : inactiveStyle);
    }

    @FXML
    public void handlePeriodChange(ActionEvent e) {
        loadChartData("hours");
    }

    @FXML
    public void handleSearch(KeyEvent e) {
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            renderCourseList(currentFilter);
            return;
        }
        courseList.getChildren().clear();
        allCourses.stream()
                .filter(c -> c.name().toLowerCase().contains(query) || c.author().toLowerCase().contains(query))
                .forEach(c -> courseList.getChildren().add(buildCourseRow(c)));
    }

    // =========================================================================
    // MESSAGES
    // =========================================================================
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
                + "-fx-background-radius: 50%; -fx-min-width: 46; -fx-min-height: 46; -fx-alignment: center;");

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

    // =========================================================================
    // SETTINGS
    // =========================================================================
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
        row.setStyle("-fx-border-color: transparent transparent #f0f0f0 transparent; -fx-border-width: 0 0 1 0;");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 13px;");
        HBox.setHgrow(lbl, Priority.ALWAYS);
        CheckBox cb = new CheckBox();
        cb.setSelected(defaultVal);
        cb.setStyle("-fx-cursor: hand;");
        row.getChildren().addAll(lbl, cb);
        return row;
    }

    @FXML
    public void handleChangePassword(ActionEvent e) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Changer le mot de passe");
        dialog.setHeaderText("Entrez votre nouveau mot de passe");

        ButtonType confirmerBtn = new ButtonType("Confirmer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmerBtn, ButtonType.CANCEL);

        PasswordField newMdp = new PasswordField();
        newMdp.setPromptText("Nouveau mot de passe");
        PasswordField confirmMdp = new PasswordField();
        confirmMdp.setPromptText("Confirmer le mot de passe");
        VBox content = new VBox(10,
                new Label("Nouveau mot de passe :"), newMdp, new Label("Confirmer :"), confirmMdp);
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
                new UtilisateurService().updateEntity(utilisateurConnecte.getId().intValue(), utilisateurConnecte);
                showAlert("✅ Succès", "Mot de passe changé avec succès !");
            } catch (SQLException ex) {
                showAlert("❌ Erreur", "Erreur : " + ex.getMessage());
            }
        });
    }

    @FXML
    public void handleDeleteAccount(ActionEvent e) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer le compte");
        confirm.setHeaderText("Êtes-vous sûr de vouloir supprimer votre compte ?");
        confirm.setContentText("Cette action est irréversible.");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    new UtilisateurService().deleteEntity(utilisateurConnecte);
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

    // =========================================================================
    // COURSES GRID
    // =========================================================================
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
            n.setStyle("-fx-background-color: #e8f5e9; -fx-text-fill: #2e7d32; -fx-padding: 2 7; -fx-background-radius: 20; -fx-font-size: 10px;");
            tags.getChildren().add(n);
        }
        if (c.isPopular()) {
            Label p = new Label("🔥 Populaire");
            p.setStyle("-fx-background-color: #fff3e0; -fx-text-fill: #e65100; -fx-padding: 2 7; -fx-background-radius: 20; -fx-font-size: 10px;");
            tags.getChildren().add(p);
        }
        card.getChildren().addAll(emoji, name, author, pb, pct, tags);
        return card;
    }

    // =========================================================================
    // PROFILE SAVE
    // =========================================================================
    @FXML
    public void handleSaveProfile(ActionEvent e) {
        String prenom = fieldFirstName.getText().trim();
        String nom = fieldLastName.getText().trim();
        String email = fieldEmail.getText().trim();

        String msgPrenom = Validation.messageNom(prenom);
        String msgNom = Validation.messageNom(nom);
        String msgEmail = Validation.messageEmail(email);

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

        utilisateurConnecte.setPrenom(prenom);
        utilisateurConnecte.setNom(nom);
        utilisateurConnecte.setEmail(email);

        try {
            new UtilisateurService().updateEntity(utilisateurConnecte.getId().intValue(), utilisateurConnecte);
            profileFullName.setText(nom + " " + prenom);
            lblGreeting.setText("Bonjour, " + prenom + " !");
            showAlert("✅ Succès", "Votre profil a été mis à jour avec succès !");
        } catch (SQLException ex) {
            showAlert("❌ Erreur", "Erreur mise à jour : " + ex.getMessage());
        }
    }

    // =========================================================================
    // MISC
    // =========================================================================
    @FXML
    public void handleNotif(ActionEvent e) {
        showAlert("Notifications", "🔔 Vous avez 3 nouvelles notifications.");
    }

    @FXML
    public void handlePremium(ActionEvent e) {
        showAlert("Premium", "🌟 Passez Premium pour 9,99 € / mois et accédez à plus de 500 cours !");
    }

    @FXML
    public void handleLogout(ActionEvent e) {
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

    // =========================================================================
    // EVENTS FEATURE
    // =========================================================================
    private void loadPersistentData() {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
        } catch (Exception ignored) {
        }
        // Favoris
        try {
            File f = new File(FAV_FILE);
            if (f.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                    String line;
                    while ((line = br.readLine()) != null)
                        if (!line.trim().isEmpty()) favorites.add(Integer.parseInt(line.trim()));
                }
            }
        } catch (Exception e) {
            System.err.println("Favoris non chargés : " + e.getMessage());
        }
        // Commentaires
        try {
            File f = new File(COMMENT_FILE);
            if (f.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] parts = line.split("\\|", 2);
                        if (parts.length == 2)
                            comments.computeIfAbsent(Integer.parseInt(parts[0]), k -> new ArrayList<>()).add(parts[1]);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Commentaires non chargés : " + e.getMessage());
        }
    }

    private void saveFavorites() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FAV_FILE))) {
            for (int id : favorites) pw.println(id);
        } catch (Exception e) {
            System.err.println("Favoris non sauvegardés : " + e.getMessage());
        }
    }

    private void saveComments() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(COMMENT_FILE))) {
            for (Map.Entry<Integer, List<String>> entry : comments.entrySet())
                for (String c : entry.getValue())
                    pw.println(entry.getKey() + "|" + c);
        } catch (Exception e) {
            System.err.println("Commentaires non sauvegardés : " + e.getMessage());
        }
    }

    private void toggleFavorite(int eventId) {
        if (favorites.contains(eventId)) favorites.remove(eventId);
        else favorites.add(eventId);
        saveFavorites();
        refreshEventDisplay();
    }

    private void initializeEventFilters() {
        if (eventTypeFilter == null || eventDateSort == null) return;
        eventTypeFilter.setItems(FXCollections.observableArrayList(
                "Tous les types", "Formation", "Conférence", "Workshop", "Sport", "Culture", "Technologie", "Autre"));
        eventTypeFilter.setValue("Tous les types");
        eventTypeFilter.setOnAction(e -> {
            String sel = eventTypeFilter.getValue();
            currentTypeFilter = "Tous les types".equals(sel) ? "all" : sel;
            appliquerFiltresEtTri();
        });
        eventDateSort.setItems(FXCollections.observableArrayList("Plus récents", "Plus anciens", "A-Z", "Z-A"));
        eventDateSort.setValue("Plus récents");
        eventDateSort.setOnAction(e -> appliquerFiltresEtTri());
    }

    private void appliquerFiltresEtTri() {
        if (tousLesEvents == null || tousLesEvents.isEmpty()) {
            if (eventCardsContainer != null) eventCardsContainer.getChildren().clear();
            if (eventCounterLabel != null) eventCounterLabel.setText("0 événement");
            return;
        }
        String sortVal = eventDateSort != null ? eventDateSort.getValue() : "Plus récents";
        Comparator<Event> cmp;
        if ("A-Z".equals(sortVal)) cmp = Comparator.comparing(ev -> (ev.getTitre() != null ? ev.getTitre() : ""));
        else if ("Z-A".equals(sortVal))
            cmp = Comparator.comparing((Event ev) -> (ev.getTitre() != null ? ev.getTitre() : "")).reversed();
        else if ("Plus anciens".equals(sortVal))
            cmp = Comparator.comparing(Event::getDateCreation, Comparator.nullsLast(Comparator.naturalOrder()));
        else cmp = Comparator.comparing(Event::getDateCreation, Comparator.nullsLast(Comparator.reverseOrder()));

        eventsFiltres = tousLesEvents.stream()
                .filter(ev -> {
                    if (!"all".equals(currentTypeFilter) && (ev.getType() == null || !ev.getType().equalsIgnoreCase(currentTypeFilter)))
                        return false;
                    if (showFavoritesOnly && !favorites.contains(ev.getId())) return false;
                    return true;
                })
                .sorted(cmp)
                .collect(Collectors.toList());

        String query = (eventSearchField != null) ? eventSearchField.getText().trim().toLowerCase() : "";
        if (!query.isEmpty()) {
            eventsFiltres = eventsFiltres.stream()
                    .filter(ev -> {
                        String t = ev.getTitre() != null ? ev.getTitre().toLowerCase() : "";
                        String p = ev.getType() != null ? ev.getType().toLowerCase() : "";
                        String d = ev.getDescription() != null ? ev.getDescription().toLowerCase() : "";
                        return t.contains(query) || p.contains(query) || d.contains(query);
                    })
                    .collect(Collectors.toList());
        }

        afficherCartes(eventsFiltres);
        int c = eventsFiltres.size();
        if (eventCounterLabel != null) eventCounterLabel.setText(c + " événement" + (c > 1 ? "s" : ""));
        if (filterStatusLabel != null) {
            String ft = "all".equals(currentTypeFilter) ? "Tous" : currentTypeFilter;
            filterStatusLabel.setText(ft + "  ·  " + (sortVal != null ? sortVal : "récents"));
        }
    }

    @FXML
    private void handleEventSearch(KeyEvent e) {
        appliquerFiltresEtTri();
    }

    private void refreshEventDisplay() {
        if (eventsFiltres != null) afficherCartes(eventsFiltres);
    }

    private void chargerEvents() {
        try {
            tousLesEvents = eventService.recupererTous();
            currentTypeFilter = "all";
            showFavoritesOnly = false;
            appliquerFiltresEtTri();
        } catch (SQLException e) {
            System.err.println("Erreur events : " + e.getMessage());
            showAlert("Erreur", "Impossible de charger les événements.");
        }
    }

    private void afficherCartes(List<Event> events) {
        if (eventCardsContainer == null) return;
        eventCardsContainer.getChildren().clear();
        if (events == null) return;
        for (int i = 0; i < events.size(); i++) {
            VBox carte = creerCarteGrid(events.get(i));
            carte.setOpacity(0);
            carte.setTranslateY(20);
            eventCardsContainer.getChildren().add(carte);
            FadeTransition ft = new FadeTransition(Duration.millis(350), carte);
            ft.setToValue(1);
            ft.setDelay(Duration.millis(i * 60L));
            TranslateTransition tt = new TranslateTransition(Duration.millis(350), carte);
            tt.setToY(0);
            tt.setDelay(Duration.millis(i * 60L));
            new ParallelTransition(ft, tt).play();
        }
    }

    private VBox creerCarteGrid(Event event) {
        boolean isFav = favorites.contains(event.getId());
        String color = getTypeColor(event.getType());

        VBox carte = new VBox();
        carte.setPrefWidth(270);
        carte.setMaxWidth(270);
        carte.setStyle("-fx-background-color: #0d0d18; -fx-background-radius: 18;"
                + "-fx-border-color: " + color + "33; -fx-border-radius: 18; -fx-border-width: 1;"
                + "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 16, 0, 0, 4);");

        StackPane imageBox = new StackPane();
        imageBox.setPrefHeight(160);
        imageBox.setStyle("-fx-background-color: linear-gradient(to bottom right, " + color + "55, " + color + "11); -fx-background-radius: 18 18 0 0;");

        String imagePath = event.getImage();
        if (imagePath != null && !imagePath.isEmpty()) {
            File imgFile = new File("src/main/resources/images/" + imagePath);
            if (imgFile.exists()) {
                try {
                    ImageView iv = new ImageView(new Image(imgFile.toURI().toString()));
                    iv.setFitWidth(270);
                    iv.setFitHeight(160);
                    iv.setPreserveRatio(false);
                    Rectangle clip = new Rectangle(270, 160);
                    clip.setArcWidth(36);
                    clip.setArcHeight(36);
                    iv.setClip(clip);
                    imageBox.getChildren().add(iv);
                } catch (Exception ex) {
                    imageBox.getChildren().add(defaultEmoji());
                }
            } else {
                imageBox.getChildren().add(defaultEmoji());
            }
        } else {
            imageBox.getChildren().add(defaultEmoji());
        }

        HBox overlay = new HBox();
        overlay.setAlignment(Pos.TOP_CENTER);
        Label badge = new Label(event.getType() != null ? event.getType().toUpperCase() : "ÉVÉNEMENT");
        badge.setStyle("-fx-background-color: " + color + "cc; -fx-text-fill: white; -fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 8;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnFav = new Button(isFav ? "♥" : "♡");
        btnFav.setStyle("-fx-background-color: " + (isFav ? "#ff6b8a" : "rgba(0,0,0,0.4)")
                + "; -fx-text-fill: " + (isFav ? "white" : "#888888")
                + "; -fx-background-radius: 50%; -fx-min-width: 30; -fx-min-height: 30; -fx-font-size: 14px; -fx-cursor: hand;");
        btnFav.setOnAction(e -> toggleFavorite(event.getId()));
        overlay.getChildren().addAll(badge, spacer, btnFav);
        StackPane.setAlignment(overlay, Pos.TOP_CENTER);
        StackPane.setMargin(overlay, new Insets(10, 10, 0, 10));
        imageBox.getChildren().add(overlay);

        VBox content = new VBox(8);
        content.setStyle("-fx-padding: 14 16 12 16;");
        Label titre = new Label(event.getTitre() != null ? event.getTitre() : "Sans titre");
        titre.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
        titre.setWrapText(true);

        HBox meta = new HBox(12);
        meta.setAlignment(Pos.CENTER_LEFT);
        String dateStr = "";
        if (event.getDateCreation() != null) {
            try {
                dateStr = event.getDateCreation().toLocalDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
            } catch (Exception ignored) {
            }
        }
        Label date = new Label("📅 " + dateStr);
        date.setStyle("-fx-font-size: 10px; -fx-text-fill: " + color + ";");
        Label userId = new Label("👤 User #" + event.getUserId());
        userId.setStyle("-fx-font-size: 10px; -fx-text-fill: #3a3a5a;");
        meta.getChildren().addAll(date, userId);

        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            String desc = event.getDescription().length() > 75 ? event.getDescription().substring(0, 75) + "…" : event.getDescription();
            Label descL = new Label(desc);
            descL.setStyle("-fx-font-size: 11px; -fx-text-fill: #5a5a7a;");
            descL.setWrapText(true);
            content.getChildren().addAll(titre, meta, descL);
        } else {
            content.getChildren().addAll(titre, meta);
        }

        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: #1a1a2e;");
        content.getChildren().add(sep);

        VBox sponsors = creerSectionSponsors(event.getId());
        if (sponsors != null) content.getChildren().add(sponsors);

        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_LEFT);
        List<String> evComments = comments.getOrDefault(event.getId(), new ArrayList<>());
        Label commLabel = new Label("💬 " + evComments.size());
        commLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #4a4a6a;");
        Button btnComment = new Button("Commenter");
        btnComment.setStyle("-fx-background-color: #1a1a2e; -fx-text-fill: #7c6fff; -fx-background-radius: 8; -fx-font-size: 11px; -fx-padding: 5 12; -fx-cursor: hand;");
        btnComment.setOnAction(e -> ouvrirDetailEvenement(event));
        Region fSpacer = new Region();
        HBox.setHgrow(fSpacer, Priority.ALWAYS);
        Button btnVoir = new Button("Voir →");
        btnVoir.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-size: 11px; -fx-padding: 5 14; -fx-cursor: hand;");
        btnVoir.setOnAction(e -> ouvrirDetailEvenement(event));
        footer.getChildren().addAll(commLabel, btnComment, fSpacer, btnVoir);
        content.getChildren().add(footer);

        carte.getChildren().addAll(imageBox, content);

        carte.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(180), carte);
            st.setToX(1.025);
            st.setToY(1.025);
            st.play();
        });
        carte.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(180), carte);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
        return carte;
    }

    private void ouvrirDetailEvenement(Event event) {
        String color = getTypeColor(event.getType());
        boolean isFav = favorites.contains(event.getId());
        List<String> evComments = comments.getOrDefault(event.getId(), new ArrayList<>());

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(event.getTitre());

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #0a0a0f; -fx-background-radius: 16;");
        root.setPrefWidth(540);

        VBox header = new VBox(8);
        header.setPadding(new Insets(24, 24, 20, 24));
        header.setStyle("-fx-background-color: linear-gradient(to bottom right, " + color + "44, " + color + "11); -fx-background-radius: 16 16 0 0;");
        Label typeLbl = new Label(event.getType() != null ? event.getType().toUpperCase() : "ÉVÉNEMENT");
        typeLbl.setStyle("-fx-background-color: " + color + "55; -fx-text-fill: " + color + "; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 8;");
        Label title = new Label(event.getTitre() != null ? event.getTitre() : "Sans titre");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white; -fx-font-family: 'Georgia';");
        title.setWrapText(true);
        String dateStr = "";
        if (event.getDateCreation() != null) {
            try {
                dateStr = event.getDateCreation().toLocalDate().format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH));
            } catch (Exception ignored) {
            }
        }
        Label dateLbl = new Label("📅 " + dateStr + "   👤 User #" + event.getUserId());
        dateLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " + color + ";");
        header.getChildren().addAll(typeLbl, title, dateLbl);

        VBox body = new VBox(14);
        body.setPadding(new Insets(20, 24, 20, 24));
        body.setStyle("-fx-background-color: #0d0d14;");

        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            Label descTitle = new Label("Description");
            descTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #4a4a6a;");
            Label descTxt = new Label(event.getDescription());
            descTxt.setStyle("-fx-font-size: 13px; -fx-text-fill: #aaaacc;");
            descTxt.setWrapText(true);
            body.getChildren().addAll(descTitle, descTxt);
        }

        Button favBtn = new Button(isFav ? "♥  Retirer des favoris" : "♡  Ajouter aux favoris");
        favBtn.setStyle("-fx-background-color: " + (isFav ? "#2a1a20" : "#1a1a2e")
                + "; -fx-text-fill: " + (isFav ? "#ff6b8a" : "#8888aa")
                + "; -fx-background-radius: 10; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px;");
        favBtn.setOnAction(e -> {
            toggleFavorite(event.getId());
            dialog.close();
        });
        body.getChildren().add(favBtn);

        Label commTitle = new Label("💬  Commentaires (" + evComments.size() + ")");
        commTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: white;");
        body.getChildren().add(commTitle);

        VBox commentsList = new VBox(8);
        for (String c : evComments) {
            String[] parts = c.split("::", 3);
            String user = parts.length > 0 ? parts[0] : "Anonyme";
            String text = parts.length > 1 ? parts[1] : "";
            String ts = parts.length > 2 ? parts[2] : "";
            VBox cb = new VBox(4);
            cb.setStyle("-fx-background-color: #0a0a0f; -fx-background-radius: 10; -fx-padding: 10 14;");
            Label userL = new Label("👤 " + user + "  ·  " + ts);
            userL.setStyle("-fx-font-size: 10px; -fx-text-fill: #4a4a6a;");
            Label textL = new Label(text);
            textL.setStyle("-fx-font-size: 12px; -fx-text-fill: #aaaacc;");
            textL.setWrapText(true);
            cb.getChildren().addAll(userL, textL);
            commentsList.getChildren().add(cb);
        }
        if (evComments.isEmpty()) {
            Label none = new Label("Soyez le premier à commenter !");
            none.setStyle("-fx-font-size: 12px; -fx-text-fill: #3a3a5a; -fx-padding: 8 0;");
            commentsList.getChildren().add(none);
        }
        ScrollPane commScroll = new ScrollPane(commentsList);
        commScroll.setFitToWidth(true);
        commScroll.setPrefHeight(140);
        commScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        body.getChildren().add(commScroll);

        HBox newComm = new HBox(10);
        newComm.setAlignment(Pos.CENTER_LEFT);
        TextField commField = new TextField();
        commField.setPromptText("Écrire un commentaire...");
        commField.setStyle("-fx-background-color: #0a0a0f; -fx-text-fill: white; -fx-border-color: #2a2a3e; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 8 12; -fx-font-size: 12px;");
        HBox.setHgrow(commField, Priority.ALWAYS);
        Button sendBtn = new Button("Envoyer");
        sendBtn.setStyle("-fx-background-color: #7c6fff; -fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px;");
        sendBtn.setOnAction(e -> {
            String txt = commField.getText().trim();
            if (!txt.isEmpty()) {
                String ts = DateTimeFormatter.ofPattern("dd/MM HH:mm").format(java.time.LocalDateTime.now());
                String entry = "Vous::" + txt + "::" + ts;
                comments.computeIfAbsent(event.getId(), k -> new ArrayList<>()).add(entry);
                saveComments();
                commField.clear();
                VBox newCb = new VBox(4);
                newCb.setStyle("-fx-background-color: #0a0a0f; -fx-background-radius: 10; -fx-padding: 10 14;");
                Label uL = new Label("👤 Vous  ·  " + ts);
                uL.setStyle("-fx-font-size: 10px; -fx-text-fill: #4a4a6a;");
                Label tL = new Label(txt);
                tL.setStyle("-fx-font-size: 12px; -fx-text-fill: #aaaacc;");
                tL.setWrapText(true);
                newCb.getChildren().addAll(uL, tL);
                commentsList.getChildren().add(newCb);
                commentsList.getChildren().removeIf(n -> n instanceof Label && ((Label) n).getText().contains("premier"));
                refreshEventDisplay();
            }
        });
        newComm.getChildren().addAll(commField, sendBtn);
        body.getChildren().add(newComm);

        root.getChildren().addAll(header, body);
        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().setStyle("-fx-background-color: #0a0a0f; -fx-background-radius: 16; -fx-padding: 0;");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().lookupButton(ButtonType.CLOSE)
                .setStyle("-fx-background-color: #1a1a2e; -fx-text-fill: #8888aa; -fx-background-radius: 8; -fx-padding: 8 16;");
        dialog.showAndWait();
    }

    private VBox creerSectionSponsors(int eventId) {
        List<Sponsor> sponsors;
        try {
            sponsors = sponsorService.recupererParEvent(eventId);
        } catch (SQLException e) {
            return null;
        }
        if (sponsors.isEmpty()) return null;

        VBox section = new VBox(6);
        section.setStyle("-fx-padding: 4 0 0 0;");
        HBox hdr = new HBox(6);
        hdr.setAlignment(Pos.CENTER_LEFT);
        hdr.getChildren().addAll(new Label("💼"), new Label("Sponsors (" + sponsors.size() + ")"));

        FlowPane flow = new FlowPane(6, 4);
        for (Sponsor s : sponsors) {
            String col = getCouleurSponsor(s.getType());
            Label bdg = new Label(s.getNomSponsor() != null ? s.getNomSponsor() : "");
            bdg.setStyle("-fx-background-color: " + col + "22; -fx-text-fill: " + col + "; -fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 6;");
            flow.getChildren().add(bdg);
        }
        section.getChildren().addAll(hdr, flow);
        return section;
    }

    private String getTypeColor(String type) {
        if (type == null) return "#7c6fff";
        return TYPE_COLORS.getOrDefault(type.toUpperCase(), "#7c6fff");
    }

    private String getCouleurSponsor(String type) {
        if (type == null) return "#888888";
        return switch (type.toLowerCase()) {
            case "or" -> "#e6a817";
            case "argent" -> "#7f8c8d";
            case "bronze" -> "#c0652b";
            case "platine" -> "#7c6fff";
            default -> "#27ae60";
        };
    }

    private Label defaultEmoji() {
        Label l = new Label("✦");
        l.setStyle("-fx-font-size: 42px; -fx-text-fill: #7c6fff;");
        return l;
    }

    private void showAlert(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.getDialogPane().setStyle("-fx-font-size: 13px;");
        a.showAndWait();
    }

    private void showAlert(String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
}