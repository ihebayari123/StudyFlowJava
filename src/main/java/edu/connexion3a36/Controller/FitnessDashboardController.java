package edu.connexion3a36.Controller;

import edu.connexion3a36.models.Event;
import edu.connexion3a36.models.Sponsor;
import edu.connexion3a36.services.EventService;
import edu.connexion3a36.services.SponsorService;
import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.Alert.AlertType;
import javafx.util.Duration;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

public class FitnessDashboardController implements Initializable {

    // =========================================================================
    // PERSISTANCE — fichiers locaux
    // =========================================================================
    private static final String DATA_DIR      = System.getProperty("user.home") + "/.connexion3a36/";
    private static final String FAV_FILE      = DATA_DIR + "favorites.txt";
    private static final String COMMENT_FILE  = DATA_DIR + "comments.txt";  // format: eventId|user|text|timestamp

    /** IDs des événements favoris */
    private final Set<Integer> favorites = new HashSet<>();
    /** eventId → liste de commentaires "user::texte::timestamp" */
    private final Map<Integer, List<String>> comments = new HashMap<>();

    // =========================================================================
    // MODÈLES
    // =========================================================================
    public static class Course {
        public final String emoji, name, author, duration, rating, tag, description;
        public int progress;
        public Course(String emoji, String name, String author,
                      String duration, String rating, String tag,
                      String description, int progress) {
            this.emoji = emoji; this.name = name; this.author = author;
            this.duration = duration; this.rating = rating; this.tag = tag;
            this.description = description; this.progress = progress;
        }
    }

    public static class Message {
        public final String sender, preview, time, body;
        public boolean unread;
        public Message(String sender, String preview, String time, String body, boolean unread) {
            this.sender = sender; this.preview = preview; this.time = time;
            this.body = body; this.unread = unread;
        }
    }

    // =========================================================================
    // DONNÉES STATIQUES
    // =========================================================================
    private final List<Course> allCourses = new ArrayList<>(Arrays.asList(
            new Course("🎨","Learn Figma","Christopher Morgan","6h 30min","4.9","top",
                    "Maîtrisez Figma de zéro jusqu'aux prototypes avancés.",35),
            new Course("📷","Photographie Argentique","Gordon Norman","3h 15min","4.7","new",
                    "Découvrez la magie de la photographie sur pellicule.",0),
            new Course("📸","Master Instagram","Sophie Gill","7h 40min","4.6","popular",
                    "Stratégies avancées pour créer un feed cohérent.",60),
            new Course("✏️","Bases du Dessin","Jean Tate","11h 30min","4.8","top",
                    "De la ligne droite aux portraits.",20),
            new Course("🖼️","Photoshop - Essence","David Green","5h 35min","4.7","popular",
                    "Retouche, montage et effets spéciaux.",80),
            new Course("🎬","Vidéo & Montage","Laura Chen","8h 00min","4.5","new",
                    "Tournez et montez des vidéos professionnelles.",0)
    ));

    private final List<Message> messages = Arrays.asList(
            new Message("Alejandro Velazquez","Excellent travail sur le module B2 !","10:32",
                    "Excellent travail sur le module B2 ! Votre progression est remarquable.",true),
            new Message("Christopher Morgan","La prochaine session Figma commence lundi...","Hier",
                    "La prochaine session Figma commence lundi. Pensez à télécharger les fichiers.",true),
            new Message("Sophie Gill","Voici quelques ressources supplémentaires...","Lun",
                    "Voici quelques ressources supplémentaires pour améliorer votre feed.",false),
            new Message("Gordon Norman","Le prochain atelier photo argentique...","Dim",
                    "Le prochain atelier a été reprogrammé au vendredi 18h.",false),
            new Message("Jean Tate","Bravo pour votre croquis de la semaine !","Sam",
                    "Bravo pour votre croquis de la semaine ! Continuez comme ça.",false)
    );

    private final List<Integer> inProgressIndexes = Arrays.asList(0,2,3,4);
    private int currentInProgressPos = 0;
    private String currentChartMode = "hours";

    private final String[] weekLabels  = {"Lun","Mar","Mer","Jeu","Ven","Sam","Dim"};
    private final double[] weekHours   = {1.5,2.0,0.5,3.0,2.5,4.0,1.0};
    private final double[] weekCourses = {1,0,1,2,1,3,0};
    private final String[] monthLabels  = {"Jan","Fév","Mar","Avr","Mai","Jun"};
    private final double[] monthHours   = {12,18,9,22,15,20};
    private final double[] monthCourses = {2,3,1,4,2,3};

    // =========================================================================
    // INJECTION FXML
    // =========================================================================
    @FXML private StackPane mainStackPane;
    @FXML private VBox viewHome, viewProfile, viewMessages, viewSettings, viewCourses, viewEvents, viewCalendar;

    // Home
    @FXML private Label lblGreeting, lblCompleted, lblInProgress, lblHours, lblBadges;
    @FXML private Label currentCourseEmoji, currentCourseName, currentCourseAuthor, lblProgress;
    @FXML private TextField searchField;
    @FXML private VBox courseList;
    @FXML private LineChart<String,Number> learningChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private ComboBox<String> weeklyFilter;
    @FXML private Button chartTabHours, chartTabCourses;

    // Profil
    @FXML private TextField fieldFirstName, fieldLastName, fieldEmail, fieldBio;
    @FXML private Label profileFullName, profileCompleted, profileInProgress;

    // Messages
    @FXML private VBox messageList;

    // Paramètres
    @FXML private VBox settingsNotifList, settingsPrivacyList;

    // Cours
    @FXML private FlowPane allCoursesGrid;

    // Événements
    @FXML private TextField eventSearchField;
    @FXML private FlowPane  eventCardsContainer;
    @FXML private ScrollPane eventScrollPane;
    @FXML private ComboBox<String> eventTypeFilter, eventDateSort;
    @FXML private Label eventCounterLabel, filterStatusLabel, favCountLabel;
    @FXML private Button btnShowCalendar, btnGridView, btnListView, btnFavoritesOnly;
    @FXML private Button tabAll, tabFormation, tabSport, tabCulture, tabTech;

    private List<Event> tousLesEvents;
    private List<Event> eventsFiltres;
    private final EventService   eventService   = new EventService();
    private final SponsorService sponsorService = new SponsorService();
    private String currentTypeFilter  = "all";
    private String currentSortOrder   = "newest";
    private String currentTabFilter   = "all";
    private boolean showFavoritesOnly = false;
    private boolean isGridView        = true;

    // Sidebar
    @FXML private Button btnHome, btnCourses, btnProfile, btnMessages, btnSettings, btnLogout, btnEvents;

    // Calendrier
    @FXML private GridPane calendarGrid, calendarHeaderGrid;
    @FXML private Label lblCurrentMonth, lblSelectedDate, lblEventCount;
    @FXML private VBox calendarEventList;
    private YearMonth currentYearMonth;
    private final Map<LocalDate, List<Event>> calendarEventsMap = new HashMap<>();

    private static final Map<String, String> TYPE_COLORS = new LinkedHashMap<>() {{
        put("FORMATION",   "#4f8ef7");
        put("ÉDUCATION",   "#4f8ef7");
        put("EDUCATION",   "#4f8ef7");
        put("CONFÉRENCE",  "#a855f7");
        put("CONFERENCE",  "#a855f7");
        put("WORKSHOP",    "#f59e0b");
        put("SPORT",       "#22c55e");
        put("SPORTIF",     "#22c55e");
        put("CULTURE",     "#ec4899");
        put("TECHNOLOGIE", "#06b6d4");
        put("AUTRE",       "#f43f5e");
    }};

