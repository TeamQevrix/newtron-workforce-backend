package com.newtron.newtron_workforce_backend.repository;

import com.newtron.newtron_workforce_backend.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.repository.query.Param;

import java.util.List;
import org.springframework.data.domain.Pageable;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    
    @Query("SELECT COUNT(j) FROM Job j WHERE j.status = 'Active'")
    long countActiveJobs();

    @Query("SELECT COUNT(j) FROM Job j WHERE j.status = 'Active' " +
           "AND j.id NOT IN :hiddenJobIds " +
           "AND COALESCE(j.workersRequired, 1) > (SELECT COUNT(a) FROM Application a WHERE a.job = j AND a.status IN ('Hired', 'Completed'))")
    long countAvailableJobsExcludingHidden(@org.springframework.data.repository.query.Param("hiddenJobIds") java.util.List<Long> hiddenJobIds);

    @Query("SELECT COUNT(j) FROM Job j WHERE j.status = 'Active' " +
           "AND COALESCE(j.workersRequired, 1) > (SELECT COUNT(a) FROM Application a WHERE a.job = j AND a.status IN ('Hired', 'Completed'))")
    long countAvailableJobs();

    @Query("SELECT j FROM Job j LEFT JOIN FETCH j.recruiter WHERE j.status = 'Active' AND (" +
           "LOWER(j.category) = LOWER(:skill1) OR LOWER(j.category) = LOWER(:skill2) OR " +
           "LOWER(j.title) LIKE LOWER(CONCAT('%', :skill1, '%')) OR LOWER(j.title) LIKE LOWER(CONCAT('%', :skill2, '%'))) " +
           "ORDER BY CASE " +
           "WHEN LOWER(j.category) = LOWER(:skill1) THEN 1 " +
           "WHEN LOWER(j.category) = LOWER(:skill2) THEN 2 " +
           "WHEN LOWER(j.title) LIKE LOWER(CONCAT('%', :skill1, '%')) THEN 3 " +
           "WHEN LOWER(j.title) LIKE LOWER(CONCAT('%', :skill2, '%')) THEN 4 " +
           "ELSE 5 END ASC, j.id DESC")
    List<Job> findActiveJobsBySkills(String skill1, String skill2, Pageable pageable);

    @Query("SELECT j FROM Job j LEFT JOIN FETCH j.recruiter WHERE j.status = 'Active' AND " +
           "j.latitude BETWEEN :minLat AND :maxLat AND " +
           "j.longitude BETWEEN :minLon AND :maxLon")
    List<Job> findActiveJobsWithinBoundingBox(
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLon") double minLon,
            @Param("maxLon") double maxLon);

    @Query("SELECT j FROM Job j LEFT JOIN FETCH j.recruiter WHERE j.status = 'Active'")
    List<Job> findAllActiveJobs();

    @Query("SELECT j FROM Job j LEFT JOIN FETCH j.recruiter WHERE j.status = 'Active' AND j.workMode = 'TEAM' ORDER BY j.id DESC")
    List<Job> findActiveTeamJobs();

    @Query("SELECT j FROM Job j LEFT JOIN FETCH j.recruiter WHERE j.status = 'Active' AND j.workMode = 'TEAM' AND " +
           "(LOWER(j.category) = LOWER(:skillName) OR LOWER(j.title) LIKE LOWER(CONCAT('%', :skillName, '%'))) " +
           "ORDER BY j.id DESC")
    List<Job> findActiveTeamJobsBySkill(@Param("skillName") String skillName);

    @Query("SELECT j FROM Job j LEFT JOIN FETCH j.recruiter r WHERE j.status = 'Active' AND (" +
           "LOWER(j.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(j.category) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(j.city) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.fullName) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Job> findActiveJobsBySearchQuery(@Param("search") String search);

    @Query("SELECT j FROM Job j WHERE j.recruiter.id = :recruiterId ORDER BY j.id DESC")
    List<Job> findByRecruiterId(@Param("recruiterId") Long recruiterId);

    List<Job> findByCompanyIdOrderByIdDesc(Long companyId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM Job j WHERE j.id = :id")
    java.util.Optional<Job> findByIdForUpdate(@Param("id") Long id);
}

