package controllers.etudiant;

import Services.EvaluationService;
import entities.Evaluation;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Catalogue des évaluations – côté étudiant.
 * Page "UNITÉS D'ÉVALUATION" avec 3 catégories (QUIZZ, TESTS, CODE)
 * puis affichage des évaluations filtrées par type.
 */
public class CatalogueEvaluationController {

    @FXML private BorderPane rootPane;

    private final EvaluationService evalService = new EvaluationService();
    private List<Evaluation> allEvaluations;
    private FlowPane cardsContainer;
    private TextField searchField;
    private VBox mainContent;

    // Filtres
    private String activeFilter = "ALL";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        // Navbar
        HBox navbar = HomeController.buildNavbar("evaluations");
        rootPane.setTop(navbar);

        // Charger les évaluations
        allEvaluations = evalService.afficher();

        // Afficher la page de sélection de catégorie
        rootPane.setCenter(buildCategorySelection());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PAGE SÉLECTION CATÉGORIE (QUIZZ / TESTS / CODE)
    // ═══════════════════════════════════════════════════════════════════════
    private StackPane buildCategorySelection() {
        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(50, 40, 40, 40));

        // "SYSTEM SELECTION" header with lines
        HBox headerLine = new HBox(16);
        headerLine.setAlignment(Pos.CENTER);

        Region l1 = new Region();
        l1.setPrefWidth(50); l1.setPrefHeight(1);
        l1.setStyle("-fx-background-color: rgba(255,255,255,0.3);");
        Region l2 = new Region();
        l2.setPrefWidth(50); l2.setPrefHeight(1);
        l2.setStyle("-fx-background-color: rgba(255,255,255,0.3);");

        Label sysLabel = new Label("S Y S T E M   S E L E C T I O N");
        sysLabel.setStyle("-fx-text-fill: #d5b9ea; -fx-font-size: 12px; -fx-font-weight: 600;");
        headerLine.getChildren().addAll(l1, sysLabel, l2);

        // Big title
        Label bigTitle = new Label("UNITÉS D'ÉVALUATION");
        bigTitle.setStyle(
            "-fx-text-fill: #d8a8ff; -fx-font-size: 48px; -fx-font-weight: 900;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(240,138,252,0.25), 18, 0.12, 0, 3);");

        Label subtitle = new Label("Initialisation du protocole de synchronisation neurale...");
        subtitle.setStyle("-fx-text-fill: #c8aed9; -fx-font-size: 13px;");

        // ── 3 Category cards ──
        HBox cardsRow = new HBox(30);
        cardsRow.setAlignment(Pos.CENTER);
        cardsRow.setPadding(new Insets(40, 0, 0, 0));

        cardsRow.getChildren().addAll(
            buildCategoryCard("📝", "QUIZZ", "Théorie & Concepts", "Quiz",
                "linear-gradient(to bottom, rgba(240,138,252,0.25), rgba(180,100,240,0.15))",
                "#f08afc"),
            buildCategoryCard("⏱", "TESTS", "Contre la montre", "Test",
                "linear-gradient(to bottom, rgba(130,102,255,0.25), rgba(100,80,220,0.15))",
                "#a787ff"),
            buildCategoryCard("💻", "CODE", "Défis Algorithmie", "Code",
                "linear-gradient(to bottom, rgba(56,189,248,0.25), rgba(80,120,220,0.15))",
                "#38bdf8")
        );

        content.getChildren().addAll(headerLine, bigTitle, subtitle, cardsRow);

