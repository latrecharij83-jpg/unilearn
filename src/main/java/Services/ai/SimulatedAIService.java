package Services.ai;

import entities.Question;

import java.util.*;

/**
 * Implémentation de secours (fallback) de AIService.
 * Fournit des questions réalistes et des exercices de codage par thème.
 */
public class SimulatedAIService implements AIService {

    private static final Map<String, List<String[]>> QUESTION_BANK  = new LinkedHashMap<>();
    private static final List<String[]>              CODE_EXERCISES  = new ArrayList<>();

    static {
        // ── Banque QCM par thème ──────────────────────────────────────────────
        QUESTION_BANK.put("java", Arrays.asList(
            new String[]{"Qu'est-ce que la JVM ?", "Java Virtual Machine : environnement d'exécution du bytecode Java",
                "Java Virtual Machine : environnement d'exécution du bytecode Java", "Java Version Manager : gestionnaire de versions", "Just Very Modern : outil de build", "Java Validation Module : outil de test"},
            new String[]{"Quel modificateur empêche une classe d'être héritée ?", "final",
                "final", "static", "abstract", "private"},
            new String[]{"Que signifie POO ?", "Programmation Orientée Objet",
                "Programmation Orientée Objet", "Protocole Orienté Opération", "Processus d'Organisation Optimale", "Plateforme d'Objets Ouverts"},
            new String[]{"Quelle est la méthode principale d'un programme Java ?", "public static void main(String[] args)",
                "public static void main(String[] args)", "void start()", "public void run()", "static int main()"},
            new String[]{"Qu'est-ce que l'encapsulation en Java ?", "Cacher les détails internes d'une classe et exposer uniquement ce qui est nécessaire",
                "Cacher les détails internes d'une classe et exposer uniquement ce qui est nécessaire", "Copier une classe", "Lier des classes entre elles", "Créer des interfaces"}
        ));

        QUESTION_BANK.put("heritage", Arrays.asList(
            new String[]{"Quel mot-clé est utilisé pour hériter d'une classe en Java ?", "extends",
                "extends", "implements", "inherits", "super"},
            new String[]{"Comment appeler le constructeur de la classe parente ?", "super()",
                "super()", "parent()", "base()", "this.parent()"},
            new String[]{"Une classe abstraite peut-elle être instanciée directement ?", "Non",
                "Non", "Oui", "Seulement si elle a un constructeur", "Uniquement avec new"},
            new String[]{"Que signifie le polymorphisme ?", "La capacité d'un objet à prendre plusieurs formes selon le contexte",
                "La capacité d'un objet à prendre plusieurs formes selon le contexte", "L'héritage multiple", "La duplication de code", "L'abstraction complète"}
        ));

        QUESTION_BANK.put("sql", Arrays.asList(
            new String[]{"Que signifie SQL ?", "Structured Query Language",
                "Structured Query Language", "Simple Query Logic", "Sequential Query Layer", "Standard Query Loop"},
            new String[]{"Quel mot-clé permet de sélectionner des données ?", "SELECT",
                "SELECT", "GET", "FETCH", "READ"},
            new String[]{"Quel mot-clé est utilisé pour filtrer les résultats ?", "WHERE",
                "WHERE", "FILTER", "HAVING", "FROM"},
            new String[]{"Quelle contrainte garantit l'unicité d'une colonne ?", "UNIQUE",
                "UNIQUE", "NOT NULL", "PRIMARY", "INDEX"},
            new String[]{"Qu'est-ce qu'une clé étrangère ?", "Une contrainte qui référence la clé primaire d'une autre table",
                "Une contrainte qui référence la clé primaire d'une autre table", "Un index secondaire", "Une clé dupliquée", "Un champ optionnel"},
            new String[]{"Quel mot-clé permet de joindre deux tables ?", "JOIN",
                "JOIN", "MERGE", "LINK", "CONNECT"}
        ));

        QUESTION_BANK.put("spring", Arrays.asList(
            new String[]{"Que signifie l'injection de dépendances ?", "Fournir les dépendances d'un objet de l'extérieur plutôt qu'il les crée lui-même",
                "Fournir les dépendances d'un objet de l'extérieur plutôt qu'il les crée lui-même", "Inclure des bibliothèques", "Hériter d'une classe", "Créer des instances manuellement"},
            new String[]{"Quelle annotation marque un bean Spring ?", "@Component",
                "@Component", "@Entity", "@Table", "@Column"},
            new String[]{"À quoi sert @RestController ?", "Combiner @Controller et @ResponseBody pour créer des API REST",
                "Combiner @Controller et @ResponseBody pour créer des API REST", "Seulement afficher des vues", "Gérer les sessions", "Configurer la base de données"},
            new String[]{"Quelle annotation configure Spring Boot automatiquement ?", "@SpringBootApplication",
                "@SpringBootApplication", "@AutoConfig", "@EnableAll", "@MainApp"}
        ));

        QUESTION_BANK.put("python", Arrays.asList(
            new String[]{"Python est-il un langage typé statiquement ?", "Non, Python est typé dynamiquement",
                "Non, Python est typé dynamiquement", "Oui, toujours", "Cela dépend de la version", "Uniquement avec annotations"},
            new String[]{"Quel opérateur est utilisé pour l'exponentiation en Python ?", "**",
                "**", "^", "^^", "exp()"},
            new String[]{"Quelle méthode ajoute un élément à une liste Python ?", "append()",
                "append()", "add()", "push()", "insert_end()"},
            new String[]{"Comment créer un dictionnaire vide en Python ?", "{}",
                "{}", "[]", "()", "dict[]"}
        ));

        QUESTION_BANK.put("algorithme", Arrays.asList(
            new String[]{"Quelle est la complexité du tri rapide (QuickSort) en moyenne ?", "O(n log n)",
                "O(n log n)", "O(n²)", "O(n)", "O(log n)"},
            new String[]{"Qu'est-ce qu'une pile (stack) ?", "Structure LIFO : le dernier élément entré est le premier à sortir",
                "Structure LIFO : le dernier élément entré est le premier à sortir", "Structure FIFO", "Un tableau dynamique", "Un arbre binaire"},
            new String[]{"Qu'est-ce qu'une file (queue) ?", "Structure FIFO : le premier entré est le premier sorti",
                "Structure FIFO : le premier entré est le premier sorti", "Structure LIFO", "Un graphe orienté", "Un tableau trié"},
            new String[]{"Que signifie la récursivité ?", "Une fonction qui s'appelle elle-même avec un cas de base pour arrêter",
                "Une fonction qui s'appelle elle-même avec un cas de base pour arrêter", "Une boucle infinie", "Un algorithme de tri", "Un type de liste"}
        ));

        QUESTION_BANK.put("reseau", Arrays.asList(
            new String[]{"Que signifie HTTP ?", "HyperText Transfer Protocol",
                "HyperText Transfer Protocol", "High Transfer Technical Protocol", "Host Type Transfer Protocol", "HyperText Transmission Process"},
            new String[]{"Quel code HTTP signifie 'Not Found' ?", "404",
                "404", "500", "200", "301"},
            new String[]{"Qu'est-ce qu'une API REST ?", "Une interface de programmation respectant les contraintes architecturales REST",
                "Une interface de programmation respectant les contraintes architecturales REST", "Un protocole réseau", "Un langage de programmation", "Une base de données"},
            new String[]{"Quel protocole sécurise HTTP ?", "TLS/SSL (HTTPS)",
                "TLS/SSL (HTTPS)", "FTP", "SMTP", "UDP"}
        ));

        // ── Banque d'exercices de CODAGE ─────────────────────────────────────
        CODE_EXERCISES.addAll(Arrays.asList(
            new String[]{
                "Exercice – Somme d'un tableau\n\n"
                + "Écrivez une méthode Java qui calcule la somme de tous les éléments d'un tableau d'entiers.\n\n"
                + "Signature attendue :\n"
                + "    public static int somme(int[] tableau)\n\n"
                + "Exemples :\n"
                + "  somme(new int[]{1, 2, 3, 4, 5}) → 15\n"
                + "  somme(new int[]{10, -5, 3})      → 8\n"
                + "  somme(new int[]{})               → 0\n\n"
                + "Contrainte : n'utilisez pas Arrays.stream() ni de bibliothèques.",
                "public static int somme(int[] tableau) {\n"
                + "    int total = 0;\n"
                + "    for (int n : tableau) {\n"
                + "        total += n;\n"
                + "    }\n"
                + "    return total;\n"
                + "}"
            },
            new String[]{
                "Exercice – Palindrome\n\n"
                + "Implémentez une méthode Java qui vérifie si une chaîne est un palindrome "
                + "(se lit identiquement dans les deux sens). La comparaison doit ignorer la casse.\n\n"
                + "Signature attendue :\n"
                + "    public static boolean estPalindrome(String s)\n\n"
                + "Exemples :\n"
                + "  estPalindrome(\"radar\")  → true\n"
                + "  estPalindrome(\"Java\")   → false\n"
                + "  estPalindrome(\"Kayak\")  → true\n"
                + "  estPalindrome(\"\")       → true",
                "public static boolean estPalindrome(String s) {\n"
                + "    if (s == null) return false;\n"
                + "    String lower = s.toLowerCase();\n"
                + "    int debut = 0, fin = lower.length() - 1;\n"
                + "    while (debut < fin) {\n"
                + "        if (lower.charAt(debut) != lower.charAt(fin)) return false;\n"
                + "        debut++; fin--;\n"
                + "    }\n"
                + "    return true;\n"
                + "}"
            },
            new String[]{
                "Exercice – Factorielle\n\n"
                + "Écrivez une méthode Java qui calcule la factorielle d'un entier n (n!).\n"
                + "Gérez le cas n = 0 et refusez les valeurs négatives.\n\n"
                + "Signature attendue :\n"
                + "    public static long factorielle(int n)\n\n"
                + "Exemples :\n"
                + "  factorielle(0)  → 1\n"
                + "  factorielle(5)  → 120\n"
                + "  factorielle(10) → 3628800\n"
                + "  factorielle(-1) → IllegalArgumentException",
                "public static long factorielle(int n) {\n"
                + "    if (n < 0) throw new IllegalArgumentException(\"n doit être >= 0\");\n"
                + "    long result = 1;\n"
                + "    for (int i = 2; i <= n; i++) result *= i;\n"
                + "    return result;\n"
                + "}"
            },
            new String[]{
                "Exercice – Tri à bulles (Bubble Sort)\n\n"
                + "Implémentez le tri à bulles en Java. L'algorithme doit trier un tableau d'entiers "
                + "en ordre croissant, en place (sans créer de nouveau tableau).\n\n"
                + "Signature attendue :\n"
                + "    public static void bulleSort(int[] arr)\n\n"
                + "Exemples :\n"
                + "  [5, 2, 8, 1, 9] → [1, 2, 5, 8, 9]\n"
                + "  [3, 1, 4, 1, 5] → [1, 1, 3, 4, 5]\n\n"
                + "Rappel : comparez chaque paire d'éléments adjacents et échangez si nécessaire.",
                "public static void bulleSort(int[] arr) {\n"
                + "    int n = arr.length;\n"
                + "    for (int i = 0; i < n - 1; i++) {\n"
                + "        for (int j = 0; j < n - i - 1; j++) {\n"
                + "            if (arr[j] > arr[j + 1]) {\n"
                + "                int temp = arr[j];\n"
                + "                arr[j] = arr[j + 1];\n"
                + "                arr[j + 1] = temp;\n"
                + "            }\n"
                + "        }\n"
                + "    }\n"
                + "}"
            },
            new String[]{
                "Exercice – Recherche binaire\n\n"
                + "Implémentez la recherche binaire dans un tableau trié en ordre croissant. "
                + "La méthode retourne l'index de l'élément trouvé, ou -1 s'il est absent.\n\n"
                + "Signature attendue :\n"
                + "    public static int rechercherBinaire(int[] tableau, int cible)\n\n"
                + "Exemples :\n"
                + "  tableau = {1,3,5,7,9}, cible = 5 → 2\n"
                + "  tableau = {1,3,5,7,9}, cible = 4 → -1\n\n"
                + "Contrainte : complexité O(log n).",
                "public static int rechercherBinaire(int[] tableau, int cible) {\n"
                + "    int bas = 0, haut = tableau.length - 1;\n"
                + "    while (bas <= haut) {\n"
                + "        int milieu = bas + (haut - bas) / 2;\n"
                + "        if (tableau[milieu] == cible) return milieu;\n"
                + "        else if (tableau[milieu] < cible) bas = milieu + 1;\n"
                + "        else haut = milieu - 1;\n"
                + "    }\n"
                + "    return -1;\n"
                + "}"
            },
            new String[]{
                "Exercice – Classe CompteBancaire\n\n"
                + "Créez une classe Java CompteBancaire avec :\n"
                + "  - Un attribut privé solde (double)\n"
                + "  - Un constructeur prenant le solde initial\n"
                + "  - void deposer(double montant)  → ajoute au solde (montant > 0 requis)\n"
                + "  - void retirer(double montant)  → retire si solde suffisant, sinon exception\n"
                + "  - double getSolde()             → retourne le solde courant\n\n"
                + "Exemples :\n"
                + "  CompteBancaire c = new CompteBancaire(100);\n"
                + "  c.deposer(50);   // solde = 150.0\n"
                + "  c.retirer(200);  // IllegalStateException : solde insuffisant",
                "public class CompteBancaire {\n"
                + "    private double solde;\n\n"
                + "    public CompteBancaire(double solde) {\n"
                + "        this.solde = solde;\n"
                + "    }\n\n"
                + "    public void deposer(double montant) {\n"
                + "        if (montant <= 0) throw new IllegalArgumentException(\"Montant invalide\");\n"
                + "        solde += montant;\n"
                + "    }\n\n"
                + "    public void retirer(double montant) {\n"
                + "        if (montant > solde) throw new IllegalStateException(\"Solde insuffisant\");\n"
                + "        solde -= montant;\n"
                + "    }\n\n"
                + "    public double getSolde() { return solde; }\n"
                + "}"
            },
            new String[]{
                "Exercice – Inverser une chaîne\n\n"
                + "Écrivez une méthode Java qui inverse une chaîne de caractères "
                + "sans utiliser StringBuilder.reverse() ou Collections.\n\n"
                + "Signature attendue :\n"
                + "    public static String inverser(String s)\n\n"
                + "Exemples :\n"
                + "  inverser(\"hello\") → \"olleh\"\n"
                + "  inverser(\"Java\")  → \"avaJ\"\n"
                + "  inverser(\"\")      → \"\"\n"
                + "  inverser(null)    → \"\"",
                "public static String inverser(String s) {\n"
                + "    if (s == null || s.isEmpty()) return \"\";\n"
                + "    char[] chars = s.toCharArray();\n"
                + "    int debut = 0, fin = chars.length - 1;\n"
                + "    while (debut < fin) {\n"
                + "        char tmp = chars[debut];\n"
                + "        chars[debut++] = chars[fin];\n"
                + "        chars[fin--] = tmp;\n"
                + "    }\n"
                + "    return new String(chars);\n"
                + "}"
            },
            new String[]{
                "Exercice – FizzBuzz\n\n"
                + "Écrivez une méthode Java qui retourne une liste de chaînes pour les entiers "
                + "de 1 à n selon les règles :\n"
                + "  - Multiple de 3 → \"Fizz\"\n"
                + "  - Multiple de 5 → \"Buzz\"\n"
                + "  - Multiple de 3 ET 5 → \"FizzBuzz\"\n"
                + "  - Sinon → le nombre sous forme de chaîne\n\n"
                + "Signature attendue :\n"
                + "    public static List<String> fizzBuzz(int n)\n\n"
                + "Exemple (n=5) → [\"1\", \"2\", \"Fizz\", \"4\", \"Buzz\"]",
                "public static List<String> fizzBuzz(int n) {\n"
                + "    List<String> result = new ArrayList<>();\n"
                + "    for (int i = 1; i <= n; i++) {\n"
                + "        if (i % 15 == 0)     result.add(\"FizzBuzz\");\n"
                + "        else if (i % 3 == 0) result.add(\"Fizz\");\n"
                + "        else if (i % 5 == 0) result.add(\"Buzz\");\n"
                + "        else                 result.add(String.valueOf(i));\n"
                + "    }\n"
                + "    return result;\n"
                + "}"
            }
        ));
    }

