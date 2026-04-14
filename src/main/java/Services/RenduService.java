package Services;

import entities.Rendu;
import utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RenduService {

    private Connection getConn() throws SQLException {
        Connection c = DatabaseConnection.getConnection();
        if (c == null) throw new SQLException("DB Connection is null");
        return c;
    }

    /** Soumet les réponses d'un étudiant → crée un Rendu avec noteObtenue = -1 */
    public void soumettre(int evaluationId, int userId, String reponsesJson) {
        String req = "INSERT INTO rendu (contenu_texte, fichier_joint, note_obtenue, feedback_enseignant, evaluation_id, user_id) " +
                     "VALUES (?, NULL, -1, NULL, ?, ?)";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(req)) {
            ps.setString(1, reponsesJson);
            ps.setInt(2, evaluationId);
            if (userId > 0) ps.setInt(3, userId);
            else            ps.setNull(3, Types.INTEGER);
            ps.executeUpdate();
            System.out.println("✅ Rendu soumis pour évaluation #" + evaluationId);
        } catch (SQLException ex) {
            System.err.println("Erreur soumettre rendu: " + ex.getMessage());
        }
    }

    public void ajouter(Rendu r) {
        String req = "INSERT INTO rendu (contenu_texte, fichier_joint, note_obtenue, feedback_enseignant, evaluation_id, user_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(req)) {
            ps.setString(1, r.getContenuTexte());
            ps.setString(2, r.getFichierJoint());
            ps.setFloat(3, r.getNoteObtenue());
            ps.setString(4, r.getFeedbackEnseignant());
            ps.setInt(5, r.getEvaluationId());
            if (r.getUserId() > 0) ps.setInt(6, r.getUserId());
            else ps.setNull(6, Types.INTEGER);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Erreur ajouter rendu: " + ex.getMessage());
        }
    }

    public void noter(int renduId, float note, String feedback) {
        String req = "UPDATE rendu SET note_obtenue=?, feedback_enseignant=? WHERE id=?";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(req)) {
            ps.setFloat(1, note);
            ps.setString(2, feedback);
            ps.setInt(3, renduId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Erreur noter rendu: " + ex.getMessage());
        }
    }

    public void supprimer(int id) {
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement("DELETE FROM rendu WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Erreur supprimer rendu: " + ex.getMessage());
        }
    }

    public List<Rendu> afficherParEvaluation(int evaluationId) {
        List<Rendu> list = new ArrayList<>();
        String req = "SELECT * FROM rendu WHERE evaluation_id=? ORDER BY id DESC";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(req)) {
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
            System.err.println("Erreur afficher rendus: " + ex.getMessage());
        }
        return list;
    }
}
