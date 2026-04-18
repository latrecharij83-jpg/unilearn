package Services.ai;

import entities.Question;

import java.util.List;
import java.util.Map;

/**
 * Interface IA – contrat commun pour :
 *   - la génération de questions pédagogiques
 *   - la correction automatique des réponses étudiant
 *   - la génération d'indices (hints)
 *
 * Implémentations disponibles :
 *   - OpenAIService  (appel API réelle OpenAI)
 *   - SimulatedAIService (simulation hors-ligne, fallback)
 */
public interface AIService {

    /**
     * Génère une liste de questions sur un sujet donné.
     *
     * @param sujet        Thème ou sujet pédagogique (ex: "Héritage en Java")
     * @param nombre       Nombre de questions à générer
     * @param type         Type : "QCM", "Réponse unique" ou "Texte libre"
     * @param evaluationId ID de l'évaluation cible
     * @return Liste de Question prêtes à insérer en base
     */
    List<Question> genererQuestions(String sujet, int nombre, String type, int evaluationId);

    /**
     * Corrige automatiquement les réponses d'un étudiant.
     *
     * @param questions     Questions de l'évaluation (avec reponseCorrecte)
     * @param reponses      Map questionId → réponse donnée par l'étudiant
     * @param typeEvaluation Type d'évaluation (Quiz, Test, Code…)
     * @param bareme        Note maximale possible
     * @return CorrectionResult contenant la note et le feedback global
     */
    CorrectionResult corrigerReponses(List<Question> questions,
                                      Map<Integer, String> reponses,
                                      String typeEvaluation,
                                      float bareme);

    /**
     * Génère un indice pour aider l'étudiant sans révéler la réponse.
     *
     * @param libelle         Texte de la question
     * @param reponseCorrecte Réponse correcte (non révélée à l'étudiant)
     * @return Texte de l'indice
     */
    String demanderIndice(String libelle, String reponseCorrecte);
}
