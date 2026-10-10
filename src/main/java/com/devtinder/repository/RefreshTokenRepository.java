package com.devtinder.repository;

import com.devtinder.entity.RefreshToken;
import com.devtinder.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByUserAndRevokedFalseOrderByLastActiveDesc(User user);

    List<RefreshToken> findByFamilyId(String familyId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken r set r.revoked = true, r.revokedAt = :now where r.familyId = :familyId")
    void revokeFamily(@Param("familyId") String familyId, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken r set r.revoked = true, r.revokedAt = :now where r.user.id = :userId and r.revoked = false")
    void revokeAllUserTokens(@Param("userId") Long userId, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RefreshToken r where r.token = :token")
    void deleteByToken(@Param("token") String token);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RefreshToken r where r.familyId = :familyId")
    void deleteByFamilyId(@Param("familyId") String familyId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RefreshToken r where r.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RefreshToken r where r.user.id = :userId and r.familyId != :currentFamilyId")
    void deleteOtherFamiliesByUserId(@Param("userId") Long userId, @Param("currentFamilyId") String currentFamilyId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RefreshToken r where r.expiryDate < :now or r.revoked = true")
    int purgeAllRevokedOrExpiredTokens(@Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RefreshToken r where r.expiryDate < :now or (r.revoked = true and (r.revokedAt is null or r.revokedAt < :cutoff))")
    int purgeOldTokens(@Param("now") Instant now, @Param("cutoff") Instant cutoff);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RefreshToken r where r.id = :id and r.user.id = :userId")
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