    // ── 1. Génération de questions ─────────────────────────────────────────────

    @Override
    public List<Question> genererQuestions(String sujet, int nombre, String type, int evaluationId) {
        System.out.println("🤖 [SimulatedAI] Génération " + nombre + " question(s) – sujet: " + sujet + " type: " + type);

        List<String[]> pool;

        if ("Code".equalsIgnoreCase(type)) {
            // Utiliser la banque d'exercices de codage
            pool = new ArrayList<>(CODE_EXERCISES);
        } else {
            pool = findQuestions(sujet.toLowerCase());
        }

        Collections.shuffle(pool);

        List<Question> result = new ArrayList<>();
        for (String[] q : pool) {
            if (result.size() >= nombre) break;
            Question question = buildQuestion(q, type, evaluationId);
            if (question != null) result.add(question);
        }

        // Compléter si insuffisant
        while (result.size() < nombre) {
            result.add(buildGenericQuestion(result.size() + 1, sujet, type, evaluationId));
        }

        System.out.println("✅ [SimulatedAI] " + result.size() + " question(s) générée(s)");
        return result;
    }

    private List<String[]> findQuestions(String sujet) {
        List<String[]> pool = new ArrayList<>();
        for (Map.Entry<String, List<String[]>> entry : QUESTION_BANK.entrySet()) {
            if (sujet.contains(entry.getKey()) || entry.getKey().contains(sujet.split(" ")[0])) {
                pool.addAll(entry.getValue());
            }
        }
        if (pool.isEmpty()) QUESTION_BANK.values().forEach(pool::addAll);
        return new ArrayList<>(pool);
    }

