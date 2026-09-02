package com.graduation.club.common;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@MappedSuperclass
public abstract class BaseEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(nullable=false, updatable=false) private LocalDateTime createdAt;
  @Column(nullable=false) private LocalDateTime updatedAt;
  @Version private long version;
  @PrePersist void onCreate() { var now=LocalDateTime.now(); createdAt=now; updatedAt=now; }
  @PreUpdate void onUpdate() { updatedAt=LocalDateTime.now(); }
  public Long getId() { return id; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public long getVersion() { return version; }
}
