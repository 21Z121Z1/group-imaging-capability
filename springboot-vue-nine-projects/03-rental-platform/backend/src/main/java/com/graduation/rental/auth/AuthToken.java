package com.graduation.rental.auth;
import com.graduation.rental.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="auth_token", indexes={@Index(name="idx_auth_token", columnList="tokenHash", unique=true), @Index(name="idx_auth_expires", columnList="expiresAt")})
public class AuthToken extends BaseEntity {
  @Column(nullable=false, unique=true, length=64) private String tokenHash;
  @Column(nullable=false) private Long userId;
  @Column(nullable=false) private LocalDateTime expiresAt;
  public String getTokenHash(){return tokenHash;} public void setTokenHash(String v){tokenHash=v;}
  public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;}
  public LocalDateTime getExpiresAt(){return expiresAt;} public void setExpiresAt(LocalDateTime v){expiresAt=v;}
}
