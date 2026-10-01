package com.newtron.newtron_workforce_backend.repository;

import com.newtron.newtron_workforce_backend.entity.RazorpayWebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RazorpayWebhookLogRepository extends JpaRepository<RazorpayWebhookLog, Long> {
    Optional<RazorpayWebhookLog> findByEventId(String eventId);
    boolean existsByEventId(String eventId);
}
