package com.example.auth.repository;

import com.example.auth.entity.OtpRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpRecord, Long> {

    Optional<OtpRecord>
    findFirstByPhoneAndVerifiedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String phone,
            LocalDateTime currentTime
    );

    @Modifying
    @Query("""
            UPDATE OtpRecord o
            SET o.verified = true
            WHERE o.phone = :phone
              AND o.verified = false
            """)
    int invalidateAllForPhone(@Param("phone") String phone);
}