package controllers.etudiant;

import Services.PDFExportService;
import Services.QuestionService;
import Services.RenduService;
import Services.ai.AIService;
import Services.ai.CorrectionResult;
import Services.ai.OpenAIService;
import entities.Evaluation;
import entities.Question;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;
import java.util.stream.Collectors;

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
    private final RenduService    renduService    = new RenduService();
    private final AIService       aiService       = new OpenAIService();

    // ── Aide étudiant ──
    private boolean      cinquanteCinquanteUtilise = false;
    private final Set<Integer> hintsUtilises       = new HashSet<>();

    // UI references
    private Label lblProgress;
    private ProgressBar progressBar;
    private Button btnPrev, btnNext, btnSubmit;
    private Label lblQuestionNum, lblQuestionText;
    private VBox optionsBox;
    private HBox stepsIndicator;
    private List<Button> stepButtons = new ArrayList<>();

    // ── Timer Quiz ──
    private static final int SECONDS_PER_QUESTION = 60;
    private Timeline questionTimer;
    private Label    lblTimer;
    private int      secondsLeft;

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

        // Timer cercle (visible uniquement en mode Quiz)
        lblTimer = new Label("60");
        lblTimer.setMinSize(52, 52);
        lblTimer.setMaxSize(52, 52);
        lblTimer.setAlignment(Pos.CENTER);
        lblTimer.setStyle(timerStyle("#6affdd", "rgba(106,255,221,0.15)"));
        boolean isQuiz = isQuizType();
        lblTimer.setVisible(isQuiz);
        lblTimer.setManaged(isQuiz);

        Region qHeaderSpacer = new Region();
        HBox.setHgrow(qHeaderSpacer, Priority.ALWAYS);
        HBox questionHeader = new HBox(lblQuestionNum, qHeaderSpacer, lblTimer);
        questionHeader.setAlignment(Pos.CENTER_LEFT);

        lblQuestionText = new Label();
        lblQuestionText.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 18px; -fx-font-weight: 700;");
        lblQuestionText.setWrapText(true);

        optionsBox = new VBox(10);
        optionsBox.setPadding(new Insets(12, 0, 0, 0));

        questionContainer.getChildren().addAll(questionHeader, lblQuestionText, optionsBox);

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
        startTimer(); // Redémarrer le timer à chaque question (Quiz seulement)
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

        // ── Boutons aide : Indice IA & 50/50 ────────────────────────────────
        HBox aideRow = new HBox(10);
        aideRow.setAlignment(Pos.CENTER_LEFT);
        aideRow.setPadding(new Insets(14, 0, 0, 0));

        // Conteneur de la bulle d'indice (vide au départ)
        VBox hintContainer = new VBox();
        hintContainer.setMaxWidth(680);

        // ·· Bouton Indice IA ··
        Button btnHint = new Button("💡 Indice IA");
        btnHint.getStyleClass().add("hint-button");
        if (hintsUtilises.contains(q.getId())) btnHint.setDisable(true);

        final Question qFinal = q;
        btnHint.setOnAction(hEv -> {
            btnHint.setDisable(true);
            btnHint.setText("⏳ Chargement...");
            hintsUtilises.add(qFinal.getId());

            Task<String> hintTask = new Task<>() {
                @Override protected String call() {
                    return aiService.demanderIndice(qFinal.getLibelle(), qFinal.getReponseCorrecte());
                }
            };
            hintTask.setOnSucceeded(ev -> {
                btnHint.setText("💡 Indice IA");
                Label hintLbl = new Label(hintTask.getValue());
                hintLbl.getStyleClass().add("hint-bubble-text");
                hintLbl.setWrapText(true);
                hintLbl.setMaxWidth(640);
                VBox bubble = new VBox(hintLbl);
                bubble.getStyleClass().add("hint-bubble");
                hintContainer.getChildren().setAll(bubble);
            });
            hintTask.setOnFailed(ev -> {
                btnHint.setText("💡 Indice IA");
                btnHint.setDisable(false);
                hintsUtilises.remove(qFinal.getId());
            });
            Thread ht = new Thread(hintTask, "hint-thread");
            ht.setDaemon(true); ht.start();
        });
        aideRow.getChildren().add(btnHint);

        // ·· Bouton 50/50 (QCM seulement, ≥3 options) ··
        if (opts != null && opts.length >= 3) {
            Button btnFifty = new Button(cinquanteCinquanteUtilise ? "✓ 50/50 utilisé" : "🎲 50/50");
            btnFifty.getStyleClass().add("fifty-button");
            btnFifty.setDisable(cinquanteCinquanteUtilise);
            btnFifty.setOnAction(fEv -> {
                cinquanteCinquanteUtilise = true;
                btnFifty.setDisable(true);
                btnFifty.setText("✓ 50/50 utilisé");
                applyFiftyFifty(qFinal.getReponseCorrecte(), optionsBox);
            });
            aideRow.getChildren().add(btnFifty);
        }

        optionsBox.getChildren().addAll(aideRow, hintContainer);

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
        stopTimer(); // Arrêter le timer avant toute validation
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

        // Soumettre via RenduService
        int renduId = renduService.soumettre(evaluation.getId(), 0, json);

        if (renduId < 0) {
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Erreur de soumission");
            error.setHeaderText("La soumission a échoué");
            error.setContentText("Impossible d'enregistrer vos réponses.\nVérifiez que XAMPP est démarré et réessayez.");
            error.showAndWait();
            return;
        }

        // ── Correction automatique par IA ─────────────────────────────────────
        Stage correctionStage = new Stage();
        correctionStage.initModality(Modality.APPLICATION_MODAL);
        correctionStage.setTitle("🤖 Correction IA en cours...");
        correctionStage.setResizable(false);

        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(52, 52);
        Label corrLabel = new Label("L'IA analyse vos réponses, veuillez patienter...");
        corrLabel.setStyle("-fx-text-fill: #a5f3fc; -fx-font-size: 13px;");
        VBox corrBox = new VBox(18, pi, corrLabel);
        corrBox.setAlignment(Pos.CENTER);
        corrBox.setPadding(new Insets(36));
        corrBox.getStyleClass().add("modal-page");
        corrBox.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        correctionStage.setScene(new Scene(corrBox, 380, 180));

        // Snapshot immuable des données pour le thread
        final List<Question> snapshotQs     = new ArrayList<>(questions);
        final Map<Integer, String> snapshotR = new HashMap<>(reponses);
        final int finalRenduId              = renduId;
        final String finalEmail             = email;

        Task<CorrectionResult> corrTask = new Task<>() {
            @Override
            protected CorrectionResult call() {
                return aiService.corrigerReponses(
                        snapshotQs, snapshotR,
                        evaluation.getType(), evaluation.getBareme());
            }
        };

        corrTask.setOnSucceeded(ev -> {
            CorrectionResult corrResult = corrTask.getValue();
            // Enregistrer note + feedback dans la BD
            renduService.noter(finalRenduId, corrResult.getNote(), corrResult.getFeedback());
            correctionStage.close();
            afficherResultatCorrection(finalEmail, corrResult);
        });

        corrTask.setOnFailed(ev -> {
            correctionStage.close();
            // Fallback : succès sans note
            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Soumission réussie ! 🎉");
            success.setHeaderText(null);
            success.setContentText(
                "✅ Vos réponses ont été soumises !\n\n" +
                "📧 Identifiant : " + finalEmail + "\n" +
                "📝 Évaluation : " + evaluation.getTitre() + "\n\n" +
                "La correction automatique a échoué. Votre copie sera notée manuellement.");
            success.showAndWait();
            HomeController.loadView(rootPane, "/views/etudiant/catalogueEvaluationsView.fxml");
        });

        Thread ct = new Thread(corrTask, "ai-correction");
        ct.setDaemon(true);
        ct.start();
        correctionStage.show(); // non-bloquant : les callbacks gèrent la fermeture
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

    // ─── 50/50 : garde 1 bonne + 1 mauvaise, cache le reste ───────────────────
    private void applyFiftyFifty(String reponseCorrecte, VBox optsBox) {
        String repTrim = reponseCorrecte.trim();

        // Ne considérer QUE les HBox contenant un RadioButton (exclut aideRow)
        List<HBox> optionRows = optsBox.getChildren().stream()
                .filter(n -> n instanceof HBox)
                .map(n -> (HBox) n)
                .filter(hb -> hb.getChildren().stream().anyMatch(c -> c instanceof RadioButton))
                .collect(Collectors.toList());

        if (optionRows.size() < 2) return; // Pas assez d'options

        // Séparer bonne réponse et mauvaises réponses
        List<HBox> wrongBoxes = new ArrayList<>();
        for (HBox hb : optionRows) {
            boolean isCorrect = hb.getChildren().stream()
                    .anyMatch(c -> c instanceof RadioButton
                            && ((RadioButton) c).getText().trim().equalsIgnoreCase(repTrim));
            if (!isCorrect) wrongBoxes.add(hb);
        }

        if (wrongBoxes.isEmpty()) {
            System.err.println("⚠️ 50/50 : aucune mauvaise réponse trouvée pour '" + repTrim + "'");
            return;
        }

        // Mélanger et garder 1 seule mauvaise, cacher les autres
        Collections.shuffle(wrongBoxes);
        for (int i = 1; i < wrongBoxes.size(); i++) {
            wrongBoxes.get(i).setVisible(false);
            wrongBoxes.get(i).setManaged(false);
        }
        System.out.println("✅ 50/50 : " + (wrongBoxes.size() - 1) + " option(s) cachée(s), 2 restantes");
    }

    // ─── Modal résultat de correction IA (toujours visible) ───────────────────
    private void afficherResultatCorrection(String email, CorrectionResult result) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Résultat de l'évaluation");
        stage.setResizable(true);

        double pct    = evaluation.getBareme() > 0 ? result.getNote() / evaluation.getBareme() : 0;
        String emoji  = pct >= 0.8 ? "🌟" : pct >= 0.6 ? "👍" : pct >= 0.4 ? "📚" : "💪";
        String noteStr = String.format("%.1f / %.0f pts", result.getNote(), evaluation.getBareme());

        // Titre
        Label lblTitre = new Label(emoji + "  Résultat de votre évaluation");
        lblTitre.setStyle("-fx-text-fill: #26f7ff; -fx-font-size: 18px; -fx-font-weight: 900;");

        // Note
        Label lblNote = new Label(noteStr);
        lblNote.setStyle("-fx-text-fill: #6affdd; -fx-font-size: 30px; -fx-font-weight: 900;");

        Label lblInfo = new Label("📧 " + email + "   •   📝 " + evaluation.getTitre());
        lblInfo.setStyle("-fx-text-fill: #c8aed9; -fx-font-size: 12px;");

        VBox scoreBox = new VBox(6, lblNote, lblInfo);
        scoreBox.setAlignment(Pos.CENTER);
        scoreBox.setStyle("-fx-background-color: rgba(106,255,221,0.12);"
                + "-fx-padding: 16; -fx-background-radius: 12;");

        // Feedback
        Label lblFeedTitle = new Label("📄 Détail de la correction :");
        lblFeedTitle.setStyle("-fx-text-fill: #f2deff; -fx-font-size: 13px; -fx-font-weight: 700;");

        String feedText = (result.getFeedback() != null && !result.getFeedback().isBlank())
                ? result.getFeedback() : "Correction non disponible.";
        TextArea ta = new TextArea(feedText);
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefRowCount(12);
        ta.setPrefWidth(460);
        ta.setStyle("-fx-control-inner-background: #150430;"
                + "-fx-text-fill: #e4d0ff;"
                + "-fx-font-size: 13px;"
                + "-fx-background-color: #150430;"
                + "-fx-border-color: rgba(255,255,255,0.12);"
                + "-fx-border-radius: 8;"
                + "-fx-background-radius: 8;");

        // Boutons
        Button btnPdf = new Button("📥 Télécharger PDF");
        btnPdf.setStyle("-fx-background-color: rgba(106,255,221,0.18);"
                + "-fx-text-fill: #6affdd; -fx-font-weight: 800; -fx-font-size: 13px;"
                + "-fx-padding: 10 20; -fx-background-radius: 26;"
                + "-fx-border-color: #6affdd; -fx-border-radius: 26;"
                + "-fx-border-width: 1; -fx-cursor: hand;");
        btnPdf.setOnAction(e -> PDFExportService.exportResultat(
                email, evaluation, result.getNote(),
                result.getFeedback(), stage));

        Button btnOk = new Button("✅ Retour au catalogue");
        btnOk.setStyle("-fx-background-color: linear-gradient(to right, #f08afc, #8e68ff);"
                + "-fx-text-fill: white; -fx-font-weight: 800; -fx-font-size: 13px;"
                + "-fx-padding: 10 24; -fx-background-radius: 26; -fx-cursor: hand;");
        btnOk.setOnAction(e -> {
            stage.close();
            HomeController.loadView(rootPane, "/views/etudiant/catalogueEvaluationsView.fxml");
        });
        HBox btnRow = new HBox(12, btnPdf, btnOk);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setStyle("-fx-padding: 12 0 0 0;");

        // Conteneur principal — fond inline garanti visible
        VBox layout = new VBox(14, lblTitre, scoreBox, lblFeedTitle, ta, btnRow);
        layout.setStyle("-fx-background-color: #1e0840; -fx-padding: 28;");
        layout.setPrefWidth(500);

        Scene scene = new Scene(layout, 520, 560);
        stage.setScene(scene);
        stage.showAndWait();
    }

    // ─── TIMER (Quiz uniquement) ─────────────────────────────────────────────

    /** Démarre un compte à rebours de 60s. Sans effet si ce n'est pas un Quiz. */
    private void startTimer() {
        stopTimer();
        if (!isQuizType()) return;

        secondsLeft = SECONDS_PER_QUESTION;
        updateTimerDisplay();

        questionTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsLeft--;
            updateTimerDisplay();
            if (secondsLeft <= 0) {
                stopTimer();
                onTimerExpired();
            }
        }));
        questionTimer.setCycleCount(Timeline.INDEFINITE);
        questionTimer.play();
    }

    /** Stoppe le timer courant proprement. */
    private void stopTimer() {
        if (questionTimer != null) {
            questionTimer.stop();
            questionTimer = null;
        }
    }

    /** Met à jour l'affichage et la couleur du timer selon le temps restant. */
    private void updateTimerDisplay() {
        if (lblTimer == null) return;
        lblTimer.setText(String.valueOf(secondsLeft));
        if (secondsLeft > 30) {
            lblTimer.setStyle(timerStyle("#6affdd", "rgba(106,255,221,0.15)"));  // vert
        } else if (secondsLeft > 10) {
            lblTimer.setStyle(timerStyle("#fbbf24", "rgba(251,191,36,0.15)"));   // orange
        } else {
            lblTimer.setStyle(timerStyle("#ff6b8a", "rgba(255,107,138,0.15)"));  // rouge
        }
    }

    /** Appelé quand le temps expire : passe à la question suivante ou soumet. */
    private void onTimerExpired() {
        saveCurrentAnswer();
        if (currentIndex < questions.size() - 1) {
            Platform.runLater(() -> showQuestion(currentIndex + 1));
        } else {
            Platform.runLater(this::handleSubmit);
        }
    }

    private boolean isQuizType() {
        return evaluation != null && "Quiz".equalsIgnoreCase(evaluation.getType());
    }

    private String timerStyle(String color, String bg) {
        return "-fx-background-color: " + bg + ";"
             + "-fx-background-radius: 50;"
             + "-fx-border-radius: 50;"
             + "-fx-border-color: " + color + ";"
             + "-fx-border-width: 2;"
             + "-fx-text-fill: " + color + ";"
             + "-fx-font-size: 16px;"
             + "-fx-font-weight: 900;";
    }
}
