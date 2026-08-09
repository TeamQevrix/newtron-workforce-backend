package com.newtron.newtron_workforce_backend.controller;

import com.newtron.newtron_workforce_backend.entity.Qualification;
import com.newtron.newtron_workforce_backend.entity.Skill;
import com.newtron.newtron_workforce_backend.repository.QualificationRepository;
import com.newtron.newtron_workforce_backend.repository.SkillRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/master")
@RequiredArgsConstructor
public class MasterDataController {

    private final SkillRepository skillRepository;
    private final QualificationRepository qualificationRepository;
    private final JdbcTemplate jdbcTemplate;

    @Getter
    @RequiredArgsConstructor
    public static class IdNameModel {
        private final long id;
        private final String name;
    }

    @GetMapping("/skills")
    public List<Skill> getSkills() {
        return skillRepository.findAll();
    }

    @GetMapping("/qualifications")
    public List<Qualification> getQualifications() {
        return qualificationRepository.findAll();
    }

    @GetMapping("/states")
    public List<IdNameModel> getStates() {
        return jdbcTemplate.query(
                "SELECT id, name FROM master_states ORDER BY name",
                (rs, rowNum) -> new IdNameModel(rs.getLong("id"), rs.getString("name"))
        );
    }

    @GetMapping("/states/{stateId}/districts")
    public List<IdNameModel> getDistricts(@PathVariable Long stateId) {
        return jdbcTemplate.query(
                "SELECT id, name FROM master_districts WHERE state_id = ? ORDER BY name",
                (rs, rowNum) -> new IdNameModel(rs.getLong("id"), rs.getString("name")),
                stateId
        );
    }

    @GetMapping("/districts/{districtId}/cities")
    public List<IdNameModel> getCities(@PathVariable Long districtId) {
        List<IdNameModel> cities = jdbcTemplate.query(
                "SELECT id, name FROM master_cities WHERE district_id = ? ORDER BY name",
                (rs, rowNum) -> new IdNameModel(rs.getLong("id"), rs.getString("name")),
                districtId
        );
        if (cities.isEmpty()) {
            String districtName = jdbcTemplate.queryForObject(
                    "SELECT name FROM master_districts WHERE id = ?",
                    String.class,
                    districtId
            );
            if (districtName != null) {
                cities = List.of(new IdNameModel(districtId, districtName));
            }
        }
        return cities;
    }
}
