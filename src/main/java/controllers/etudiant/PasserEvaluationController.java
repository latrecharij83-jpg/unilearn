package controllers.etudiant;

import Services.QuestionService;
import Services.RenduService;
import entities.Evaluation;
import entities.Question;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur pour passer une évaluation – côté étudiant.
 * Affiche les questions une par une avec navigation Suivant/Précédent,
 * barre de progression, indicateur de questions, identification par email
 * et soumission finale en JSON vers la table rendu.
 */
public class PasserEvaluationController {

    @FXML private BorderPane rootPane;

    private Evaluation evaluation;
    private List<Question> questions;
    private int currentIndex = 0;

    // Stocke la réponse de chaque question (questionId → réponse)
    private final Map<Integer, String> reponses = new HashMap<>();

    private final QuestionService questionService = new QuestionService();
    private final RenduService renduService = new RenduService();

    // UI references
    private Label lblProgress;
    private ProgressBar progressBar;
    private Button btnPrev, btnNext, btnSubmit;
    private Label lblQuestionNum, lblQuestionText;
    private VBox optionsBox;
    private HBox stepsIndicator;
    private List<Button> stepButtons = new ArrayList<>();

    // Email identification
    private TextField emailField;

    /** Appelé après FXMLLoader.load() pour passer l'évaluation sélectionnée */
    public void setEvaluation(Evaluation ev) {
        this.evaluation = ev;
        this.questions = questionService.afficherParEvaluation(ev.getId());
        this.currentIndex = 0;
        buildUI();
    }

    @FXML
    public void initialize() {
        // Top navbar
        HBox navbar = HomeController.buildNavbar("evaluations");
        rootPane.setTop(navbar);
    }

    // ─── BUILD UI ───────────────────────────────────────────────────────────
    private void buildUI() {
        // ── Header ──
        Label title = new Label("✍ " + evaluation.getTitre());
        title.getStyleClass().add("page-title");
        title.setStyle("-fx-font-size: 26px;");

        Label subtitle = new Label(evaluation.getDescription() != null ? evaluation.getDescription() : "");
        subtitle.getStyleClass().add("page-subtitle");
        subtitle.setWrapText(true);

        // Badge infos
        Label typeBadge = new Label(evaluation.getType().toUpperCase() +
                "  |  Barème : " + String.format("%.0f", evaluation.getBareme()) + " pts" +
                "  |  " + questions.size() + " questions");
        typeBadge.setStyle("-fx-text-fill: #6affdd; -fx-font-size: 12px; -fx-font-weight: 700;");

        VBox header = new VBox(6, title, subtitle, typeBadge);
        header.setPadding(new Insets(0, 0, 4, 0));

        // ── Email identification ──
        HBox emailRow = buildEmailRow();

        // ── Progress ──
        HBox progressRow = buildProgressBar();

        // ── Step indicators ──
        stepsIndicator = buildStepsIndicator();

        // ── Question area ──
        VBox questionContainer = new VBox(18);
        questionContainer.setPadding(new Insets(24));
        questionContainer.getStyleClass().add("module-card");
        questionContainer.setMaxWidth(700);
        questionContainer.setMinHeight(260);

        lblQuestionNum = new Label();
        lblQuestionNum.setStyle("-fx-text-fill: #f08afc; -fx-font-size: 13px; -fx-font-weight: 800;");

        lblQuestionText = new Label();
        lblQuestionText.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 18px; -fx-font-weight: 700;");
        lblQuestionText.setWrapText(true);

        optionsBox = new VBox(10);
        optionsBox.setPadding(new Insets(12, 0, 0, 0));

        questionContainer.getChildren().addAll(lblQuestionNum, lblQuestionText, optionsBox);

        // ── Nav buttons ──
        HBox navButtons = buildNavButtons();

        // ── Empty state ──
        if (questions.isEmpty()) {
            Label empty = new Label("⚠ Aucune question disponible pour cette évaluation.");
            empty.setStyle("-fx-text-fill: #ff6b8a; -fx-font-size: 15px; -fx-font-weight: 700;");
            questionContainer.getChildren().clear();
            questionContainer.getChildren().add(empty);
            navButtons.setVisible(false);
            stepsIndicator.setVisible(false);
        }

        // ── Assemble into scrollable content ──
        VBox content = new VBox(16, header, emailRow, progressRow, stepsIndicator, questionContainer, navButtons);
        content.setPadding(new Insets(36, 40, 36, 40));
        content.getStyleClass().add("main-content");

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("content-scroll");
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        rootPane.setCenter(scroll);

        if (!questions.isEmpty()) {
            showQuestion(0);
        }
    }

