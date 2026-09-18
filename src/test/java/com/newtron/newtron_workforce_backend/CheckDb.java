package com.newtron.newtron_workforce_backend;
import java.sql.*;
public class CheckDb {
    public static void main(String[] args) throws Exception {
        String DB_URL = "jdbc:mysql://localhost:3306/newtron_workforce?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata&characterEncoding=UTF-8";
        try (Connection conn = DriverManager.getConnection(DB_URL, "root", "Ankit@420")) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 2");
            while(rs.next()) {
                System.out.println("Flyway Version: " + rs.getString("version") + " | " + rs.getString("description"));
            }
            rs = conn.createStatement().executeQuery("SELECT id, team_name FROM teams");
            System.out.println("Teams:");
            while(rs.next()) {
                System.out.println("Team ID: " + rs.getLong("id") + " | Name: " + rs.getString("team_name"));
            }
        }
    }
}
