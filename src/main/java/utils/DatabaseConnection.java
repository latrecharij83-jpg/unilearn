package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL      = "jdbc:mysql://localhost:3306/unilearn?autoReconnect=true&useSSL=false&serverTimezone=UTC";
    private static final String USER     = "root";
    private static final String PASSWORD = "";

    private static Connection instance = null;

    /**
     * Retourne toujours une connexion valide.
     * Si la connexion est nulle ou fermée, elle est réouverte automatiquement.
     */
    public static Connection getConnection() {
        try {
            if (instance == null || instance.isClosed()) {
                instance = DriverManager.getConnection(URL, USER, PASSWORD);
                instance.setAutoCommit(true);   // ← garantit que chaque exécution est immédiatement persistée
                System.out.println("✅ Nouvelle connexion DB établie.");
            }
            return instance;
        } catch (SQLException e) {
            System.err.println("❌ Erreur de connexion DB : " + e.getMessage());
            return null;
        }
    }
}