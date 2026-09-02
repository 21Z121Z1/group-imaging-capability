package com.graduation.flower.auth;
import com.graduation.flower.common.BaseEntity;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
@Entity @Table(name="app_user", indexes={@Index(name="idx_user_username", columnList="username", unique=true)})
public class User extends BaseEntity {
  @Column(nullable=false, unique=true, length=64) private String username;
  @JsonIgnore @Column(nullable=false) private String passwordHash;
  @Column(nullable=false, length=64) private String displayName;
  @Column(nullable=false, length=16) private String role="USER";
  @Column(nullable=false) private boolean enabled=true;
  public String getUsername(){return username;} public void setUsername(String v){username=v;}
  public String getPasswordHash(){return passwordHash;} public void setPasswordHash(String v){passwordHash=v;}
  public String getDisplayName(){return displayName;} public void setDisplayName(String v){displayName=v;}
  public String getRole(){return role;} public void setRole(String v){role=v;}
  public boolean isEnabled(){return enabled;} public void setEnabled(boolean v){enabled=v;}
}
