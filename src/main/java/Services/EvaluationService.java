package Services;

import entities.Evaluation;
import utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD pour la table evaluation :
 *   id, titre, description, type, date_limite, bareme, quizz_data
 */
public class EvaluationService {

    private Connection getConn() throws SQLException {
        Connection c = DatabaseConnection.getConnection();
        if (c == null) throw new SQLException("DB Connection is null");
        return c;
    }

    public boolean ajouter(Evaluation e) {
        String req =
            "INSERT INTO evaluation (titre, description, type, date_limite, bareme, quizz_data) " +
            "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(req)) {
            ps.setString(1, e.getTitre());
            ps.setString(2, nvl(e.getDescription()));
            ps.setString(3, e.getType());
            ps.setTimestamp(4, Timestamp.valueOf(
                    e.getDateLimite() != null ? e.getDateLimite() : LocalDateTime.now()));
            ps.setFloat(5, e.getBareme());
            ps.setString(6, nvl(e.getQuizzData()));
            ps.executeUpdate();
            System.out.println("✅ Évaluation ajoutée : " + e.getTitre());
            return true;
        } catch (SQLException ex) {
            System.err.println("Erreur ajouter evaluation: " + ex.getMessage());
            return false;
        }
    }

    public boolean modifier(Evaluation e) {
        String req =
            "UPDATE evaluation SET titre=?, description=?, type=?, date_limite=?, bareme=?, quizz_data=? " +
            "WHERE id=?";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(req)) {
            ps.setString(1, e.getTitre());
            ps.setString(2, nvl(e.getDescription()));
            ps.setString(3, e.getType());
            ps.setTimestamp(4, Timestamp.valueOf(
                    e.getDateLimite() != null ? e.getDateLimite() : LocalDateTime.now()));
            ps.setFloat(5, e.getBareme());
            ps.setString(6, nvl(e.getQuizzData()));
            ps.setInt(7, e.getId());
            ps.executeUpdate();
            System.out.println("✅ Évaluation modifiée : " + e.getTitre());
            return true;
        } catch (SQLException ex) {
            System.err.println("Erreur modifier evaluation: " + ex.getMessage());
            return false;
        }
    }

    public void supprimer(int id) {
        try (Connection c = getConn()) {
            // 1. Supprimer les rendus liés
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM rendu WHERE evaluation_id=?")) {
                ps.setInt(1, id); ps.executeUpdate();
            }
            // 2. Supprimer les questions liées
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM question WHERE evaluation_id=?")) {
                ps.setInt(1, id); ps.executeUpdate();
            }
            // 3. Supprimer l'évaluation
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM evaluation WHERE id=?")) {
                ps.setInt(1, id); ps.executeUpdate();
            }
            System.out.println("✅ Évaluation supprimée id=" + id + " (rendus + questions inclus)");
        } catch (SQLException ex) {
            System.err.println("Erreur supprimer evaluation: " + ex.getMessage());
        }
    }

    public List<Evaluation> afficher() {
        List<Evaluation> list = new ArrayList<>();
        String req = "SELECT id, titre, description, type, date_limite, bareme, quizz_data FROM evaluation ORDER BY id DESC";
        try (Connection c = getConn();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("date_limite");
                LocalDateTime dl = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
                list.add(new Evaluation(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        rs.getString("type"),
                        dl,
                        rs.getFloat("bareme"),
                        rs.getString("quizz_data")
                ));
            }
        } catch (SQLException ex) {
            System.err.println("Erreur afficher evaluations: " + ex.getMessage());
        }
        return list;
    }

    private String nvl(String s) { return (s != null && !s.isBlank()) ? s : "{}"; }
}