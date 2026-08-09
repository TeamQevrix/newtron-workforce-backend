package com.newtron.newtron_workforce_backend.auth.repository;

import com.newtron.newtron_workforce_backend.auth.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findByRefreshTokenHashAndActiveTrue(String refreshTokenHash);

    Optional<UserSession> findByUserIdAndDeviceIdAndActiveTrue(Long userId, String deviceId);

    List<UserSession> findAllByUserIdAndActiveTrue(Long userId);
}
