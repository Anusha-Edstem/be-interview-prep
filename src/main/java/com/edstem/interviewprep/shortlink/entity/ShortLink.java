package com.edstem.interviewprep.shortlink.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "short_links")
public class ShortLink {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "code", nullable = false, updatable = false, length = 8, unique = true)
  private String code;

  @Column(name = "original_url", nullable = false, length = 2048)
  private String originalUrl;

  @Column(name = "expires_at")
  private Instant expiresAt;

  @Column(name = "visit_count", nullable = false)
  private long visitCount;

  @CreationTimestamp
  @Column(name = "created_date", nullable = false, updatable = false)
  private Instant createdDate;

  protected ShortLink() {}

  private ShortLink(String code, String originalUrl, Instant expiresAt) {
    this.code = code;
    this.originalUrl = originalUrl;
    this.expiresAt = expiresAt;
    this.visitCount = 0L;
  }

  public static ShortLink create(String code, String originalUrl, Instant expiresAt) {
    return new ShortLink(code, originalUrl, expiresAt);
  }

  public boolean hasExpiredAt(Instant moment) {
    return expiresAt != null && !expiresAt.isAfter(moment);
  }

  public UUID getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public String getOriginalUrl() {
    return originalUrl;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public long getVisitCount() {
    return visitCount;
  }

  public Instant getCreatedDate() {
    return createdDate;
  }
}