    // =========================================================================
    // INITIALIZE
    // =========================================================================
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        currentYearMonth = YearMonth.now();
        loadPersistentData();
        renderCourses("all");
        weeklyFilter.setItems(FXCollections.observableArrayList("Semaine","Mois"));
        weeklyFilter.setValue("Semaine");
        updateChart();
        refreshCurrentCourse();
        renderMessages();
        renderSettings();
        initializeEventFilters();
    }

    // =========================================================================
    // PERSISTANCE
    // =========================================================================
    private void loadPersistentData() {
        try { Files.createDirectories(Paths.get(DATA_DIR)); } catch (Exception ignored) {}
        // Chargement des favoris
        try {
            File f = new File(FAV_FILE);
            if (f.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (!line.trim().isEmpty()) favorites.add(Integer.parseInt(line.trim()));
                    }
                }
            }
        } catch (Exception e) { System.err.println("Favoris non chargés : " + e.getMessage()); }
        // Chargement des commentaires
        try {
            File f = new File(COMMENT_FILE);
            if (f.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] parts = line.split("\\|", 2);
                        if (parts.length == 2) {
                            int id = Integer.parseInt(parts[0]);
                            comments.computeIfAbsent(id, k -> new ArrayList<>()).add(parts[1]);
                        }
                    }
                }
            }
        } catch (Exception e) { System.err.println("Commentaires non chargés : " + e.getMessage()); }
    }

    private void saveFavorites() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FAV_FILE))) {
            for (int id : favorites) pw.println(id);
        } catch (Exception e) { System.err.println("Favoris non sauvegardés : " + e.getMessage()); }
    }

    private void saveComments() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(COMMENT_FILE))) {
            for (Map.Entry<Integer, List<String>> entry : comments.entrySet()) {
                for (String c : entry.getValue()) {
                    pw.println(entry.getKey() + "|" + c);
                }
            }
        } catch (Exception e) { System.err.println("Commentaires non sauvegardés : " + e.getMessage()); }
    }

    private void toggleFavorite(int eventId) {
        if (favorites.contains(eventId)) {
            favorites.remove(eventId);
        } else {
            favorites.add(eventId);
            // Animation flash
            playFavoriteFlash();
        }
        saveFavorites();
        refreshEventDisplay();
        updateFavCount();
    }

    private void playFavoriteFlash() {
        if (favCountLabel != null) {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), favCountLabel);
            st.setFromX(1); st.setFromY(1); st.setToX(1.3); st.setToY(1.3);
            st.setAutoReverse(true); st.setCycleCount(2); st.play();
        }
    }

    private void updateFavCount() {
        if (favCountLabel != null)
            favCountLabel.setText("♥ " + favorites.size() + " favori" + (favorites.size() > 1 ? "s" : ""));
    }

    // =========================================================================
    // FILTRES ÉVÉNEMENTS
    // =========================================================================
    private void initializeEventFilters() {
        eventTypeFilter.setItems(FXCollections.observableArrayList(
                "Tous les types","Formation","Conférence","Workshop","Sport","Culture","Technologie","Autre"));
        eventTypeFilter.setValue("Tous les types");
        eventTypeFilter.setOnAction(e -> handleEventTypeFilter());
        eventDateSort.setItems(FXCollections.observableArrayList("Plus récents","Plus anciens","A-Z","Z-A"));
        eventDateSort.setValue("Plus récents");
        eventDateSort.setOnAction(e -> handleEventDateSort());
    }

    private void handleEventTypeFilter() {
        String sel = eventTypeFilter.getValue();
        currentTypeFilter = "Tous les types".equals(sel) ? "all" : sel;
        appliquerFiltresEtTri();
    }

    private void handleEventDateSort() { appliquerFiltresEtTri(); }

    @FXML
    private void handleTabFilter(javafx.event.ActionEvent e) {
        currentTabFilter = (String) ((Button) e.getSource()).getUserData();
        styleTabButtons(currentTabFilter);
        appliquerFiltresEtTri();
    }

    private void styleTabButtons(String active) {
        String activeStyle  = "-fx-background-color: #7c6fff; -fx-text-fill: white; -fx-background-radius: 8 8 0 0; -fx-padding: 10 20; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand;";
        String inactiveStyle= "-fx-background-color: transparent; -fx-text-fill: #4a4a6a; -fx-background-radius: 8 8 0 0; -fx-padding: 10 20; -fx-font-size: 12px; -fx-cursor: hand;";
        if (tabAll != null) {
            Map<Button, String> tabs = new LinkedHashMap<>();
            tabs.put(tabAll, "all"); tabs.put(tabFormation, "Formation");
            tabs.put(tabSport, "sport"); tabs.put(tabCulture, "culture"); tabs.put(tabTech, "technologie");
            tabs.forEach((btn, key) -> btn.setStyle(key.equalsIgnoreCase(active) ? activeStyle : inactiveStyle));
        }
    }

    @FXML private void handleFavoritesFilter() {
        showFavoritesOnly = !showFavoritesOnly;
        if (btnFavoritesOnly != null) {
            btnFavoritesOnly.setStyle(showFavoritesOnly
                    ? "-fx-background-color: #ff6b8a; -fx-text-fill: white; -fx-border-color: #ff6b8a; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10 18; -fx-font-size: 12px; -fx-cursor: hand;"
                    : "-fx-background-color: #14141f; -fx-text-fill: #4a4a6a; -fx-border-color: #2a2a3e; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10 18; -fx-font-size: 12px; -fx-cursor: hand;");
        }
        appliquerFiltresEtTri();
    }

    @FXML private void handleGridView() {
        isGridView = true;
        if (btnGridView != null)
            btnGridView.setStyle("-fx-background-color: #7c6fff; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-size: 16px; -fx-min-width: 34; -fx-min-height: 34; -fx-cursor: hand;");
        if (btnListView != null)
            btnListView.setStyle("-fx-background-color: transparent; -fx-text-fill: #4a4a6a; -fx-background-radius: 8; -fx-font-size: 16px; -fx-min-width: 34; -fx-min-height: 34; -fx-cursor: hand;");
        if (eventCardsContainer != null) {
            eventCardsContainer.setPrefWrapLength(900);
            eventCardsContainer.setHgap(18);
        }
        afficherCartes(eventsFiltres);
    }

    @FXML private void handleListView() {
        isGridView = false;
        if (btnListView != null)
            btnListView.setStyle("-fx-background-color: #7c6fff; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-size: 16px; -fx-min-width: 34; -fx-min-height: 34; -fx-cursor: hand;");
        if (btnGridView != null)
            btnGridView.setStyle("-fx-background-color: transparent; -fx-text-fill: #4a4a6a; -fx-background-radius: 8; -fx-font-size: 16px; -fx-min-width: 34; -fx-min-height: 34; -fx-cursor: hand;");
        if (eventCardsContainer != null) {
            eventCardsContainer.setPrefWrapLength(10000); // force 1 colonne
            eventCardsContainer.setHgap(0);
        }
        afficherCartes(eventsFiltres);
    }

    private void appliquerFiltresEtTri() {
        if (tousLesEvents == null || tousLesEvents.isEmpty()) {
            if (eventCardsContainer != null) eventCardsContainer.getChildren().clear();
            if (eventCounterLabel   != null) eventCounterLabel.setText("0 événement");
            return;
        }
        String sortVal = eventDateSort.getValue();
        Comparator<Event> cmp;
        if ("A-Z".equals(sortVal)) {
            cmp = Comparator.comparing(ev -> (ev.getTitre() != null ? ev.getTitre() : ""));
        } else if ("Z-A".equals(sortVal)) {
            cmp = Comparator.comparing((Event ev) -> (ev.getTitre() != null ? ev.getTitre() : "")).reversed();
        } else if ("Plus anciens".equals(sortVal)) {
            cmp = Comparator.comparing(Event::getDateCreation, Comparator.nullsLast(Comparator.naturalOrder()));
        } else {
            cmp = Comparator.comparing(Event::getDateCreation, Comparator.nullsLast(Comparator.reverseOrder()));
        }

        eventsFiltres = tousLesEvents.stream()
                .filter(ev -> {
                    // Filtre par onglet
                    if (!"all".equals(currentTabFilter)) {
                        String t = ev.getType() != null ? ev.getType().toLowerCase() : "";
                        if (!t.contains(currentTabFilter.toLowerCase())) return false;
                    }
                    // Filtre par combobox type
                    if (!"all".equals(currentTypeFilter)) {
                        if (ev.getType() == null || !ev.getType().equalsIgnoreCase(currentTypeFilter)) return false;
                    }
                    // Filtre favoris
                    if (showFavoritesOnly && !favorites.contains(ev.getId())) return false;
                    return true;
                })
                .sorted(cmp)
                .collect(Collectors.toList());

        // Filtre texte
        String query = eventSearchField != null ? eventSearchField.getText().trim().toLowerCase() : "";
        if (!query.isEmpty()) {
            eventsFiltres = eventsFiltres.stream()
                    .filter(ev -> {
                        String t = ev.getTitre()       != null ? ev.getTitre().toLowerCase()       : "";
                        String p = ev.getType()        != null ? ev.getType().toLowerCase()        : "";
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
        updateFavCount();
    }

    private void refreshEventDisplay() {
        if (eventsFiltres != null) afficherCartes(eventsFiltres);
    }

    // =========================================================================
    // NAVIGATION
    // =========================================================================
    @FXML private void handleNav(javafx.event.ActionEvent e) {
        showPage((String)((Button)e.getSource()).getUserData());
    }

    private void showPage(String pageId) {
        hideAllViews();
        VBox target = switch (pageId) {
            case "profile"  -> viewProfile;
            case "messages" -> viewMessages;
            case "settings" -> viewSettings;
            case "courses"  -> viewCourses;
            case "events"   -> viewEvents;
            default         -> viewHome;
        };
        target.setVisible(true); target.setManaged(true);
        // Fade in
        FadeTransition ft = new FadeTransition(Duration.millis(250), target);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
        resetNavButtons();
        Button active = switch (pageId) {
            case "courses"  -> btnCourses;
            case "profile"  -> btnProfile;
            case "messages" -> btnMessages;
            case "settings" -> btnSettings;
            case "events"   -> btnEvents;
            default         -> btnHome;
        };
        active.setStyle("-fx-font-size: 18px; -fx-background-color: #1e1e30; -fx-text-fill: #7c6fff; -fx-cursor: hand; -fx-background-radius: 12; -fx-min-width: 46; -fx-min-height: 46; -fx-border-color: #2e2a50; -fx-border-radius: 12;");
        if ("courses".equals(pageId)) renderAllCourses();
        if ("events".equals(pageId))  chargerEvents();
    }

    private void hideAllViews() {
        for (VBox v : List.of(viewHome,viewProfile,viewMessages,viewSettings,viewCourses,viewEvents,viewCalendar)) {
            v.setVisible(false); v.setManaged(false);
        }
    }

    private void resetNavButtons() {
        String base = "-fx-font-size: 18px; -fx-background-color: transparent; -fx-text-fill: #4a4a6a; -fx-cursor: hand; -fx-background-radius: 12; -fx-min-width: 46; -fx-min-height: 46;";
        for (Button b : List.of(btnHome,btnCourses,btnProfile,btnMessages,btnSettings,btnEvents)) b.setStyle(base);
    }

    @FXML private void goHome(javafx.event.ActionEvent e) { showPage("home"); }
    @FXML private void goToProfile(MouseEvent e)          { showPage("profile"); }

    // =========================================================================
    // LOGOUT
    // =========================================================================
    @FXML private void handleLogout() {
        Alert a = new Alert(AlertType.CONFIRMATION);
        a.setTitle("Déconnexion"); a.setHeaderText("Se déconnecter ?");
        a.setContentText("Êtes-vous sûr de vouloir quitter votre session ?");
        a.showAndWait().ifPresent(r -> { if (r == ButtonType.OK) showInfo("Au revoir !","Vous avez été déconnecté."); });
    }

    // =========================================================================
    // SEARCH / NOTIF
    // =========================================================================
    @FXML private void handleSearch(KeyEvent e) {
        String q = searchField.getText().trim().toLowerCase();
        if (q.isEmpty()) { renderCourses("all"); return; }
        renderCourseItems(allCourses.stream().filter(c -> c.name.toLowerCase().contains(q) || c.author.toLowerCase().contains(q)).collect(Collectors.toList()));
    }

    @FXML private void handleNotif() {
        showInfo("Notifications (3)","📚 Alejandro a commenté\n🏆 Nouveau badge\n🆕 Nouveau cours UX");
    }

    // =========================================================================
    // COURS EN COURS
    // =========================================================================
    @FXML private void handlePrevCourse() { currentInProgressPos=(currentInProgressPos-1+inProgressIndexes.size())%inProgressIndexes.size(); refreshCurrentCourse(); }
    @FXML private void handleNextCourse() { currentInProgressPos=(currentInProgressPos+1)%inProgressIndexes.size(); refreshCurrentCourse(); }
    private void refreshCurrentCourse() {
        Course c = allCourses.get(inProgressIndexes.get(currentInProgressPos));
        currentCourseEmoji.setText(c.emoji); currentCourseName.setText(c.name);
        currentCourseAuthor.setText("par "+c.author); lblProgress.setText(c.progress+"%");
    }
    @FXML private void handleContinue() { showInfo("Reprise","Vous reprenez «"+allCourses.get(inProgressIndexes.get(currentInProgressPos)).name+"» ▶"); }

    // =========================================================================
    // FILTRES COURS
    // =========================================================================
    @FXML private void handleFilter(javafx.event.ActionEvent e) {
        String tag = (String)((Button)e.getSource()).getUserData(); renderCourses(tag);
        String in = "-fx-background-color: transparent; -fx-text-fill: #888; -fx-cursor: hand; -fx-padding: 4 0;";
        String ac = in+"-fx-font-weight: bold; -fx-text-fill: #111; -fx-border-color: transparent transparent #111 transparent; -fx-border-width: 0 0 2 0;";
        HBox tabs = (HBox)((Button)e.getSource()).getParent();
        tabs.getChildren().forEach(n -> { if (n instanceof Button b) b.setStyle(tag.equals(b.getUserData())?ac:in); });
    }
    private void renderCourses(String tag) { renderCourseItems("all".equals(tag)?allCourses:allCourses.stream().filter(c->c.tag.equals(tag)).collect(Collectors.toList())); }
    private void renderCourseItems(List<Course> items) { courseList.getChildren().clear(); items.forEach(c->courseList.getChildren().add(buildCourseRow(c))); }
    private HBox buildCourseRow(Course c) {
        HBox row = new HBox(12); row.setAlignment(Pos.CENTER_LEFT); row.setPrefHeight(52); row.setPadding(new Insets(8,14,8,14));
        row.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-cursor: hand;");
        row.setOnMouseEntered(e->row.setStyle("-fx-background-color: #f8f8f8; -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.08),8,0,0,2);"));
        row.setOnMouseExited(e->row.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-cursor: hand;"));
        Label emoji=new Label(c.emoji); emoji.setStyle("-fx-font-size: 20px;");
        VBox info=new VBox(2); HBox.setHgrow(info,Priority.ALWAYS);
        Label name=new Label(c.name); name.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        Label author=new Label("par "+c.author); author.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
        info.getChildren().addAll(name,author);
        Label meta=new Label("⏱ "+c.duration+"   ⭐ "+c.rating); meta.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        Button btn=new Button(c.progress>0?"Continuer":"Voir");
        btn.setStyle("-fx-background-color: #111; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-size: 11px; -fx-padding: 6 12; -fx-cursor: hand;");
        btn.setOnAction(e->openCourseDetail(c));
        row.getChildren().addAll(emoji,info,meta,btn); return row;
    }
    private void openCourseDetail(Course c) {
        Alert a=new Alert(AlertType.CONFIRMATION); a.setTitle(c.name); a.setHeaderText(c.emoji+"  "+c.name+"   ⭐ "+c.rating);
        a.setContentText("Instructeur : "+c.author+"\nDurée : "+c.duration+"\nProgression : "+c.progress+"%\n\n"+c.description);
        ButtonType btn=new ButtonType(c.progress>0?"Continuer":"S'inscrire"); a.getButtonTypes().setAll(btn,ButtonType.CANCEL);
        a.showAndWait().ifPresent(r->{ if(r==btn){ if(c.progress==0){c.progress=5; showInfo("Inscription","Bienvenue dans «"+c.name+"» 🎉"); lblInProgress.setText(String.valueOf(Integer.parseInt(lblInProgress.getText())+1));} else showInfo("Reprise","Vous reprenez «"+c.name+"» ▶");}});
    }

    // =========================================================================
    // GRAPHIQUE
    // =========================================================================
    @FXML private void handleChartTab(javafx.event.ActionEvent e) {
        currentChartMode=(String)((Button)e.getSource()).getUserData();
        String ac="-fx-background-color: transparent; -fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #111; -fx-cursor: hand; -fx-border-color: transparent transparent #111 transparent; -fx-border-width: 0 0 2 0;";
        String in="-fx-background-color: transparent; -fx-font-size: 12px; -fx-text-fill: #aaa; -fx-cursor: hand;";
        chartTabHours.setStyle("hours".equals(currentChartMode)?ac:in); chartTabCourses.setStyle("courses".equals(currentChartMode)?ac:in);
        updateChart();
    }
    @FXML private void handlePeriodChange() { updateChart(); }
    private void updateChart() {
        boolean w="Semaine".equals(weeklyFilter.getValue())||weeklyFilter.getValue()==null;
        String[] labels=w?weekLabels:monthLabels;
        double[] data=switch(currentChartMode){case "hours"->w?weekHours:monthHours; case "courses"->w?weekCourses:monthCourses; default->weekHours;};
        double max=Arrays.stream(data).max().orElse(5);
        yAxis.setUpperBound(Math.ceil(max+1)); yAxis.setTickUnit(Math.ceil((max+1)/5.0));
        XYChart.Series<String,Number> s=new XYChart.Series<>();
        s.setName("courses".equals(currentChartMode)?"Cours":"Heures");
        for(int i=0;i<labels.length;i++) s.getData().add(new XYChart.Data<>(labels[i],data[i]));
        learningChart.getData().clear(); learningChart.getData().add(s);
        learningChart.lookupAll(".chart-series-line").forEach(n->n.setStyle("-fx-stroke: #111; -fx-stroke-width: 2;"));
        learningChart.lookupAll(".chart-line-symbol").forEach(n->n.setStyle("-fx-background-color: #111,white; -fx-background-radius: 5;"));
    }

    @FXML private void handlePremium() {
        Alert a=new Alert(AlertType.CONFIRMATION); a.setTitle("Premium"); a.setHeaderText("🧠 Premium — 9,99 €/mois");
        a.setContentText("✅ 500+ cours\n✅ Certificats\n✅ Hors-ligne"); ButtonType s=new ButtonType("Souscrire");
        a.getButtonTypes().setAll(s,ButtonType.CANCEL); a.showAndWait().ifPresent(r->{if(r==s) showInfo("Premium activé !","Tous les avantages sont disponibles 🎉");});
    }

    @FXML private void handleSaveProfile() {
        String f=fieldFirstName.getText().trim(), l=fieldLastName.getText().trim();
        if(f.isEmpty()||l.isEmpty()){showError("Erreur","Prénom et nom obligatoires.");return;}
        lblGreeting.setText("Bonjour, "+f+" !"); profileFullName.setText(f+" "+l);
        showInfo("Profil mis à jour","Informations enregistrées ✅");
    }

    // =========================================================================
    // MESSAGES
    // =========================================================================
    private void renderMessages() {
        messageList.getChildren().clear(); messages.forEach(m->messageList.getChildren().add(buildMessageRow(m)));
    }
    private HBox buildMessageRow(Message m) {
        HBox row=new HBox(12); row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(14,16,14,16)); row.setMaxWidth(620);
        String base=m.unread?"-fx-background-color: white; -fx-background-radius: 14; -fx-cursor: hand; -fx-border-color: #111 #eee #eee #111; -fx-border-radius: 14; -fx-border-width: 0 0 0 3;"
                :"-fx-background-color: white; -fx-background-radius: 14; -fx-cursor: hand; -fx-border-color: #eee; -fx-border-radius: 14;";
        row.setStyle(base);
        row.setOnMouseEntered(e->row.setStyle(base+" -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.08),8,0,0,2);"));
        row.setOnMouseExited(e->row.setStyle(base));
        VBox info=new VBox(3); HBox.setHgrow(info,Priority.ALWAYS);
        Label sender=new Label(m.sender); sender.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        Label preview=new Label(m.preview); preview.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");
        info.getChildren().addAll(sender,preview);
        Label time=new Label(m.time); time.setStyle("-fx-font-size: 11px; -fx-text-fill: #aaa;");
        row.getChildren().addAll(info,time); row.setOnMouseClicked(e->openMessage(m)); return row;
    }
    private void openMessage(Message m) {
        m.unread=false; renderMessages();
        Alert a=new Alert(AlertType.CONFIRMATION); a.setTitle("Message de "+m.sender); a.setHeaderText(m.sender); a.setContentText(m.body);
        ButtonType r=new ButtonType("Répondre"); a.getButtonTypes().setAll(r,ButtonType.CLOSE);
        a.showAndWait().ifPresent(res->{if(res==r) showInfo("Envoyé","Réponse transmise à "+m.sender+".");});
    }

    // =========================================================================
    // PARAMÈTRES
    // =========================================================================
    private void renderSettings() {
        String[][] notif={{"Notifications par email","true"},{"Rappels de cours","true"},{"Messages instructeurs","false"},{"Nouveaux cours","true"}};
        settingsNotifList.getChildren().clear(); for(String[] i:notif) settingsNotifList.getChildren().add(buildSettingRow(i[0],"true".equals(i[1])));
        String[][] priv={{"Profil public","true"},{"Partager progression","false"}};
        settingsPrivacyList.getChildren().clear(); for(String[] i:priv) settingsPrivacyList.getChildren().add(buildSettingRow(i[0],"true".equals(i[1])));
    }
    private HBox buildSettingRow(String label, boolean isOn) {
        HBox row=new HBox(); row.setAlignment(Pos.CENTER_LEFT); row.setPrefHeight(44);
        row.setStyle("-fx-border-color: transparent transparent #f0f0f0 transparent; -fx-border-width: 0 0 1 0;");
        Label lbl=new Label(label); lbl.setStyle("-fx-font-size: 13px;"); HBox.setHgrow(lbl,Priority.ALWAYS);
        CheckBox cb=new CheckBox(); cb.setSelected(isOn); cb.setStyle("-fx-cursor: hand;");
        cb.selectedProperty().addListener((o,v,nv)->showInfo("Paramètre "+(nv?"activé":"désactivé"),label));
        row.getChildren().addAll(lbl,cb); return row;
    }
    @FXML private void handleChangePassword() { showInfo("Email envoyé","Email de réinitialisation envoyé."); }
    @FXML private void handleDeleteAccount() { Alert a=new Alert(AlertType.WARNING); a.setTitle("Supprimer"); a.setHeaderText("Action irréversible"); a.setContentText("Désactivé en mode démonstration."); a.showAndWait(); }

    // =========================================================================
    // COURS GRID
    // =========================================================================
    private void renderAllCourses() {
        allCoursesGrid.getChildren().clear(); allCourses.forEach(c->allCoursesGrid.getChildren().add(buildCourseCard(c)));
    }
    private VBox buildCourseCard(Course c) {
        VBox card=new VBox(10); card.setPrefWidth(250); card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-border-color: #eee; -fx-border-radius: 16; -fx-cursor: hand;");
        card.setOnMouseEntered(e->card.setStyle(card.getStyle()+" -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.1),10,0,0,3);"));
        card.setOnMouseExited(e->card.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-border-color: #eee; -fx-border-radius: 16; -fx-cursor: hand;"));
        Label emoji=new Label(c.emoji); emoji.setStyle("-fx-font-size: 34px;");
        Label name=new Label(c.name); name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        Label auth=new Label("par "+c.author); auth.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");
        Label meta=new Label("⏱ "+c.duration+"   ⭐ "+c.rating); meta.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        if(c.progress>0){
            Label p=new Label("Progression : "+c.progress+"%"); p.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
            HBox bg=new HBox(); bg.setStyle("-fx-background-color: #eee; -fx-background-radius: 4; -fx-pref-height: 6;"); bg.setPrefWidth(210);
            HBox fg=new HBox(); fg.setPrefHeight(6); fg.setPrefWidth(210*c.progress/100.0); fg.setStyle("-fx-background-color: #111; -fx-background-radius: 4;");
            bg.getChildren().add(fg); card.getChildren().addAll(emoji,name,auth,meta,p,bg);
        } else {
            Label ns=new Label("Non commencé"); ns.setStyle("-fx-font-size: 11px; -fx-text-fill: #aaa;");
            card.getChildren().addAll(emoji,name,auth,meta,ns);
        }
        Button btn=new Button(c.progress>0?"Continuer":"Voir le cours"); btn.setPrefWidth(210);
        btn.setStyle("-fx-background-color: #111; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 0; -fx-cursor: hand;");
        btn.setOnAction(e->openCourseDetail(c)); card.getChildren().add(btn); return card;
    }

    // =========================================================================
    // ÉVÉNEMENTS DB
    // =========================================================================
    private void chargerEvents() {
        try {
            tousLesEvents = eventService.recupererTous();
            currentTypeFilter="all"; currentSortOrder="newest"; currentTabFilter="all"; showFavoritesOnly=false;
            calendarEventsMap.clear();
            for(Event ev:tousLesEvents) {
                if(ev.getDateCreation()!=null) {
                    try { LocalDate d=ev.getDateCreation().toLocalDate(); calendarEventsMap.computeIfAbsent(d,k->new ArrayList<>()).add(ev); } catch(Exception ignored){}
                }
            }
            styleTabButtons("all");
            appliquerFiltresEtTri();
        } catch(SQLException e) {
            System.err.println("Erreur events : "+e.getMessage()); showError("Erreur","Impossible de charger les événements.");
        }
    }

    private void afficherCartes(List<Event> events) {
        if(eventCardsContainer==null) return;
        eventCardsContainer.getChildren().clear();
        if(events==null) return;
        for(int i=0;i<events.size();i++) {
            VBox carte = isGridView ? creerCarteGrid(events.get(i)) : creerCarteListe(events.get(i));
            carte.setOpacity(0); carte.setTranslateY(20);
            eventCardsContainer.getChildren().add(carte);
            // Animation staggerée
            FadeTransition ft=new FadeTransition(Duration.millis(350),carte); ft.setToValue(1); ft.setDelay(Duration.millis(i*60L));
            TranslateTransition tt=new TranslateTransition(Duration.millis(350),carte); tt.setToY(0); tt.setDelay(Duration.millis(i*60L));
            ParallelTransition pt=new ParallelTransition(ft,tt); pt.play();
        }
    }

    // ── CARTE GRID (design dark luxury) ────────────────────────────
    private VBox creerCarteGrid(Event event) {
        boolean isFav = favorites.contains(event.getId());
        String color  = getTypeColor(event.getType());

        VBox carte = new VBox(); carte.setPrefWidth(270); carte.setMaxWidth(270);
        carte.setStyle(
                "-fx-background-color: #0d0d18;" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: " + color + "33;" +
                        "-fx-border-radius: 18;" +
                        "-fx-border-width: 1;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 16, 0, 0, 4);"
        );

        // ── IMAGE / GRADIENT HEADER
        StackPane imageBox = new StackPane(); imageBox.setPrefHeight(160);
        imageBox.setStyle("-fx-background-color: linear-gradient(to bottom right, " + color + "55, " + color + "11); -fx-background-radius: 18 18 0 0;");

        String imagePath = event.getImage();
        if(imagePath!=null&&!imagePath.isEmpty()) {
            File imgFile=new File("src/main/resources/images/"+imagePath);
            if(imgFile.exists()) {
                try {
                    ImageView iv=new ImageView(new Image(imgFile.toURI().toString()));
                    iv.setFitWidth(270); iv.setFitHeight(160); iv.setPreserveRatio(false);
                    Rectangle clip=new Rectangle(270,160); clip.setArcWidth(36); clip.setArcHeight(36);
                    iv.setClip(clip); imageBox.getChildren().add(iv);
                } catch(Exception ex){ imageBox.getChildren().add(defaultEmoji()); }
            } else { imageBox.getChildren().add(defaultEmoji()); }
        } else { imageBox.getChildren().add(defaultEmoji()); }

        // Type badge + FAV button overlay
        HBox overlay = new HBox(); overlay.setAlignment(Pos.TOP_CENTER); overlay.setSpacing(0);
        Label badge = new Label(event.getType()!=null?event.getType().toUpperCase():"ÉVÉNEMENT");
        badge.setStyle("-fx-background-color: "+color+"cc; -fx-text-fill: white; -fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 8;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        // BOUTON FAVORI
        Button btnFav = new Button(isFav ? "♥" : "♡");
        btnFav.setStyle("-fx-background-color: " + (isFav ? "#ff6b8a" : "rgba(0,0,0,0.4)") +
                "; -fx-text-fill: " + (isFav ? "white" : "#888888") +
                "; -fx-background-radius: 50%; -fx-min-width: 30; -fx-min-height: 30; -fx-font-size: 14px; -fx-cursor: hand;");
        btnFav.setOnAction(e -> {
            toggleFavorite(event.getId());
            // Refresh la carte dans la liste
        });
        overlay.getChildren().addAll(badge, spacer, btnFav);
        StackPane.setAlignment(overlay, Pos.TOP_CENTER);
        StackPane.setMargin(overlay, new Insets(10, 10, 0, 10));
        imageBox.getChildren().add(overlay);

        // ── CONTENU
        VBox content = new VBox(8); content.setStyle("-fx-padding: 14 16 12 16;");

        // Titre
        Label titre = new Label(event.getTitre()!=null?event.getTitre():"Sans titre");
        titre.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
        titre.setWrapText(true);

        // Date + User
        HBox meta = new HBox(12); meta.setAlignment(Pos.CENTER_LEFT);
        String dateStr = "";
        if(event.getDateCreation()!=null) { try { dateStr=event.getDateCreation().toLocalDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")); } catch(Exception ignored){} }
        Label date = new Label("📅 "+dateStr); date.setStyle("-fx-font-size: 10px; -fx-text-fill: "+color+";");
        Label userId = new Label("👤 User #"+event.getUserId()); userId.setStyle("-fx-font-size: 10px; -fx-text-fill: #3a3a5a;");
        meta.getChildren().addAll(date, userId);

        // Description
        if(event.getDescription()!=null&&!event.getDescription().isEmpty()) {
            Label desc = new Label(event.getDescription().length()>75?event.getDescription().substring(0,75)+"…":event.getDescription());
            desc.setStyle("-fx-font-size: 11px; -fx-text-fill: #5a5a7a;"); desc.setWrapText(true);
            content.getChildren().addAll(titre, meta, desc);
        } else { content.getChildren().addAll(titre, meta); }

        // Séparateur
        Region sep = new Region(); sep.setPrefHeight(1); sep.setStyle("-fx-background-color: #1a1a2e;");
        content.getChildren().add(sep);

        // Sponsors inline
        VBox sponsors = creerSectionSponsors(event.getId());
        if(sponsors!=null) content.getChildren().add(sponsors);

        // ── FOOTER : commentaires + voir
        HBox footer = new HBox(8); footer.setAlignment(Pos.CENTER_LEFT);
        List<String> evComments = comments.getOrDefault(event.getId(), new ArrayList<>());
        Label commLabel = new Label("💬 "+evComments.size()); commLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #4a4a6a;");
        Button btnComment = new Button("Commenter"); btnComment.setStyle(
                "-fx-background-color: #1a1a2e; -fx-text-fill: #7c6fff; -fx-background-radius: 8; -fx-font-size: 11px; -fx-padding: 5 12; -fx-cursor: hand;");
        btnComment.setOnAction(e -> ouvrirCommentaires(event));
        Region fSpacer = new Region(); HBox.setHgrow(fSpacer, Priority.ALWAYS);
        Button btnVoir = new Button("Voir →"); btnVoir.setStyle(
                "-fx-background-color: "+color+"; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-size: 11px; -fx-padding: 5 14; -fx-cursor: hand;");
        btnVoir.setOnAction(e -> ouvrirDetailEvenement(event));
        footer.getChildren().addAll(commLabel, btnComment, fSpacer, btnVoir);
        content.getChildren().add(footer);

        carte.getChildren().addAll(imageBox, content);

        // Hover animation
        carte.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(180), carte); st.setToX(1.025); st.setToY(1.025); st.play();
            carte.setStyle(carte.getStyle().replace("rgba(0,0,0,0.5)", "rgba(124,111,255,0.35)").replace("-fx-border-color: "+color+"33", "-fx-border-color: "+color+"88"));
        });
        carte.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(180), carte); st.setToX(1.0); st.setToY(1.0); st.play();
            carte.setStyle(carte.getStyle().replace("rgba(124,111,255,0.35)", "rgba(0,0,0,0.5)").replace("-fx-border-color: "+color+"88", "-fx-border-color: "+color+"33"));
        });
        return carte;
    }

    // ── CARTE LISTE ─────────────────────────────────────────────────
    private VBox creerCarteListe(Event event) {
        boolean isFav = favorites.contains(event.getId());
        String color  = getTypeColor(event.getType());
        List<String> evComments = comments.getOrDefault(event.getId(), new ArrayList<>());

        VBox carte = new VBox(); carte.setMaxWidth(Double.MAX_VALUE);
        carte.setStyle("-fx-background-color: #0d0d18; -fx-background-radius: 14; -fx-border-color: #1a1a2e; -fx-border-radius: 14; -fx-border-width: 1; -fx-cursor: hand;");
        carte.setOnMouseEntered(e -> carte.setStyle(carte.getStyle().replace("#0d0d18","#111122").replace("#1a1a2e",color+"55")));
        carte.setOnMouseExited(e  -> carte.setStyle(carte.getStyle().replace("#111122","#0d0d18").replace(color+"55","#1a1a2e")));

        HBox row = new HBox(16); row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(16, 18, 16, 18));

        // Accent bar
        Region bar = new Region(); bar.setMinWidth(4); bar.setPrefWidth(4);
        bar.setStyle("-fx-background-color: "+color+"; -fx-background-radius: 2;");

        VBox info = new VBox(6); HBox.setHgrow(info, Priority.ALWAYS);
        HBox titleRow = new HBox(10); titleRow.setAlignment(Pos.CENTER_LEFT);
        Label titre = new Label(event.getTitre()!=null?event.getTitre():"Sans titre");
        titre.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label badgeL = new Label(event.getType()!=null?event.getType().toUpperCase():"—");
        badgeL.setStyle("-fx-background-color: "+color+"22; -fx-text-fill: "+color+"; -fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 6;");
        titleRow.getChildren().addAll(titre, badgeL);

        Label desc = new Label(event.getDescription()!=null?(event.getDescription().length()>100?event.getDescription().substring(0,100)+"…":event.getDescription()):"");
        desc.setStyle("-fx-font-size: 11px; -fx-text-fill: #4a4a6a;"); desc.setWrapText(true);

        String dateStr="";
        if(event.getDateCreation()!=null) { try { dateStr=event.getDateCreation().toLocalDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")); } catch(Exception ignored){} }
        Label dateLbl = new Label("📅 "+dateStr+"   👤 User #"+event.getUserId()+"   💬 "+evComments.size()+" commentaire(s)");
        dateLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #3a3a5a;");
        info.getChildren().addAll(titleRow, desc, dateLbl);

        // Actions
        VBox actions = new VBox(8); actions.setAlignment(Pos.CENTER_RIGHT);
        Button btnFav = new Button(isFav?"♥":"♡");
        btnFav.setStyle("-fx-background-color: "+(isFav?"#ff6b8a":"#1a1a2e")+"; -fx-text-fill: "+(isFav?"white":"#4a4a6a")+"; -fx-background-radius: 8; -fx-padding: 6 12; -fx-cursor: hand;");
        btnFav.setOnAction(e -> toggleFavorite(event.getId()));
        Button btnVoir = new Button("Voir"); btnVoir.setStyle("-fx-background-color: "+color+"; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 6 14; -fx-cursor: hand; -fx-font-size: 11px;");
        btnVoir.setOnAction(e -> ouvrirDetailEvenement(event));
        actions.getChildren().addAll(btnFav, btnVoir);

        row.getChildren().addAll(bar, info, actions);
        carte.getChildren().add(row);
        return carte;
    }

    // ── Ouvrir le détail complet d'un événement ─────────────────────
    private void ouvrirDetailEvenement(Event event) {
        String color = getTypeColor(event.getType());
        boolean isFav = favorites.contains(event.getId());
        List<String> evComments = comments.getOrDefault(event.getId(), new ArrayList<>());

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(event.getTitre());

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #0a0a0f; -fx-background-radius: 16;");
        root.setPrefWidth(540);

        // Header couleur
        VBox header = new VBox(8);
        header.setPadding(new Insets(24, 24, 20, 24));
        header.setStyle("-fx-background-color: linear-gradient(to bottom right, "+color+"44, "+color+"11); -fx-background-radius: 16 16 0 0;");

        Label typeLbl = new Label(event.getType()!=null?event.getType().toUpperCase():"ÉVÉNEMENT");
        typeLbl.setStyle("-fx-background-color: "+color+"55; -fx-text-fill: "+color+"; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 8;");
        Label title = new Label(event.getTitre()!=null?event.getTitre():"Sans titre");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white; -fx-font-family: 'Georgia';");
        title.setWrapText(true);
        String dateStr="";
        if(event.getDateCreation()!=null) { try { dateStr=event.getDateCreation().toLocalDate().format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy",Locale.FRENCH)); } catch(Exception ignored){} }
        Label dateLbl = new Label("📅 "+dateStr+"   👤 User #"+event.getUserId());
        dateLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: "+color+";");
        header.getChildren().addAll(typeLbl, title, dateLbl);

        // Body
        VBox body = new VBox(14); body.setPadding(new Insets(20,24,20,24));
        body.setStyle("-fx-background-color: #0d0d14;");

        if(event.getDescription()!=null&&!event.getDescription().isEmpty()) {
            Label descTitle = new Label("Description"); descTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #4a4a6a;");
            Label descTxt = new Label(event.getDescription()); descTxt.setStyle("-fx-font-size: 13px; -fx-text-fill: #aaaacc;"); descTxt.setWrapText(true);
            body.getChildren().addAll(descTitle, descTxt);
        }

        // Favoris
        HBox favRow = new HBox(10); favRow.setAlignment(Pos.CENTER_LEFT);
        Button favBtn = new Button(isFav ? "♥  Retirer des favoris" : "♡  Ajouter aux favoris");
        favBtn.setStyle("-fx-background-color: "+(isFav?"#2a1a20":"#1a1a2e")+"; -fx-text-fill: "+(isFav?"#ff6b8a":"#8888aa")+"; -fx-background-radius: 10; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px;");
        favBtn.setOnAction(e -> { toggleFavorite(event.getId()); dialog.close(); });
        favRow.getChildren().add(favBtn);
        body.getChildren().add(favRow);

        // Section commentaires
        Label commTitle = new Label("💬  Commentaires ("+evComments.size()+")");
        commTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: white;");
        body.getChildren().add(commTitle);

        VBox commentsList = new VBox(8);
        for(String c : evComments) {
            String[] parts = c.split("::", 3);
            String user = parts.length>0?parts[0]:"Anonyme";
            String text = parts.length>1?parts[1]:"";
            String ts   = parts.length>2?parts[2]:"";
            VBox cb = new VBox(4);
            cb.setStyle("-fx-background-color: #0a0a0f; -fx-background-radius: 10; -fx-padding: 10 14;");
            Label userL = new Label("👤 "+user+"  ·  "+ts); userL.setStyle("-fx-font-size: 10px; -fx-text-fill: #4a4a6a;");
            Label textL = new Label(text); textL.setStyle("-fx-font-size: 12px; -fx-text-fill: #aaaacc;"); textL.setWrapText(true);
            cb.getChildren().addAll(userL, textL);
            commentsList.getChildren().add(cb);
        }
        if(evComments.isEmpty()) {
            Label none = new Label("Soyez le premier à commenter !");
            none.setStyle("-fx-font-size: 12px; -fx-text-fill: #3a3a5a; -fx-padding: 8 0;");
            commentsList.getChildren().add(none);
        }
        ScrollPane commScroll = new ScrollPane(commentsList); commScroll.setFitToWidth(true); commScroll.setPrefHeight(140);
        commScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        body.getChildren().add(commScroll);

        // Nouveau commentaire
        HBox newComm = new HBox(10); newComm.setAlignment(Pos.CENTER_LEFT);
        TextField commField = new TextField(); commField.setPromptText("Écrire un commentaire...");
        commField.setStyle("-fx-background-color: #0a0a0f; -fx-text-fill: white; -fx-border-color: #2a2a3e; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 8 12; -fx-font-size: 12px;");
        HBox.setHgrow(commField, Priority.ALWAYS);
        Button sendBtn = new Button("Envoyer");
        sendBtn.setStyle("-fx-background-color: #7c6fff; -fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 12px;");
        sendBtn.setOnAction(e -> {
            String txt = commField.getText().trim();
            if(!txt.isEmpty()) {
                String ts = DateTimeFormatter.ofPattern("dd/MM HH:mm").format(java.time.LocalDateTime.now());
                String entry = "Vous::" + txt + "::" + ts;
                comments.computeIfAbsent(event.getId(), k -> new ArrayList<>()).add(entry);
                saveComments();
                commField.clear();
                // Rafraîchir
                VBox newCb = new VBox(4); newCb.setStyle("-fx-background-color: #0a0a0f; -fx-background-radius: 10; -fx-padding: 10 14;");
                Label uL = new Label("👤 Vous  ·  "+ts); uL.setStyle("-fx-font-size: 10px; -fx-text-fill: #4a4a6a;");
                Label tL = new Label(txt); tL.setStyle("-fx-font-size: 12px; -fx-text-fill: #aaaacc;"); tL.setWrapText(true);
                newCb.getChildren().addAll(uL,tL);
                commentsList.getChildren().add(newCb);
                // Enlever message vide
                commentsList.getChildren().removeIf(n -> n instanceof Label && ((Label)n).getText().contains("premier"));
                refreshEventDisplay();
            }
        });
        newComm.getChildren().addAll(commField, sendBtn);
        body.getChildren().add(newComm);

        root.getChildren().addAll(header, body);
        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().setStyle("-fx-background-color: #0a0a0f; -fx-background-radius: 16; -fx-padding: 0;");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setStyle("-fx-background-color: #1a1a2e; -fx-text-fill: #8888aa; -fx-background-radius: 8; -fx-padding: 8 16;");
        dialog.showAndWait();
    }

    // ── Dialogue commentaires rapide ─────────────────────────────────
    private void ouvrirCommentaires(Event event) { ouvrirDetailEvenement(event); }

    private Label defaultEmoji() { Label l=new Label("✦"); l.setStyle("-fx-font-size: 42px; -fx-text-fill: #7c6fff;"); return l; }

    private String getTypeColor(String type) {
        if(type==null) return "#7c6fff";
        return TYPE_COLORS.getOrDefault(type.toUpperCase(), "#7c6fff");
    }

    private VBox creerSectionSponsors(int eventId) {
        List<Sponsor> sponsors;
        try { sponsors=sponsorService.recupererParEvent(eventId); } catch(SQLException e){ return null; }
        if(sponsors.isEmpty()) return null;
        VBox section=new VBox(6); section.setStyle("-fx-padding: 4 0 0 0;");
        HBox hdr=new HBox(6); hdr.setAlignment(Pos.CENTER_LEFT);
        Label ic=new Label("💼"); ic.setStyle("-fx-font-size: 10px;");
        Label lb=new Label("Sponsors ("+sponsors.size()+")"); lb.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #3a3a5a;");
        hdr.getChildren().addAll(ic,lb);
        FlowPane flow=new FlowPane(6,4);
        for(Sponsor s:sponsors) {
            String col=getCouleurSponsor(s.getType());
            Label bdg=new Label(s.getNomSponsor()!=null?s.getNomSponsor():""); bdg.setStyle("-fx-background-color: "+col+"22; -fx-text-fill: "+col+"; -fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 6;");
            flow.getChildren().add(bdg);
        }
        section.getChildren().addAll(hdr,flow); return section;
    }
    private String getCouleurSponsor(String type) {
        if(type==null) return "#888888";
        return switch(type.toLowerCase()){case "or"->"#e6a817";case "argent"->"#7f8c8d";case "bronze"->"#c0652b";case "platine"->"#7c6fff";default->"#27ae60";};
    }

    @FXML private void handleEventSearch(KeyEvent e) { appliquerFiltresEtTri(); }

    @FXML private void handleGererEvents() {
        try {
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/event.fxml")); Node page=loader.load();
            StackPane ca=(StackPane)eventCardsContainer.getScene().lookup("#contentArea");
            if(ca!=null) ca.getChildren().setAll(page);
        } catch(Exception ex){ showError("Erreur","Impossible d'ouvrir la gestion des événements."); }
    }

    // =========================================================================
    // CALENDRIER
    // =========================================================================
    @FXML private void handleShowCalendar() {
        if(tousLesEvents==null) chargerEvents();
        buildCalendarDayHeaders(); buildCalendarGrid(); showDefaultDetailMessage();
        hideAllViews();
        viewCalendar.setVisible(true); viewCalendar.setManaged(true);
        FadeTransition ft=new FadeTransition(Duration.millis(300),viewCalendar); ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    @FXML private void handleCalendarBack() {
        hideAllViews(); viewEvents.setVisible(true); viewEvents.setManaged(true);
        FadeTransition ft=new FadeTransition(Duration.millis(200),viewEvents); ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    @FXML private void handlePrevMonth() { currentYearMonth=currentYearMonth.minusMonths(1); buildCalendarGrid(); showDefaultDetailMessage(); }
    @FXML private void handleNextMonth() { currentYearMonth=currentYearMonth.plusMonths(1);  buildCalendarGrid(); showDefaultDetailMessage(); }
    @FXML private void handleGoToday()   { currentYearMonth=YearMonth.now();                  buildCalendarGrid(); showDefaultDetailMessage(); }

    private void buildCalendarDayHeaders() {
        if(calendarHeaderGrid==null) return;
        calendarHeaderGrid.getChildren().clear();
        String[] days={"Lun","Mar","Mer","Jeu","Ven","Sam","Dim"};
        for(int i=0;i<7;i++) {
            Label lbl=new Label(days[i]); lbl.setMaxWidth(Double.MAX_VALUE); lbl.setAlignment(Pos.CENTER);
            lbl.setStyle("-fx-text-fill: "+(i>=5?"#3a3a5a":"#4a4a6a")+"; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 0 0 8 0;");
            calendarHeaderGrid.add(lbl,i,0);
        }
    }

    private void buildCalendarGrid() {
        if(calendarGrid==null||lblCurrentMonth==null) return;
        calendarGrid.getChildren().clear(); calendarGrid.getRowConstraints().clear();
        String mn=currentYearMonth.getMonth().getDisplayName(TextStyle.FULL,Locale.FRENCH);
        mn=Character.toUpperCase(mn.charAt(0))+mn.substring(1);
        lblCurrentMonth.setText(mn+" "+currentYearMonth.getYear());
        LocalDate fd=currentYearMonth.atDay(1); int sc=fd.getDayOfWeek().getValue()-1; int dim=currentYearMonth.lengthOfMonth();
        for(int i=0;i<sc;i++) calendarGrid.add(emptyCalCell(),i,0);
        int row=0,col=sc;
        for(int day=1;day<=dim;day++) {
            LocalDate date=currentYearMonth.atDay(day);
            List<Event> dEvs=calendarEventsMap.getOrDefault(date,List.of());
            calendarGrid.add(createDayCell(day,date,dEvs),col,row);
            col++; if(col==7){col=0;row++;}
        }
        int tr=row+(col>0?1:0);
        for(int r=0;r<tr;r++){RowConstraints rc=new RowConstraints(); rc.setMinHeight(78); rc.setVgrow(Priority.ALWAYS); calendarGrid.getRowConstraints().add(rc);}
    }

    private StackPane createDayCell(int day, LocalDate date, List<Event> events) {
        VBox content=new VBox(4); content.setPadding(new Insets(8)); content.setAlignment(Pos.TOP_LEFT);
        boolean isToday=date.equals(LocalDate.now()), hasEvents=!events.isEmpty(), isWeekend=date.getDayOfWeek().getValue()>=6;
        if(isToday) {
            Label dl=new Label(String.valueOf(day)); dl.setStyle("-fx-text-fill: #0a0a0f; -fx-font-weight: bold; -fx-font-size: 13px;");
            StackPane circ=new StackPane(dl); circ.setStyle("-fx-background-color: #7c6fff; -fx-background-radius: 50%;");
            circ.setMaxWidth(28); circ.setMaxHeight(28); circ.setMinWidth(28); circ.setMinHeight(28);
            content.getChildren().add(circ);
        } else {
            Label dl=new Label(String.valueOf(day)); dl.setStyle("-fx-text-fill: "+(hasEvents?"white":isWeekend?"#2a2a3a":"#3a3a5a")+"; -fx-font-size: 13px; -fx-font-weight: bold;");
            content.getChildren().add(dl);
        }
        if(hasEvents) {
            HBox dots=new HBox(4); dots.setAlignment(Pos.CENTER_LEFT);
            int shown=Math.min(events.size(),3);
            for(int i=0;i<shown;i++) { String col=getTypeColor(events.get(i).getType()); Circle dot=new Circle(4); dot.setFill(Color.web(col)); dots.getChildren().add(dot); }
            if(events.size()>3) { Label more=new Label("+"+(events.size()-3)); more.setStyle("-fx-text-fill: #4a4a6a; -fx-font-size: 9px;"); dots.getChildren().add(more); }
            content.getChildren().add(dots);
            String prev=events.size()==1?truncate(events.get(0).getTitre()!=null?events.get(0).getTitre():"",16):events.size()+" événements";
            Label pl=new Label(prev); pl.setStyle("-fx-text-fill: #4a4a5a; -fx-font-size: 10px;"); content.getChildren().add(pl);
        }
        String bg=isToday?"#14102a":hasEvents?"#0d1020":isWeekend?"#0c0c12":"#0d0d14";
        String bdr=isToday?"#7c6fff":hasEvents?"#1e1e30":"#141420";
        String bgH=isToday?"#1a1640":hasEvents?"#111228":"#111118";
        StackPane cell=new StackPane(content); StackPane.setAlignment(content,Pos.TOP_LEFT);
        cell.setStyle(cellStyle(bg,bdr)); cell.setMinHeight(78);
        cell.setOnMouseEntered(e->cell.setStyle(cellStyle(bgH,bdr)));
        cell.setOnMouseExited(e->cell.setStyle(cellStyle(bg,bdr)));
        cell.setOnMouseClicked(e->showDayEvents(date,events));
        return cell;
    }

    private String cellStyle(String bg, String border) {
        return "-fx-background-color: "+bg+"; -fx-background-radius: 10; -fx-border-color: "+border+"; -fx-border-radius: 10; -fx-border-width: 1; -fx-cursor: hand;";
    }

    private StackPane emptyCalCell() { StackPane sp=new StackPane(); sp.setStyle("-fx-background-color: transparent;"); sp.setMinHeight(78); return sp; }

    private void showDayEvents(LocalDate date, List<Event> events) {
        if(lblSelectedDate==null||calendarEventList==null) return;
        DateTimeFormatter fmt=DateTimeFormatter.ofPattern("EEEE d MMMM yyyy",Locale.FRENCH);
        String ds=date.format(fmt); lblSelectedDate.setText(Character.toUpperCase(ds.charAt(0))+ds.substring(1));
        if(lblEventCount!=null) lblEventCount.setText(events.isEmpty()?"Aucun événement":events.size()+" événement"+(events.size()>1?"s":""));
        calendarEventList.getChildren().clear();
        if(events.isEmpty()) {
            VBox empty=new VBox(8); empty.setAlignment(Pos.CENTER); empty.setPadding(new Insets(40,0,0,0));
            Label i=new Label("📭"); i.setStyle("-fx-font-size: 28px;");
            Label m=new Label("Aucun événement ce jour"); m.setStyle("-fx-text-fill: #3a3a5a; -fx-font-size: 13px;");
            empty.getChildren().addAll(i,m); calendarEventList.getChildren().add(empty); return;
        }
        events.forEach(ev->calendarEventList.getChildren().add(createCalEventCard(ev)));
    }

    private void showDefaultDetailMessage() {
        if(lblSelectedDate==null) return;
        lblSelectedDate.setText("Sélectionnez un jour");
        if(lblEventCount!=null) lblEventCount.setText("");
        if(calendarEventList!=null) {
            calendarEventList.getChildren().clear();
            VBox hint=new VBox(8); hint.setAlignment(Pos.CENTER); hint.setPadding(new Insets(40,0,0,0));
            Label i=new Label("👆"); i.setStyle("-fx-font-size: 26px;");
            Label m=new Label("Cliquez sur un jour\npour voir ses événements"); m.setStyle("-fx-text-fill: #3a3a5a; -fx-font-size: 12px; -fx-text-alignment: center;"); m.setWrapText(true);
            hint.getChildren().addAll(i,m); calendarEventList.getChildren().add(hint);
        }
    }

    private VBox createCalEventCard(Event ev) {
        String color=getTypeColor(ev.getType()); String nom=ev.getTitre()!=null?ev.getTitre():"Sans titre";
        VBox card=new VBox(8); card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: #0d0d18; -fx-background-radius: 12; -fx-border-color: "+color+"; -fx-border-radius: 12; -fx-border-width: 0 0 0 3; -fx-cursor: hand;");
        card.setOnMouseEntered(e->card.setStyle(card.getStyle().replace("#0d0d18","#111122")));
        card.setOnMouseExited(e->card.setStyle(card.getStyle().replace("#111122","#0d0d18")));
        card.setOnMouseClicked(e->ouvrirDetailEvenement(ev));
        HBox tr=new HBox(6); tr.setAlignment(Pos.CENTER_LEFT);
        Circle dot=new Circle(5); dot.setFill(Color.web(color));
        Label tl=new Label(ev.getType()!=null?ev.getType().toUpperCase():"—"); tl.setStyle("-fx-text-fill: "+color+"; -fx-font-size: 10px; -fx-font-weight: bold;");
        boolean isFav=favorites.contains(ev.getId());
        Region sp=new Region(); HBox.setHgrow(sp,Priority.ALWAYS);
        Label fav=new Label(isFav?"♥":""); fav.setStyle("-fx-text-fill: #ff6b8a; -fx-font-size: 12px;");
        tr.getChildren().addAll(dot,tl,sp,fav);
        Label titleL=new Label(nom); titleL.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;"); titleL.setWrapText(true);
        card.getChildren().addAll(tr,titleL);
        List<String> evComments=comments.getOrDefault(ev.getId(),new ArrayList<>());
        if(!evComments.isEmpty()) {
            Label cl=new Label("💬 "+evComments.size()+" commentaire(s)"); cl.setStyle("-fx-text-fill: #3a3a5a; -fx-font-size: 10px;");
            card.getChildren().add(cl);
        }
        return card;
    }

    // =========================================================================
    // UTILITAIRES
    // =========================================================================
    private void showInfo(String h, String c) { Alert a=new Alert(AlertType.INFORMATION); a.setTitle("Info"); a.setHeaderText(h); a.setContentText(c); a.showAndWait(); }
    private void showError(String h, String c) { Alert a=new Alert(AlertType.ERROR); a.setTitle("Erreur"); a.setHeaderText(h); a.setContentText(c); a.showAndWait(); }
    private String truncate(String s, int max) { return s.length()<=max?s:s.substring(0,max)+"…"; }
}
