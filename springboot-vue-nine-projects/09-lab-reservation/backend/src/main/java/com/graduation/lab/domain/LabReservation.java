package com.graduation.lab.domain;
    import com.graduation.lab.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
    @Entity @Table(name="lab_reservation")
    public class LabReservation extends BaseEntity {
      @Column(nullable=false) private Long labId;
  @Column(nullable=false) private Long userId;
  @Column(nullable=false) private String title;
  @Column(length=2000) private String purpose;
  @Column(nullable=false) private LocalDateTime startTime;
  @Column(nullable=false) private LocalDateTime endTime;
  @Column(nullable=false) private String status;
      public Long getLabId(){return labId;}
  public void setLabId(Long v){labId=v;}
  public Long getUserId(){return userId;}
  public void setUserId(Long v){userId=v;}
  public String getTitle(){return title;}
  public void setTitle(String v){title=v;}
  public String getPurpose(){return purpose;}
  public void setPurpose(String v){purpose=v;}
  public LocalDateTime getStartTime(){return startTime;}
  public void setStartTime(LocalDateTime v){startTime=v;}
  public LocalDateTime getEndTime(){return endTime;}
  public void setEndTime(LocalDateTime v){endTime=v;}
  public String getStatus(){return status;}
  public void setStatus(String v){status=v;}
    }
