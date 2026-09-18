package com.newtron.newtron_workforce_backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

@SpringBootTest
public class CheckIndexTest {

    @Autowired
    private DataSource dataSource;

    @Test
    public void testCheckIndexes() throws Exception {
        try (Connection c = dataSource.getConnection()) {
            ResultSet rs = c.getMetaData().getIndexInfo(null, null, "chat_sessions", false, false);
            System.out.println("----- START INDEX INFO -----");
            while (rs.next()) {
                System.out.println("INDEX_NAME: " + rs.getString("INDEX_NAME") + " on COLUMN_NAME: " + rs.getString("COLUMN_NAME"));
            }
            System.out.println("----- END INDEX INFO -----");
        }
    }
}
