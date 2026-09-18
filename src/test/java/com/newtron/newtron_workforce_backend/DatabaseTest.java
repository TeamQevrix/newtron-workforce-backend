package com.newtron.newtron_workforce_backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
public class DatabaseTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void insertTeamJob() {
        jdbcTemplate.execute("INSERT INTO jobs (title, category, city, salary, duration, distance, latitude, longitude, description, status, workers_required, company_name, work_mode) VALUES ('Construction Team Required', 'Construction', 'Delhi NCR', '1500', 'Full Time', '5.0 KM', 28.6139, 77.2090, 'Need a full team of 5 workers for building construction.', 'Active', 5, 'Metro Builders', 'TEAM')");
        System.out.println("INSERTED TEAM JOB SUCCESSFULLY");
    }
}
