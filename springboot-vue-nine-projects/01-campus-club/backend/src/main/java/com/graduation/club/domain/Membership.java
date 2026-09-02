package com.graduation.club.domain;
    import com.graduation.club.common.BaseEntity;
import jakarta.persistence.*;
    @Entity @Table(name="club_membership", uniqueConstraints=@UniqueConstraint(name="uk_membership_club_user", columnNames={"clubId","userId"}))
    public class Membership extends BaseEntity {
      @Column(nullable=false) private Long clubId;
  @Column(nullable=false) private Long userId;
  @Column(length=1000) private String motivation;
  @Column(nullable=false) private String status;
      public Long getClubId(){return clubId;}
  public void setClubId(Long v){clubId=v;}
  public Long getUserId(){return userId;}
  public void setUserId(Long v){userId=v;}
  public String getMotivation(){return motivation;}
  public void setMotivation(String v){motivation=v;}
  public String getStatus(){return status;}
  public void setStatus(String v){status=v;}
    }
