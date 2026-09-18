package com.newtron.newtron_workforce_backend.repository;

import com.newtron.newtron_workforce_backend.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    Optional<ChatSession> findByJobIdAndTeamIdAndApplicationId(Long jobId, Long teamId, Long applicationId);
    Optional<ChatSession> findByTeamIdAndCompanyId(Long teamId, Long companyId);
    
    @org.springframework.data.jpa.repository.Query("SELECT s FROM ChatSession s WHERE s.team.ownerWorkerProfile.user.id = :userId OR s.company.owner.id = :userId")
    java.util.List<ChatSession> findUserConversations(@org.springframework.data.repository.query.Param("userId") Long userId);
}
