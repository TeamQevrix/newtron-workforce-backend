package com.newtron.newtron_workforce_backend.repository;

import com.newtron.newtron_workforce_backend.entity.WorkerSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkerSkillRepository extends JpaRepository<WorkerSkill, Long> {
    List<WorkerSkill> findByWorkerProfileId(Long workerProfileId);
    void deleteByWorkerProfileId(Long workerProfileId);
}
