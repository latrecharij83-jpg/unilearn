package entities;

public class Rendu {
    private int id;
    private String contenuTexte;
    private String fichierJoint;   // nullable
    private float noteObtenue;     // -1 si non noté
    private String feedbackEnseignant; // nullable
    private int evaluationId;
    private int userId;            // 0 si non défini

    public Rendu() { this.noteObtenue = -1; }

    public Rendu(String contenuTexte, String fichierJoint, float noteObtenue,
                 String feedbackEnseignant, int evaluationId, int userId) {
        this.contenuTexte       = contenuTexte;
        this.fichierJoint       = fichierJoint;
        this.noteObtenue        = noteObtenue;
        this.feedbackEnseignant = feedbackEnseignant;
        this.evaluationId       = evaluationId;
        this.userId             = userId;
    }

    public Rendu(int id, String contenuTexte, String fichierJoint, float noteObtenue,
                 String feedbackEnseignant, int evaluationId, int userId) {
        this(contenuTexte, fichierJoint, noteObtenue, feedbackEnseignant, evaluationId, userId);
        this.id = id;
    }

    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }
    public String getContenuTexte()                 { return contenuTexte; }
    public void setContenuTexte(String ct)          { this.contenuTexte = ct; }
    public String getFichierJoint()                 { return fichierJoint; }
    public void setFichierJoint(String fj)          { this.fichierJoint = fj; }
    public float getNoteObtenue()                   { return noteObtenue; }
    public void setNoteObtenue(float n)             { this.noteObtenue = n; }
    public String getFeedbackEnseignant()           { return feedbackEnseignant; }
    public void setFeedbackEnseignant(String fb)    { this.feedbackEnseignant = fb; }
    public int getEvaluationId()                    { return evaluationId; }
    public void setEvaluationId(int eid)            { this.evaluationId = eid; }
    public int getUserId()                          { return userId; }
    public void setUserId(int uid)                  { this.userId = uid; }

    /** Affichage note : "Non noté" si -1 */
    public String getNoteDisplay() {
        return noteObtenue < 0 ? "Non noté" : String.format("%.1f / 20", noteObtenue);
    }

    /** Contenu tronqué pour TableView */
    public String getContenuTronque() {
        if (contenuTexte == null || contenuTexte.isBlank()) return "—";
        return contenuTexte.length() > 70 ? contenuTexte.substring(0, 67) + "..." : contenuTexte;
    }
}