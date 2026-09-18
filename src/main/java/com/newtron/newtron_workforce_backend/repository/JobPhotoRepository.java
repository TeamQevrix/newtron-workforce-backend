package com.newtron.newtron_workforce_backend.repository;

import com.newtron.newtron_workforce_backend.entity.JobPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobPhotoRepository extends JpaRepository<JobPhoto, Long> {
    List<JobPhoto> findByJobIdOrderByDisplayOrderAsc(Long jobId);
}
