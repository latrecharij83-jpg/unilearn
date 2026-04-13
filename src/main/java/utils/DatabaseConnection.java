package utils; // Indispensable car votre fichier est dans le dossier 'utils'

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // Remplacez 'votre_nom_bdd' par le nom réel dans XAMPP (ex: unilearn)
    private static final String URL = "jdbc:mysql://localhost:3306/unilearn";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.err.println("Erreur de connexion : " + e.getMessage());
            return null;
        }
    }
}