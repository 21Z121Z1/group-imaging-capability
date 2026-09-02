package com.graduation.club.domain;
    import com.graduation.club.common.BaseEntity;
import jakarta.persistence.*;
    @Entity @Table(name="club")
    public class Club extends BaseEntity {
      @Column(nullable=false) private String name;
  @Column(nullable=false) private String category;
  @Column(length=2000) private String description;
  private String president;
  private String contact;
  private int memberLimit;
  @Column(nullable=false) private String status;
      public String getName(){return name;}
  public void setName(String v){name=v;}
  public String getCategory(){return category;}
  public void setCategory(String v){category=v;}
  public String getDescription(){return description;}
  public void setDescription(String v){description=v;}
  public String getPresident(){return president;}
  public void setPresident(String v){president=v;}
  public String getContact(){return contact;}
  public void setContact(String v){contact=v;}
  public int getMemberLimit(){return memberLimit;}
  public void setMemberLimit(int v){memberLimit=v;}
  public String getStatus(){return status;}
  public void setStatus(String v){status=v;}
    }
