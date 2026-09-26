package io.github.khaytul_illia.inventory_manager_api.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Query("select rt from RefreshToken rt join fetch rt.session join fetch rt.session.user where rt.tokenValue = :tokenValue")
    Optional<RefreshToken> findByTokenValue(String tokenValue);

}
