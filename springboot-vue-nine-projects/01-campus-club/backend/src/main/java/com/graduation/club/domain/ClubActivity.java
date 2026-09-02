package com.graduation.club.domain;
    import com.graduation.club.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
    @Entity @Table(name="club_activity")
    public class ClubActivity extends BaseEntity {
      @Column(nullable=false) private Long clubId;
  @Column(nullable=false) private String title;
  private String location;
  @Column(nullable=false) private LocalDateTime startTime;
  @Column(nullable=false) private LocalDateTime endTime;
  private int capacity;
  @Column(nullable=false) private String status;
      public Long getClubId(){return clubId;}
  public void setClubId(Long v){clubId=v;}
  public String getTitle(){return title;}
  public void setTitle(String v){title=v;}
  public String getLocation(){return location;}
  public void setLocation(String v){location=v;}
  public LocalDateTime getStartTime(){return startTime;}
  public void setStartTime(LocalDateTime v){startTime=v;}
  public LocalDateTime getEndTime(){return endTime;}
  public void setEndTime(LocalDateTime v){endTime=v;}
  public int getCapacity(){return capacity;}
  public void setCapacity(int v){capacity=v;}
  public String getStatus(){return status;}
  public void setStatus(String v){status=v;}
    }
