package com.newtron.newtron_workforce_backend.repository;

import com.newtron.newtron_workforce_backend.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {
}
