package com.newtron.newtron_workforce_backend;

import com.newtron.newtron_workforce_backend.entity.Team;
import com.newtron.newtron_workforce_backend.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class DbTest {

    @Autowired
    private TeamRepository teamRepository;

    @Test
    public void testPrintTeams() {
        List<Team> teams = teamRepository.findAll();
        System.out.println("==== FOUND " + teams.size() + " TEAMS ====");
        for (Team t : teams) {
            System.out.println("ID: " + t.getId() + ", Name: " + t.getTeamName());
        }
    }
}
