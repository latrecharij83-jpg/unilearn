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
        if (c == null) throw new SQLException("DB Connection is null – vérifiez que XAMPP est démarré.");
        return c;
    }

    /** Détecte les vrais noms de colonnes via SHOW COLUMNS (scopé à la BD active) */
    private void scanColumns() {
        if (scanned) return;
        scanned = true;
        Set<String> cols = new HashSet<>();
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery("SHOW COLUMNS FROM question")) {
            while (rs.next()) {
                cols.add(rs.getString("Field").toLowerCase());
            }
        } catch (SQLException ex) {
            System.err.println("❌ Erreur scan colonnes question: " + ex.getMessage());
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
    public boolean ajouter(Question q) {
        scanColumns();
        String req = "INSERT INTO question (libelle, " + colReponse + ", " + colOptions + ", " + colEvalId + ") " +
                     "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(req)) {
            ps.setString(1, q.getLibelle());
            ps.setString(2, q.getReponseCorrecte());

            String optJson = q.getOptionsJson();
            if (optJson == null) ps.setNull(3, Types.VARCHAR);
            else                 ps.setString(3, optJson);

            ps.setInt(4, q.getEvaluationId());
            int rows = ps.executeUpdate();
            System.out.println(rows > 0 ? "✅ Question ajoutée." : "⚠️ Aucune ligne insérée pour question.");
            return rows > 0;
        } catch (SQLException ex) {
            System.err.println("❌ Erreur ajouter question: " + ex.getMessage());
            return false;
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────
    public boolean modifier(Question q) {
        scanColumns();
        String req = "UPDATE question SET libelle=?, " + colReponse + "=?, " + colOptions + "=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(req)) {
            ps.setString(1, q.getLibelle());
            ps.setString(2, q.getReponseCorrecte());

            String optJson = q.getOptionsJson();
            if (optJson == null) ps.setNull(3, Types.VARCHAR);
            else                 ps.setString(3, optJson);

            ps.setInt(4, q.getId());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Question modifiée id=" + q.getId());
                return true;
            }
            System.err.println("⚠️ Aucune ligne mise à jour pour question id=" + q.getId());
            return false;
        } catch (SQLException ex) {
            System.err.println("❌ Erreur modifier question: " + ex.getMessage());
            return false;
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────────
    public boolean supprimer(int id) {
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM question WHERE id=?")) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Question supprimée id=" + id);
                return true;
            }
            System.err.println("⚠️ Aucune question trouvée avec id=" + id);
            return false;
        } catch (SQLException ex) {
            System.err.println("❌ Erreur supprimer question: " + ex.getMessage());
            return false;
        }
    }

    // ── SELECT ────────────────────────────────────────────────────────────────
    public List<Question> afficherParEvaluation(int evaluationId) {
        scanColumns();
        List<Question> list = new ArrayList<>();
        String req = "SELECT * FROM question WHERE " + colEvalId + "=? ORDER BY id";
        try (PreparedStatement ps = getConn().prepareStatement(req)) {
            ps.setInt(1, evaluationId);
            ResultSet rs = ps.executeQuery();

            ResultSetMetaData meta = rs.getMetaData();
            Set<String> rsCols = new HashSet<>();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                rsCols.add(meta.getColumnName(i).toLowerCase());
            }

            while (rs.next()) {
                String rep = safeGet(rs, rsCols,
                        "reponse_correcte", "reponsecorrecte", "correct_answer", "reponse", "answer");
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
            System.err.println("❌ Erreur afficher questions: " + ex.getMessage());
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
