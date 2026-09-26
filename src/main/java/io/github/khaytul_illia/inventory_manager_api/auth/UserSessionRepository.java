package io.github.khaytul_illia.inventory_manager_api.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    @Query("select count(us.id) from UserSession us where us.user.id = :userId and us.valid = true and us.expiresAt > current_timestamp")
    int countOpenUserSessions(Long userId);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Modifying
    @Query("update UserSession us set us.valid = false where us.id = :sessionId")
    void invalidateSessionById(Long sessionId);

}
