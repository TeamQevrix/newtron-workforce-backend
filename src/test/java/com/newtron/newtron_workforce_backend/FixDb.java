package com.newtron.newtron_workforce_backend;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class FixDb {
    public static void main(String[] args) throws Exception {
        String DB_URL = "jdbc:mysql://localhost:3306/newtron_workforce?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata&characterEncoding=UTF-8";
        String USER = "root";
        String PASS = "Ankit@420";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            System.out.println("Connected to MySQL!");
            Statement stmt = conn.createStatement();
            
            System.out.println("Restoring active_status...");
            try {
                stmt.executeUpdate("ALTER TABLE teams ADD COLUMN active_status INT GENERATED ALWAYS AS (IF(deleted = FALSE, 1, NULL)) STORED");
            } catch (Exception e) { System.out.println("Add col failed: " + e.getMessage()); }

            System.out.println("Restoring uq_active_team_owner...");
            try {
                stmt.executeUpdate("ALTER TABLE teams ADD UNIQUE KEY uq_active_team_owner (owner_worker_profile_id, active_status)");
            } catch (Exception e) { System.out.println("Add key failed: " + e.getMessage()); }
            
            System.out.println("Deleting V26 from flyway_schema_history...");
            stmt.executeUpdate("DELETE FROM flyway_schema_history WHERE version = '26'");
            
            System.out.println("DONE.");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("FixDb completed!");
    }
}
