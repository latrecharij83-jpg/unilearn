package Services;

import entities.Rendu;
import utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RenduService {

    private Connection getConn() throws SQLException {
        Connection c = DatabaseConnection.getConnection();
        if (c == null) throw new SQLException("DB Connection is null – vérifiez que XAMPP est démarré.");
        return c;
    }

    /** Soumet les réponses d'un étudiant → crée un Rendu avec noteObtenue = -1.
     *  @return l'id du rendu inséré, ou -1 si erreur */
    public int soumettre(int evaluationId, int userId, String reponsesJson) {
        String req = "INSERT INTO rendu (contenu_texte, fichier_joint, note_obtenue, feedback_enseignant, evaluation_id, user_id) " +
                     "VALUES (?, NULL, -1, NULL, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(req, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, reponsesJson);
            ps.setInt(2, evaluationId);
            if (userId > 0) ps.setInt(3, userId);
            else            ps.setNull(3, Types.INTEGER);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (var keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int renduId = keys.getInt(1);
                        System.out.println("✅ Rendu soumis id=" + renduId + " pour évaluation #" + evaluationId);
                        return renduId;
                    }
                }
            }
            System.err.println("⚠️ Aucune ligne insérée pour rendu évaluation #" + evaluationId);
            return -1;
        } catch (SQLException ex) {
            System.err.println("❌ Erreur soumettre rendu: " + ex.getMessage());
            return -1;
        }
    }

    public boolean ajouter(Rendu r) {
        String req = "INSERT INTO rendu (contenu_texte, fichier_joint, note_obtenue, feedback_enseignant, evaluation_id, user_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(req)) {
            ps.setString(1, r.getContenuTexte());
            ps.setString(2, r.getFichierJoint());
            ps.setFloat(3, r.getNoteObtenue());
            ps.setString(4, r.getFeedbackEnseignant());
            ps.setInt(5, r.getEvaluationId());
            if (r.getUserId() > 0) ps.setInt(6, r.getUserId());
            else ps.setNull(6, Types.INTEGER);
            int rows = ps.executeUpdate();
            System.out.println(rows > 0 ? "✅ Rendu ajouté." : "⚠️ Aucune ligne insérée.");
            return rows > 0;
        } catch (SQLException ex) {
            System.err.println("❌ Erreur ajouter rendu: " + ex.getMessage());
            return false;
        }
    }

    public boolean noter(int renduId, float note, String feedback) {
        String req = "UPDATE rendu SET note_obtenue=?, feedback_enseignant=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(req)) {
            ps.setFloat(1, note);
            ps.setString(2, feedback);
            ps.setInt(3, renduId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Rendu noté id=" + renduId + " note=" + note);
                return true;
            }
            System.err.println("⚠️ Aucune ligne mise à jour pour rendu id=" + renduId);
            return false;
        } catch (SQLException ex) {
            System.err.println("❌ Erreur noter rendu: " + ex.getMessage());
            return false;
        }
    }

    public boolean supprimer(int id) {
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM rendu WHERE id=?")) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Rendu supprimé id=" + id);
                return true;
            }
            System.err.println("⚠️ Aucun rendu trouvé avec id=" + id);
            return false;
        } catch (SQLException ex) {
            System.err.println("❌ Erreur supprimer rendu: " + ex.getMessage());
            return false;
        }
    }

    public List<Rendu> afficherParEvaluation(int evaluationId) {
        List<Rendu> list = new ArrayList<>();
        String req = "SELECT * FROM rendu WHERE evaluation_id=? ORDER BY id DESC";
        try (PreparedStatement ps = getConn().prepareStatement(req)) {
            ps.setInt(1, evaluationId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Rendu(
                        rs.getInt("id"),
                        rs.getString("contenu_texte"),
                        rs.getString("fichier_joint"),
                        rs.getFloat("note_obtenue"),
                        rs.getString("feedback_enseignant"),
                        rs.getInt("evaluation_id"),
                        rs.getInt("user_id")
                ));
            }
        } catch (SQLException ex) {
            System.err.println("❌ Erreur afficher rendus: " + ex.getMessage());
        }
        return list;
    }
}
