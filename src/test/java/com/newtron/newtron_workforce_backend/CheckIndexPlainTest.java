package com.newtron.newtron_workforce_backend;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

public class CheckIndexPlainTest {

    @Test
    public void testCheckIndexes() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection c = DriverManager.getConnection("jdbc:mysql://localhost:3306/newtron_workforce?useSSL=false&allowPublicKeyRetrieval=true", "root", "Ankit@420")) {
            ResultSet rs = c.getMetaData().getIndexInfo(null, null, "chat_sessions", false, false);
            System.out.println("----- START INDEX INFO -----");
            while (rs.next()) {
                System.out.println("INDEX_NAME: " + rs.getString("INDEX_NAME") + " on COLUMN_NAME: " + rs.getString("COLUMN_NAME") + " NON_UNIQUE: " + rs.getBoolean("NON_UNIQUE"));
            }
            System.out.println("----- END INDEX INFO -----");
        }
    }
}
