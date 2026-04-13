package controllers.etudiant;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Stage;

public class HomeController {

    @FXML private BorderPane rootPane;

    @FXML
    public void initialize() {
        // ── Navbar en haut ──
        HBox navbar = buildNavbar("home");
        rootPane.setTop(navbar);

        // ── Contenu principal (hero + stats) ──
        rootPane.setCenter(buildHeroContent());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  NAVBAR HORIZONTALE (réutilisable par toutes les vues)
    // ═══════════════════════════════════════════════════════════════════════
    public static HBox buildNavbar(String activePage) {
        HBox navbar = new HBox(0);
        navbar.getStyleClass().add("top-navbar");
        navbar.setAlignment(Pos.CENTER_LEFT);
        navbar.setPadding(new Insets(0, 20, 0, 20));

        // Logo
        Label logoIcon = new Label("🎓");
        logoIcon.setStyle("-fx-font-size: 22px; -fx-padding: 0 6 0 0;");
        Label logoText = new Label("UNILEARN");
        logoText.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 18px; -fx-font-weight: 800; -fx-font-family: 'Segoe UI';");

        HBox logoBox = new HBox(6, logoIcon, logoText);
        logoBox.setAlignment(Pos.CENTER_LEFT);
        logoBox.setPadding(new Insets(0, 24, 0, 0));

        // Nav links
        String[][] links = {
            {"Home", "home"}, {"Cours", "cours"}, {"Evaluations", "evaluations"},
            {"Combat", "combat"}, {"Forum", "forum"}, {"Reclamation", "reclamation"},
            {"Postuler", "postuler"}, {"Déconnexion", "deconnexion"}, {"Admin", "admin_evals"}
        };

        HBox navLinks = new HBox(2);
        navLinks.setAlignment(Pos.CENTER_LEFT);

        for (String[] link : links) {
            Button btn = new Button(link[0]);
            btn.getStyleClass().add("navbar-link");
            if (link[1].equals(activePage)) {
                btn.getStyleClass().add("active");
            }
            btn.setOnAction(e -> navigateTo(btn, link[1]));
            navLinks.getChildren().add(btn);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        navbar.getChildren().addAll(logoBox, navLinks, spacer);
        return navbar;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  NAVIGATION CENTRALE
    // ═══════════════════════════════════════════════════════════════════════
    static void navigateTo(javafx.scene.Node source, String module) {
        switch (module) {
            case "home"            -> loadView(source, "/views/etudiant/homeView.fxml");
            case "evaluations"     -> loadView(source, "/views/etudiant/catalogueEvaluationsView.fxml");
            case "admin_evals"     -> loadView(source, "/views/evaluationView.fxml");
            case "admin_questions" -> showInfo("Questions", "Sélectionnez une évaluation depuis « Gestion Évaluations »\npuis cliquez sur le bouton « ❓ Questions ».");
            case "admin_rendus"    -> showInfo("Rendus", "Sélectionnez une évaluation depuis « Gestion Évaluations »\npuis cliquez sur le bouton « 📋 Rendus ».");
            default -> showInfo("Module en construction", "🚧 Le module \"" + module + "\" sera disponible prochainement.");
        }
    }

    private static void showInfo(String titre, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(titre);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    static void loadView(javafx.scene.Node source, String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(HomeController.class.getResource(fxml));
            Parent root = loader.load();
            Stage stage = (Stage) source.getScene().getWindow();
            Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());
            scene.setFill(javafx.scene.paint.Color.web("#0a0a0a"));
            stage.setScene(scene);
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur chargement : " + ex.getMessage()).showAndWait();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HERO CONTENT (page d'accueil)
    // ═══════════════════════════════════════════════════════════════════════
    private StackPane buildHeroContent() {
        VBox content = new VBox(0);
        content.setAlignment(Pos.CENTER);

        // ── Hero section ──
        VBox hero = new VBox(16);
        hero.setAlignment(Pos.CENTER);
        hero.setPadding(new Insets(60, 40, 40, 40));

        // "WELCOME TO THE FUTURE" with decorative lines
        HBox welcomeRow = new HBox(16);
        welcomeRow.setAlignment(Pos.CENTER);

        Region line1 = new Region();
        line1.setPrefWidth(60);
        line1.setPrefHeight(1);
        line1.setStyle("-fx-background-color: rgba(255,255,255,0.3);");

        Label welcomeLabel = new Label("W E L C O M E   T O   T H E   F U T U R E");
        welcomeLabel.setStyle("-fx-text-fill: #d5b9ea; -fx-font-size: 12px; -fx-font-weight: 600; -fx-letter-spacing: 4px;");

        Region line2 = new Region();
        line2.setPrefWidth(60);
        line2.setPrefHeight(1);
        line2.setStyle("-fx-background-color: rgba(255,255,255,0.3);");

        welcomeRow.getChildren().addAll(line1, welcomeLabel, line2);

        // Big title "UNILEARN"
        Label bigTitle = new Label("UNILEARN");
        bigTitle.setStyle(
            "-fx-text-fill: linear-gradient(to right, #d8a8ff, #ff9df4);" +
            "-fx-font-size: 64px; -fx-font-weight: 900; -fx-font-family: 'Segoe UI';" +
            "-fx-effect: dropshadow(three-pass-box, rgba(240,138,252,0.30), 20, 0.15, 0, 4);");

        // Description
        Label desc = new Label(
            "UniLearn est une plateforme web universitaire qui regroupe les cours, les quiz\n" +
            "et le suivi des étudiants dans un seul espace, afin de rendre l'apprentissage\n" +
            "plus simple, organisé et motivant.");
        desc.setStyle("-fx-text-fill: #c8b8d9; -fx-font-size: 14px; -fx-text-alignment: center; -fx-line-spacing: 4;");
        desc.setWrapText(true);
        desc.setMaxWidth(560);

        hero.getChildren().addAll(welcomeRow, bigTitle, desc);

        // ── Stats row ──
        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER);
        statsRow.setPadding(new Insets(40, 40, 60, 40));

        statsRow.getChildren().addAll(
            buildStatBox("99.9%", "Disponibilité"),
            buildStatBox("∞", "Ressources"),
            buildStatBox("0.001", "Latence"),
            buildStatBox("24/7", "Support")
        );

        content.getChildren().addAll(hero, statsRow);

        // Wrap in StackPane for centered overlay
        StackPane wrapper = new StackPane(content);
        wrapper.getStyleClass().add("hero-background");
        return wrapper;
    }

    private VBox buildStatBox(String value, String label) {
        Label val = new Label(value);
        val.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 28px; -fx-font-weight: 800;");

        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #c8aed9; -fx-font-size: 11px; -fx-font-weight: 600;");

        VBox box = new VBox(4, val, lbl);
        box.setAlignment(Pos.CENTER);
        box.setPrefSize(140, 80);
        box.getStyleClass().add("stat-box");

        return box;
    }
}
