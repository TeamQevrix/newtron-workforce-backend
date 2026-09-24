package com.newtron.newtron_workforce_backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
public class AuditDbTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testAudit() {
        System.out.println("=== START PRE-MIGRATION COUNTS ===");
        
        Integer count1 = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM worker_memberships " +
            "WHERE status = 'ACTIVE' AND expires_at IS NULL AND activated_at IS NOT NULL", 
            Integer.class
        );
        
        Integer count2 = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM worker_memberships " +
            "WHERE status = 'ACTIVE' AND expires_at IS NULL AND activated_at IS NULL", 
            Integer.class
        );

        System.out.println("POST-MIGRATION ACTIVE + NULL expires_at + non-null activated_at: " + count1);
        System.out.println("POST-MIGRATION ACTIVE + NULL expires_at + NULL activated_at: " + count2);
        
        System.out.println("=== END POST-MIGRATION COUNTS ===");
    }
}
