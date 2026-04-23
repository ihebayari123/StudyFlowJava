package edu.connexion3a36.Controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.prefs.Preferences;

/**
 * Controller for the VIP Payment page.
 * Embedded inside fitness_dashboard2.fxml (vipArea StackPane).
 * Supports Stripe payment (card) and D17 (Tunisian card).
 */
public class VipPaymentController {

    // ── Stripe test key — non utilisée en mode local ─────────────────
    // Pour activer l'API Stripe réelle, décommentez et renseignez votre clé :
    // private static final String STRIPE_SECRET_KEY = "sk_test_VOTRE_CLE_ICI";

    // ── Offres VIP ────────────────────────────────────────────────────
    private static final String[][] PLANS = {
        {"1 Mois",   "9.99",  "30",  "Accès complet 1 mois",   "#1565c0"},
        {"3 Mois",   "24.99", "90",  "Économisez 17% — 3 mois","#6a1b9a"},
        {"1 An",     "79.99", "365", "Meilleure offre — 1 an",  "#1b5e20"},
    };

    // ── Préférences (persistance VIP) ─────────────────────────────────
    private static final Preferences PREFS = Preferences.userNodeForPackage(VipPaymentController.class);
    private static final String PREF_VIP_EXPIRY = "vip_expiry";

    // ── État ──────────────────────────────────────────────────────────
    private FitnessDashboardController dashboardController;
    private int selectedPlanIndex = 1; // 3 mois par défaut
    private String selectedPaymentMethod = "card"; // "card" ou "d17"
    private Timeline renewalTimer;

    // ── UI refs ───────────────────────────────────────────────────────
    private VBox[] planCards;
    private Label lblStatus;
    private Label lblExpiry;
    private TextField tfCardNumber, tfExpiry, tfCvv, tfName;
    private Button btnPay;
    private ProgressIndicator spinner;
    private VBox formCard;
    private VBox successPane;

    public void setDashboardController(FitnessDashboardController ctrl) {
        this.dashboardController = ctrl;
    }

    /** Appelé par FitnessDashboardController pour construire l'UI dans vipArea. */
    public VBox buildUI() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #0d1117;");

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: #0d1117; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox content = new VBox(28);
        content.setPadding(new Insets(32, 40, 40, 40));
        content.setStyle("-fx-background-color: #0d1117;");

        // ── Titre ────────────────────────────────────────────────────
        VBox titleBox = new VBox(6);
        Label title = new Label("👑  Choisissez votre offre VIP");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #ffd700; -fx-font-family: 'Georgia';");
        Label subtitle = new Label("Accédez à l'analyse de sommeil par IA et aux fonctionnalités exclusives");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #8b949e;");
        titleBox.getChildren().addAll(title, subtitle);

        // ── Statut VIP actuel ─────────────────────────────────────────
        HBox statusBox = buildStatusBox();

        // ── Cartes d'offres ───────────────────────────────────────────
        HBox plansRow = buildPlansRow();

        // ── Méthode de paiement ───────────────────────────────────────
        VBox paymentMethodBox = buildPaymentMethodSelector();

        // ── Formulaire de paiement ────────────────────────────────────
        formCard = buildPaymentForm();

        // ── Bouton Payer ──────────────────────────────────────────────
        HBox payRow = buildPayButton();

        // ── Sécurité ──────────────────────────────────────────────────
        HBox securityRow = buildSecurityBadges();

        // ── Panneau succès (caché) ────────────────────────────────────
        successPane = buildSuccessPane();
        successPane.setVisible(false);
        successPane.setManaged(false);

        content.getChildren().addAll(
            titleBox, statusBox, plansRow,
            paymentMethodBox, formCard, payRow,
            securityRow, successPane
        );

