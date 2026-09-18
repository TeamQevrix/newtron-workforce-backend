package com.newtron.newtron_workforce_backend.repository;

import com.newtron.newtron_workforce_backend.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    @Query("SELECT tm FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.deleted = false")
    List<TeamMember> findAllByTeamId(@Param("teamId") Long teamId);

    @Query("SELECT tm FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.id = :memberId AND tm.deleted = false")
    Optional<TeamMember> findByTeamIdAndId(@Param("teamId") Long teamId, @Param("memberId") Long memberId);

    @Query("SELECT COUNT(tm) > 0 FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.mobileNumber = :mobileNumber AND tm.deleted = false")
    boolean existsByTeamIdAndMobileNumberAndDeletedFalse(@Param("teamId") Long teamId, @Param("mobileNumber") String mobileNumber);

    @Query("SELECT COUNT(tm) FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.deleted = false")
    long countByTeamId(@Param("teamId") Long teamId);
}
