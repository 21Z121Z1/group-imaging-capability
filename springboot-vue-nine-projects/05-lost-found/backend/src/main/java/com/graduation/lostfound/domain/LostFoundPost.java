package com.graduation.lostfound.domain;
    import com.graduation.lostfound.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
    @Entity @Table(name="lost_found_post")
    public class LostFoundPost extends BaseEntity {
      @Column(nullable=false) private String type;
  @Column(nullable=false) private String title;
  private String category;
  private String location;
  private LocalDateTime eventTime;
  @Column(length=3000) private String description;
  private String contact;
  @Column(nullable=false) private Long ownerUserId;
  @Column(nullable=false) private String status;
  private String imageUrl;
      public String getType(){return type;}
  public void setType(String v){type=v;}
  public String getTitle(){return title;}
  public void setTitle(String v){title=v;}
  public String getCategory(){return category;}
  public void setCategory(String v){category=v;}
  public String getLocation(){return location;}
  public void setLocation(String v){location=v;}
  public LocalDateTime getEventTime(){return eventTime;}
  public void setEventTime(LocalDateTime v){eventTime=v;}
  public String getDescription(){return description;}
  public void setDescription(String v){description=v;}
  public String getContact(){return contact;}
  public void setContact(String v){contact=v;}
  public Long getOwnerUserId(){return ownerUserId;}
  public void setOwnerUserId(Long v){ownerUserId=v;}
  public String getStatus(){return status;}
  public void setStatus(String v){status=v;}
  public String getImageUrl(){return imageUrl;}
  public void setImageUrl(String v){imageUrl=v;}
    }
