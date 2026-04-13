package org.example;

import utils.DatabaseConnection;
import java.sql.Connection;

public class TestConnexion {
    public static void main(String[] args) {
        System.out.println("Tentative de connexion à XAMPP...");

        // Appel de la méthode de votre classe utils
        Connection c = DatabaseConnection.getConnection();

        if (c != null) {
            System.out.println("✅ SUCCÈS : La base de données est bien liée !");
            try {
                c.close(); // Toujours fermer la connexion pour économiser la mémoire
            } catch (Exception e) {}
        } else {
            System.out.println("❌ ÉCHEC : Vérifiez XAMPP ou le nom de la base dans DatabaseConnection.java");
        }
    }
}

