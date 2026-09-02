package com.graduation.lab.domain;
    import com.graduation.lab.common.BaseEntity;
import jakarta.persistence.*;
    @Entity @Table(name="laboratory")
    public class Laboratory extends BaseEntity {
      @Column(nullable=false) private String name;
  private String building;
  private String room;
  private int capacity;
  @Column(length=2000) private String equipment;
  private String openTime;
  private String closeTime;
  @Column(nullable=false) private String status;
      public String getName(){return name;}
  public void setName(String v){name=v;}
  public String getBuilding(){return building;}
  public void setBuilding(String v){building=v;}
  public String getRoom(){return room;}
  public void setRoom(String v){room=v;}
  public int getCapacity(){return capacity;}
  public void setCapacity(int v){capacity=v;}
  public String getEquipment(){return equipment;}
  public void setEquipment(String v){equipment=v;}
  public String getOpenTime(){return openTime;}
  public void setOpenTime(String v){openTime=v;}
  public String getCloseTime(){return closeTime;}
  public void setCloseTime(String v){closeTime=v;}
  public String getStatus(){return status;}
  public void setStatus(String v){status=v;}
    }
