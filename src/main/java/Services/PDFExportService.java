package Services;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import entities.Evaluation;
import entities.Rendu;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Génère un PDF de rapport de rendu/correction.
 * Utilisé côté admin (depuis RenduController) et côté étudiant (résultat IA).
 */
public class PDFExportService {

    // ── Palette couleurs UniLearn ────────────────────────────────────────────
    private static final Color COL_BG_HEADER  = new Color(30,   8,  96);   // violet foncé
    private static final Color COL_ACCENT     = new Color(106, 255, 221);   // turquoise néon
    private static final Color COL_PINK       = new Color(240, 138, 252);   // rose néon
    private static final Color COL_TEXT_LIGHT = new Color(242, 222, 255);   // lavande claire
    private static final Color COL_DARK       = new Color(20,   4,  50);    // fond sombre
    private static final Color COL_GOOD       = new Color(34,  197, 94);    // vert succès
    private static final Color COL_WARN       = new Color(251, 191, 36);    // orange
    private static final Color COL_BAD        = new Color(239, 68,  68);    // rouge

    // ── Fonts OpenPDF ───────────────────────────────────────────────────────
    private static final Font FONT_TITLE   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   22, COL_ACCENT);
    private static final Font FONT_HEADER  = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   13, COL_PINK);
    private static final Font FONT_LABEL   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   10, COL_ACCENT);
    private static final Font FONT_VALUE   = FontFactory.getFont(FontFactory.HELVETICA,        10, Color.WHITE);
    private static final Font FONT_SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   11, COL_PINK);
    private static final Font FONT_BODY    = FontFactory.getFont(FontFactory.HELVETICA,         9, COL_TEXT_LIGHT);
    private static final Font FONT_NOTE_BIG= FontFactory.getFont(FontFactory.HELVETICA_BOLD,   36, COL_ACCENT);
    private static final Font FONT_SMALL   = FontFactory.getFont(FontFactory.HELVETICA,         8, Color.GRAY);

    /**
     * Ouvre un FileChooser, puis génère le PDF du rendu (côté admin).
     *
     * @param rendu      Entité Rendu à exporter
     * @param evaluation Évaluation associée
     * @param window     Fenêtre parente pour le FileChooser
     */
    public static void exportRendu(Rendu rendu, Evaluation evaluation, Window window) {
        File dest = chooseSavePath(window,
                "rendu_" + rendu.getId() + ".pdf");
        if (dest == null) return;

        try (Document doc = new Document(PageSize.A4, 40, 40, 50, 50)) {
            PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(dest));
            doc.open();
            addContent(doc, writer,
                    evaluation.getTitre(),
                    evaluation.getType(),
                    evaluation.getBareme(),
                    "Étudiant #" + rendu.getUserId(),
                    rendu.getNoteObtenue() >= 0 ? rendu.getNoteObtenue() : -1,
                    rendu.getFeedbackEnseignant() != null ? rendu.getFeedbackEnseignant() : "",
                    rendu.getContenuTexte() != null ? rendu.getContenuTexte() : "");
        } catch (Exception e) {
            System.err.println("❌ Erreur génération PDF (admin) : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Ouvre un FileChooser, puis génère le PDF du résultat IA (côté étudiant).
     *
     * @param email      Email de l'étudiant
     * @param evaluation Évaluation passée
     * @param note       Note obtenue
     * @param feedback   Feedback de correction
     * @param window     Fenêtre parente
     */
    public static void exportResultat(String email, Evaluation evaluation,
                                      float note, String feedback, Window window) {
        String safeName = email.replaceAll("[^a-zA-Z0-9]", "_");
        File dest = chooseSavePath(window,
                "resultat_" + safeName + "_" + evaluation.getId() + ".pdf");
        if (dest == null) return;

        try (Document doc = new Document(PageSize.A4, 40, 40, 50, 50)) {
            PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(dest));
            doc.open();
            addContent(doc, writer,
                    evaluation.getTitre(),
                    evaluation.getType(),
                    evaluation.getBareme(),
                    email,
                    note,
                    feedback != null ? feedback : "",
                    "");
        } catch (Exception e) {
            System.err.println("❌ Erreur génération PDF (étudiant) : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Sélecteur de fichier ──────────────────────────────────────────────────
    private static File chooseSavePath(Window owner, String defaultName) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le rapport PDF");
        fc.setInitialFileName(defaultName);
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers PDF (*.pdf)", "*.pdf"));
        return fc.showSaveDialog(owner);
    }

    // ── Construction du contenu PDF ───────────────────────────────────────────
    private static void addContent(Document doc, PdfWriter writer,
                                   String titre, String type, float bareme,
                                   String etudiant, float note,
                                   String feedback, String contenu) throws Exception {

        String dateStr = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        // ── Fond de page sombre ──
        PdfContentByte canvas = writer.getDirectContentUnder();
        canvas.saveState();
        canvas.setColorFill(new Color(15, 4, 40));
        canvas.rectangle(0, 0, PageSize.A4.getWidth(), PageSize.A4.getHeight());
        canvas.fill();
        canvas.restoreState();

        // ── Bandeau header ────────────────────────────────────────────────────
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{3, 1});

        // Colonne gauche : titre
        PdfPCell cellTitle = new PdfPCell();
        cellTitle.setBorder(Rectangle.NO_BORDER);
        cellTitle.setBackgroundColor(COL_BG_HEADER);
        cellTitle.setPadding(14);
        cellTitle.addElement(new Phrase("UniLearn", FONT_TITLE));
        cellTitle.addElement(new Phrase("Rapport d'évaluation", FONT_HEADER));
        headerTable.addCell(cellTitle);

        // Colonne droite : date
        PdfPCell cellDate = new PdfPCell(new Phrase(dateStr + "\n" + type, FONT_SMALL));
        cellDate.setBorder(Rectangle.NO_BORDER);
        cellDate.setBackgroundColor(COL_BG_HEADER);
        cellDate.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellDate.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cellDate.setPadding(14);
        headerTable.addCell(cellDate);

        doc.add(headerTable);
        doc.add(Chunk.NEWLINE);

        // ── Bloc infos évaluation ─────────────────────────────────────────────
        PdfPTable infoTable = new PdfPTable(4);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingBefore(8);

        addInfoCell(infoTable, "Évaluation", titre);
        addInfoCell(infoTable, "Type",       type);
        addInfoCell(infoTable, "Barème",     String.format("%.0f pts", bareme));
        addInfoCell(infoTable, "Étudiant",   etudiant);

        doc.add(infoTable);
        doc.add(Chunk.NEWLINE);

        // ── Note obtenue ──────────────────────────────────────────────────────
        PdfPTable noteTable = new PdfPTable(1);
        noteTable.setWidthPercentage(100);
        noteTable.setSpacingBefore(12);

        String noteText;
        Color noteColor;
        if (note < 0) {
            noteText = "Non noté";
            noteColor = Color.GRAY;
        } else {
            noteText = String.format("%.1f / %.0f pts", note, bareme);
            double pct = bareme > 0 ? note / bareme : 0;
            noteColor = pct >= 0.8 ? COL_GOOD : pct >= 0.5 ? COL_WARN : COL_BAD;
        }

        Font noteFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 32, noteColor);
        PdfPCell noteCell = new PdfPCell(new Phrase(noteText, noteFont));
        noteCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        noteCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        noteCell.setPadding(20);
        noteCell.setBorder(Rectangle.BOX);
        noteCell.setBorderColor(noteColor);
        noteCell.setBackgroundColor(new Color(20, 5, 60));
        noteTable.addCell(noteCell);

        doc.add(noteTable);
        doc.add(Chunk.NEWLINE);

        // ── Feedback / Correction ─────────────────────────────────────────────
        if (!feedback.isBlank()) {
            Paragraph fbTitle = new Paragraph("Détail de la correction", FONT_SECTION);
            fbTitle.setSpacingBefore(14);
            fbTitle.setSpacingAfter(6);
            doc.add(fbTitle);

            // Séparateur
            addSeparator(doc, writer);

            // Texte feedback ligne par ligne
            for (String line : feedback.split("\n")) {
                Font lf = line.startsWith("✅") ? FontFactory.getFont(FontFactory.HELVETICA, 9, COL_GOOD)
                        : line.startsWith("❌") ? FontFactory.getFont(FontFactory.HELVETICA, 9, COL_BAD)
                        : line.startsWith("━") ? FontFactory.getFont(FontFactory.HELVETICA, 9, COL_ACCENT)
                        : FONT_BODY;
                Paragraph p = new Paragraph(line.isEmpty() ? " " : line, lf);
                p.setLeading(13);
                doc.add(p);
            }
        }

        // ── Contenu de la réponse (admin) ─────────────────────────────────────
        if (!contenu.isBlank()) {
            doc.add(Chunk.NEWLINE);
            Paragraph cTitle = new Paragraph("Réponse de l'étudiant", FONT_SECTION);
            cTitle.setSpacingBefore(14);
            cTitle.setSpacingAfter(6);
            doc.add(cTitle);
            addSeparator(doc, writer);

            Paragraph cBody = new Paragraph(contenu, FONT_BODY);
            cBody.setLeading(13);
            doc.add(cBody);
        }

        // ── Pied de page ──────────────────────────────────────────────────────
        doc.add(Chunk.NEWLINE);
        Paragraph footer = new Paragraph(
                "Généré par UniLearn · " + dateStr + "  —  Document officiel",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7, Color.GRAY));
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);
    }

    // ── Cellule de métadonnée ─────────────────────────────────────────────────
    private static void addInfoCell(PdfPTable t, String label, String value) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(new Color(30, 10, 70));
        c.setBorderColor(COL_BG_HEADER);
        c.setPadding(9);
        c.addElement(new Phrase(label, FONT_LABEL));
        c.addElement(new Phrase(value != null ? value : "—", FONT_VALUE));
        t.addCell(c);
    }

    // ── Ligne de séparation colorée ───────────────────────────────────────────
    private static void addSeparator(Document doc, PdfWriter writer) throws Exception {
        PdfContentByte cb = writer.getDirectContent();
        cb.saveState();
        cb.setColorStroke(COL_ACCENT);
        cb.setLineWidth(0.8f);
        float y = writer.getVerticalPosition(true);
        cb.moveTo(doc.leftMargin(), y);
        cb.lineTo(PageSize.A4.getWidth() - doc.rightMargin(), y);
        cb.stroke();
        cb.restoreState();
        doc.add(new Paragraph(" "));
    }
}
