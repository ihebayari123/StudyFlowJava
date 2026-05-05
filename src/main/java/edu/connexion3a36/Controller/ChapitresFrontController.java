package edu.connexion3a36.Controller;

import edu.connexion3a36.entities.Chapitre;
import edu.connexion3a36.entities.Cours;
import edu.connexion3a36.services.AiSummaryService;
import edu.connexion3a36.services.ChapitreService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ChapitresFrontController {

    @FXML private Button btnBack;
    @FXML private Label  courseTitle;
    @FXML private Label  courseDescription;
    @FXML private Label  courseIcon;
    @FXML private Label  lblTotalChapitres;
    @FXML private Label  lblWithVideo;
    @FXML private VBox   chapitresContainer;

    private ChapitreService  chapitreService;
    private AiSummaryService aiSummaryService;
    private Cours            currentCourse;
    private Runnable         onBackAction;

    private static final String[] COLORS = {
            "#e8f0fe", "#fce4ec", "#e8f5e9", "#fff3e0", "#f3e5f5",
            "#e0f7fa", "#fff8e1", "#fbe9e7", "#ede7f6", "#e0f2f1"
    };

    // ── Init ──────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        chapitreService  = new ChapitreService();
        aiSummaryService = new AiSummaryService();
        btnBack.setOnAction(e -> { if (onBackAction != null) onBackAction.run(); });
    }

    public void setCours(Cours cours, Runnable onBack) {
        this.currentCourse = cours;
        this.onBackAction  = onBack;
        loadChapitres();
    }

    // ── Load chapters ─────────────────────────────────────────────────────
    private void loadChapitres() {
        if (currentCourse == null) return;

        courseTitle.setText(currentCourse.getTitre());
        String desc = currentCourse.getDescription() != null ? currentCourse.getDescription() : "";
        if (desc.length() > 100) desc = desc.substring(0, 100) + "...";
        courseDescription.setText(desc);
        courseIcon.setText(getCourseIcon(currentCourse.getTitre()));

        List<Chapitre> chapitres = chapitreService.findByCourse(currentCourse.getId());

        lblTotalChapitres.setText(String.valueOf(chapitres.size()));
        long withVideo = chapitres.stream()
                .filter(ch -> ch.getVideoUrl() != null && !ch.getVideoUrl().isEmpty()).count();
        lblWithVideo.setText(String.valueOf(withVideo));

        chapitresContainer.getChildren().clear();
        if (chapitres.isEmpty()) {
            Label empty = new Label("Aucun chapitre disponible pour ce cours.");
            empty.setStyle("-fx-text-fill:#9e9e9e;-fx-font-size:14px;");
            empty.setPadding(new Insets(40));
            chapitresContainer.getChildren().add(empty);
        } else {
            for (int i = 0; i < chapitres.size(); i++)
                chapitresContainer.getChildren().add(buildChapitreCard(chapitres.get(i), i));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CHAPTER CARD
    // ════════════════════════════════════════════════════════════════════════
    private VBox buildChapitreCard(Chapitre ch, int index) {
        String bgColor = COLORS[index % COLORS.length];

        VBox card = new VBox(0);
        card.setStyle("-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),10,0,0,3);");

        // ── Header row ────────────────────────────────────────────────────
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 16, 20));
        applyHeaderStyle(header, "white", bgColor);

        Label orderLabel = new Label(String.valueOf(ch.getOrdre() != null ? ch.getOrdre() : index + 1));
        orderLabel.setMinWidth(34); orderLabel.setMinHeight(34);
        orderLabel.setAlignment(Pos.CENTER);
        orderLabel.setStyle("-fx-background-color:" + bgColor +
                ";-fx-text-fill:#333;-fx-font-size:14px;-fx-font-weight:bold;-fx-background-radius:50%;");

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label titleLabel = new Label(ch.getTitre());
        titleLabel.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#1a1a2e;");
        HBox tags = new HBox(6);
        if (ch.getVideoUrl() != null && !ch.getVideoUrl().isEmpty())
            tags.getChildren().add(createTag("▶ Vidéo", "#e3f2fd", "#1565c0"));
        if (ch.getDurationMinutes() != null)
            tags.getChildren().add(createTag("⏱ " + ch.getDurationMinutes() + " min", "#fce4ec", "#c62828"));
        if (ch.getLinks() != null && !ch.getLinks().isEmpty())
            tags.getChildren().add(createTag("🔗 " + ch.getLinks().size() + " liens", "#e8f5e9", "#2e7d32"));
        info.getChildren().addAll(titleLabel, tags);

        Button summaryBtn = buildPillButton("✨ Résumer", "#2979FF");
        summaryBtn.setOnAction(e -> {
            e.consume();
            handleGenerateSummary(ch, summaryBtn);
        });

        Label arrowLabel = new Label("▼");
        arrowLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#aaa;");

        header.getChildren().addAll(orderLabel, info, summaryBtn, arrowLabel);

        // ── Detail panel ──────────────────────────────────────────────────
        VBox detailPanel = createDetailPanel(ch, bgColor);
        detailPanel.setVisible(false);
        detailPanel.setManaged(false);

        header.setOnMouseClicked(e -> {
            boolean vis = detailPanel.isVisible();
            detailPanel.setVisible(!vis);
            detailPanel.setManaged(!vis);
            arrowLabel.setText(vis ? "▼" : "▲");
        });
        header.setOnMouseEntered(e -> applyHeaderStyle(header, "#fafafa", bgColor));
        header.setOnMouseExited(e  -> applyHeaderStyle(header, "white",   bgColor));

        card.getChildren().addAll(header, detailPanel);
        return card;
    }

    private void applyHeaderStyle(HBox h, String bg, String border) {
        h.setStyle("-fx-background-color:" + bg +
                ";-fx-background-radius:16;-fx-border-color:" + border +
                ";-fx-border-radius:16;-fx-border-width:1.5;-fx-cursor:hand;");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DETAIL PANEL
    // ════════════════════════════════════════════════════════════════════════
    private VBox createDetailPanel(Chapitre ch, String bgColor) {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(0, 20, 20, 20));
        panel.setStyle("-fx-background-color:white;" +
                "-fx-border-color:" + bgColor + ";" +
                "-fx-border-width:0 1.5 1.5 1.5;" +
                "-fx-border-radius:0 0 16 16;" +
                "-fx-background-radius:0 0 16 16;");

        // Content text
        if (ch.getContenu() != null && !ch.getContenu().isEmpty()) {
            Label c = new Label(ch.getContenu());
            c.setStyle("-fx-font-size:13px;-fx-text-fill:#555;-fx-wrap-text:true;");
            c.setMaxWidth(Double.MAX_VALUE);
            panel.getChildren().addAll(sectionLabel("📝 Contenu"), c);
        }

        // Video
        if (ch.getVideoUrl() != null && !ch.getVideoUrl().isEmpty()) {
            panel.getChildren().add(sectionLabel("▶ Vidéo"));
            HBox vBox = new HBox(10);
            vBox.setAlignment(Pos.CENTER_LEFT);
            vBox.setPadding(new Insets(10, 14, 10, 14));
            vBox.setStyle("-fx-background-color:#e3f2fd;-fx-background-radius:10;");
            Label url = new Label(ch.getVideoUrl());
            url.setStyle("-fx-font-size:12px;-fx-text-fill:#1565c0;");
            HBox.setHgrow(url, Priority.ALWAYS);
            Button ob = new Button("Ouvrir");
            ob.setStyle("-fx-background-color:#1565c0;-fx-text-fill:white;" +
                    "-fx-background-radius:8;-fx-font-size:12px;-fx-padding:6 14;-fx-cursor:hand;");
            ob.setOnAction(e -> openUrl(ch.getVideoUrl()));
            vBox.getChildren().addAll(url, ob);
            panel.getChildren().add(vBox);
        }

        // Links
        if (ch.getLinks() != null && !ch.getLinks().isEmpty()) {
            panel.getChildren().add(sectionLabel("🔗 Ressources"));
            for (String link : ch.getLinks()) {
                HBox lb = new HBox(10);
                lb.setAlignment(Pos.CENTER_LEFT);
                lb.setPadding(new Insets(8, 12, 8, 12));
                lb.setStyle("-fx-background-color:#e8f5e9;-fx-background-radius:8;");
                Label ll = new Label("🔗 " + link);
                ll.setStyle("-fx-font-size:12px;-fx-text-fill:#2e7d32;");
                HBox.setHgrow(ll, Priority.ALWAYS);
                Button ol = new Button("Ouvrir");
                ol.setStyle("-fx-background-color:#2e7d32;-fx-text-fill:white;" +
                        "-fx-background-radius:6;-fx-font-size:11px;-fx-padding:4 10;-fx-cursor:hand;");
                ol.setOnAction(e -> openUrl(link));
                lb.getChildren().addAll(ll, ol);
                panel.getChildren().add(lb);
            }
        }

        // Image
        if (ch.getImageUrl() != null && !ch.getImageUrl().isEmpty()) {
            try {
                ImageView iv = new ImageView(new Image(ch.getImageUrl(), 400, 200, true, true));
                iv.setPreserveRatio(true);
                iv.setStyle("-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.1),8,0,0,2);");
                panel.getChildren().add(iv);
            } catch (Exception ignored) {
                panel.getChildren().add(new Label("⚠️ Image non disponible"));
            }
        }

        if (panel.getChildren().isEmpty()) {
            Label empty = new Label("Aucun contenu détaillé pour ce chapitre.");
            empty.setStyle("-fx-text-fill:#9e9e9e;-fx-font-size:13px;-fx-padding:12 0;");
            panel.getChildren().add(empty);
        }

        // ── Q&A always at the bottom ───────────────────────────────────────
        panel.getChildren().add(buildQaSection(ch));
        return panel;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Q&A ASSISTANT SECTION
    // ════════════════════════════════════════════════════════════════════════
    private VBox buildQaSection(Chapitre ch) {
        // Per-chapter conversation history  [{role, content}, ...]
        List<String[]> history = new ArrayList<>();

        // ── Outer box ─────────────────────────────────────────────────────
        VBox qaBox = new VBox(0);
        VBox.setMargin(qaBox, new Insets(10, 0, 0, 0));
        qaBox.setStyle("-fx-background-color:#F8F9FF;" +
                "-fx-background-radius:14;" +
                "-fx-border-color:#E0E7FF;" +
                "-fx-border-radius:14;" +
                "-fx-border-width:1.5;");

        // ── Header ────────────────────────────────────────────────────────
        HBox qaHeader = new HBox(8);
        qaHeader.setAlignment(Pos.CENTER_LEFT);
        qaHeader.setPadding(new Insets(10, 14, 10, 14));
        qaHeader.setStyle("-fx-background-color:#EEF2FF;-fx-background-radius:14 14 0 0;");
        Label qaIcon  = new Label("🤖");
        qaIcon.setStyle("-fx-font-size:16px;");
        Label qaTitle = new Label("Assistant Q&A — Posez une question sur ce chapitre");
        qaTitle.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#3730a3;");
        qaHeader.getChildren().addAll(qaIcon, qaTitle);

        // ── Chat messages scrollable area ─────────────────────────────────
        VBox messagesBox = new VBox(8);
        messagesBox.setPadding(new Insets(12, 14, 8, 14));

        // Hint message to start
        messagesBox.getChildren().add(buildHintBubble(
                "Bonjour ! Je suis votre assistant pour « " + ch.getTitre() +
                        " ». Posez-moi n'importe quelle question sur ce chapitre 💡"));

        ScrollPane chatScroll = new ScrollPane(messagesBox);
        chatScroll.setFitToWidth(true);
        chatScroll.setPrefHeight(210);
        chatScroll.setMaxHeight(300);
        chatScroll.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;");
        chatScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        chatScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // ── Input row ─────────────────────────────────────────────────────
        TextField inputField = new TextField();
        inputField.setPromptText("Ex: Qu'est-ce qu'une variable en Python ? (Entrée pour envoyer)");
        inputField.setStyle("-fx-background-color:white;" +
                "-fx-border-color:#c7d2fe;" +
                "-fx-border-radius:10;" +
                "-fx-background-radius:10;" +
                "-fx-padding:9 12;" +
                "-fx-font-size:12px;");
        HBox.setHgrow(inputField, Priority.ALWAYS);

        Button sendBtn = buildPillButton("Envoyer ↩", "#4f46e5");

        HBox inputRow = new HBox(8, inputField, sendBtn);
        inputRow.setAlignment(Pos.CENTER);
        inputRow.setPadding(new Insets(8, 14, 12, 14));

        // ── Send logic ────────────────────────────────────────────────────
        Runnable doSend = () -> {
            String question = inputField.getText().trim();
            if (question.isEmpty()) return;

            // Show user bubble
            messagesBox.getChildren().add(buildUserBubble(question));
            inputField.clear();
            sendBtn.setDisable(true);
            sendBtn.setText("⏳");
            scrollToBottom(chatScroll);

            // Add to history
            history.add(new String[]{"user", question});

            // Show typing indicator
            HBox typingRow = buildTypingIndicator();
            messagesBox.getChildren().add(typingRow);
            scrollToBottom(chatScroll);

            Task<String> task = new Task<>() {
                @Override
                protected String call() throws Exception {
                    return aiSummaryService.askQuestion(ch, question, history);
                }
            };

            task.setOnSucceeded(ev -> {
                String answer = task.getValue();
                history.add(new String[]{"assistant", answer});
                messagesBox.getChildren().remove(typingRow);
                messagesBox.getChildren().add(buildAiBubble(answer));
                sendBtn.setDisable(false);
                sendBtn.setText("Envoyer ↩");
                scrollToBottom(chatScroll);
            });

            task.setOnFailed(ev -> {
                messagesBox.getChildren().remove(typingRow);
                messagesBox.getChildren().add(
                        buildErrorBubble(task.getException().getMessage()));
                sendBtn.setDisable(false);
                sendBtn.setText("Envoyer ↩");
                scrollToBottom(chatScroll);
            });

            Thread t = new Thread(task);
            t.setDaemon(true);
            t.start();
        };

        sendBtn.setOnAction(e -> doSend.run());
        inputField.setOnAction(e -> doSend.run()); // Enter key

        qaBox.getChildren().addAll(qaHeader, chatScroll, inputRow);
        return qaBox;
    }

    // ── Scroll helper ─────────────────────────────────────────────────────
    private void scrollToBottom(ScrollPane sp) {
        Platform.runLater(() -> { sp.layout(); sp.setVvalue(1.0); });
    }

    // ── Bubble builders ───────────────────────────────────────────────────

    /** User message — right-aligned indigo */
    private HBox buildUserBubble(String text) {
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setMaxWidth(340);
        lbl.setPadding(new Insets(9, 13, 9, 13));
        lbl.setStyle("-fx-background-color:#4f46e5;" +
                "-fx-text-fill:white;" +
                "-fx-font-size:12px;" +
                "-fx-background-radius:14 14 2 14;");
        HBox row = new HBox(lbl);
        row.setAlignment(Pos.CENTER_RIGHT);
        return row;
    }

    /** AI answer — left-aligned white card */
    private HBox buildAiBubble(String text) {
        // Robot avatar
        Label avatar = new Label("🤖");
        avatar.setStyle("-fx-font-size:18px;");

        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setMaxWidth(320);
        lbl.setPadding(new Insets(9, 13, 9, 13));
        lbl.setStyle("-fx-background-color:white;" +
                "-fx-text-fill:#1a1a2e;" +
                "-fx-font-size:12px;" +
                "-fx-background-radius:14 14 14 2;" +
                "-fx-border-color:#e0e7ff;" +
                "-fx-border-radius:14 14 14 2;" +
                "-fx-border-width:1;");

        HBox row = new HBox(6, avatar, lbl);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /** Subtle centred hint */
    private HBox buildHintBubble(String text) {
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setMaxWidth(380);
        lbl.setPadding(new Insets(8, 12, 8, 12));
        lbl.setStyle("-fx-background-color:#EEF2FF;" +
                "-fx-text-fill:#6366f1;" +
                "-fx-font-size:11px;" +
                "-fx-font-style:italic;" +
                "-fx-background-radius:10;");
        HBox row = new HBox(lbl);
        row.setAlignment(Pos.CENTER);
        return row;
    }

    /** Animated "typing..." indicator */
    private HBox buildTypingIndicator() {
        Label avatar = new Label("🤖");
        avatar.setStyle("-fx-font-size:18px;");
        Label dots = new Label("● ● ●");
        dots.setStyle("-fx-text-fill:#6366f1;-fx-font-size:14px;");
        dots.setPadding(new Insets(8, 12, 8, 12));
        dots.setStyle("-fx-background-color:white;" +
                "-fx-text-fill:#6366f1;" +
                "-fx-font-size:12px;" +
                "-fx-background-radius:14;" +
                "-fx-border-color:#e0e7ff;" +
                "-fx-border-radius:14;" +
                "-fx-border-width:1;");
        HBox row = new HBox(6, avatar, dots);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /** Error bubble */
    private HBox buildErrorBubble(String text) {
        Label lbl = new Label("⚠️ " + text);
        lbl.setWrapText(true);
        lbl.setMaxWidth(340);
        lbl.setPadding(new Insets(8, 12, 8, 12));
        lbl.setStyle("-fx-background-color:#fff0f0;" +
                "-fx-text-fill:#c62828;" +
                "-fx-font-size:11px;" +
                "-fx-background-radius:10;" +
                "-fx-border-color:#ffcdd2;" +
                "-fx-border-radius:10;" +
                "-fx-border-width:1;");
        HBox row = new HBox(lbl);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  AI SUMMARY (unchanged)
    // ════════════════════════════════════════════════════════════════════════
    private void handleGenerateSummary(Chapitre ch, Button btn) {
        btn.setDisable(true);
        btn.setText("⏳ Génération...");
        Task<String> task = new Task<>() {
            @Override protected String call() throws Exception {
                return aiSummaryService.generateSummary(ch);
            }
        };
        task.setOnSucceeded(ev -> {
            btn.setDisable(false); btn.setText("✨ Résumer");
            showSummaryDialog(ch, task.getValue());
        });
        task.setOnFailed(ev -> {
            btn.setDisable(false); btn.setText("✨ Résumer");
            showAlert("Erreur IA",
                    "Impossible de générer le résumé:\n" + task.getException().getMessage(),
                    Alert.AlertType.ERROR);
        });
        new Thread(task) {{ setDaemon(true); }}.start();
    }

    private void showSummaryDialog(Chapitre ch, String summary) {
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.initOwner(chapitresContainer.getScene().getWindow());
        dialog.setTitle("✨ Résumé IA — " + ch.getTitre());

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:#F5F6FA;");

        HBox hdr = new HBox(12);
        hdr.setAlignment(Pos.CENTER_LEFT);
        hdr.setPadding(new Insets(18, 24, 18, 24));
        hdr.setStyle("-fx-background-color:#1A1A2E;");
        Label ico = new Label("✨"); ico.setStyle("-fx-font-size:24px;");
        VBox hi = new VBox(2);
        Label ht = new Label("Résumé IA");
        ht.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:white;");
        Label hs = new Label(ch.getTitre());
        hs.setStyle("-fx-font-size:11px;-fx-text-fill:#9e9e9e;");
        hi.getChildren().addAll(ht, hs);
        hdr.getChildren().addAll(ico, hi);

        VBox body = new VBox(6);
        body.setPadding(new Insets(20));
        for (String line : summary.split("\n")) {
            String t = line.trim();
            if (t.isEmpty()) continue;
            Label lbl = new Label(t);
            lbl.setWrapText(true); lbl.setMaxWidth(460);
            if      (t.matches("^\\d+\\..*")) lbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#2979FF;-fx-padding:6 0 2 0;");
            else if (t.startsWith("•"))       lbl.setStyle("-fx-font-size:12px;-fx-text-fill:#333;-fx-padding:1 0 1 12;");
            else if (t.startsWith("✓"))       lbl.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#2e7d32;-fx-padding:1 0 1 12;");
            else                              lbl.setStyle("-fx-font-size:12px;-fx-text-fill:#555;");
            body.getChildren().add(lbl);
        }

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;");
        scroll.setPrefHeight(340);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Button dlBtn = buildPillButton("⬇ Télécharger en PDF", "#4CAF50");
        dlBtn.setOnAction(e -> handleDownloadPdf(ch, summary, dialog));
        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color:#E0E0E0;-fx-text-fill:#333;" +
                "-fx-font-size:13px;-fx-padding:10 24;-fx-background-radius:10;-fx-cursor:hand;");
        closeBtn.setOnAction(e -> dialog.close());

        HBox footer = new HBox(12, dlBtn, closeBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 20, 14, 20));
        footer.setStyle("-fx-background-color:white;-fx-border-color:#e0e0e0;-fx-border-width:1 0 0 0;");

        root.getChildren().addAll(hdr, scroll, footer);
        dialog.setScene(new javafx.scene.Scene(root, 520, 480));
        dialog.show();
    }

    private void handleDownloadPdf(Chapitre ch, String summary, javafx.stage.Stage owner) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choisir le dossier de téléchargement");
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File folder = chooser.showDialog(owner);
        if (folder == null) return;

        Task<File> task = new Task<>() {
            @Override protected File call() throws Exception {
                return aiSummaryService.generatePdf(ch, summary, folder.getAbsolutePath());
            }
        };
        task.setOnSucceeded(ev -> {
            showAlert("PDF sauvegardé !",
                    "Fichier : " + task.getValue().getAbsolutePath(),
                    Alert.AlertType.INFORMATION);
            try { java.awt.Desktop.getDesktop().open(task.getValue()); } catch (Exception ignored) {}
        });
        task.setOnFailed(ev -> showAlert("Erreur PDF",
                task.getException().getMessage(), Alert.AlertType.ERROR));
        new Thread(task) {{ setDaemon(true); }}.start();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SHARED UTILS
    // ════════════════════════════════════════════════════════════════════════
    private Button buildPillButton(String text, String hex) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:" + hex +
                ";-fx-text-fill:white;-fx-font-size:11px;-fx-font-weight:bold;" +
                "-fx-padding:5 12;-fx-background-radius:20;-fx-cursor:hand;");
        return btn;
    }

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#333;-fx-padding:8 0 0 0;");
        return l;
    }

    private Label createTag(String text, String bgColor, String textColor) {
        Label tag = new Label(text);
        tag.setStyle("-fx-background-color:" + bgColor + ";-fx-text-fill:" + textColor +
                ";-fx-font-size:10px;-fx-font-weight:bold;-fx-padding:2 8;-fx-background-radius:20;");
        return tag;
    }

    private String getCourseIcon(String title) {
        String t = title.toLowerCase();
        if (t.contains("python") || t.contains("java")) return "🐍";
        if (t.contains("design") || t.contains("ui"))   return "🎨";
        if (t.contains("data"))                          return "📊";
        if (t.contains("english"))                       return "🇬🇧";
        if (t.contains("spanish"))                       return "🇪🇸";
        if (t.contains("guitar"))                        return "🎸";
        if (t.contains("photo"))                         return "📸";
        return "📚";
    }

    private void openUrl(String url) {
        try { java.awt.Desktop.getDesktop().browse(new java.net.URI(url)); }
        catch (Exception ignored) {}
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert a = new Alert(type);
            a.setTitle(title); a.setHeaderText(null); a.setContentText(content);
            a.showAndWait();
        });
    }
}