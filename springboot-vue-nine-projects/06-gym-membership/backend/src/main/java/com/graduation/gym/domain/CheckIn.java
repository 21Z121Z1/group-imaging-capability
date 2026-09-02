package com.graduation.gym.domain;
    import com.graduation.gym.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
    @Entity @Table(name="gym_checkin")
    public class CheckIn extends BaseEntity {
      @Column(nullable=false) private Long userId;
  @Column(nullable=false) private LocalDateTime checkInAt;
  private String source;
      public Long getUserId(){return userId;}
  public void setUserId(Long v){userId=v;}
  public LocalDateTime getCheckInAt(){return checkInAt;}
  public void setCheckInAt(LocalDateTime v){checkInAt=v;}
  public String getSource(){return source;}
  public void setSource(String v){source=v;}
    }
