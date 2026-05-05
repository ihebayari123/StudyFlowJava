package edu.connexion3a36.Controller;

import edu.connexion3a36.services.AnthropicAIService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;

/**
 * Feature 2 – Course Outline Generator
 *
 * Opens as a modal dialog. Teacher enters a topic and number of chapters,
 * clicks Generate, ticks the chapters they want to keep, then clicks "Utiliser
 * la sélection" which returns the selected chapter titles to the caller via a
 * callback.
 *
 * Usage (from CoursController or any other controller):
 *
 *   CourseOutlineDialog.show(ownerWindow, selectedChapters -> {
 *       // selectedChapters is a List<ChapterSuggestion>
 *       // open AddChapitreController for each one, pre-filling titre & contenu
 *   });
 */
public class CourseOutlineDialog {

    // ── Static factory ────────────────────────────────────────────────────────

    public interface OutlineCallback {
        void onSelected(List<AnthropicAIService.ChapterSuggestion> selected);
    }

    public static void show(Window owner, OutlineCallback callback) {
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(owner);
        stage.setTitle("✨ Générateur de plan de cours");
        stage.setResizable(false);

        CourseOutlineDialog dialog = new CourseOutlineDialog(stage, callback);
        stage.setScene(new Scene(dialog.buildUI(), 620, 640));
        stage.show();
    }

    // ── Instance ──────────────────────────────────────────────────────────────

    private final Stage stage;
    private final OutlineCallback callback;

    private TextField topicField;
    private Spinner<Integer> countSpinner;
    private Button generateBtn;
    private Label statusLabel;
    private VBox chapterListBox;
    private Button useSelectionBtn;

    private final ObservableList<SelectableChapter> items = FXCollections.observableArrayList();

    private CourseOutlineDialog(Stage stage, OutlineCallback callback) {
        this.stage    = stage;
        this.callback = callback;
    }

    // ── UI builder ────────────────────────────────────────────────────────────

    private VBox buildUI() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #F5F6FA;");

        // Header
        HBox header = new HBox();
        header.setStyle("-fx-background-color: #1A1A2E; -fx-padding: 16 24;");
        Label title = new Label("✨ Générateur de plan de cours");
        title.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: white;");
        header.getChildren().add(title);

        // Input area
        VBox inputArea = new VBox(12);
        inputArea.setStyle("-fx-background-color: #EEF2FF; -fx-padding: 16 24;");

        Label topicLbl = new Label("Sujet ou nom du cours");
        topicLbl.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #3730A3;");
        topicField = new TextField();
        topicField.setPromptText("ex : Programmation Java pour débutants");
        topicField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-padding: 8 12;");

        HBox countRow = new HBox(12);
        countRow.setAlignment(Pos.CENTER_LEFT);
        Label countLbl = new Label("Nombre de chapitres :");
        countLbl.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #3730A3;");
        countSpinner = new Spinner<>(3, 15, 6);
        countSpinner.setPrefWidth(80);
        countRow.getChildren().addAll(countLbl, countSpinner);

        generateBtn = new Button("✨ Générer le plan");
        generateBtn.setStyle("-fx-background-color: #4F46E5; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 9 18; -fx-cursor: hand; " +
                "-fx-font-size: 13; -fx-font-weight: bold;");
        generateBtn.setOnAction(e -> handleGenerate());

        statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #6366F1;");

        inputArea.getChildren().addAll(topicLbl, topicField, countRow, generateBtn, statusLabel);

        // Results area
        VBox resultsArea = new VBox(0);
        resultsArea.setStyle("-fx-padding: 0;");

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: white; -fx-border-color: transparent;");
        scroll.setPrefHeight(300);

        chapterListBox = new VBox(0);
        chapterListBox.setStyle("-fx-background-color: white; -fx-padding: 8 0;");
        scroll.setContent(chapterListBox);
        resultsArea.getChildren().add(scroll);

        // Bottom buttons
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-background-color: white; -fx-padding: 14 24; " +
                "-fx-border-color: #E0E0E0 transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        Button cancelBtn = new Button("Fermer");
        cancelBtn.setStyle("-fx-background-color: #E0E0E0; -fx-text-fill: #333; " +
                "-fx-cursor: hand; -fx-background-radius: 8; -fx-padding: 9 18;");
        cancelBtn.setOnAction(e -> stage.close());

