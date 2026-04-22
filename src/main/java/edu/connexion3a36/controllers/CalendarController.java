package edu.connexion3a36.controllers;

import edu.connexion3a36.models.Event;
import edu.connexion3a36.services.EventService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

public class CalendarController implements Initializable {

    @FXML private GridPane calendarGrid;
    @FXML private GridPane headerGrid;
    @FXML private Label    lblCurrentMonth;
    @FXML private Label    lblSelectedDate;
    @FXML private VBox     eventListContainer;
    @FXML private ScrollPane eventScrollPane;

    private YearMonth   currentYearMonth;
    private List<Event> allEvents = new ArrayList<>();
    private Runnable    onBackCallback;

    // Couleurs selon le type d'événement
    private static final Map<String, String> TYPE_COLORS = Map.of(
            "SPORT",       "#4CAF50",
            "EDUCATION",   "#2196F3",
            "TECHNOLOGIE", "#9C27B0",
            "SANTE",       "#F44336",
            "AUTRE",       "#FF9800"
    );

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        currentYearMonth = YearMonth.now();
        buildDayHeaders();
        loadEvents();
        buildCalendar();
    }

    public void setOnBackCallback(Runnable callback) {
        this.onBackCallback = callback;
    }

    // ── Charger les événements depuis la DB ──────────────────────────
    private void loadEvents() {
        try {
            EventService service = new EventService();
            allEvents = service.recupererTous();          // ← corrigé
        } catch (Exception e) {
            allEvents = new ArrayList<>();
            System.err.println("Erreur chargement événements : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── En-têtes Lun / Mar / ... ────────────────────────────────────
    private void buildDayHeaders() {
        String[] days = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        for (int i = 0; i < 7; i++) {
            Label lbl = new Label(days[i]);
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setAlignment(Pos.CENTER);
            lbl.setStyle(
                    "-fx-text-fill: #888888; -fx-font-size: 12px; " +
                            "-fx-font-weight: bold; -fx-padding: 6 0;"
            );
            headerGrid.add(lbl, i, 0);
        }
    }

    // ── Construire la grille du mois ────────────────────────────────
    private void buildCalendar() {
        calendarGrid.getChildren().clear();
        calendarGrid.getRowConstraints().clear();

        String monthName = currentYearMonth.getMonth()
                .getDisplayName(TextStyle.FULL, Locale.FRENCH);
        monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1);
        lblCurrentMonth.setText(monthName + " " + currentYearMonth.getYear());

        LocalDate firstDay    = currentYearMonth.atDay(1);
        int       startCol    = firstDay.getDayOfWeek().getValue() - 1;
        int       daysInMonth = currentYearMonth.lengthOfMonth();

        Map<LocalDate, List<Event>> eventsMap = buildEventsMap();

        int row = 0, col = startCol;

        for (int i = 0; i < startCol; i++) {
            calendarGrid.add(emptyCell(), i, 0);
        }

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate   date      = currentYearMonth.atDay(day);
            List<Event> dayEvents = eventsMap.getOrDefault(date, List.of());

            StackPane cell = createDayCell(day, date, dayEvents);
            calendarGrid.add(cell, col, row);

            col++;
            if (col == 7) { col = 0; row++; }
        }

        for (int r = 0; r <= row; r++) {
            RowConstraints rc = new RowConstraints();
            rc.setMinHeight(80);
            rc.setVgrow(Priority.ALWAYS);
            calendarGrid.getRowConstraints().add(rc);
        }
    }

    // ── Créer une cellule jour ──────────────────────────────────────
    private StackPane createDayCell(int day, LocalDate date, List<Event> events) {
        VBox content = new VBox(4);
        content.setPadding(new Insets(8));
        content.setAlignment(Pos.TOP_LEFT);

        boolean isToday   = date.equals(LocalDate.now());
        boolean hasEvents = !events.isEmpty();

        Label dayLabel = new Label(String.valueOf(day));
        if (isToday) {
            dayLabel.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; " +
                            "-fx-font-weight: bold; -fx-font-size: 13px; " +
                            "-fx-background-radius: 50%; -fx-min-width: 26; " +
                            "-fx-min-height: 26; -fx-alignment: CENTER;"
            );
        } else {
            dayLabel.setStyle(
                    "-fx-text-fill: " + (hasEvents ? "white" : "#666666") + "; " +
                            "-fx-font-size: 13px; -fx-font-weight: bold;"
            );
        }
        content.getChildren().add(dayLabel);

        if (hasEvents) {
            HBox dots = new HBox(3);
            dots.setAlignment(Pos.CENTER_LEFT);
            int shown = Math.min(events.size(), 3);
            for (int i = 0; i < shown; i++) {
                Event ev = events.get(i);                                   // ← Event
                String color = TYPE_COLORS.getOrDefault(
                        ev.getType() != null ? ev.getType().toUpperCase() : "",
                        "#FF9800"
                );
                Circle dot = new Circle(4);
                dot.setFill(Color.web(color));
                dots.getChildren().add(dot);
            }
            if (events.size() > 3) {
                Label more = new Label("+" + (events.size() - 3));
                more.setStyle("-fx-text-fill: #888888; -fx-font-size: 9px;");
                dots.getChildren().add(more);
            }
            content.getChildren().add(dots);
        }

        StackPane cell = new StackPane(content);
        StackPane.setAlignment(content, Pos.TOP_LEFT);

        String bgColor     = hasEvents ? "#1e1e1e" : "#161616";
        String borderColor = isToday   ? "white"   : (hasEvents ? "#333333" : "#1e1e1e");

        applyCellStyle(cell, bgColor, borderColor);

        final String origBg     = bgColor;
        final String origBorder = borderColor;

        cell.setOnMouseEntered(e -> applyCellStyle(cell, "#252525", origBorder));
        cell.setOnMouseExited (e -> applyCellStyle(cell, origBg,    origBorder));
        cell.setOnMouseClicked(e -> showDayEvents(date, events));

        return cell;
    }

    private void applyCellStyle(StackPane cell, String bgColor, String borderColor) {
        cell.setStyle(
                "-fx-background-color: " + bgColor + "; " +
                        "-fx-background-radius: 10; " +
                        "-fx-border-color: " + borderColor + "; " +
                        "-fx-border-radius: 10; " +
                        "-fx-border-width: 1; " +
                        "-fx-cursor: hand;"
        );
    }

    private StackPane emptyCell() {
        StackPane cell = new StackPane();
        cell.setStyle("-fx-background-color: transparent;");
        return cell;
    }

    // ── Afficher les événements d'un jour ───────────────────────────
    private void showDayEvents(LocalDate date, List<Event> events) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
        String dateStr = date.format(fmt);
        dateStr = dateStr.substring(0, 1).toUpperCase() + dateStr.substring(1);
        lblSelectedDate.setText(dateStr);

        eventListContainer.getChildren().clear();

        if (events.isEmpty()) {
            Label noEvent = new Label("Aucun événement ce jour");
            noEvent.setStyle("-fx-text-fill: #555555; -fx-font-size: 13px; -fx-padding: 20 0;");
            eventListContainer.getChildren().add(noEvent);
            return;
        }

        // Trier par date de création
        events.sort(Comparator.comparing(
                Event::getDateCreation,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));

        for (Event ev : events) {
            eventListContainer.getChildren().add(createEventCard(ev));
        }
    }

    // ── Carte d'un événement ────────────────────────────────────────
    private VBox createEventCard(Event ev) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14));
        card.setStyle(
                "-fx-background-color: #222222; " +
                        "-fx-background-radius: 12; " +
                        "-fx-border-color: #333333; " +
                        "-fx-border-radius: 12; " +
                        "-fx-border-width: 1;"
        );

        String color = TYPE_COLORS.getOrDefault(
                ev.getType() != null ? ev.getType().toUpperCase() : "", "#FF9800");

        HBox typeBar = new HBox(8);
        typeBar.setAlignment(Pos.CENTER_LEFT);
        Circle dot = new Circle(5);
        dot.setFill(Color.web(color));
        Label typeLabel = new Label(ev.getType() != null ? ev.getType() : "Autre");
        typeLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11px; -fx-font-weight: bold;");
        typeBar.getChildren().addAll(dot, typeLabel);

        // ← getTitre() au lieu de getNom()
        Label title = new Label(ev.getTitre());
        title.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        title.setWrapText(true);

        // ← getDateCreation() au lieu de getDate()
        String dateInfo = formatDate(ev.getDateCreation());
        if (dateInfo != null && !dateInfo.isEmpty()) {
            Label dateLabel = new Label(dateInfo);
            dateLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
            card.getChildren().add(dateLabel);
        }

        if (ev.getDescription() != null && !ev.getDescription().isEmpty()) {
            Label desc = new Label(ev.getDescription().length() > 80
                    ? ev.getDescription().substring(0, 80) + "…"
                    : ev.getDescription());
            desc.setStyle("-fx-text-fill: #888888; -fx-font-size: 12px;");
            desc.setWrapText(true);
            card.getChildren().addAll(typeBar, title, desc);
        } else {
            card.getChildren().addAll(typeBar, title);
        }

        return card;
    }

    // ── Formater la date ────────────────────────────────────────────
    private String formatDate(Object date) {
        if (date == null) return null;
        if (date instanceof LocalDate) {
            return ((LocalDate) date).format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH));
        } else if (date instanceof LocalDateTime) {
            return ((LocalDateTime) date).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH));
        } else if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } else if (date instanceof java.util.Date) {
            LocalDate d = ((java.util.Date) date).toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            return d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
        return null;
    }

    // ── Map date → liste d'événements ──────────────────────────────
    private Map<LocalDate, List<Event>> buildEventsMap() {
        Map<LocalDate, List<Event>> map = new HashMap<>();
        for (Event ev : allEvents) {
            // ← getDateCreation() au lieu de getDate()
            LocalDate d = convertToLocalDate(ev.getDateCreation());
            if (d != null) {
                map.computeIfAbsent(d, k -> new ArrayList<>()).add(ev);
            }
        }
        return map;
    }

    private LocalDate convertToLocalDate(Object date) {
        if (date == null) return null;
        if (date instanceof LocalDate)     return (LocalDate) date;
        if (date instanceof LocalDateTime) return ((LocalDateTime) date).toLocalDate();
        if (date instanceof java.sql.Date) return ((java.sql.Date) date).toLocalDate();
        if (date instanceof java.util.Date)
            return ((java.util.Date) date).toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
        return null;
    }

    // ── Navigation ──────────────────────────────────────────────────
    @FXML
    private void handlePrevMonth() {
        currentYearMonth = currentYearMonth.minusMonths(1);
        buildCalendar();
    }

    @FXML
    private void handleNextMonth() {
        currentYearMonth = currentYearMonth.plusMonths(1);
        buildCalendar();
    }

    @FXML
    private void handleBack() {
        if (onBackCallback != null) onBackCallback.run();
    }
}
