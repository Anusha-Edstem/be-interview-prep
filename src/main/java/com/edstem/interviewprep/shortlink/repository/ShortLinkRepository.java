package com.edstem.interviewprep.shortlink.repository;

import com.edstem.interviewprep.shortlink.entity.ShortLink;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShortLinkRepository extends JpaRepository<ShortLink, UUID> {

  Optional<ShortLink> findByCode(String code);

  boolean existsByCode(String code);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("update ShortLink link set link.visitCount = link.visitCount + 1 where link.code = :code")
  int incrementVisitCount(@Param("code") String code);
}
