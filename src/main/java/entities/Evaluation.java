package entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Evaluation {
    private int id;
    private String titre;
    private String description;
    private String type; // "Quiz", "Test", "Code"
    private LocalDateTime dateLimite;
    private float bareme;
    private String quizzData; // nullable

    private List<Question> questions;
    private List<Rendu> rendus;

    public Evaluation() {
        this.questions = new ArrayList<>();
        this.rendus    = new ArrayList<>();
    }

    /** Constructeur INSERT (sans id) */
    public Evaluation(String titre, String description, String type,
                      LocalDateTime dateLimite, float bareme, String quizzData) {
        this();
        this.titre      = titre;
        this.description = description;
        this.type       = type;
        this.dateLimite = dateLimite;
        this.bareme     = bareme;
        this.quizzData  = quizzData;
    }

    /** Constructeur SELECT (avec id) */
    public Evaluation(int id, String titre, String description, String type,
                      LocalDateTime dateLimite, float bareme, String quizzData) {
        this(titre, description, type, dateLimite, bareme, quizzData);
        this.id = id;
    }

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }
    public String getTitre()                    { return titre; }
    public void setTitre(String t)              { this.titre = t; }
    public String getDescription()              { return description; }
    public void setDescription(String d)        { this.description = d; }
    public String getType()                     { return type; }
    public void setType(String t)               { this.type = t; }
    public LocalDateTime getDateLimite()        { return dateLimite; }
    public void setDateLimite(LocalDateTime dl) { this.dateLimite = dl; }
    public float getBareme()                    { return bareme; }
    public void setBareme(float b)              { this.bareme = b; }
    public String getQuizzData()                { return quizzData; }
    public void setQuizzData(String qd)         { this.quizzData = qd; }
    public List<Question> getQuestions()        { return questions; }
    public void setQuestions(List<Question> q)  { this.questions = q; }
    public List<Rendu> getRendus()              { return rendus; }
    public void setRendus(List<Rendu> r)        { this.rendus = r; }

    @Override public String toString() { return titre; }
}