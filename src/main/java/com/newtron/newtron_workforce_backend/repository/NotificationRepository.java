package com.newtron.newtron_workforce_backend.repository;

import com.newtron.newtron_workforce_backend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndIsReadFalseAndDeletedFalse(Long userId);

    Optional<Notification> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);
}
