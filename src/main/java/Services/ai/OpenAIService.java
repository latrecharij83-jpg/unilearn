package Services.ai;

import entities.Question;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Implémentation AIService utilisant l'API OpenAI (gpt-3.5-turbo / gpt-4).
 *
 * Configuration : src/main/resources/ai_config.properties
 *   openai.api.key = sk-...
 *   openai.model   = gpt-3.5-turbo
 *
 * Si la clé est absente ou invalide, les méthodes basculent automatiquement
 * sur SimulatedAIService (fallback transparent).
 */
public class OpenAIService implements AIService {

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    private final String     apiKey;
    private final String     model;
    private final HttpClient httpClient;
    private final AIService  fallback;

    public OpenAIService() {
        Properties props = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/ai_config.properties")) {
            if (is != null) props.load(is);
        } catch (Exception e) {
            System.err.println("⚠️ Impossible de lire ai_config.properties : " + e.getMessage());
        }
        this.apiKey   = props.getProperty("openai.api.key", "").trim();
        this.model    = props.getProperty("openai.model", "gpt-3.5-turbo").trim();
        this.fallback = new SimulatedAIService();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    // ── Appel HTTP vers l'API OpenAI ──────────────────────────────────────────

    /**
     * Envoie un prompt au modèle et retourne le contenu de la réponse.
     * Retourne null si la clé n'est pas configurée ou si l'appel échoue.
     */
    private String callOpenAI(String prompt) {
        if (apiKey.isBlank() || apiKey.equals("YOUR_API_KEY_HERE")) {
            System.out.println("ℹ️ Clé OpenAI non configurée → simulation activée.");
            return null;
        }

        String requestBody = new JSONObject()
                .put("model", model)
                .put("messages", new JSONArray()
                        .put(new JSONObject().put("role", "user").put("content", prompt)))
                .put("temperature", 0.7)
                .put("max_tokens", 2000)
                .toString();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("❌ OpenAI API erreur " + response.statusCode()
                        + " : " + response.body());
                return null;
            }

