package com.graduation.rental.domain;
    import com.graduation.rental.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
    @Entity @Table(name="viewing_appointment")
    public class ViewingAppointment extends BaseEntity {
      @Column(nullable=false) private Long listingId;
  @Column(nullable=false) private Long userId;
  @Column(nullable=false) private LocalDateTime visitTime;
  @Column(nullable=false) private String status;
  @Column(length=1000) private String note;
      public Long getListingId(){return listingId;}
  public void setListingId(Long v){listingId=v;}
  public Long getUserId(){return userId;}
  public void setUserId(Long v){userId=v;}
  public LocalDateTime getVisitTime(){return visitTime;}
  public void setVisitTime(LocalDateTime v){visitTime=v;}
  public String getStatus(){return status;}
  public void setStatus(String v){status=v;}
  public String getNote(){return note;}
  public void setNote(String v){note=v;}
    }
