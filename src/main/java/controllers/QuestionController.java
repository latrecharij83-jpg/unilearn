package controllers;

import Services.QuestionService;
import entities.Evaluation;
import entities.Question;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;

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

            if (existing == null) {
                service.ajouter(new Question(fLibelle.getText(), fReponse.getText(), opts, evaluation.getId()));
            } else {
                existing.setLibelle(fLibelle.getText());
                existing.setReponseCorrecte(fReponse.getText());
                existing.setOptions(opts);
                service.modifier(existing);
            }
            refresh();
            stage.close();
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
        if (r.isPresent() && r.get() == ButtonType.OK) { service.supprimer(q.getId()); refresh(); }
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
}
