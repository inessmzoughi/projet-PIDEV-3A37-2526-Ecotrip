package org.example.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Base {
    private final String URL = "jdbc:mysql://localhost:3306/ecotrip";
    private final String USER = "root";
    private final String PASS = "";
    private Connection connection;

    private  static Base instance;
    public Base(){
        try {
            connection = DriverManager.getConnection(URL,USER,PASS);
            System.out.println("Connection réussite");
        } catch (SQLException e) {
            System.err.println("Échec de la connexion : " + e.getMessage());
        }
    }

    public static Base getInstance() {
        if(instance == null)
            instance = new Base();
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

}