            JSONObject json = new JSONObject(response.body());
            String content  = json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
            System.out.println("✅ Réponse OpenAI reçue (" + content.length() + " chars)");
            return content;

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'appel OpenAI : " + e.getMessage());
            return null;
        }
    }

    // ── Nettoyage du JSON renvoyé par GPT ─────────────────────────────────────
    private String cleanJson(String raw) {
        if (raw == null) return null;
        raw = raw.trim();
        // Supprimer les blocs ```json ... ```
        if (raw.startsWith("```")) {
            raw = raw.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```", "").trim();
        }
        return raw;
    }

    // ── 1. Génération de questions ────────────────────────────────────────────

    @Override
    public List<Question> genererQuestions(String sujet, int nombre, String type, int evaluationId) {

        String prompt;

        if ("Code".equalsIgnoreCase(type)) {
            // ── Prompt spécifique : énoncés de codage ──────────────────────────
            prompt = "Tu es un formateur expert en développement logiciel.\n"
                + "Génère exactement " + nombre + " exercice(s) de PROGRAMMATION sur le sujet \"" + sujet + "\" en français.\n\n"
                + "IMPORTANT : retourne UNIQUEMENT un tableau JSON valide, sans aucun texte avant ou après.\n"
                + "Format exact :\n"
                + "[\n"
                + "  {\n"
                + "    \"libelle\": \"Énoncé complet : contexte, signature de la fonction/classe à écrire, "
                + "paramètres d'entrée, valeur de retour, contraintes et au moins 2 exemples (input → output)\",\n"
                + "    \"reponseCorrecte\": \"Code solution complet, fonctionnel et commenté\",\n"
                + "    \"options\": []\n"
                + "  }\n"
                + "]\n\n"
                + "Règles strictes :\n"
                + "- libelle doit être un ÉNONCÉ DE CODAGE détaillé (jamais une question théorique)\n"
                + "- Inclure la signature attendue dans le libelle (ex: public static int somme(int[] arr))\n"
                + "- Inclure des exemples concrets : somme([1,2,3]) → 6\n"
                + "- reponseCorrecte contient seulement le code solution complet\n"
                + "- options est TOUJOURS []\n"
                + "- Varier les difficultés : débutant, intermédiaire, avancé";
        } else {
            // ── Prompt standard : questions théoriques / QCM ──────────────────
            prompt = "Tu es un assistant pédagogique expert en éducation supérieure.\n"
                + "Génère exactement " + nombre + " questions de type \"" + type + "\" sur le sujet \"" + sujet + "\" en français.\n\n"
                + "IMPORTANT : retourne UNIQUEMENT un tableau JSON valide, sans aucun texte avant ou après.\n"
                + "Format exact :\n"
                + "[\n"
                + "  {\n"
                + "    \"libelle\": \"Texte complet de la question ?\",\n"
                + "    \"reponseCorrecte\": \"La réponse correcte exacte\",\n"
                + "    \"options\": [\"Option A\", \"Option B\", \"Option C\", \"Option D\"]\n"
                + "  }\n"
                + "]\n\n"
                + "Règles strictes :\n"
                + "- \"QCM\" ou \"Quiz\" → options contient exactement 4 choix, l'un = reponseCorrecte\n"
                + "- \"Réponse unique\", \"Texte libre\", \"Test\" → options est []\n"
                + "- Questions pédagogiquement pertinentes et variées en difficulté\n"
                + "- Rédige en français académique clair";
        }

        String content = cleanJson(callOpenAI(prompt));
        if (content == null) {
            System.out.println("🔄 Fallback → SimulatedAIService");
            return fallback.genererQuestions(sujet, nombre, type, evaluationId);
        }
        return parseQuestionsJson(content, evaluationId);
    }


    private List<Question> parseQuestionsJson(String content, int evaluationId) {
        List<Question> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(content);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj   = arr.getJSONObject(i);
                String libelle   = obj.getString("libelle");
                String reponse   = obj.getString("reponseCorrecte");
                JSONArray opArr  = obj.optJSONArray("options");
                String[] options = new String[0];
                if (opArr != null && opArr.length() > 0) {
                    options = new String[opArr.length()];
                    for (int j = 0; j < opArr.length(); j++) options[j] = opArr.getString(j);
                }
                list.add(new Question(libelle, reponse, options, evaluationId));
            }
            System.out.println("✅ " + list.size() + " question(s) parsée(s) depuis OpenAI");
        } catch (Exception e) {
            System.err.println("❌ Parsing questions JSON échoué : " + e.getMessage());
            System.err.println("Contenu brut : " + content);
        }
        return list;
    }

    // ── 2. Correction automatique ─────────────────────────────────────────────

    @Override
    public CorrectionResult corrigerReponses(List<Question> questions,
                                             Map<Integer, String> reponses,
                                             String typeEvaluation,
                                             float bareme) {
        if (questions.isEmpty()) return new CorrectionResult(0, "Aucune question à corriger.");

        StringBuilder qa = new StringBuilder();
        for (Question q : questions) {
            String rep = reponses.getOrDefault(q.getId(), "(sans réponse)");
            qa.append(String.format(
                    "• Question : %s%n  Réponse correcte : %s%n  Réponse étudiant : %s%n%n",
                    q.getLibelle(), q.getReponseCorrecte(), rep));
        }

        String prompt = String.format("""
                Tu es un correcteur pédagogique bienveillant et rigoureux.
                Évalue les réponses d'un étudiant pour une évaluation de type "%s".
                Le barème total est de %.0f points (%d questions).

                Questions et réponses :
                %s

                Instructions :
                - Attribue une note sur %.0f points basée sur la pertinence des réponses
                - Génère un feedback global constructif en français (3-5 phrases)
                - Pour les QCM : une mauvaise réponse = 0 point pour cette question
                - Pour le texte libre : évalue la pertinence du contenu

                IMPORTANT : retourne UNIQUEMENT un objet JSON valide sans aucun texte avant ou après :
                {"note": X.X, "feedback": "Ton feedback détaillé ici"}
                """, typeEvaluation, bareme, questions.size(), qa, bareme);

        String content = cleanJson(callOpenAI(prompt));
        if (content == null) return fallback.corrigerReponses(questions, reponses, typeEvaluation, bareme);

        try {
            JSONObject obj = new JSONObject(content);
            float note     = (float) obj.getDouble("note");
            note = Math.max(0, Math.min(bareme, note)); // clamp
            String feedback = obj.getString("feedback");
            return new CorrectionResult(note, feedback);
        } catch (Exception e) {
            System.err.println("❌ Parsing correction JSON échoué : " + e.getMessage());
            return fallback.corrigerReponses(questions, reponses, typeEvaluation, bareme);
        }
    }

    // ── 3. Indice IA ──────────────────────────────────────────────────────────

    @Override
    public String demanderIndice(String libelle, String reponseCorrecte) {
        String prompt = String.format("""
                Tu es un assistant pédagogique. Génère un indice utile pour aider un étudiant
                à répondre à la question suivante SANS révéler la réponse directement.

                Question : %s
                Réponse correcte (à ne PAS révéler) : %s

                Retourne UNIQUEMENT l'indice en 1-2 phrases courtes et pédagogiques en français.
                Commence toujours par "💡 Indice :".
                """, libelle, reponseCorrecte);

        String content = callOpenAI(prompt);
        if (content == null) return fallback.demanderIndice(libelle, reponseCorrecte);
        return content.trim();
    }
}