    private Question buildQuestion(String[] data, String type, int evaluationId) {
        String libelle = data[0];
        String reponse = data[1];
        String[] options;

        if ("Code".equalsIgnoreCase(type)) {
            // Exercice de code : jamais d'options QCM
            options = new String[0];
        } else if (("QCM".equalsIgnoreCase(type) || "Quiz".equalsIgnoreCase(type)) && data.length > 2) {
            options = Arrays.copyOfRange(data, 2, Math.min(data.length, 6));
        } else if (("QCM".equalsIgnoreCase(type) || "Quiz".equalsIgnoreCase(type))) {
            options = new String[]{reponse, "Réponse B (simulée)", "Réponse C (simulée)", "Réponse D (simulée)"};
        } else {
            options = new String[0];
        }
        return new Question(libelle, reponse, options, evaluationId);
    }

    private Question buildGenericQuestion(int num, String sujet, String type, int evaluationId) {
        String libelle, reponse;
        if ("Code".equalsIgnoreCase(type)) {
            libelle = "Exercice " + num + " – " + sujet + "\n\n"
                    + "Écrivez une méthode Java qui effectue une opération liée à : " + sujet + ".\n\n"
                    + "Signature attendue :\n"
                    + "    public static void exercice" + num + "()\n\n"
                    + "Décrivez l'algorithme, les entrées, les sorties et fournissez au moins un exemple.";
            reponse = "// Solution à implémenter\npublic static void exercice" + num + "() {\n    // TODO\n}";
        } else {
            libelle = "Question " + num + " sur le thème '" + sujet + "' : expliquez ce concept.";
            reponse = "La réponse correcte concerne " + sujet + " (générée par simulation).";
        }
        String[] opts = ("QCM".equalsIgnoreCase(type) || "Quiz".equalsIgnoreCase(type))
                ? new String[]{reponse, "Option incorrecte A", "Option incorrecte B", "Option incorrecte C"}
                : new String[0];
        return new Question(libelle, reponse, opts, evaluationId);
    }

