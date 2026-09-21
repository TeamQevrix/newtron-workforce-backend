package com.newtron.newtron_workforce_backend.repository;

import com.newtron.newtron_workforce_backend.entity.Agreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgreementRepository extends JpaRepository<Agreement, Long> {
    Optional<Agreement> findByApplicationId(Long applicationId);
    boolean existsByApplicationId(Long applicationId);
}
