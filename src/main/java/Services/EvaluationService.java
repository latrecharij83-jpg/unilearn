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
        if (c == null) throw new SQLException("DB Connection is null – vérifiez que XAMPP est démarré.");
        return c;
    }

    public boolean ajouter(Evaluation e) {
        String req =
            "INSERT INTO evaluation (titre, description, type, date_limite, bareme, quizz_data) " +
            "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(req)) {
            ps.setString(1, e.getTitre());
            ps.setString(2, nvl(e.getDescription()));
            ps.setString(3, e.getType());
            ps.setTimestamp(4, Timestamp.valueOf(
                    e.getDateLimite() != null ? e.getDateLimite() : LocalDateTime.now()));
            ps.setFloat(5, e.getBareme());
            ps.setString(6, nvl(e.getQuizzData()));
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Évaluation ajoutée : " + e.getTitre());
                return true;
            } else {
                System.err.println("⚠️ Aucune ligne insérée pour : " + e.getTitre());
                return false;
            }
        } catch (SQLException ex) {
            System.err.println("❌ Erreur ajouter evaluation: " + ex.getMessage());
            return false;
        }
    }

    public boolean modifier(Evaluation e) {
        String req =
            "UPDATE evaluation SET titre=?, description=?, type=?, date_limite=?, bareme=?, quizz_data=? " +
            "WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(req)) {
            ps.setString(1, e.getTitre());
            ps.setString(2, nvl(e.getDescription()));
            ps.setString(3, e.getType());
            ps.setTimestamp(4, Timestamp.valueOf(
                    e.getDateLimite() != null ? e.getDateLimite() : LocalDateTime.now()));
            ps.setFloat(5, e.getBareme());
            ps.setString(6, nvl(e.getQuizzData()));
            ps.setInt(7, e.getId());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Évaluation modifiée id=" + e.getId() + " : " + e.getTitre());
                return true;
            } else {
                System.err.println("⚠️ Aucune ligne mise à jour pour id=" + e.getId() + " – l'ID existe-t-il en DB ?");
                return false;
            }
        } catch (SQLException ex) {
            System.err.println("❌ Erreur modifier evaluation: " + ex.getMessage());
            return false;
        }
    }

    public boolean supprimer(int id) {
        try {
            Connection c = getConn();
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
                ps.setInt(1, id);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    System.out.println("✅ Évaluation supprimée id=" + id + " (rendus + questions inclus)");
                    return true;
                } else {
                    System.err.println("⚠️ Aucune évaluation trouvée avec id=" + id);
                    return false;
                }
            }
        } catch (SQLException ex) {
            System.err.println("❌ Erreur supprimer evaluation: " + ex.getMessage());
            return false;
        }
    }

    public List<Evaluation> afficher() {
        List<Evaluation> list = new ArrayList<>();
        String req = "SELECT id, titre, description, type, date_limite, bareme, quizz_data FROM evaluation ORDER BY id DESC";
        try (Statement st = getConn().createStatement();
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
            System.err.println("❌ Erreur afficher evaluations: " + ex.getMessage());
        }
        return list;
    }

    private String nvl(String s) { return (s != null && !s.isBlank()) ? s : "{}"; }
}