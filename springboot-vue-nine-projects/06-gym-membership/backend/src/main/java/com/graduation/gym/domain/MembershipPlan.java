package com.graduation.gym.domain;
    import com.graduation.gym.common.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
    @Entity @Table(name="membership_plan")
    public class MembershipPlan extends BaseEntity {
      @Column(nullable=false) private String name;
  private int durationDays;
  @Column(nullable=false,precision=12,scale=2) private BigDecimal price;
  @Column(length=2000) private String description;
  @Column(nullable=false) private String status;
      public String getName(){return name;}
  public void setName(String v){name=v;}
  public int getDurationDays(){return durationDays;}
  public void setDurationDays(int v){durationDays=v;}
  public BigDecimal getPrice(){return price;}
  public void setPrice(BigDecimal v){price=v;}
  public String getDescription(){return description;}
  public void setDescription(String v){description=v;}
  public String getStatus(){return status;}
  public void setStatus(String v){status=v;}
    }