    // ── 2. Correction automatique ──────────────────────────────────────────────

    @Override
    public CorrectionResult corrigerReponses(List<Question> questions,
                                             Map<Integer, String> reponses,
                                             String typeEvaluation,
                                             float bareme) {
        if (questions.isEmpty()) return new CorrectionResult(0, "Aucune question à corriger.");

        int total  = questions.size();
        int bonnes = 0;
        StringBuilder sb = new StringBuilder("━━━ Résultats de l'évaluation ━━━\n\n");

        for (Question q : questions) {
            String repEtudiant = reponses.getOrDefault(q.getId(), "").trim();
            String repCorrecte = q.getReponseCorrecte().trim();

            boolean correct = repEtudiant.equalsIgnoreCase(repCorrecte)
                    || (repCorrecte.toLowerCase().contains(repEtudiant.toLowerCase()) && repEtudiant.length() > 3);

            if (correct) {
                bonnes++;
                sb.append("✅ ").append(q.getLibelle().split("\n")[0]).append("\n")
                  .append("   Votre réponse : ").append(repEtudiant.length() > 80 ? repEtudiant.substring(0, 80) + "..." : repEtudiant).append("\n\n");
            } else {
                sb.append("❌ ").append(q.getLibelle().split("\n")[0]).append("\n")
                  .append("   Votre réponse : ").append(repEtudiant.isBlank() ? "(sans réponse)" :
                        (repEtudiant.length() > 80 ? repEtudiant.substring(0, 80) + "..." : repEtudiant)).append("\n")
                  .append("   Réponse attendue : ").append(repCorrecte.length() > 100 ? repCorrecte.substring(0, 100) + "..." : repCorrecte).append("\n\n");
            }
        }

        float note = Math.round((bareme / total) * bonnes * 10f) / 10f;

        double pct = (double) bonnes / total;
        String appreciation = pct >= 0.8 ? "🌟 Excellent travail ! Continuez ainsi."
                : pct >= 0.6 ? "👍 Bon résultat. Quelques points à revoir."
                : pct >= 0.4 ? "📚 Résultat passable. Révisez les notions manquantes."
                : "💪 Des efforts sont nécessaires. N'hésitez pas à retravailler le cours.";

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
          .append(String.format("Score : %d/%d question(s) correctes%n", bonnes, total))
          .append(appreciation);

        return new CorrectionResult(note, sb.toString());
    }

