package com.aliahmed.Vercel.Repositories;

import com.aliahmed.Vercel.entity.AuthCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface AuthCodeRepository extends JpaRepository<AuthCode, Long> {

    Optional<AuthCode> findByCodeHash(String codeHash);

    /**
     * Burns a code and reports whether this call was the one that burned it.
     *
     * <p>Single-use has to be decided by the database, not by read-then-write in
     * Java: two simultaneous requests could both read an unused code and both
     * proceed. One UPDATE with the conditions in the WHERE clause makes the
     * winner unambiguous — it returns 1 exactly once.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update AuthCode a
               set a.usedAt = :now
             where a.codeHash = :codeHash
               and a.usedAt is null
               and a.expiresAt > :now
            """)
    int markUsed(@Param("codeHash") String codeHash, @Param("now") Instant now);

    @Modifying
    @Query("delete from AuthCode a where a.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
