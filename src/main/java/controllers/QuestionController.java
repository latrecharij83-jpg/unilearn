package controllers;

import Services.QuestionService;
import Services.ai.AIService;
import Services.ai.OpenAIService;
import entities.Evaluation;
import entities.Question;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;

import java.util.List;
import java.util.Optional;

public class QuestionController {

    @FXML private Label                         lblEvaluation;
    @FXML private TableView<Question>           tableQuestions;
    @FXML private TableColumn<Question, String> colLibelle;
    @FXML private TableColumn<Question, String> colReponse;
    @FXML private TableColumn<Question, String> colOptions;
    @FXML private TableColumn<Question, Void>   colActions;

    private final QuestionService service = new QuestionService();
    private Evaluation evaluation;

    public void setEvaluation(Evaluation ev) {
        this.evaluation = ev;
        lblEvaluation.setText("Questions – " + ev.getTitre());
        refresh();
    }

    @FXML
    public void initialize() {
        colLibelle.setCellValueFactory(new PropertyValueFactory<>("libelle"));
        colReponse.setCellValueFactory(new PropertyValueFactory<>("reponseCorrecte"));
        colOptions.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getOptionsString()));
        tableQuestions.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setupActions();
    }

    private void setupActions() {
        colActions.setCellFactory(col -> new TableCell<>() {
            final Button btnMod = makeBtn("✏ Modifier",  "action-button-edit",   90);
            final Button btnSup = makeBtn("🗑 Supprimer", "action-button-delete", 90);
            final HBox box = new HBox(6, btnMod, btnSup);
            {
                box.setAlignment(Pos.CENTER_LEFT);
                box.setPadding(new Insets(3, 6, 3, 6));
                btnMod.setOnAction(e -> openModal(getTableView().getItems().get(getIndex())));
                btnSup.setOnAction(e -> confirmer(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    @FXML private void handleAjouter() { openModal(null); }

    @FXML private void handleRetour() {
        ((Stage) tableQuestions.getScene().getWindow()).close();
    }

    // ─── Modal Formulaire ────────────────────────────────────────────────────
    private void openModal(Question existing) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(existing == null ? "Ajouter une question" : "Modifier la question");
        stage.setResizable(true);

        // ── Champs principaux ──
        TextArea fLibelle = nArea();
        fLibelle.setPromptText("Texte complet de la question...");

        ComboBox<String> fType = new ComboBox<>();
        fType.getItems().addAll("QCM", "Réponse unique", "Texte libre");
        fType.setPromptText("Type de question");
        fType.getStyleClass().add("neural-input");
        fType.setMaxWidth(Double.MAX_VALUE);

        // ── 4 champs options QCM ──
        TextField fOpt1 = nInput("Option 1");
        TextField fOpt2 = nInput("Option 2");
        TextField fOpt3 = nInput("Option 3");
        TextField fOpt4 = nInput("Option 4");

        // Container options – masqué par défaut
        VBox optionsBox = new VBox(8,
                lbl("Options QCM :"),
                fOpt1, fOpt2, fOpt3, fOpt4,
                note("ⓘ Renseignez les 4 options. La réponse correcte doit être identique à l'une d'elles.")
        );
        optionsBox.setVisible(false);
        optionsBox.setManaged(false);

        TextField fReponse = nInput("Réponse correcte");

        // Afficher/masquer options selon le type choisi
        fType.setOnAction(e -> {
            boolean qcm = "QCM".equals(fType.getValue());
            optionsBox.setVisible(qcm);
            optionsBox.setManaged(qcm);
            if (!qcm) { fOpt1.clear(); fOpt2.clear(); fOpt3.clear(); fOpt4.clear(); }
        });

        // ── Pré-remplissage ──
        if (existing != null) {
            fLibelle.setText(existing.getLibelle());
            fReponse.setText(existing.getReponseCorrecte());
            if (existing.getOptions() != null && existing.getOptions().length > 0) {
                fType.setValue("QCM");
                optionsBox.setVisible(true);
                optionsBox.setManaged(true);
                String[] opts = existing.getOptions();
                if (opts.length > 0) fOpt1.setText(opts[0]);
                if (opts.length > 1) fOpt2.setText(opts[1]);
                if (opts.length > 2) fOpt3.setText(opts[2]);
                if (opts.length > 3) fOpt4.setText(opts[3]);
            } else {
                fType.setValue("Réponse unique");
            }
        }

        // ── Grille principale ──
        GridPane grid = new GridPane();
        grid.setHgap(14); grid.setVgap(12);
        grid.setPadding(new Insets(24, 24, 8, 24));
        grid.getColumnConstraints().addAll(col(140), col2());

        grid.add(lbl("Libellé *"),          0, 0); grid.add(fLibelle,    1, 0);
        grid.add(lbl("Type *"),             0, 1); grid.add(fType,       1, 1);
        grid.add(lbl(""),                   0, 2); grid.add(optionsBox,  1, 2);
        grid.add(lbl("Réponse correcte *"), 0, 3); grid.add(fReponse,    1, 3);

        // ── Boutons (toujours visibles en bas) ──
        Button btnSave   = new Button(existing == null ? "Enregistrer" : "Mettre à jour");
        Button btnCancel = new Button("Annuler");
        btnSave.getStyleClass().add("btn-primary");
        btnCancel.getStyleClass().add("neon-button");
        HBox btnRow = new HBox(12, btnSave, btnCancel);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(12, 24, 20, 24));
        btnRow.setStyle("-fx-background-color: rgba(0,0,0,0.25); -fx-border-color: rgba(255,255,255,0.08); -fx-border-width: 1 0 0 0;");

        btnCancel.setOnAction(e -> stage.close());

        btnSave.setOnAction(e -> {
            if (fLibelle.getText().isBlank()) { err("Le libellé est obligatoire."); return; }
            if (fType.getValue() == null)     { err("Le type est obligatoire."); return; }
            if (fReponse.getText().isBlank())  { err("La réponse correcte est obligatoire."); return; }

            String[] opts = new String[0];
            if ("QCM".equals(fType.getValue())) {
                if (fOpt1.getText().isBlank() || fOpt2.getText().isBlank()
                        || fOpt3.getText().isBlank() || fOpt4.getText().isBlank()) {
                    err("Pour un QCM, les 4 options doivent être renseignées."); return;
                }
                opts = new String[]{
                    fOpt1.getText().trim(), fOpt2.getText().trim(),
                    fOpt3.getText().trim(), fOpt4.getText().trim()
                };
                final String rep = fReponse.getText().trim();
                boolean valide = false;
                for (String o : opts) { if (o.equalsIgnoreCase(rep)) { valide = true; break; } }
                if (!valide) {
                    err("La réponse correcte doit être identique à l'une des 4 options."); return;
                }
            }

            boolean ok;
            if (existing == null) {
                ok = service.ajouter(new Question(fLibelle.getText(), fReponse.getText(), opts, evaluation.getId()));
            } else {
                existing.setLibelle(fLibelle.getText());
                existing.setReponseCorrecte(fReponse.getText());
                existing.setOptions(opts);
                ok = service.modifier(existing);
            }
            if (ok) { refresh(); stage.close(); }
            else { err("Opération échouée – vérifiez que XAMPP est démarré."); }
        });

        // ── Mise en page : ScrollPane pour le formulaire + boutons fixes en bas ──
        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scroll.getStyleClass().add("content-scroll");

        BorderPane root = new BorderPane();
        root.setCenter(scroll);
        root.setBottom(btnRow);
        root.getStyleClass().add("modal-page");
        root.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        // Hauteur adaptative : petite si simple, grande si QCM
        Scene scene = new Scene(root, 520, 360);
        stage.setScene(scene);
        stage.setMinWidth(480);
        stage.setMinHeight(320);
        stage.setMaxHeight(700);
        stage.showAndWait();
    }

    private void confirmer(Question q) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirmer"); a.setHeaderText("Supprimer cette question ?");
        String l = q.getLibelle();
        a.setContentText(l.length() > 100 ? l.substring(0, 97) + "..." : l);
        Optional<ButtonType> r = a.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            boolean ok = service.supprimer(q.getId());
            if (ok) { refresh(); }
            else { err("Suppression échouée – vérifiez que XAMPP est démarré."); }
        }
    }

    private void refresh() {
        tableQuestions.setItems(FXCollections.observableArrayList(
                service.afficherParEvaluation(evaluation.getId())));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private TextField nInput(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.getStyleClass().add("neural-input");
        return f;
    }
    private TextArea nArea() {
        TextArea a = new TextArea();
        a.getStyleClass().add("neural-input");
        a.setPrefHeight(75); a.setWrapText(true);
        return a;
    }
    private Label lbl(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("form-label");
        return l;
    }
    private Label note(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill:#bda7cb; -fx-font-size:11px;");
        l.setWrapText(true);
        return l;
    }
    private Button makeBtn(String t, String css, double w) {
        Button b = new Button(t); b.getStyleClass().add(css);
        b.setMinWidth(w); b.setPrefWidth(w); return b;
    }
    private ColumnConstraints col(double w) {
        ColumnConstraints c = new ColumnConstraints(); c.setMinWidth(w); c.setPrefWidth(w); return c;
    }
    private ColumnConstraints col2() {
        ColumnConstraints c = new ColumnConstraints(); c.setHgrow(Priority.ALWAYS); return c;
    }
    private void err(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setHeaderText(null);
        a.setContentText(msg); a.showAndWait();
    }

    // ─── Génération de questions par IA ─────────────────────────────────────
    @FXML
    private void handleGenererIA() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("🤖 Générer des questions avec l'IA");
        stage.setResizable(false);

        // ── Champs du formulaire ──
        TextField fSujet = new TextField();
        fSujet.getStyleClass().add("neural-input");
        fSujet.setPromptText("Ex: Héritage Java, SQL, Spring Boot...");

        Spinner<Integer> fNombre = new Spinner<>(1, 10, 3);
        fNombre.setEditable(true);
        fNombre.setPrefWidth(110);
        fNombre.getStyleClass().add("neural-input");

        ComboBox<String> fType = new ComboBox<>();
        fType.getItems().addAll("QCM", "R\u00e9ponse unique", "Texte libre", "Code");
        fType.setValue("QCM");
        fType.getStyleClass().add("neural-input");
        fType.setMaxWidth(Double.MAX_VALUE);

        ProgressIndicator progress = new ProgressIndicator();
        progress.setVisible(false);
        progress.setPrefSize(28, 28);

        Label lblStatus = new Label("Saisissez un sujet et cliquez sur Générer.");
        lblStatus.setStyle("-fx-text-fill: #c8aed9; -fx-font-size: 12px;");
        lblStatus.setWrapText(true);

        // ── Grille des champs ──
        GridPane grid = new GridPane();
        grid.setHgap(14); grid.setVgap(14);
        grid.setPadding(new Insets(24));
        grid.getColumnConstraints().addAll(col(160), col2());

        Label lSujet  = lbl("Sujet / Thème *");
        Label lNombre = lbl("Nombre de questions");
        Label lType   = lbl("Type de questions");

        grid.add(lSujet,  0, 0); grid.add(fSujet,  1, 0);
        grid.add(lNombre, 0, 1); grid.add(fNombre, 1, 1);
        grid.add(lType,   0, 2); grid.add(fType,   1, 2);
        grid.add(lblStatus, 0, 3, 2, 1);

        // ── Boutons ──
        Button btnGenerer = new Button("🧠 Générer");
        btnGenerer.getStyleClass().add("ai-button");
        Button btnCancel  = new Button("Annuler");
        btnCancel.getStyleClass().add("neon-button");

        HBox btnRow = new HBox(12, btnGenerer, progress, btnCancel);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(0, 24, 24, 24));
        btnRow.setStyle("-fx-background-color: rgba(0,0,0,0.20); -fx-border-color: rgba(255,255,255,0.07); -fx-border-width: 1 0 0 0;");

        btnCancel.setOnAction(e -> stage.close());

        btnGenerer.setOnAction(e -> {
            if (fSujet.getText().isBlank()) { err("Veuillez saisir un sujet ou thème."); return; }

            String sujet  = fSujet.getText().trim();
            int    nombre = fNombre.getValue();
            String type   = fType.getValue();

            // Désactiver UI pendant la génération
            btnGenerer.setDisable(true);
            progress.setVisible(true);
            lblStatus.setText("⏳ Génération en cours via l'IA, veuillez patienter...");
            lblStatus.setStyle("-fx-text-fill: #a5f3fc; -fx-font-size: 12px;");

            Task<List<Question>> task = new Task<>() {
                @Override
                protected List<Question> call() {
                    AIService ai = new OpenAIService();
                    return ai.genererQuestions(sujet, nombre, type, evaluation.getId());
                }
            };

            task.setOnSucceeded(ev -> {
                progress.setVisible(false);
                List<Question> generated = task.getValue();

                if (generated.isEmpty()) {
                    lblStatus.setText("❌ Aucune question générée. Vérifiez votre clé API ou réessayez.");
                    lblStatus.setStyle("-fx-text-fill: #ff6b8a; -fx-font-size: 12px;");
                    btnGenerer.setDisable(false);
                    return;
                }

                int added = 0;
                for (Question q : generated) {
                    if (service.ajouter(q)) added++;
                }
                refresh();
                stage.close();

                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("✅ Génération réussie");
                info.setHeaderText(null);
                info.setContentText(added + " question(s) générée(s) par l'IA et ajoutée(s) avec succès !");
                info.showAndWait();
            });

            task.setOnFailed(ev -> {
                progress.setVisible(false);
                btnGenerer.setDisable(false);
                String msg = task.getException() != null ? task.getException().getMessage() : "Erreur inconnue";
                lblStatus.setText("❌ Erreur : " + msg);
                lblStatus.setStyle("-fx-text-fill: #ff6b8a; -fx-font-size: 12px;");
            });

            Thread t = new Thread(task, "ai-question-gen");
            t.setDaemon(true);
            t.start();
        });

        // ── Mise en page ──
        VBox root = new VBox(grid, btnRow);
        root.getStyleClass().add("modal-page");
        root.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        stage.setScene(new Scene(root, 500, 310));
        stage.showAndWait();
    }
}
