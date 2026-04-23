package edu.connexion3a36.Controller;

import edu.connexion3a36.models.Event;
import edu.connexion3a36.services.EventService;
import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import java.net.URL;
import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class EventCalendarController implements Initializable {

    @FXML private HBox    headerRow;
    @FXML private VBox    calendarGrid;
    @FXML private Label   lblMonthYear;
    @FXML private Label   lblEventCount;
    @FXML private ScrollPane upcomingScroll;
    @FXML private VBox    legendBox;

    private final EventService eventService = new EventService();
    private List<Event> allEvents = new ArrayList<>();

    private YearMonth currentYearMonth = YearMonth.now();

    private static final String[] WEEKDAYS = {"Dim","Lun","Mar","Mer","Jeu","Ven","Sam"};
    private static final String[] MONTHS_FR = {
            "Janvier","Février","Mars","Avril","Mai","Juin",
            "Juillet","Août","Septembre","Octobre","Novembre","Décembre"
    };

    private static final Map<String, String> TYPE_COLORS = new LinkedHashMap<>() {{
        put("TECHNOLOGIE", "#4f8ef7");
        put("FORMATION",   "#4f8ef7");
        put("ÉDUCATION",   "#7c6fff");
        put("EDUCATION",   "#7c6fff");
        put("CONFÉRENCE",  "#a855f7");
        put("CONFERENCE",  "#a855f7");
        put("WORKSHOP",    "#f59e0b");
        put("SPORT",       "#22c55e");
        put("CULTURE",     "#ec4899");
        put("AUTRE",       "#f43f5e");
    }};

    // Callback to go back (set by FitnessDashboardController)
    private Runnable onGoBack;
    public void setOnGoBack(Runnable r) { this.onGoBack = r; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadEvents();
        buildLegend();
    }

    private void loadEvents() {
        try {
            allEvents = eventService.recupererTous();
        } catch (SQLException e) {
            allEvents = new ArrayList<>();
            System.err.println("Erreur chargement events : " + e.getMessage());
        }
        renderCalendar();
    }

    private void renderCalendar() {
        // Header row (weekday labels)
        headerRow.getChildren().clear();
        for (String day : WEEKDAYS) {
            Label lbl = new Label(day);
            lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.35);"
                    + "-fx-font-weight: bold; -fx-alignment: CENTER;");
            lbl.setPrefWidth(Double.MAX_VALUE);
            HBox.setHgrow(lbl, Priority.ALWAYS);
            headerRow.getChildren().add(lbl);
        }

        calendarGrid.getChildren().clear();
        lblMonthYear.setText(MONTHS_FR[currentYearMonth.getMonthValue() - 1] + " " + currentYearMonth.getYear());

        int firstDayOfWeek = currentYearMonth.atDay(1).getDayOfWeek().getValue() % 7;
        int daysInMonth    = currentYearMonth.lengthOfMonth();
        YearMonth prevMonth = currentYearMonth.minusMonths(1);
        int prevDays = prevMonth.lengthOfMonth();

        // Count events this month
        long count = allEvents.stream().filter(e -> eventInMonth(e, currentYearMonth)).count();
        lblEventCount.setText(count + " événement" + (count > 1 ? "s" : ""));

        // Build 6 rows x 7 cols
        int day = 1;
        int nextDay = 1;
        for (int row = 0; row < 6; row++) {
            HBox hrow = new HBox(3);
            hrow.setAlignment(Pos.TOP_LEFT);
            VBox.setVgrow(hrow, Priority.ALWAYS);

            for (int col = 0; col < 7; col++) {
                int cellIndex = row * 7 + col;
                int dayNum;
                boolean isCurrentMonth;

                if (cellIndex < firstDayOfWeek) {
                    dayNum = prevDays - firstDayOfWeek + 1 + cellIndex;
                    isCurrentMonth = false;
                } else if (day <= daysInMonth) {
                    dayNum = day++;
                    isCurrentMonth = true;
                } else {
                    dayNum = nextDay++;
                    isCurrentMonth = false;
                }

                LocalDate cellDate = isCurrentMonth
                        ? currentYearMonth.atDay(dayNum)
                        : (cellIndex < firstDayOfWeek
                        ? prevMonth.atDay(dayNum)
                        : currentYearMonth.plusMonths(1).atDay(dayNum));

                VBox cell = buildDayCell(dayNum, cellDate, isCurrentMonth);
                HBox.setHgrow(cell, Priority.ALWAYS);
                hrow.getChildren().add(cell);
            }
            calendarGrid.getChildren().add(hrow);
        }

        renderUpcoming();
    }

    private VBox buildDayCell(int dayNum, LocalDate date, boolean isCurrentMonth) {
        VBox cell = new VBox(2);
        cell.setPrefWidth(Double.MAX_VALUE);
        cell.setPrefHeight(90);
        cell.setPadding(new Insets(4, 5, 4, 5));

        boolean isToday = date.equals(LocalDate.now());
        List<Event> dayEvents = allEvents.stream()
                .filter(e -> eventOnDate(e, date))
                .collect(Collectors.toList());

        String border = dayEvents.isEmpty()
                ? "rgba(255,255,255,0.07)"
                : getTypeColor(dayEvents.get(0).getType()) + "66";
        String bg = isToday
                ? "rgba(124,111,255,0.15)"
                : (isCurrentMonth ? "#0d0d18" : "#09090f");

        cell.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 8;"
                + "-fx-border-color: " + border + "; -fx-border-radius: 8; -fx-border-width: 1; -fx-cursor: hand;");

        // Day number
        Label numLbl = new Label(String.valueOf(dayNum));
        if (isToday) {
            numLbl.setStyle("-fx-background-color: #7c6fff; -fx-text-fill: white;"
                    + "-fx-background-radius: 50%; -fx-min-width: 22; -fx-min-height: 22;"
                    + "-fx-alignment: CENTER; -fx-font-size: 11px; -fx-font-weight: bold;");
        } else {
            numLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: " + (isCurrentMonth ? "bold" : "normal")
                    + "; -fx-text-fill: rgba(255,255,255," + (isCurrentMonth ? "0.9" : "0.25") + ");");
        }
        cell.getChildren().add(numLbl);

        // Event pills
        int shown = 0;
        for (Event e : dayEvents) {
            if (shown >= 2) break;
            Label pill = new Label(e.getTitre());
            String color = getTypeColor(e.getType());
            pill.setStyle("-fx-background-color: " + color + "33; -fx-text-fill: " + color + ";"
                    + "-fx-font-size: 9px; -fx-font-weight: bold; -fx-background-radius: 4;"
                    + "-fx-padding: 2 5; -fx-max-width: Infinity;");
            pill.setMaxWidth(Double.MAX_VALUE);
            pill.setEllipsisString("…");
            Tooltip tp = new Tooltip(e.getTitre() + "\n" + (e.getDescription() != null ? e.getDescription() : ""));
            tp.setStyle("-fx-font-size: 12px;");
            Tooltip.install(pill, tp);
            cell.getChildren().add(pill);
            shown++;
        }
        if (dayEvents.size() > 2) {
            Label more = new Label("+" + (dayEvents.size() - 2) + " autres");
            more.setStyle("-fx-font-size: 9px; -fx-text-fill: rgba(255,255,255,0.3); -fx-padding: 0 2;");
            cell.getChildren().add(more);
        }

        // Hover effect
        cell.setOnMouseEntered(e -> cell.setStyle(
                "-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 8;"
                        + "-fx-border-color: " + border + "; -fx-border-radius: 8; -fx-border-width: 1; -fx-cursor: hand;"));
        cell.setOnMouseExited(e -> cell.setStyle(
                "-fx-background-color: " + bg + "; -fx-background-radius: 8;"
                        + "-fx-border-color: " + border + "; -fx-border-radius: 8; -fx-border-width: 1; -fx-cursor: hand;"));

        // Click → show day detail
        cell.setOnMouseClicked(e -> showDayDetail(date, dayEvents));
        return cell;
    }

    private void renderUpcoming() {
        VBox list = new VBox(8);
        list.setStyle("-fx-background-color: transparent;");
        LocalDate today = LocalDate.now();

        allEvents.stream()
                .filter(e -> {
                    if (e.getDateCreation() == null) return false;
                    LocalDate d = e.getDateCreation().toLocalDate();
                    return !d.isBefore(today);
                })
                .sorted(Comparator.comparing(e -> e.getDateCreation().toLocalDate()))
                .limit(6)
                .forEach(ev -> {
                    HBox item = new HBox(10);
                    item.setAlignment(Pos.CENTER_LEFT);
                    item.setPadding(new Insets(9, 12, 9, 12));
                    item.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 10; -fx-cursor: hand;");

                    String color = getTypeColor(ev.getType());
                    Circle dot = new Circle(5);
                    dot.setStyle("-fx-fill: " + color + ";");

                    VBox info = new VBox(2);
                    HBox.setHgrow(info, Priority.ALWAYS);
                    Label t = new Label(ev.getTitre() != null ? ev.getTitre() : "");
                    t.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white; -fx-wrap-text: true;");
                    t.setMaxWidth(130);
                    Label tp = new Label(ev.getType() != null ? ev.getType() : "");
                    tp.setStyle("-fx-font-size: 10px; -fx-text-fill: " + color + ";");
                    info.getChildren().addAll(t, tp);

                    String dateStr = ev.getDateCreation().toLocalDate()
                            .format(DateTimeFormatter.ofPattern("dd MMM"));
                    Label dateLbl = new Label(dateStr);
                    dateLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: rgba(255,255,255,0.4);");

                    item.getChildren().addAll(dot, info, dateLbl);
                    item.setOnMouseEntered(e -> item.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 10; -fx-cursor: hand;"));
                    item.setOnMouseExited(e -> item.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 10; -fx-cursor: hand;"));
                    list.getChildren().add(item);
                });

        if (list.getChildren().isEmpty()) {
            Label none = new Label("Aucun événement à venir");
            none.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.3);");
            list.getChildren().add(none);
        }
        upcomingScroll.setContent(list);
    }

    private void buildLegend() {
        legendBox.getChildren().clear();
        Map<String, String> shown = new LinkedHashMap<>();
        shown.put("Technologie", "#4f8ef7");
        shown.put("Éducation",   "#7c6fff");
        shown.put("Sport",       "#22c55e");
        shown.put("Culture",     "#ec4899");
        shown.put("Conférence",  "#a855f7");
        shown.forEach((name, color) -> {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            Circle c = new Circle(5);
            c.setStyle("-fx-fill: " + color + ";");
            Label l = new Label(name);
            l.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.5);");
            row.getChildren().addAll(c, l);
            legendBox.getChildren().add(row);
        });
    }

    private void showDayDetail(LocalDate date, List<Event> events) {
        if (events.isEmpty()) return;
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Événements du " + date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH)));
        dialog.setHeaderText(null);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.setStyle("-fx-background-color: #0d0d18;");
        dialog.getDialogPane().setStyle("-fx-background-color: #0d0d18;");

        for (Event ev : events) {
            String color = getTypeColor(ev.getType());
            HBox row = new HBox(12);
            row.setPadding(new Insets(10, 14, 10, 14));
            row.setStyle("-fx-background-color: " + color + "22; -fx-background-radius: 10;"
                    + "-fx-border-color: " + color + "44; -fx-border-radius: 10; -fx-border-width: 1;");
            VBox info = new VBox(4);
            Label title = new Label(ev.getTitre() != null ? ev.getTitre() : "");
            title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
            Label type  = new Label(ev.getType() != null ? ev.getType() : "");
            type.setStyle("-fx-font-size: 11px; -fx-text-fill: " + color + ";");
            Label desc  = new Label(ev.getDescription() != null ? ev.getDescription() : "");
            desc.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.6); -fx-wrap-text: true;");
            desc.setMaxWidth(380);
            info.getChildren().addAll(title, type, desc);
            row.getChildren().add(info);
            content.getChildren().add(row);
        }

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true); sp.setPrefHeight(300);
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        dialog.getDialogPane().setContent(sp);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────
    private boolean eventOnDate(Event e, LocalDate date) {
        if (e.getDateCreation() == null) return false;
        return e.getDateCreation().toLocalDate().equals(date);
    }

    private boolean eventInMonth(Event e, YearMonth ym) {
        if (e.getDateCreation() == null) return false;
        LocalDate d = e.getDateCreation().toLocalDate();
        return d.getYear() == ym.getYear() && d.getMonthValue() == ym.getMonthValue();
    }

    private String getTypeColor(String type) {
        if (type == null) return "#7c6fff";
        return TYPE_COLORS.getOrDefault(type.toUpperCase(), "#7c6fff");
    }

    // ── ACTIONS ───────────────────────────────────────────────────────────────
    @FXML public void prevMonth(ActionEvent e)  { currentYearMonth = currentYearMonth.minusMonths(1); renderCalendar(); }
    @FXML public void nextMonth(ActionEvent e)  { currentYearMonth = currentYearMonth.plusMonths(1); renderCalendar(); }
    @FXML public void goToday(ActionEvent e)    { currentYearMonth = YearMonth.now(); renderCalendar(); }
    @FXML public void goBack(ActionEvent e)     { if (onGoBack != null) onGoBack.run(); }
}