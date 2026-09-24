package com.newtron.newtron_workforce_backend.auth.repository;


import com.newtron.newtron_workforce_backend.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByMobile(String mobile);

    Optional<User> findByEmail(String email);

    Optional<User> findByUuid(String uuid);

    boolean existsByMobile(String mobile);

    boolean existsByEmail(String email);

    Optional<User> findByPasswordResetTokenHash(String tokenHash);
}
