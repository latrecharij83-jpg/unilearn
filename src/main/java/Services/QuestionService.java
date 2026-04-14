package Services;

import entities.Question;
import utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QuestionService {

    private String colReponse = null;
    private String colOptions = null;
    private String colEvalId  = null;
    private boolean scanned   = false;

    private Connection getConn() throws SQLException {
        Connection c = DatabaseConnection.getConnection();
        if (c == null) throw new SQLException("DB Connection is null");
        return c;
    }

    /** Détecte les vrais noms de colonnes via SHOW COLUMNS (scopé à la BD active) */
    private void scanColumns() {
        if (scanned) return;
        scanned = true;
        Set<String> cols = new HashSet<>();
        try (Connection c = getConn();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SHOW COLUMNS FROM question")) {
            while (rs.next()) {
                cols.add(rs.getString("Field").toLowerCase());
            }
        } catch (SQLException ex) {
            System.err.println("Erreur scan colonnes question: " + ex.getMessage());
        }

        System.out.println("📋 Colonnes réelles de la table question : " + cols);

        // Détection réponse correcte
        if      (cols.contains("reponse_correcte"))  colReponse = "reponse_correcte";
        else if (cols.contains("reponsecorrecte"))    colReponse = "reponseCorrecte";
        else if (cols.contains("correct_answer"))     colReponse = "correct_answer";
        else if (cols.contains("reponse"))            colReponse = "reponse";
        else if (cols.contains("answer"))             colReponse = "answer";
        else colReponse = cols.stream()
                .filter(c2 -> c2.contains("repon") || c2.contains("answer") || c2.contains("correct"))
                .findFirst().orElse("reponse_correcte");

        // Détection options
        if      (cols.contains("options"))  colOptions = "options";
        else if (cols.contains("choices"))  colOptions = "choices";
        else                                colOptions = "options";

        // Détection evaluation_id
        if      (cols.contains("evaluation_id"))  colEvalId = "evaluation_id";
        else if (cols.contains("evaluationid"))   colEvalId = "evaluationId";
        else                                      colEvalId = "evaluation_id";

        System.out.println("✅ Colonnes mappées → reponse='" + colReponse
                + "', options='" + colOptions + "', evalId='" + colEvalId + "'");
    }

    // ── INSERT ────────────────────────────────────────────────────────────────
    public void ajouter(Question q) {
        scanColumns();
        String req = "INSERT INTO question (libelle, " + colReponse + ", " + colOptions + ", " + colEvalId + ") " +
                     "VALUES (?, ?, ?, ?)";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(req)) {
            ps.setString(1, q.getLibelle());
            ps.setString(2, q.getReponseCorrecte());

            // Options : NULL si vide, sinon JSON valide ["opt1","opt2",...]
            String optJson = q.getOptionsJson();
            if (optJson == null) ps.setNull(3, Types.VARCHAR);
            else                 ps.setString(3, optJson);

            ps.setInt(4, q.getEvaluationId());
            ps.executeUpdate();
            System.out.println("✅ Question ajoutée.");
        } catch (SQLException ex) {
            System.err.println("Erreur ajouter question: " + ex.getMessage());
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────
    public void modifier(Question q) {
        scanColumns();
        String req = "UPDATE question SET libelle=?, " + colReponse + "=?, " + colOptions + "=? WHERE id=?";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(req)) {
            ps.setString(1, q.getLibelle());
            ps.setString(2, q.getReponseCorrecte());

            String optJson = q.getOptionsJson();
            if (optJson == null) ps.setNull(3, Types.VARCHAR);
            else                 ps.setString(3, optJson);

            ps.setInt(4, q.getId());
            ps.executeUpdate();
            System.out.println("✅ Question modifiée.");
        } catch (SQLException ex) {
            System.err.println("Erreur modifier question: " + ex.getMessage());
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────────
    public void supprimer(int id) {
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement("DELETE FROM question WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Erreur supprimer question: " + ex.getMessage());
        }
    }

    // ── SELECT ────────────────────────────────────────────────────────────────
    public List<Question> afficherParEvaluation(int evaluationId) {
        scanColumns();
        List<Question> list = new ArrayList<>();
        String req = "SELECT * FROM question WHERE " + colEvalId + "=? ORDER BY id";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(req)) {
            ps.setInt(1, evaluationId);
            ResultSet rs = ps.executeQuery();

            // Lire les noms de colonnes réels du ResultSet
            ResultSetMetaData meta = rs.getMetaData();
            Set<String> rsCols = new HashSet<>();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                rsCols.add(meta.getColumnName(i).toLowerCase());
            }

            while (rs.next()) {
                // Lecture réponse correcte
                String rep = safeGet(rs, rsCols,
                        "reponse_correcte", "reponsecorrecte", "correct_answer", "reponse", "answer");

                // Lecture options (JSON → tableau)
                String rawOpts = safeGet(rs, rsCols, "options", "choices");
                String[] opts = Question.parseOptions(rawOpts);

                list.add(new Question(
                        rs.getInt("id"),
                        rs.getString("libelle"),
                        rep != null ? rep : "",
                        opts,
                        evaluationId
                ));
            }
        } catch (SQLException ex) {
            System.err.println("Erreur afficher questions: " + ex.getMessage());
        }
        return list;
    }

    /** Essaie plusieurs noms de colonnes et retourne la première valeur trouvée */
    private String safeGet(ResultSet rs, Set<String> cols, String... names) throws SQLException {
        for (String name : names) {
            if (cols.contains(name.toLowerCase())) return rs.getString(name);
        }
        return null;
    }
}