        Button selectAllBtn = new Button("Tout sélectionner");
        selectAllBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #4F46E5; " +
                "-fx-cursor: hand; -fx-font-size: 12;");
        selectAllBtn.setOnAction(e -> { items.forEach(i -> i.selected.set(true)); renderList(); });

        useSelectionBtn = new Button("✅ Utiliser la sélection");
        useSelectionBtn.setDisable(true);
        useSelectionBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 8; -fx-padding: 9 18;");
        useSelectionBtn.setOnAction(e -> handleUseSelection());

        footer.getChildren().addAll(cancelBtn, selectAllBtn, useSelectionBtn);

        root.getChildren().addAll(header, inputArea, resultsArea, footer);
        VBox.setVgrow(resultsArea, Priority.ALWAYS);
        return root;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void handleGenerate() {
        String topic = topicField.getText().trim();
        if (topic.isEmpty()) {
            statusLabel.setText("⚠️ Veuillez saisir un sujet.");
            statusLabel.setStyle("-fx-text-fill: #F44336; -fx-font-size: 11;");
            return;
        }

        int count = countSpinner.getValue();
        generateBtn.setDisable(true);
        generateBtn.setText("⏳ Génération...");
        statusLabel.setText("🤖 L'IA génère le plan du cours...");
        statusLabel.setStyle("-fx-text-fill: #6366F1; -fx-font-size: 11;");
        chapterListBox.getChildren().clear();
        items.clear();
        useSelectionBtn.setDisable(true);

        Task<List<AnthropicAIService.ChapterSuggestion>> task = new Task<>() {
            @Override
            protected List<AnthropicAIService.ChapterSuggestion> call() {
                return AnthropicAIService.getInstance().generateCourseOutline(topic, count);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            List<AnthropicAIService.ChapterSuggestion> suggestions = task.getValue();
            if (suggestions.isEmpty()) {
                statusLabel.setText("❌ Aucun chapitre généré. Réessayez.");
                statusLabel.setStyle("-fx-text-fill: #F44336; -fx-font-size: 11;");
            } else {
                suggestions.forEach(s -> items.add(new SelectableChapter(s, true)));
                renderList();
                statusLabel.setText("✅ " + suggestions.size() + " chapitres générés. Cochez ceux que vous voulez garder.");
                statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 11;");
                useSelectionBtn.setDisable(false);
            }
            generateBtn.setDisable(false);
            generateBtn.setText("✨ Générer le plan");
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            String msg = task.getException() != null ? task.getException().getMessage() : "Erreur inconnue";
            statusLabel.setText("❌ Erreur : " + msg);
            statusLabel.setStyle("-fx-text-fill: #F44336; -fx-font-size: 11;");
            generateBtn.setDisable(false);
            generateBtn.setText("✨ Générer le plan");
        }));

        new Thread(task, "ai-outline-thread").start();
    }

    private void renderList() {
        chapterListBox.getChildren().clear();
        for (int i = 0; i < items.size(); i++) {
            chapterListBox.getChildren().add(buildChapterRow(items.get(i), i + 1));
        }
    }

    private HBox buildChapterRow(SelectableChapter item, int index) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.TOP_LEFT);
        row.setStyle("-fx-padding: 10 24; -fx-border-color: transparent transparent #F0F0F0 transparent; " +
                "-fx-border-width: 0 0 1 0;");
        row.setOnMouseEntered(e -> row.setStyle("-fx-padding: 10 24; -fx-background-color: #F9F9FF; " +
                "-fx-border-color: transparent transparent #F0F0F0 transparent; -fx-border-width: 0 0 1 0;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-padding: 10 24; -fx-background-color: white; " +
                "-fx-border-color: transparent transparent #F0F0F0 transparent; -fx-border-width: 0 0 1 0;"));

        CheckBox cb = new CheckBox();
        cb.setSelected(item.selected.get());
        cb.selectedProperty().addListener((obs, o, n) -> item.selected.set(n));

        Label num = new Label("Ch." + index);
        num.setStyle("-fx-font-size: 11; -fx-text-fill: #9E9E9E; -fx-min-width: 34;");

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label titre = new Label(item.suggestion.getTitre());
        titre.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #1A1A2E;");
        titre.setWrapText(true);
        Label desc = new Label(item.suggestion.getDescription());
        desc.setStyle("-fx-font-size: 12; -fx-text-fill: #666;");
        desc.setWrapText(true);
        info.getChildren().addAll(titre, desc);

        row.getChildren().addAll(cb, num, info);
        return row;
    }

    private void handleUseSelection() {
        List<AnthropicAIService.ChapterSuggestion> selected = new ArrayList<>();
        for (SelectableChapter item : items) {
            if (item.selected.get()) selected.add(item.suggestion);
        }
        if (selected.isEmpty()) {
            statusLabel.setText("⚠️ Veuillez sélectionner au moins un chapitre.");
            statusLabel.setStyle("-fx-text-fill: #F44336; -fx-font-size: 11;");
            return;
        }
        stage.close();
        if (callback != null) callback.onSelected(selected);
    }

    // ── Helper record ─────────────────────────────────────────────────────────

    private static class SelectableChapter {
        final AnthropicAIService.ChapterSuggestion suggestion;
        final javafx.beans.property.BooleanProperty selected;

        SelectableChapter(AnthropicAIService.ChapterSuggestion s, boolean sel) {
            this.suggestion = s;
            this.selected   = new javafx.beans.property.SimpleBooleanProperty(sel);
        }
    }
}