        StackPane wrapper = new StackPane(content);
        wrapper.getStyleClass().add("hero-background");
        return wrapper;
    }

    private VBox buildCategoryCard(String emoji, String titre, String desc, String filterType,
                                    String bgGradient, String accentColor) {
        // Icon
        Label icon = new Label(emoji);
        icon.setStyle("-fx-font-size: 42px;");

        // Title
        Label titleLbl = new Label(titre);
        titleLbl.setStyle("-fx-text-fill: " + accentColor + "; -fx-font-size: 20px; -fx-font-weight: 900;");

        // Description
        Label descLbl = new Label(desc);
        descLbl.setStyle("-fx-text-fill: #c8aed9; -fx-font-size: 12px; -fx-font-style: italic;");

        // Count badge
        long count = allEvaluations.stream()
            .filter(ev -> ev.getType().equalsIgnoreCase(filterType))
            .count();
        Label countLbl = new Label(count + " évaluations");
        countLbl.setStyle("-fx-text-fill: " + accentColor + "; -fx-font-size: 11px; -fx-font-weight: 700;");

        // Button
        Button btnAccess = new Button("ACCÉDER ▸");
        btnAccess.setStyle(
            "-fx-background-color: linear-gradient(to right, " + accentColor + ", " + accentColor + "80);" +
            "-fx-text-fill: white; -fx-font-weight: 800; -fx-font-size: 11px;" +
            "-fx-background-radius: 20; -fx-padding: 7 20; -fx-cursor: hand;");
        btnAccess.setOnAction(e -> showEvaluationsByType(filterType));

        VBox card = new VBox(14, icon, titleLbl, descLbl, countLbl, btnAccess);
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(220, 280);
        card.setPadding(new Insets(28, 20, 20, 20));
        card.setStyle(
            "-fx-background-color: " + bgGradient + ";" +
            "-fx-background-radius: 20; -fx-border-radius: 20;" +
            "-fx-border-color: rgba(255,255,255,0.12); -fx-border-width: 1;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 14, 0.15, 0, 6);");
        card.setCursor(javafx.scene.Cursor.HAND);

        // Hover
        String baseStyle = card.getStyle();
        card.setOnMouseEntered(e -> card.setStyle(baseStyle +
            "-fx-border-color: " + accentColor + "90;" +
            "-fx-effect: dropshadow(three-pass-box, " + accentColor + "50, 24, 0.2, 0, 8);" +
            "-fx-translate-y: -6;"));
        card.setOnMouseExited(e -> card.setStyle(baseStyle));

        card.setOnMouseClicked(e -> showEvaluationsByType(filterType));

        return card;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  LISTE DES ÉVALUATIONS PAR TYPE
    // ═══════════════════════════════════════════════════════════════════════
    private void showEvaluationsByType(String type) {
        activeFilter = type;

        mainContent = new VBox(20);
        mainContent.setPadding(new Insets(30, 36, 36, 36));
        mainContent.getStyleClass().add("main-content");

        // ── Back button + Header ──
        Button btnBack = new Button("◀  Retour aux catégories");
        btnBack.getStyleClass().add("neon-button");
        btnBack.setOnAction(e -> rootPane.setCenter(buildCategorySelection()));

        Label title = new Label(typeEmoji(type) + " Évaluations — " + type.toUpperCase());
        title.getStyleClass().add("page-title");
        title.setStyle("-fx-font-size: 28px;");

        Label subtitle = new Label("Sélectionne une évaluation pour commencer.");
        subtitle.getStyleClass().add("page-subtitle");

        // ── Search ──
        searchField = new TextField();
        searchField.setPromptText("🔍 Rechercher par titre...");
        searchField.getStyleClass().add("search-bar");
        searchField.setPrefWidth(300);
        searchField.textProperty().addListener((obs, o, n) -> refreshCards());

        HBox headerRow = new HBox(16, btnBack, new Region() {{ HBox.setHgrow(this, Priority.ALWAYS); }}, searchField);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        // ── Cards container ──
        cardsContainer = new FlowPane();
        cardsContainer.setHgap(20);
        cardsContainer.setVgap(20);
        cardsContainer.setPadding(new Insets(10, 0, 0, 0));

        ScrollPane scroll = new ScrollPane(cardsContainer);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("content-scroll");
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        mainContent.getChildren().addAll(headerRow, new VBox(4, title, subtitle), scroll);
        refreshCards();

        rootPane.setCenter(mainContent);
    }

    private String typeEmoji(String type) {
        return switch (type.toLowerCase()) {
            case "quiz" -> "📝";
            case "test" -> "⏱";
            case "code" -> "💻";
            default -> "📊";
        };
    }

    // ─── CARDS ──────────────────────────────────────────────────────────────
    private void refreshCards() {
        cardsContainer.getChildren().clear();

        String search = searchField != null ? searchField.getText().toLowerCase().trim() : "";

        List<Evaluation> filtered = allEvaluations.stream()
            .filter(ev -> "ALL".equals(activeFilter) || ev.getType().equalsIgnoreCase(activeFilter))
            .filter(ev -> search.isEmpty() || ev.getTitre().toLowerCase().contains(search))
            .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            Label empty = new Label("Aucune évaluation trouvée pour cette catégorie.");
            empty.getStyleClass().add("page-subtitle");
            empty.setPadding(new Insets(40));
            cardsContainer.getChildren().add(empty);
            return;
        }

        for (Evaluation ev : filtered) {
            cardsContainer.getChildren().add(buildEvalCard(ev));
        }
    }

    private VBox buildEvalCard(Evaluation ev) {
        // Type badge
        Label typeBadge = new Label(ev.getType().toUpperCase());
        typeBadge.setStyle(
            "-fx-background-color: " + typeColor(ev.getType()) + ";" +
            "-fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: 800;" +
            "-fx-padding: 3 10; -fx-background-radius: 10;");

        // Titre
        Label titre = new Label(ev.getTitre());
        titre.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 16px; -fx-font-weight: 800;");
        titre.setWrapText(true);

        // Description
        Label desc = new Label(ev.getDescription() != null && !ev.getDescription().isBlank()
                ? (ev.getDescription().length() > 80 ? ev.getDescription().substring(0, 77) + "..." : ev.getDescription())
                : "Aucune description.");
        desc.setStyle("-fx-text-fill: #c8aed9; -fx-font-size: 12px;");
        desc.setWrapText(true);
        desc.setMaxHeight(40);

        // Infos
        Label bareme = new Label("📏 Barème : " + String.format("%.0f", ev.getBareme()) + " pts");
        bareme.setStyle("-fx-text-fill: #d5b9ea; -fx-font-size: 11px;");

        String dateTxt = ev.getDateLimite() != null ? ev.getDateLimite().format(FMT) : "—";
        boolean expired = ev.getDateLimite() != null && ev.getDateLimite().isBefore(LocalDateTime.now());
        Label date = new Label("📅 " + dateTxt);
        date.setStyle("-fx-text-fill:" + (expired ? "#ff6b8a" : "#6affdd") + "; -fx-font-size: 11px;");

        // Bouton COMMENCER
        Button btnCommencer = new Button(expired ? "⏰ Expiré" : "▶  COMMENCER");
        if (expired) {
            btnCommencer.getStyleClass().add("neon-button");
            btnCommencer.setDisable(true);
            btnCommencer.setOpacity(0.5);
        } else {
            btnCommencer.getStyleClass().add("btn-primary");
            btnCommencer.setOnAction(e -> openEvaluation(ev));
        }
        btnCommencer.setMaxWidth(Double.MAX_VALUE);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox card = new VBox(10, typeBadge, titre, desc, bareme, date, spacer, btnCommencer);
        card.setPrefWidth(260);
        card.setPrefHeight(260);
        card.setPadding(new Insets(18));
        card.getStyleClass().add("module-card");

        // Hover
        card.setOnMouseEntered(e -> card.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, rgba(240,138,252,0.18), rgba(142,104,255,0.16));" +
            "-fx-background-radius: 20; -fx-border-radius: 20;" +
            "-fx-border-color: rgba(240,138,252,0.55); -fx-border-width: 1.5;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(240,138,252,0.40), 22, 0.2, 0, 5);"));
        card.setOnMouseExited(e -> card.setStyle(""));

        return card;
    }

    private String typeColor(String type) {
        return switch (type.toLowerCase()) {
            case "quiz" -> "linear-gradient(to right, #f08afc, #d946ef)";
            case "test" -> "linear-gradient(to right, #a787ff, #6366f1)";
            case "code" -> "linear-gradient(to right, #38bdf8, #34d399)";
            default -> "#8b5cf6";
        };
    }

    // ─── OUVRIR ÉVALUATION ──────────────────────────────────────────────────
    private void openEvaluation(Evaluation ev) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/views/etudiant/passerEvaluationView.fxml"));
            javafx.scene.Parent root = loader.load();
            PasserEvaluationController ctrl = loader.getController();
            ctrl.setEvaluation(ev);

            javafx.stage.Stage stage = (javafx.stage.Stage) rootPane.getScene().getWindow();
            javafx.scene.Scene scene = new javafx.scene.Scene(root,
                    stage.getScene().getWidth(), stage.getScene().getHeight());
            scene.setFill(javafx.scene.paint.Color.web("#0a0a0a"));
            stage.setScene(scene);
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur : " + ex.getMessage()).showAndWait();
        }
    }
}
