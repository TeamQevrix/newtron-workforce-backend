package com.newtron.newtron_workforce_backend.repository;

import com.newtron.newtron_workforce_backend.entity.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    Optional<WorkOrder> findByAgreementId(Long agreementId);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"company", "job", "worker"})
    Optional<WorkOrder> findById(Long id);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"company", "job", "worker"})
    java.util.List<WorkOrder> findByCompanyIdOrderByIdDesc(Long companyId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"company", "job", "worker"})
    Optional<WorkOrder> findByWorkOrderNumber(String workOrderNumber);
}
