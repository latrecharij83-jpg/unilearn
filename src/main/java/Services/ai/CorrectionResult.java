package Services.ai;

/**
 * Résultat d'une correction automatique par IA.
 * Contient la note obtenue et le feedback global généré.
 */
public class CorrectionResult {

    private final float  note;
    private final String feedback;

    public CorrectionResult(float note, String feedback) {
        this.note     = note;
        this.feedback = feedback;
    }

    public float  getNote()     { return note; }
    public String getFeedback() { return feedback; }

    @Override
    public String toString() {
        return String.format("CorrectionResult{note=%.1f, feedback='%s'}", note, feedback);
    }
}
