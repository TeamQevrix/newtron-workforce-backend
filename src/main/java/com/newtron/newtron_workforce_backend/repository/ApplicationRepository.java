package com.newtron.newtron_workforce_backend.repository;

import com.newtron.newtron_workforce_backend.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    
    long countByWorkerId(Long workerId);
    List<Application> findByWorkerId(Long workerId);
    long countByJobIdAndStatus(Long jobId, String status);
    long countByWorkerIdAndStatus(Long workerId, String status);
    Optional<Application> findByWorkerIdAndJobId(Long workerId, Long jobId);
    Optional<Application> findByTeamIdAndJobId(Long teamId, Long jobId);

    @org.springframework.data.jpa.repository.Query("SELECT a FROM Application a JOIN FETCH a.job j LEFT JOIN FETCH j.recruiter r WHERE a.worker.id = :workerId AND a.team IS NULL ORDER BY a.id DESC")
    List<Application> findByWorkerIdAndTeamIsNullWithJobAndRecruiter(@org.springframework.data.repository.query.Param("workerId") Long workerId);

    @org.springframework.data.jpa.repository.Query("SELECT a FROM Application a JOIN FETCH a.job j LEFT JOIN FETCH j.recruiter r WHERE a.team.id = :teamId ORDER BY a.id DESC")
    List<Application> findByTeamIdWithJobAndRecruiter(@org.springframework.data.repository.query.Param("teamId") Long teamId);

    @org.springframework.data.jpa.repository.Query("SELECT a FROM Application a JOIN FETCH a.job j LEFT JOIN FETCH j.recruiter r WHERE a.worker.id = :workerId ORDER BY a.id DESC")
    List<Application> findByWorkerIdWithJobAndRecruiter(@org.springframework.data.repository.query.Param("workerId") Long workerId);

    @org.springframework.data.jpa.repository.Query("SELECT a FROM Application a JOIN FETCH a.job j LEFT JOIN FETCH j.recruiter r WHERE a.worker.id = :workerId AND a.status IN ('Hired', 'Completed') ORDER BY a.id DESC")
    List<Application> findWorkerJobHistory(@org.springframework.data.repository.query.Param("workerId") Long workerId);

    @org.springframework.data.jpa.repository.Query("SELECT a.job.id, COUNT(a) FROM Application a WHERE a.job.recruiter.id = :recruiterId GROUP BY a.job.id")
    List<Object[]> countApplicantsByRecruiterJobId(@org.springframework.data.repository.query.Param("recruiterId") Long recruiterId);

    @org.springframework.data.jpa.repository.Query("SELECT a.job.id, COUNT(a) FROM Application a WHERE a.job.recruiter.id = :recruiterId AND a.status IN ('Hired', 'Completed') GROUP BY a.job.id")
    List<Object[]> countHiredByRecruiterJobId(@org.springframework.data.repository.query.Param("recruiterId") Long recruiterId);

    @org.springframework.data.jpa.repository.Query("SELECT a.job.id, COUNT(a) FROM Application a WHERE a.job.company.id = :companyId GROUP BY a.job.id")
    List<Object[]> countApplicantsByCompanyId(@org.springframework.data.repository.query.Param("companyId") Long companyId);

    @org.springframework.data.jpa.repository.Query("SELECT a.job.id, COUNT(a) FROM Application a WHERE a.job.company.id = :companyId AND a.status IN ('Hired', 'Completed') GROUP BY a.job.id")
    List<Object[]> countHiredByCompanyId(@org.springframework.data.repository.query.Param("companyId") Long companyId);

    @org.springframework.data.jpa.repository.Query("SELECT a FROM Application a JOIN FETCH a.worker w WHERE a.job.id = :jobId ORDER BY a.id DESC")
    List<Application> findByJobId(@org.springframework.data.repository.query.Param("jobId") Long jobId);

    @org.springframework.data.jpa.repository.Query("SELECT a.worker.id, COUNT(a) FROM Application a WHERE a.worker.id IN :workerIds AND a.status = 'Completed' GROUP BY a.worker.id")
    List<Object[]> countCompletedJobsByWorkerIds(@org.springframework.data.repository.query.Param("workerIds") List<Long> workerIds);

    @org.springframework.data.jpa.repository.Query("SELECT a.job.id, COUNT(a) FROM Application a WHERE a.job.id IN :jobIds AND a.status IN ('Hired', 'Completed') GROUP BY a.job.id")
    List<Object[]> countFilledByJobIds(@org.springframework.data.repository.query.Param("jobIds") List<Long> jobIds);

    @org.springframework.data.jpa.repository.Query("SELECT a FROM Application a JOIN FETCH a.job j WHERE a.worker.id = :workerId ORDER BY a.id DESC")
    List<Application> findRecentByWorkerId(@org.springframework.data.repository.query.Param("workerId") Long workerId, org.springframework.data.domain.Pageable pageable);
}