        scroll.setContent(content);
        root.getChildren().add(scroll);
        return root;
    }

    // ── Statut VIP ────────────────────────────────────────────────────
    private HBox buildStatusBox() {
        HBox box = new HBox(12);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-background-color: #161b22; -fx-background-radius: 12; -fx-padding: 14 20;");

        String expiry = PREFS.get(PREF_VIP_EXPIRY, null);
        boolean isVip = expiry != null && LocalDateTime.parse(expiry).isAfter(LocalDateTime.now());

        Label icon = new Label(isVip ? "✅" : "🔒");
        icon.setStyle("-fx-font-size: 20px;");

        VBox info = new VBox(3);
        lblStatus = new Label(isVip ? "Statut VIP : ACTIF" : "Statut VIP : Inactif");
        lblStatus.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + (isVip ? "#4caf50" : "#8b949e") + ";");
        lblExpiry = new Label(isVip ? "Expire le : " + LocalDateTime.parse(expiry).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Aucun abonnement actif");
        lblExpiry.setStyle("-fx-font-size: 11px; -fx-text-fill: #8b949e;");
        info.getChildren().addAll(lblStatus, lblExpiry);

        box.getChildren().addAll(icon, info);
        return box;
    }

    // ── Cartes d'offres ───────────────────────────────────────────────
    private HBox buildPlansRow() {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER);
        planCards = new VBox[PLANS.length];

        for (int i = 0; i < PLANS.length; i++) {
            final int idx = i;
            String[] plan = PLANS[i];
            VBox card = new VBox(10);
            card.setAlignment(Pos.CENTER);
            card.setPadding(new Insets(20, 18, 20, 18));
            card.setPrefWidth(200);
            card.setStyle(getCardStyle(i == selectedPlanIndex, plan[4]));
            card.setCursor(javafx.scene.Cursor.HAND);

            // Badge populaire
            if (i == 1) {
                Label badge = new Label("⭐ POPULAIRE");
                badge.setStyle("-fx-background-color: #ffd700; -fx-text-fill: #1a1a2e; " +
                               "-fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 20;");
                card.getChildren().add(badge);
            }

            Label duration = new Label(plan[0]);
            duration.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

            Label price = new Label(plan[1] + " €");
            price.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #ffd700;");

            Label desc = new Label(plan[3]);
            desc.setStyle("-fx-font-size: 11px; -fx-text-fill: #8b949e; -fx-wrap-text: true; -fx-text-alignment: center;");
            desc.setMaxWidth(160);

            Label features = new Label("✦ Analyse sommeil IA\n✦ Chatbot avancé\n✦ Rapports détaillés");
            features.setStyle("-fx-font-size: 11px; -fx-text-fill: #c0c0c0; -fx-line-spacing: 4;");

            card.getChildren().addAll(duration, price, desc, features);
            card.setOnMouseClicked(e -> selectPlan(idx));

            planCards[i] = card;
            HBox.setHgrow(card, Priority.ALWAYS);
            row.getChildren().add(card);
        }
        return row;
    }

    private String getCardStyle(boolean selected, String color) {
        if (selected) {
            return "-fx-background-color: #161b22; -fx-background-radius: 16; " +
                   "-fx-border-color: " + color + "; -fx-border-radius: 16; -fx-border-width: 2.5; " +
                   "-fx-effect: dropshadow(gaussian, " + color + ", 15, 0.3, 0, 0);";
        }
        return "-fx-background-color: #161b22; -fx-background-radius: 16; " +
               "-fx-border-color: #30363d; -fx-border-radius: 16; -fx-border-width: 1.5;";
    }

    private void selectPlan(int idx) {
        selectedPlanIndex = idx;
        for (int i = 0; i < planCards.length; i++) {
            planCards[i].setStyle(getCardStyle(i == idx, PLANS[i][4]));
        }
        updatePayButton();
    }

    // ── Méthode de paiement ───────────────────────────────────────────
    private VBox buildPaymentMethodSelector() {
        VBox box = new VBox(12);
        Label lbl = new Label("Méthode de paiement");
        lbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e6edf3;");

        HBox methods = new HBox(12);
        Button btnCard = buildMethodBtn("💳  Carte Bancaire", "card");
        Button btnD17  = buildMethodBtn("🏦  Carte D17 (Tunisie)", "d17");
        methods.getChildren().addAll(btnCard, btnD17);

        box.getChildren().addAll(lbl, methods);
        return box;
    }

    private Button buildMethodBtn(String text, String method) {
        Button btn = new Button(text);
        boolean selected = method.equals(selectedPaymentMethod);
        btn.setStyle(selected
            ? "-fx-background-color: #1565c0; -fx-text-fill: white; -fx-font-size: 13px; " +
              "-fx-background-radius: 10; -fx-padding: 10 20; -fx-cursor: hand; -fx-font-weight: bold;"
            : "-fx-background-color: #161b22; -fx-text-fill: #8b949e; -fx-font-size: 13px; " +
              "-fx-background-radius: 10; -fx-padding: 10 20; -fx-cursor: hand; " +
              "-fx-border-color: #30363d; -fx-border-radius: 10; -fx-border-width: 1;");
        btn.setOnAction(e -> {
            selectedPaymentMethod = method;
            // Refresh parent
            if (formCard != null) refreshFormFields();
        });
        return btn;
    }

    // ── Formulaire de paiement ────────────────────────────────────────
    private VBox buildPaymentForm() {
        VBox card = new VBox(16);
        card.setStyle("-fx-background-color: #161b22; -fx-background-radius: 16; " +
                      "-fx-border-color: #30363d; -fx-border-radius: 16; -fx-border-width: 1;");
        card.setPadding(new Insets(24));

        Label title = new Label("💳  Informations de paiement");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #e6edf3;");

        // Hint cartes de test
        VBox testHint = new VBox(4);
        testHint.setStyle("-fx-background-color: #0d2818; -fx-background-radius: 8; -fx-padding: 10 14;");
        Label hintTitle = new Label("🧪  Mode test — Cartes virtuelles acceptées :");
        hintTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #4caf50;");
        Label hintCards = new Label(
            "✦ 4242 4242 4242 4242  (Visa)\n" +
            "✦ 5555 5555 5555 4444  (Mastercard)\n" +
            "✦ 3782 822463 10005    (Amex)\n" +
            "✦ CVV : n'importe quel nombre à 3 chiffres  •  Date : toute date future"
        );
        hintCards.setStyle("-fx-font-size: 11px; -fx-text-fill: #8b949e; -fx-line-spacing: 3;");
        testHint.getChildren().addAll(hintTitle, hintCards);

        // Nom sur la carte
        tfName = buildField("Nom sur la carte", "Ex: JEAN DUPONT");

        // Numéro de carte
        HBox cardRow = new HBox(12);
        tfCardNumber = buildField("Numéro de carte", "1234 5678 9012 3456");
        HBox.setHgrow(tfCardNumber, Priority.ALWAYS);
        // Format automatique
        tfCardNumber.textProperty().addListener((obs, old, val) -> {
            String digits = val.replaceAll("[^0-9]", "");
            if (digits.length() > 16) digits = digits.substring(0, 16);
            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < digits.length(); i++) {
                if (i > 0 && i % 4 == 0) formatted.append(" ");
                formatted.append(digits.charAt(i));
            }
            if (!formatted.toString().equals(val)) {
                tfCardNumber.setText(formatted.toString());
                tfCardNumber.positionCaret(formatted.length());
            }
        });
        cardRow.getChildren().add(tfCardNumber);

        // Expiry + CVV
        HBox expiryRow = new HBox(12);
        tfExpiry = buildField("MM/AA", "12/26");
        tfExpiry.setPrefWidth(120);
        tfExpiry.textProperty().addListener((obs, old, val) -> {
            String digits = val.replaceAll("[^0-9]", "");
            if (digits.length() > 4) digits = digits.substring(0, 4);
            String fmt = digits.length() > 2 ? digits.substring(0, 2) + "/" + digits.substring(2) : digits;
            if (!fmt.equals(val)) { tfExpiry.setText(fmt); tfExpiry.positionCaret(fmt.length()); }
        });
        tfCvv = buildField("CVV", "123");
        tfCvv.setPrefWidth(100);
        expiryRow.getChildren().addAll(tfName, tfExpiry, tfCvv);
        HBox.setHgrow(tfName, Priority.ALWAYS);

        card.getChildren().addAll(title, testHint, cardRow, expiryRow);
        return card;
    }

    private TextField buildField(String label, String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color: #0d1117; -fx-text-fill: #e6edf3; " +
                    "-fx-border-color: #30363d; -fx-border-radius: 8; -fx-background-radius: 8; " +
                    "-fx-padding: 10 14; -fx-font-size: 13px; -fx-prompt-text-fill: #484f58;");
        tf.setPrefHeight(42);
        return tf;
    }

    private void refreshFormFields() {
        // Mise à jour du placeholder selon D17
        if ("d17".equals(selectedPaymentMethod)) {
            tfCardNumber.setPromptText("Numéro D17 (20 chiffres)");
            tfCvv.setPromptText("Code D17");
        } else {
            tfCardNumber.setPromptText("1234 5678 9012 3456");
            tfCvv.setPromptText("CVV");
        }
    }

    // ── Bouton Payer ──────────────────────────────────────────────────
    private HBox buildPayButton() {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);

        btnPay = new Button();
        updatePayButton();
        btnPay.setMaxWidth(Double.MAX_VALUE);
        btnPay.setStyle("-fx-background-color: linear-gradient(to right, #ffd700, #ffb300); " +
                        "-fx-text-fill: #1a1a2e; -fx-font-size: 15px; -fx-font-weight: bold; " +
                        "-fx-background-radius: 14; -fx-padding: 14 0; -fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(255,215,0,0.4), 12, 0, 0, 4);");
        btnPay.setOnAction(e -> processPayment());
        HBox.setHgrow(btnPay, Priority.ALWAYS);

        spinner = new ProgressIndicator();
        spinner.setPrefSize(32, 32);
        spinner.setStyle("-fx-progress-color: #ffd700;");
        spinner.setVisible(false);

        row.getChildren().addAll(btnPay, spinner);
        return row;
    }

    private void updatePayButton() {
        if (btnPay != null && selectedPlanIndex >= 0) {
            String[] plan = PLANS[selectedPlanIndex];
            btnPay.setText("🔒  Payer " + plan[1] + " € — " + plan[0]);
        }
    }

    // ── Badges sécurité ───────────────────────────────────────────────
    private HBox buildSecurityBadges() {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER);
        for (String badge : new String[]{"🔒 SSL 256-bit", "💳 Stripe Secure", "🛡️ PCI DSS", "↩️ Remboursement 7j"}) {
            Label l = new Label(badge);
            l.setStyle("-fx-font-size: 11px; -fx-text-fill: #484f58;");
            row.getChildren().add(l);
        }
        return row;
    }

    // ── Panneau succès ────────────────────────────────────────────────
    private VBox buildSuccessPane() {
        VBox pane = new VBox(20);
        pane.setAlignment(Pos.CENTER);
        pane.setStyle("-fx-background-color: #0d2818; -fx-background-radius: 20; " +
                      "-fx-border-color: #4caf50; -fx-border-radius: 20; -fx-border-width: 2;");
        pane.setPadding(new Insets(40));

        Label icon = new Label("✅");
        icon.setStyle("-fx-font-size: 60px;");

        Label title = new Label("Paiement réussi !");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #4caf50; -fx-font-family: 'Georgia';");

        Label msg = new Label("Votre accès VIP est maintenant actif.");
        msg.setStyle("-fx-font-size: 14px; -fx-text-fill: #c0c0c0;");

        Button btnSleep = new Button("😴  Tester votre heure de sommeil");
        btnSleep.setStyle("-fx-background-color: linear-gradient(to right, #7c4dff, #651fff); " +
                          "-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; " +
                          "-fx-background-radius: 14; -fx-padding: 16 32; -fx-cursor: hand; " +
                          "-fx-effect: dropshadow(gaussian, rgba(124,77,255,0.5), 14, 0, 0, 4);");
        btnSleep.setOnAction(e -> {
            if (dashboardController != null) dashboardController.handleOpenSleep();
        });

        pane.getChildren().addAll(icon, title, msg, btnSleep);
        return pane;
    }

    // ── Cartes de test Stripe acceptées ──────────────────────────────
    // Source : https://stripe.com/docs/testing#cards
    private static final java.util.Set<String> STRIPE_TEST_CARDS = new java.util.HashSet<>(java.util.Arrays.asList(
        "4242424242424242",  // Visa — succès
        "4000056655665556",  // Visa (débit) — succès
        "5555555555554444",  // Mastercard — succès
        "5200828282828210",  // Mastercard (débit) — succès
        "5105105105105100",  // Mastercard (prépayée) — succès
        "378282246310005",   // American Express — succès
        "371449635398431",   // American Express — succès
        "6011111111111117",  // Discover — succès
        "6011000990139424",  // Discover — succès
        "3056930009020004",  // Diners Club — succès
        "36227206271667",    // Diners Club (14 chiffres) — succès
        "3566002020360505",  // JCB — succès
        "6200000000000005"   // UnionPay — succès
    ));

    // Cartes de test qui doivent être refusées
    private static final java.util.Set<String> STRIPE_DECLINED_CARDS = new java.util.HashSet<>(java.util.Arrays.asList(
        "4000000000000002",  // Refusée (generic_decline)
        "4000000000009995",  // Refusée (insufficient_funds)
        "4000000000000069",  // Refusée (expired_card)
        "4000000000000127",  // Refusée (incorrect_cvc)
        "4000000000000119"   // Refusée (processing_error)
    ));

    // ── Traitement du paiement ────────────────────────────────────────
    private void processPayment() {
        String cardNum = tfCardNumber.getText().replaceAll("\\s", "");
        String expiry  = tfExpiry.getText().trim();
        String cvv     = tfCvv.getText().trim();
        String name    = tfName.getText().trim();

        // ── Validation des champs ─────────────────────────────────────
        if (name.isEmpty()) {
            showError("Veuillez saisir le nom sur la carte.");
            tfName.requestFocus();
            return;
        }
        if (cardNum.length() < 13 || cardNum.length() > 19) {
            showError("Numéro de carte invalide.\n\nCarte de test : 4242 4242 4242 4242");
            tfCardNumber.requestFocus();
            return;
        }
        if (expiry.length() < 4) {
            showError("Date d'expiration invalide (format MM/AA).");
            tfExpiry.requestFocus();
            return;
        }
        if (cvv.length() < 3) {
            showError("CVV invalide (3 chiffres minimum).");
            tfCvv.requestFocus();
            return;
        }

        // ── Vérifier expiration ───────────────────────────────────────
        try {
            String[] parts = expiry.split("/");
            int month = Integer.parseInt(parts[0]);
            int year  = 2000 + Integer.parseInt(parts[1]);
            YearMonth cardYM = YearMonth.of(year, month);
            if (cardYM.isBefore(YearMonth.now())) {
                showError("Cette carte est expirée.");
                tfExpiry.requestFocus();
                return;
            }
        } catch (Exception ignored) {
            showError("Date d'expiration invalide (format MM/AA).");
            return;
        }

        btnPay.setDisable(true);
        spinner.setVisible(true);

        // Simuler un délai réseau (1.5s) puis valider
        new Thread(() -> {
            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}

            final boolean success  = validateTestCard(cardNum);
            final boolean declined = STRIPE_DECLINED_CARDS.contains(cardNum);

            Platform.runLater(() -> {
                spinner.setVisible(false);
                if (declined) {
                    btnPay.setDisable(false);
                    showError("❌ Carte refusée.\n\nUtilisez une carte de test valide :\n• 4242 4242 4242 4242 (Visa)\n• 5555 5555 5555 4444 (Mastercard)");
                } else if (success) {
                    onPaymentSuccess();
                } else {
                    btnPay.setDisable(false);
                    showError("❌ Carte non reconnue.\n\nCartes de test acceptées :\n• 4242 4242 4242 4242\n• 5555 5555 5555 4444\n• 3782 822463 10005 (Amex)");
                }
            });
        }).start();
    }

    /**
     * Valide une carte de test Stripe.
     * Accepte les cartes de la liste officielle Stripe test cards.
     * Utilise aussi l'algorithme de Luhn pour valider le format.
     */
    private boolean validateTestCard(String cardNum) {
        // 1. Vérifier dans la liste des cartes de test connues
        if (STRIPE_TEST_CARDS.contains(cardNum)) return true;

        // 2. Pour D17 : accepter tout numéro de 16-20 chiffres valide (Luhn)
        if ("d17".equals(selectedPaymentMethod) && cardNum.length() >= 16) {
            return luhnCheck(cardNum);
        }

        // 3. Vérification Luhn générique (pour cartes non listées mais valides)
        return luhnCheck(cardNum);
    }

    /**
     * Algorithme de Luhn — vérifie la validité mathématique d'un numéro de carte.
     */
    private boolean luhnCheck(String number) {
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int n = number.charAt(i) - '0';
            if (n < 0 || n > 9) return false;
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    private void onPaymentSuccess() {
        // Sauvegarder l'expiration VIP
        int days = Integer.parseInt(PLANS[selectedPlanIndex][2]);
        LocalDateTime expiry = LocalDateTime.now().plusDays(days);
        PREFS.put(PREF_VIP_EXPIRY, expiry.toString());

        // Mettre à jour le statut
        lblStatus.setText("Statut VIP : ACTIF");
        lblStatus.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #4caf50;");
        lblExpiry.setText("Expire le : " + expiry.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        // Afficher le panneau succès
        successPane.setVisible(true);
        successPane.setManaged(true);

        // Démarrer le timer de renouvellement
        startRenewalTimer(days);
    }

    private void startRenewalTimer(int days) {
        if (renewalTimer != null) renewalTimer.stop();
        long totalSeconds = (long) days * 24 * 3600;
        renewalTimer = new Timeline(new KeyFrame(Duration.seconds(totalSeconds), e -> {
            Platform.runLater(this::onVipExpired);
        }));
        renewalTimer.play();
    }

    private void onVipExpired() {
        PREFS.remove(PREF_VIP_EXPIRY);
        lblStatus.setText("Statut VIP : Expiré");
        lblStatus.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #f44336;");
        lblExpiry.setText("Votre abonnement a expiré — renouvelez pour continuer.");
        successPane.setVisible(false);
        successPane.setManaged(false);
        btnPay.setDisable(false);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Abonnement VIP expiré");
        alert.setHeaderText("Votre accès VIP a expiré");
        alert.setContentText("Renouvelez votre abonnement pour continuer à profiter des fonctionnalités VIP.");
        alert.showAndWait();
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Erreur de paiement");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    /** Vérifie si l'utilisateur a un accès VIP actif. */
    public static boolean isVipActive() {
        String expiry = PREFS.get(PREF_VIP_EXPIRY, null);
        return expiry != null && LocalDateTime.parse(expiry).isAfter(LocalDateTime.now());
    }
}
