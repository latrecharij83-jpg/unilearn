package controllers;

import Services.PDFExportService;
import Services.RenduService;
import entities.Evaluation;
import entities.Rendu;
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

public class RenduController {

    @FXML private Label                           lblEvaluation;
    @FXML private TableView<Rendu>                tableRendus;
    @FXML private TableColumn<Rendu, String>      colContenu;
    @FXML private TableColumn<Rendu, String>      colFichier;
    @FXML private TableColumn<Rendu, String>      colNote;
    @FXML private TableColumn<Rendu, String>      colFeedback;
    @FXML private TableColumn<Rendu, Void>        colActions;

    private final RenduService service = new RenduService();
    private Evaluation evaluation;

    public void setEvaluation(Evaluation ev) {
        this.evaluation = ev;
        lblEvaluation.setText("Rendus – " + ev.getTitre());
        refresh();
    }

    @FXML
    public void initialize() {
        colContenu.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getContenuTronque()));
        colFichier.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        d.getValue().getFichierJoint() != null ? d.getValue().getFichierJoint() : "—"));
        colNote.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getNoteDisplay()));
        colFeedback.setCellValueFactory(d -> {
            String fb = d.getValue().getFeedbackEnseignant();
            if (fb == null || fb.isBlank()) return new javafx.beans.property.SimpleStringProperty("—");
            return new javafx.beans.property.SimpleStringProperty(fb.length() > 40 ? fb.substring(0, 37) + "..." : fb);
        });
        tableRendus.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setupActions();
    }

    private void setupActions() {
        colActions.setCellFactory(col -> new TableCell<>() {
            final Button btnVoir    = btn("Voir détail", "action-button-nav");
            final Button btnNoter   = btn("Noter",       "action-button-success");
            final Button btnPdf     = btn("📥 PDF",      "action-button-nav");
            final Button btnSupp    = btn("Supprimer",   "action-button-delete");
            final HBox box = new HBox(6, btnVoir, btnNoter, btnPdf, btnSupp);

            {
                box.setAlignment(Pos.CENTER_LEFT);
                btnVoir.setOnAction(e  -> voirDetail(getTableView().getItems().get(getIndex())));
                btnNoter.setOnAction(e -> openNotation(getTableView().getItems().get(getIndex())));
                btnSupp.setOnAction(e  -> confirmer(getTableView().getItems().get(getIndex())));
                btnPdf.setOnAction(e   -> {
                    Rendu r = getTableView().getItems().get(getIndex());
                    PDFExportService.exportRendu(r, evaluation,
                            btnPdf.getScene().getWindow());
                });
            }

            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    @FXML private void handleRetour() {
        ((Stage) tableRendus.getScene().getWindow()).close();
    }

    private void voirDetail(Rendu r) {
        Stage s = new Stage();
        s.initModality(Modality.APPLICATION_MODAL);
        s.setTitle("Détail du rendu");

        Label info = new Label(String.format("Étudiant #%d  |  Note : %s  |  Fichier : %s",
                r.getUserId(), r.getNoteDisplay(),
                r.getFichierJoint() != null ? r.getFichierJoint() : "aucun"));
        info.setStyle("-fx-text-fill:#d5b9ea; -fx-font-size:12px;");

        TextArea area = new TextArea(r.getContenuTexte());
        area.setEditable(false); area.setWrapText(true); area.setPrefHeight(240);
        area.getStyleClass().add("neural-input");

        Button btnPdf = new Button("📥 Télécharger le rapport PDF");
        btnPdf.getStyleClass().add("btn-primary");
        btnPdf.setOnAction(e -> PDFExportService.exportRendu(r, evaluation, s));

        Button btnClose = new Button("Fermer"); btnClose.getStyleClass().add("neon-button");
        btnClose.setOnAction(e -> s.close());
        HBox btnRow = new HBox(12, btnPdf, btnClose);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(12, info, area, btnRow);
        root.setPadding(new Insets(24));
        root.getStyleClass().add("modal-page");
        root.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        s.setScene(new Scene(root, 620, 420));
        s.showAndWait();
    }

    private void openNotation(Rendu rendu) {
        Stage s = new Stage();
        s.initModality(Modality.APPLICATION_MODAL);
        s.setTitle("Noter le rendu");

        TextField fNote = new TextField(); fNote.getStyleClass().add("neural-input");
        fNote.setPromptText("Note entre 0 et 20 (ex: 15.5)");
        if (rendu.getNoteObtenue() >= 0) fNote.setText(String.valueOf(rendu.getNoteObtenue()));

        TextArea fFeedback = new TextArea(); fFeedback.getStyleClass().add("neural-input");
        fFeedback.setPromptText("Commentaire pour l'étudiant (optionnel)...");
        fFeedback.setPrefHeight(90); fFeedback.setWrapText(true);
        if (rendu.getFeedbackEnseignant() != null) fFeedback.setText(rendu.getFeedbackEnseignant());

        GridPane grid = new GridPane();
        grid.setHgap(14); grid.setVgap(14); grid.setPadding(new Insets(24));
        grid.getColumnConstraints().addAll(col(130), col2());
        addRow(grid, 0, "Note (0-20) *", fNote);
        addRow(grid, 1, "Feedback",      fFeedback);

        Button btnSave   = new Button("Enregistrer la note"); btnSave.getStyleClass().add("btn-primary");
        Button btnCancel = new Button("Annuler");             btnCancel.getStyleClass().add("neon-button");
        HBox btnRow = new HBox(12, btnSave, btnCancel);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(0, 24, 24, 24));

        btnCancel.setOnAction(e -> s.close());
        btnSave.setOnAction(e -> {
            float note;
            try {
                note = Float.parseFloat(fNote.getText().replace(',', '.'));
                if (note < 0 || note > 20) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                err("La note doit être un nombre entre 0 et 20."); return;
            }
            boolean ok = service.noter(rendu.getId(), note, fFeedback.getText());
            if (ok) { refresh(); s.close(); }
            else { err("Notation échouée – vérifiez que XAMPP est démarré."); }
        });

        VBox root = new VBox(grid, btnRow);
        root.getStyleClass().add("modal-page");
        root.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        s.setScene(new Scene(root, 480, 310));
        s.showAndWait();
    }

    private void confirmer(Rendu r) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirmer"); a.setHeaderText("Supprimer ce rendu ?");
        a.setContentText("Cette action est irréversible.");
        Optional<ButtonType> res = a.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            boolean ok = service.supprimer(r.getId());
            if (ok) { refresh(); }
            else { err("Suppression échouée – vérifiez que XAMPP est démarré."); }
        }
    }

    private void refresh() {
        tableRendus.setItems(FXCollections.observableArrayList(service.afficherParEvaluation(evaluation.getId())));
    }

    private Button btn(String t, String css) { Button b = new Button(t); b.getStyleClass().add(css); return b; }
    private ColumnConstraints col(double w) { ColumnConstraints c = new ColumnConstraints(); c.setMinWidth(w); c.setPrefWidth(w); return c; }
    private ColumnConstraints col2() { ColumnConstraints c = new ColumnConstraints(); c.setHgrow(Priority.ALWAYS); return c; }
    private void addRow(GridPane g, int row, String lbl, javafx.scene.Node f) {
        Label l = new Label(lbl); l.getStyleClass().add("form-label"); g.add(l, 0, row); g.add(f, 1, row);
    }
    private void err(String msg) { Alert a = new Alert(Alert.AlertType.ERROR); a.setHeaderText(null); a.setContentText(msg); a.showAndWait(); }
}
