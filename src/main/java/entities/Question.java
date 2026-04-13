package entities;

public class Question {
    private int id;
    private String libelle;
    private String reponseCorrecte;
    private String[] options; // stocké en BDD comme JSON ["opt1","opt2",...]
    private int evaluationId;

    public Question() {}

    public Question(String libelle, String reponseCorrecte, String[] options, int evaluationId) {
        this.libelle         = libelle;
        this.reponseCorrecte = reponseCorrecte;
        this.options         = options;
        this.evaluationId    = evaluationId;
    }

    public Question(int id, String libelle, String reponseCorrecte, String[] options, int evaluationId) {
        this(libelle, reponseCorrecte, options, evaluationId);
        this.id = id;
    }

    public int getId()                       { return id; }
    public void setId(int id)                { this.id = id; }
    public String getLibelle()               { return libelle; }
    public void setLibelle(String l)         { this.libelle = l; }
    public String getReponseCorrecte()       { return reponseCorrecte; }
    public void setReponseCorrecte(String r) { this.reponseCorrecte = r; }
    public String[] getOptions()             { return options; }
    public void setOptions(String[] o)       { this.options = o; }
    public int getEvaluationId()             { return evaluationId; }
    public void setEvaluationId(int eid)     { this.evaluationId = eid; }

    /**
     * Sérialise les options en JSON pour la BDD.
     * Retourne null si pas d'options (colonne JSON accepte NULL).
     * Ex: ["Vrai","Faux","Les deux","Aucun"]
     */
    public String getOptionsJson() {
        if (options == null || options.length == 0) return null;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < options.length; i++) {
            sb.append("\"").append(options[i].replace("\"", "\\\"")).append("\"");
            if (i < options.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Désérialise depuis le JSON stocké en BDD.
     * Supporte aussi l'ancien format séparé par ";" pour compatibilité.
     */
    public static String[] parseOptions(String raw) {
        if (raw == null || raw.isBlank()) return new String[0];
        raw = raw.trim();
        // Format JSON : ["opt1","opt2",...]
        if (raw.startsWith("[")) {
            raw = raw.substring(1, raw.length() - 1); // enlever [ ]
            if (raw.isBlank()) return new String[0];
            // Séparer par virgule en dehors des guillemets
            String[] parts = raw.split("\",\"");
            String[] result = new String[parts.length];
            for (int i = 0; i < parts.length; i++) {
                result[i] = parts[i].replaceAll("^\"|\"$", "").replace("\\\"", "\"");
            }
            return result;
        }
        // Fallback : format séparé par ";"
        return raw.split(";");
    }

    /** Affichage concaténé pour TableView */
    public String getOptionsString() {
        if (options == null || options.length == 0) return "—";
        String s = String.join(" | ", options);
        return s.length() > 60 ? s.substring(0, 57) + "..." : s;
    }
}