    // ── 3. Indice contextuel ───────────────────────────────────────────────────

    @Override
    public String demanderIndice(String libelle, String reponseCorrecte) {
        String q = libelle.toLowerCase();

        // Détection du domaine
        String domaine = "informatique";
        if (q.contains("java") || q.contains("jvm") || q.contains("classe") ||
            q.contains("méthode") || q.contains("objet") || q.contains("héritage") ||
            q.contains("interface") || q.contains("polymorphisme") || q.contains("encapsulation"))
            domaine = "Java / Programmation Orientée Objet";
        else if (q.contains("sql") || q.contains("base de donn") || q.contains("table") ||
                 q.contains("requête") || q.contains("jointure") || q.contains("clé"))
            domaine = "SQL / Bases de données";
        else if (q.contains("spring") || q.contains("bean") || q.contains("rest") || q.contains("injection"))
            domaine = "Spring Framework";
        else if (q.contains("python") || q.contains("liste") || q.contains("dictionnaire"))
            domaine = "Python";
        else if (q.contains("algorithme") || q.contains("complexit") || q.contains("tri") ||
                 q.contains("pile") || q.contains("file") || q.contains("arbre"))
            domaine = "Algorithmique / Structures de données";
        else if (q.contains("http") || q.contains("réseau") || q.contains("protocole"))
            domaine = "Réseaux / Web";

        // Indice pour exercice de code
        if (q.contains("exercice") || q.contains("implémentez") || q.contains("écrivez une méthode") ||
            q.contains("écrivez une classe") || q.contains("signature attendue")) {
            String premierMot = reponseCorrecte.contains(" ")
                    ? reponseCorrecte.substring(0, reponseCorrecte.indexOf(" ")) : reponseCorrecte.substring(0, Math.min(4, reponseCorrecte.length()));
            return "💡 Indice : Commencez par définir la signature de la méthode et identifiez les cas limites. "
                 + "Pensez au cas de base (tableau vide, null, n=0...) avant d'implémenter la logique principale. "
                 + "La solution commence par : « " + premierMot + "... »";
        }

        // Génération de l'indice basé sur le type de question théorique
        String premierMot = reponseCorrecte.contains(" ")
                ? reponseCorrecte.substring(0, reponseCorrecte.indexOf(" "))
                : reponseCorrecte;

        String hint;
        if (q.startsWith("qu'est-ce que") || q.startsWith("c'est quoi") ||
            q.startsWith("définissez") || q.startsWith("définir")) {
            hint = "C'est un concept fondamental de " + domaine + ". " +
                   "La réponse commence par « " + premierMot + " » et décrit un rôle ou un mécanisme précis.";
        } else if (q.contains("mot-clé") || q.contains("keyword") || q.contains("annotation") || q.contains("modificateur")) {
            hint = "C'est un mot réservé ou une annotation du langage " + domaine + ". " +
                   "Il contient " + reponseCorrecte.length() + " caractère(s) et commence par « " +
                   reponseCorrecte.substring(0, 1) + " ».";
        } else if (q.contains("complexit") || q.contains("o(n") || q.contains("o(log")) {
            hint = "Référez-vous à la notation Big-O. Pensez au nombre d'opérations effectuées dans le cas moyen.";
        } else if (q.startsWith("quel") || q.startsWith("quelle")) {
            hint = reponseCorrecte.length() <= 6
                ? "La réponse est un terme court (" + reponseCorrecte.length() + " caractères) appartenant au vocabulaire de " + domaine + "."
                : "En " + domaine + ", la réponse commence par « " + premierMot + " ». Pensez aux éléments vus en cours.";
        } else if (q.startsWith("comment") || q.startsWith("expliquez")) {
            hint = "Réfléchissez au processus ou à la procédure en " + domaine + ". La réponse décrit une étape spécifique.";
        } else if (q.startsWith("pourquoi")) {
            hint = "Cette question porte sur l'utilité d'un concept en " + domaine + ". Pensez aux avantages ou problèmes que ce mécanisme résout.";
        } else if (q.contains("vrai") || q.contains("faux") || q.contains("true") || q.contains("false")) {
            hint = "Réfléchissez bien à l'affirmation. En " + domaine + ", les exceptions à la règle générale sont fréquentes.";
        } else {
            String debut = reponseCorrecte.length() > 3 ? reponseCorrecte.substring(0, 3) + "..." : reponseCorrecte;
            hint = "Cette question porte sur " + domaine + ". La réponse débute par « " + debut + " » — revoyez le cours correspondant.";
        }

        return "💡 Indice : " + hint;
    }
}