    // ─── EMAIL ROW ──────────────────────────────────────────────────────────
    private HBox buildEmailRow() {
        Label lbl = new Label("📧 Votre email :");
        lbl.getStyleClass().add("form-label");

        emailField = new TextField();
        emailField.getStyleClass().add("neural-input");
        emailField.setPromptText("etudiant@unilearn.tn");
        emailField.setPrefWidth(280);

        HBox row = new HBox(12, lbl, emailField);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(700);
        return row;
    }

    // ─── PROGRESS BAR ───────────────────────────────────────────────────────
    private HBox buildProgressBar() {
        lblProgress = new Label();
        lblProgress.setStyle("-fx-text-fill: #d5b9ea; -fx-font-size: 13px; -fx-font-weight: 700;");

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);
        progressBar.setPrefHeight(8);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(16, lblProgress, spacer, progressBar);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(700);
        return row;
    }

    // ─── STEP INDICATORS ────────────────────────────────────────────────────
    private HBox buildStepsIndicator() {
        HBox steps = new HBox(6);
        steps.setAlignment(Pos.CENTER_LEFT);
        steps.setMaxWidth(700);
        stepButtons.clear();

        for (int i = 0; i < questions.size(); i++) {
            final int idx = i;
            Button dot = new Button(String.valueOf(i + 1));
            dot.setMinSize(32, 32);
            dot.setMaxSize(32, 32);
            dot.setStyle(stepStyleDefault());
            dot.setCursor(javafx.scene.Cursor.HAND);
            dot.setOnAction(e -> {
                saveCurrentAnswer();
                showQuestion(idx);
            });
            stepButtons.add(dot);
            steps.getChildren().add(dot);
        }
        return steps;
    }

    private String stepStyleDefault() {
        return "-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: #c8aed9;" +
               "-fx-font-size: 11px; -fx-font-weight: 700; -fx-background-radius: 50;" +
               "-fx-border-radius: 50; -fx-border-color: rgba(255,255,255,0.18); -fx-border-width: 1;";
    }

    private String stepStyleActive() {
        return "-fx-background-color: linear-gradient(to right, #f08afc, #8e68ff); -fx-text-fill: #ffffff;" +
               "-fx-font-size: 11px; -fx-font-weight: 800; -fx-background-radius: 50;" +
               "-fx-border-radius: 50; -fx-border-color: rgba(255,255,255,0.45); -fx-border-width: 1;" +
               "-fx-effect: dropshadow(three-pass-box, rgba(240,138,252,0.45), 10, 0.2, 0, 2);";
    }

    private String stepStyleAnswered() {
        return "-fx-background-color: rgba(106,255,221,0.20); -fx-text-fill: #6affdd;" +
               "-fx-font-size: 11px; -fx-font-weight: 700; -fx-background-radius: 50;" +
               "-fx-border-radius: 50; -fx-border-color: #6affdd; -fx-border-width: 1;";
    }

    private void updateStepIndicators() {
        for (int i = 0; i < stepButtons.size(); i++) {
            Button dot = stepButtons.get(i);
            if (i == currentIndex) {
                dot.setStyle(stepStyleActive());
            } else if (reponses.containsKey(questions.get(i).getId())) {
                dot.setStyle(stepStyleAnswered());
            } else {
                dot.setStyle(stepStyleDefault());
            }
        }
    }

    // ─── NAV BUTTONS ────────────────────────────────────────────────────────
    private HBox buildNavButtons() {
        btnPrev = new Button("◀  Précédent");
        btnPrev.getStyleClass().add("neon-button");
        btnPrev.setOnAction(e -> navigate(-1));

        btnNext = new Button("Suivant  ▶");
        btnNext.getStyleClass().add("btn-primary");
        btnNext.setOnAction(e -> navigate(1));

        btnSubmit = new Button("✅  Soumettre mes réponses");
        btnSubmit.getStyleClass().add("action-button-success");
        btnSubmit.setStyle(
                "-fx-font-size: 14px; -fx-padding: 10 28; -fx-font-weight: 800;");
        btnSubmit.setOnAction(e -> handleSubmit());
        btnSubmit.setVisible(false);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox nav = new HBox(12, btnPrev, spacer, btnNext, btnSubmit);
        nav.setAlignment(Pos.CENTER);
        nav.setPadding(new Insets(4, 0, 0, 0));
        nav.setMaxWidth(700);
        return nav;
    }

    // ─── SHOW QUESTION ──────────────────────────────────────────────────────
    private void showQuestion(int index) {
        if (index < 0 || index >= questions.size()) return;

        // Save current answer before switching
        saveCurrentAnswer();

        currentIndex = index;
        Question q = questions.get(index);

        lblQuestionNum.setText("QUESTION " + (index + 1) + " / " + questions.size());
        lblQuestionText.setText(q.getLibelle());

        // Progress
        lblProgress.setText("Question " + (index + 1) + " sur " + questions.size());
        progressBar.setProgress((double)(index + 1) / questions.size());

        // Build options/input based on question type
        optionsBox.getChildren().clear();
        String[] opts = q.getOptions();
        String savedAnswer = reponses.get(q.getId());

        if (opts != null && opts.length > 0) {
            // QCM — RadioButtons with styled containers
            ToggleGroup group = new ToggleGroup();
            char letter = 'A';
            for (String opt : opts) {
                if (opt == null || opt.isBlank()) continue;

                RadioButton rb = new RadioButton(opt);
                rb.setToggleGroup(group);
                rb.setWrapText(true);
                rb.setStyle("-fx-text-fill: #f2deff; -fx-font-size: 14px;");
                rb.setCursor(javafx.scene.Cursor.HAND);
                rb.setPadding(new Insets(10, 14, 10, 14));

                if (opt.equals(savedAnswer)) {
                    rb.setSelected(true);
                }

                // Wrap in a styled HBox for visual container
                Label letterLabel = new Label(String.valueOf(letter));
                letterLabel.setStyle(
                    "-fx-text-fill: #f08afc; -fx-font-size: 13px; -fx-font-weight: 800;" +
                    "-fx-min-width: 28; -fx-min-height: 28; -fx-max-width: 28; -fx-max-height: 28;" +
                    "-fx-alignment: center; -fx-background-color: rgba(240,138,252,0.15);" +
                    "-fx-background-radius: 50; -fx-border-radius: 50; -fx-border-color: rgba(240,138,252,0.35);" +
                    "-fx-border-width: 1;");

                HBox optionRow = new HBox(12, letterLabel, rb);
                optionRow.setAlignment(Pos.CENTER_LEFT);
                optionRow.setPadding(new Insets(6, 12, 6, 12));
                optionRow.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 12;" +
                    "-fx-border-radius: 12; -fx-border-color: rgba(255,255,255,0.08); -fx-border-width: 1;");

                // Hover effect on option row
                optionRow.setOnMouseEntered(e -> optionRow.setStyle(
                    "-fx-background-color: rgba(240,138,252,0.08); -fx-background-radius: 12;" +
                    "-fx-border-radius: 12; -fx-border-color: rgba(240,138,252,0.30); -fx-border-width: 1;"));
                optionRow.setOnMouseExited(e -> optionRow.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 12;" +
                    "-fx-border-radius: 12; -fx-border-color: rgba(255,255,255,0.08); -fx-border-width: 1;"));
                optionRow.setOnMouseClicked(e -> rb.setSelected(true));
                optionRow.setCursor(javafx.scene.Cursor.HAND);

                optionsBox.getChildren().add(optionRow);
                letter++;
            }
        } else {
            // Texte libre — TextArea with label
            Label hint = new Label("Rédigez votre réponse ci-dessous :");
            hint.setStyle("-fx-text-fill: #c8aed9; -fx-font-size: 12px;");

            TextArea ta = new TextArea();
            ta.getStyleClass().add("neural-input");
            ta.setPromptText("Écrivez votre réponse ici...");
            ta.setPrefHeight(140);
            ta.setWrapText(true);
            if (savedAnswer != null) ta.setText(savedAnswer);
            optionsBox.getChildren().addAll(hint, ta);
        }

        // Nav button visibility
        btnPrev.setDisable(index == 0);
        boolean isLast = (index == questions.size() - 1);
        btnNext.setVisible(!isLast);
        btnSubmit.setVisible(isLast);

        // Update step indicators
        updateStepIndicators();
    }

    // ─── NAVIGATION ─────────────────────────────────────────────────────────
    private void navigate(int delta) {
        saveCurrentAnswer();
        int newIdx = currentIndex + delta;
        if (newIdx >= 0 && newIdx < questions.size()) {
            showQuestion(newIdx);
        }
    }

    // ─── SAVE CURRENT ANSWER ────────────────────────────────────────────────
    private void saveCurrentAnswer() {
        if (questions == null || questions.isEmpty() || currentIndex < 0 || currentIndex >= questions.size()) return;
        Question q = questions.get(currentIndex);

        for (javafx.scene.Node node : optionsBox.getChildren()) {
            // RadioButtons are wrapped in HBox > RadioButton
            if (node instanceof HBox row) {
                for (javafx.scene.Node child : row.getChildren()) {
                    if (child instanceof RadioButton rb && rb.isSelected()) {
                        reponses.put(q.getId(), rb.getText());
                        return;
                    }
                }
            }
            if (node instanceof TextArea ta) {
                String text = ta.getText();
                if (text != null && !text.isBlank()) {
                    reponses.put(q.getId(), text.trim());
                }
                return;
            }
        }
    }

    // ─── SUBMIT ─────────────────────────────────────────────────────────────
    private void handleSubmit() {
        saveCurrentAnswer();

        // Validate email
        String email = emailField.getText() != null ? emailField.getText().trim() : "";
        if (email.isEmpty() || !email.contains("@")) {
            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setTitle("Email requis");
            a.setHeaderText("Veuillez saisir votre adresse email");
            a.setContentText("L'email est nécessaire pour identifier votre soumission.");
            a.showAndWait();
            emailField.requestFocus();
            return;
        }

        // Vérifier que toutes les questions ont une réponse
        StringBuilder missing = new StringBuilder();
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            String ans = reponses.get(q.getId());
            if (ans == null || ans.isBlank()) {
                missing.append("  • Question ").append(i + 1).append("\n");
            }
        }

        if (missing.length() > 0) {
            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setTitle("Réponses manquantes");
            a.setHeaderText("Vous n'avez pas répondu à toutes les questions :");
            a.setContentText(missing.toString());
            a.showAndWait();
            return;
        }

        // Confirmation dialog
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la soumission");
        confirm.setHeaderText("Soumettre vos réponses ?");
        confirm.setContentText(
            "📧 Email : " + email + "\n" +
            "📝 Évaluation : " + evaluation.getTitre() + "\n" +
            "❓ Questions : " + questions.size() + "\n\n" +
            "Une fois soumis, vous ne pourrez plus modifier vos réponses.");
        var result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        // Construire le JSON des réponses (inclut l'email)
        String json = buildReponsesJson(email);

        // Soumettre via RenduService (userId = 0 car pas de système d'auth)
        renduService.soumettre(evaluation.getId(), 0, json);

        // Succès
        Alert success = new Alert(Alert.AlertType.INFORMATION);
        success.setTitle("Soumission réussie ! 🎉");
        success.setHeaderText(null);
        success.setContentText(
            "✅ Vos réponses ont été soumises avec succès !\n\n" +
            "📧 Identifiant : " + email + "\n" +
            "📝 Évaluation : " + evaluation.getTitre() + "\n\n" +
            "Votre copie est en attente de notation par l'enseignant.");
        success.showAndWait();

        // Retour au catalogue
        HomeController.loadView(rootPane, "/views/etudiant/catalogueEvaluationsView.fxml");
    }

    private String buildReponsesJson(String email) {
        StringBuilder sb = new StringBuilder("{\"email\":\"").append(escapeJson(email)).append("\",\"reponses\":[");
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            String ans = reponses.getOrDefault(q.getId(), "");
            sb.append("{\"questionId\":").append(q.getId())
              .append(",\"libelle\":\"").append(escapeJson(q.getLibelle()))
              .append("\",\"reponse\":\"").append(escapeJson(ans)).append("\"}");
            if (i < questions.size() - 1) sb.append(",");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
