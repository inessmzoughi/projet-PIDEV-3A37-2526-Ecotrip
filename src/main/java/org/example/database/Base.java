package org.example.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Base {

    private static final String URL  = "jdbc:mysql://localhost:3306/ecotrip"
            + "?useSSL=false"
            + "&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASS = "";

    private static Base instance;
    private Connection connection;

    private Base() {
        connect(); // connect on first creation
    }

    public static Base getInstance() {
        if (instance == null)
            instance = new Base();
        return instance;
    }

    // Establish the connection
    private void connect() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASS);
            System.out.println("Connection réussite");
        } catch (SQLException e) {
            System.err.println("Échec de la connexion : " + e.getMessage());
        }
    }

    // ✅ Returns connection, reconnects automatically if it died
    public Connection getConnection() {
        try {
            // isClosed() checks if Java knows it's closed
            // isValid(2) actually pings MySQL to check if it's still alive
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                System.out.println("Connection lost — reconnecting...");
                connect(); // reconnect silently
            }
        } catch (SQLException e) {
            System.err.println("Connection check failed: " + e.getMessage());
            connect();
        }
        return connection;
    }
}