package com.graduation.snack.domain;
    import com.graduation.snack.common.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
    @Entity @Table(name="group_campaign")
    public class GroupCampaign extends BaseEntity {
      @Column(nullable=false) private Long productId;
  @Column(nullable=false) private String title;
  @Column(nullable=false,precision=12,scale=2) private BigDecimal groupPrice;
  private int targetQuantity;
  private int soldQuantity;
  @Column(nullable=false) private LocalDateTime startTime;
  @Column(nullable=false) private LocalDateTime endTime;
  @Column(nullable=false) private String status;
      public Long getProductId(){return productId;}
  public void setProductId(Long v){productId=v;}
  public String getTitle(){return title;}
  public void setTitle(String v){title=v;}
  public BigDecimal getGroupPrice(){return groupPrice;}
  public void setGroupPrice(BigDecimal v){groupPrice=v;}
  public int getTargetQuantity(){return targetQuantity;}
  public void setTargetQuantity(int v){targetQuantity=v;}
  public int getSoldQuantity(){return soldQuantity;}
  public void setSoldQuantity(int v){soldQuantity=v;}
  public LocalDateTime getStartTime(){return startTime;}
  public void setStartTime(LocalDateTime v){startTime=v;}
  public LocalDateTime getEndTime(){return endTime;}
  public void setEndTime(LocalDateTime v){endTime=v;}
  public String getStatus(){return status;}
  public void setStatus(String v){status=v;}
    }
