package com.graduation.gym.domain;
    import com.graduation.gym.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
    @Entity @Table(name="gym_membership")
    public class GymMembership extends BaseEntity {
      @Column(nullable=false) private Long userId;
  @Column(nullable=false) private Long planId;
  @Column(nullable=false) private LocalDate startDate;
  @Column(nullable=false) private LocalDate endDate;
  @Column(nullable=false) private String status;
      public Long getUserId(){return userId;}
  public void setUserId(Long v){userId=v;}
  public Long getPlanId(){return planId;}
  public void setPlanId(Long v){planId=v;}
  public LocalDate getStartDate(){return startDate;}
  public void setStartDate(LocalDate v){startDate=v;}
  public LocalDate getEndDate(){return endDate;}
  public void setEndDate(LocalDate v){endDate=v;}
  public String getStatus(){return status;}
  public void setStatus(String v){status=v;}
    }
