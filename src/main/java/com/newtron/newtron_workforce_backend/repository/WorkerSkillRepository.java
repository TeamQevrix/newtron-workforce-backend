package com.newtron.newtron_workforce_backend.repository;

import com.newtron.newtron_workforce_backend.entity.WorkerSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkerSkillRepository extends JpaRepository<WorkerSkill, Long> {
    List<WorkerSkill> findByWorkerProfileId(Long workerProfileId);
    void deleteByWorkerProfileId(Long workerProfileId);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM WorkerSkill s JOIN FETCH s.skill WHERE s.workerProfile.user.id IN :workerIds AND s.isPrimary = true")
    List<WorkerSkill> findPrimarySkillsByWorkerIds(@org.springframework.data.repository.query.Param("workerIds") List<Long> workerIds);
}
