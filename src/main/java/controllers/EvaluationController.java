package controllers;

import Services.EvaluationService;
import entities.Evaluation;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class EvaluationController {

    @FXML private TableView<Evaluation>           tableEvaluations;
    @FXML private TableColumn<Evaluation, String> colTitre;
    @FXML private TableColumn<Evaluation, String> colType;
    @FXML private TableColumn<Evaluation, Float>  colBareme;
    @FXML private TableColumn<Evaluation, String> colDateLimite;
    @FXML private TableColumn<Evaluation, Void>   colActions;
    // Sidebar nav
    @FXML private Button btnNavStats;
    @FXML private Button btnNavEvals;
    @FXML private Button btnVueEtudiant;
    // Header
    @FXML private TextField searchField;
    @FXML private Label     lblCount;
    // Stats statiques (sans Actives)
    @FXML private Label statTotal;
    @FXML private Label statQuiz;
    @FXML private Label statTest;
    @FXML private Label statCode;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final EvaluationService service = new EvaluationService();
    private javafx.collections.ObservableList<Evaluation> allEvaluations;

    @FXML
    public void initialize() {
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colBareme.setCellValueFactory(new PropertyValueFactory<>("bareme"));
        colDateLimite.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        d.getValue().getDateLimite() != null
                        ? d.getValue().getDateLimite().format(FMT) : "—"));

        tableEvaluations.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        colTitre.setPrefWidth(190);    colTitre.setMinWidth(140);
        colType.setPrefWidth(85);       colType.setMinWidth(75);
        colBareme.setPrefWidth(85);     colBareme.setMinWidth(75);
        colDateLimite.setPrefWidth(150); colDateLimite.setMinWidth(130);
        colActions.setPrefWidth(430);   colActions.setMinWidth(430);

        setupActionsColumn();
        refresh();

        // ── Live search ──
        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> filterTable(n));
        }

        // ── Sidebar navigation ──
        if (btnVueEtudiant != null) {
            btnVueEtudiant.setOnAction(e -> navigateHome(btnVueEtudiant));
        }
        if (btnNavStats != null) btnNavStats.setOnAction(e -> ouvrirStatsModal());
    }

    private void filterTable(String text) {
        if (allEvaluations == null) return;
        if (text == null || text.isBlank()) {
            tableEvaluations.setItems(allEvaluations);
            updateCount(allEvaluations.size());
            return;
        }
        String q = text.toLowerCase();
        javafx.collections.ObservableList<Evaluation> filtered =
            allEvaluations.filtered(ev -> ev.getTitre().toLowerCase().contains(q)
                || ev.getType().toLowerCase().contains(q));
        tableEvaluations.setItems(filtered);
        updateCount(filtered.size());
    }

    private void updateCount(int n) {
        if (lblCount != null) lblCount.setText(n + " évaluation" + (n > 1 ? "s" : "") + " affichée" + (n > 1 ? "s" : ""));
    }

    private void navigateHome(javafx.scene.Node source) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/etudiant/homeView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) source.getScene().getWindow();
            Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());
            scene.setFill(javafx.scene.paint.Color.web("#0a0a0a"));
            stage.setScene(scene);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void showInfo(String titre, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(titre); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    // ── Modal PieChart statistiques ──────────────────────────────────────────
    private void ouvrirStatsModal() {
        if (allEvaluations == null || allEvaluations.isEmpty()) {
            showInfo("Statistiques", "Aucune évaluation enregistrée."); return;
        }

        long quiz = allEvaluations.stream().filter(e -> "Quiz".equalsIgnoreCase(e.getType())).count();
        long test = allEvaluations.stream().filter(e -> "Test".equalsIgnoreCase(e.getType())).count();
        long code = allEvaluations.stream().filter(e -> "Code".equalsIgnoreCase(e.getType())).count();

        PieChart chart = new PieChart();
        chart.getData().addAll(
            new PieChart.Data("Quiz (" + quiz + ")", quiz),
            new PieChart.Data("Test (" + test + ")", test),
            new PieChart.Data("Code (" + code + ")", code)
        );
        chart.setTitle("Répartition des évaluations");
        chart.setLabelsVisible(true);
        chart.setLegendVisible(true);
        chart.setPrefSize(480, 360);
        chart.setStyle("-fx-background-color: #1a0535;");

        // Couleurs néon sur les tranches
        String[] colors = {"#f08afc", "#a787ff", "#38bdf8"};
        for (int i = 0; i < chart.getData().size(); i++) {
            chart.getData().get(i).getNode().setStyle(
                "-fx-pie-color: " + colors[i] + ";");
        }

        // Titre et sous-titre
        Label title = new Label("📊  Statistiques des Évaluations");
        title.setStyle("-fx-text-fill: #26f7ff; -fx-font-size: 18px; -fx-font-weight: 900;");

        Label sub = new Label("Total : " + allEvaluations.size()
            + "  |  Quiz : " + quiz + "  |  Test : " + test + "  |  Code : " + code);
        sub.setStyle("-fx-text-fill: #c8aed9; -fx-font-size: 12px;");

        // Légende sous forme de barres
        VBox legend = new VBox(8);
        legend.setPadding(new Insets(12, 0, 0, 0));
        String[][] items = {{"Quiz", String.valueOf(quiz), "#f08afc"},
                            {"Test", String.valueOf(test), "#a787ff"},
                            {"Code", String.valueOf(code), "#38bdf8"}};
        long total = quiz + test + code;
        for (String[] item : items) {
            double pct = total == 0 ? 0 : Long.parseLong(item[1]) * 100.0 / total;
            Label lbl = new Label(item[0] + "  —  " + item[1] + "  (" + String.format("%.0f", pct) + "%)");
            lbl.setStyle("-fx-text-fill: " + item[2] + "; -fx-font-size: 13px; -fx-font-weight: 700;");

            ProgressBar bar = new ProgressBar(total == 0 ? 0 : (double) Long.parseLong(item[1]) / total);
            bar.setPrefWidth(360);
            bar.setPrefHeight(10);
            bar.setStyle("-fx-accent: " + item[2] + ";");

            legend.getChildren().addAll(lbl, bar);
        }

        VBox content = new VBox(12, title, sub, chart, legend);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: #1a0535;");
        content.setAlignment(Pos.TOP_CENTER);

        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Statistiques");
        Scene scene = new Scene(content, 520, 600);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        scene.setFill(javafx.scene.paint.Color.web("#1a0535"));
        modal.setScene(scene);
        modal.setResizable(false);
        modal.showAndWait();
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(col -> new TableCell<>() {
            final Button btnModifier  = makeBtn("✏ Modifier",   "action-button-edit",    90);
            final Button btnSupprimer = makeBtn("🗑 Supprimer",  "action-button-delete",  90);
            final Button btnQuestions = makeBtn("❓ Questions",  "action-button-nav",     90);
            final Button btnRendus    = makeBtn("📋 Rendus",     "action-button-success", 90);
            final HBox box = new HBox(4, btnModifier, btnSupprimer, btnQuestions, btnRendus);

            {
                box.setAlignment(Pos.CENTER_LEFT);
                box.setPadding(new Insets(4, 6, 4, 6));
                btnModifier.setOnAction(e  -> openModal(getTableView().getItems().get(getIndex())));
                btnSupprimer.setOnAction(e -> confirmerSuppression(getTableView().getItems().get(getIndex())));
                btnQuestions.setOnAction(e -> ouvrirVue("/views/questionView.fxml", "Questions",
                        getTableView().getItems().get(getIndex())));
                btnRendus.setOnAction(e    -> ouvrirVue("/views/renduView.fxml", "Rendus",
                        getTableView().getItems().get(getIndex())));
            }

            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    /** Crée un bouton avec texte, style CSS et largeur minimale fixe */
    private Button makeBtn(String text, String cssClass, double minWidth) {
        Button b = new Button(text);
        b.getStyleClass().add(cssClass);
        b.setMinWidth(minWidth);
        b.setPrefWidth(minWidth);
        return b;
    }


    @FXML private void handleAjouter() { openModal(null); }

    private void openModal(Evaluation existing) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(existing == null ? "Nouvelle évaluation" : "Modifier l'évaluation");

        // ── Champs ──
        TextField   fTitre  = nInput(null);       fTitre.setPromptText("Titre de l'évaluation");
        TextArea    fDesc   = nArea(null);         fDesc.setPromptText("Description...");
        ComboBox<String> fType = new ComboBox<>(); fType.getItems().addAll("Quiz","Test","Code");
        fType.setPromptText("Choisir un type"); fType.getStyleClass().add("neural-input");
        fType.setMaxWidth(Double.MAX_VALUE);
        TextField   fBareme = nInput(null);       fBareme.setPromptText("Ex: 20.0");
        DatePicker  fDate   = new DatePicker();
        fDate.setMaxWidth(Double.MAX_VALUE);
        // ── Visibilité DatePicker sur fond sombre ──
        fDate.setStyle(
            "-fx-background-color: rgba(255,255,255,0.08);" +
            "-fx-border-color: rgba(255,255,255,0.25);" +
            "-fx-border-radius: 10; -fx-background-radius: 10;" +
            "-fx-text-fill: #ffffff;" +
            "-fx-prompt-text-fill: #bda7cb;"
        );
        // Forcer la couleur du texte de l'éditeur intégré
        fDate.getEditor().setStyle(
            "-fx-text-fill: #ffffff; -fx-background-color: transparent;" +
            "-fx-prompt-text-fill: #bda7cb;"
        );
        fDate.setPromptText("jj/mm/aaaa");
        Spinner<Integer> fHeure = new Spinner<>(0,23,8);
        Spinner<Integer> fMin   = new Spinner<>(0,59,0);
        fHeure.setEditable(true); fMin.setEditable(true);
        fHeure.setPrefWidth(75);  fMin.setPrefWidth(75);
        HBox heureBox = new HBox(8, styled(new Label("h :")), fHeure, styled(new Label("min :")), fMin);
        heureBox.setAlignment(Pos.CENTER_LEFT);

        // Pré-remplissage
        if (existing != null) {
            fTitre.setText(existing.getTitre());
            fDesc.setText(existing.getDescription());
            fType.setValue(existing.getType());
            fBareme.setText(String.valueOf(existing.getBareme()));
            if (existing.getDateLimite() != null) {
                fDate.setValue(existing.getDateLimite().toLocalDate());
                fHeure.getValueFactory().setValue(existing.getDateLimite().getHour());
                fMin.getValueFactory().setValue(existing.getDateLimite().getMinute());
            }
        }

        // ── Grille ──
        GridPane grid = new GridPane();
        grid.setHgap(14); grid.setVgap(14);
        grid.setPadding(new Insets(24));
        grid.getColumnConstraints().addAll(col(120), col2());
        addRow(grid, 0, "Titre *",       fTitre);
        addRow(grid, 1, "Description *", fDesc);
        addRow(grid, 2, "Type *",        fType);
        addRow(grid, 3, "Barème *",      fBareme);
        addRow(grid, 4, "Date limite *", fDate);
        addRow(grid, 5, "Heure limite",  heureBox);

        // ── Boutons ──
        Button btnSave   = new Button(existing == null ? "Enregistrer" : "Mettre à jour");
        Button btnCancel = new Button("Annuler");
        btnSave.getStyleClass().add("btn-primary");
        btnCancel.getStyleClass().add("neon-button");
        HBox btnRow = new HBox(12, btnSave, btnCancel);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(0, 24, 24, 24));

        btnCancel.setOnAction(e -> stage.close());
        btnSave.setOnAction(e -> {
            // Validation
            if (fTitre.getText().trim().isBlank()) { err("Le titre est obligatoire."); return; }
            if (fTitre.getText().trim().length() < 3) { err("Le titre doit contenir au moins 3 caractères."); return; }
            if (fDesc.getText().trim().isBlank())  { err("La description est obligatoire."); return; }
            if (fType.getValue() == null)   { err("Le type est obligatoire."); return; }
            if (fDate.getValue() == null)   { err("La date limite est obligatoire."); return; }
            float bareme;
            try {
                bareme = Float.parseFloat(fBareme.getText().replace(',', '.'));
                if (bareme <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                err("Le barème doit être un nombre positif (ex: 20)."); return;
            }
            LocalDateTime dl = LocalDateTime.of(fDate.getValue(),
                    LocalTime.of(fHeure.getValue(), fMin.getValue()));
            if (dl.isBefore(LocalDateTime.now()) && existing == null) {
                err("La date limite doit être dans le futur."); return;
            }
            boolean success;
            if (existing == null) {
                success = service.ajouter(new Evaluation(fTitre.getText().trim(), fDesc.getText().trim(),
                        fType.getValue(), dl, bareme, "")); // pas NULL
            } else {
                existing.setTitre(fTitre.getText().trim());
                existing.setDescription(fDesc.getText().trim());
                existing.setType(fType.getValue());
                existing.setDateLimite(dl);
                existing.setBareme(bareme);
                success = service.modifier(existing);
            }
            
            if (success) {
                refresh();
                stage.close();
            } else {
                err("Erreur lors de l'enregistrement dans la base de données. Assurez-vous que xampp est activé.");
            }
        });

        // Boutons fixés en bas (toujours visibles)
        btnRow.setStyle("-fx-background-color: rgba(0,0,0,0.25); -fx-border-color: rgba(255,255,255,0.08); -fx-border-width: 1 0 0 0;");

        // ScrollPane pour le formulaire
        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scroll.getStyleClass().add("content-scroll");

        // BorderPane : formulaire scrollable, boutons fixes en bas
        BorderPane root = new BorderPane();
        root.setCenter(scroll);
        root.setBottom(btnRow);
        root.getStyleClass().add("modal-page");
        root.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        stage.setScene(new Scene(root, 540, 520));
        stage.setMinHeight(480);
        stage.setMinWidth(480);
        stage.setResizable(true);
        stage.showAndWait();
    }

    private void confirmerSuppression(Evaluation ev) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirmer la suppression");
        a.setHeaderText("Supprimer \"" + ev.getTitre() + "\" ?");
        a.setContentText("Toutes les questions et rendus associés seront aussi supprimés.");
        Optional<ButtonType> r = a.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) { service.supprimer(ev.getId()); refresh(); }
    }

    private void ouvrirVue(String fxml, String titre, Evaluation ev) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            if ("Questions".equals(titre)) {
                ((QuestionController) loader.getController()).setEvaluation(ev);
            } else {
                ((RenduController) loader.getController()).setEvaluation(ev);
            }
            Stage s = new Stage();
            s.initModality(Modality.APPLICATION_MODAL);
            s.setTitle(titre + " – " + ev.getTitre());
            s.setScene(new Scene(root, 1050, 620));
            s.showAndWait();
        } catch (Exception ex) {
            err("Impossible d'ouvrir la vue : " + ex.getMessage());
        }
    }

    private void refresh() {
        allEvaluations = FXCollections.observableArrayList(service.afficher());
        tableEvaluations.setItems(allEvaluations);
        updateCount(allEvaluations.size());
        updateStats();
    }

    private void updateStats() {
        if (allEvaluations == null) return;
        long total = allEvaluations.size();
        // trim() pour éviter les espaces DB, null-safe
        long quiz  = allEvaluations.stream()
            .filter(e -> e.getType() != null && e.getType().trim().equalsIgnoreCase("Quiz")).count();
        long test  = allEvaluations.stream()
            .filter(e -> e.getType() != null && e.getType().trim().equalsIgnoreCase("Test")).count();
        long code  = allEvaluations.stream()
            .filter(e -> e.getType() != null && e.getType().trim().equalsIgnoreCase("Code")).count();
        System.out.println("[Stats] total=" + total + " quiz=" + quiz + " test=" + test + " code=" + code);
        if (statTotal != null) statTotal.setText(String.valueOf(total));
        if (statQuiz  != null) statQuiz.setText(String.valueOf(quiz));
        if (statTest  != null) statTest.setText(String.valueOf(test));
        if (statCode  != null) statCode.setText(String.valueOf(code));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private TextField nInput(String v) {
        TextField f = new TextField(v != null ? v : "");
        f.getStyleClass().add("neural-input"); return f;
    }
    private TextArea nArea(String v) {
        TextArea a = new TextArea(v != null ? v : "");
        a.getStyleClass().add("neural-input");
        a.setPrefHeight(75); a.setWrapText(true); return a;
    }
    private Label styled(Label l) { l.setStyle("-fx-text-fill:#f2deff; -fx-font-size:12px;"); return l; }
    private Button btn(String text, String css) { Button b = new Button(text); b.getStyleClass().add(css); return b; }
    private ColumnConstraints col(double w) { ColumnConstraints c = new ColumnConstraints(); c.setMinWidth(w); c.setPrefWidth(w); return c; }
    private ColumnConstraints col2() { ColumnConstraints c = new ColumnConstraints(); c.setHgrow(Priority.ALWAYS); return c; }
    private void addRow(GridPane g, int row, String lbl, javafx.scene.Node field) {
        Label l = new Label(lbl); l.getStyleClass().add("form-label");
        g.add(l, 0, row); g.add(field, 1, row);
    }
    private void err(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}
