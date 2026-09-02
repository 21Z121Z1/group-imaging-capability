package com.graduation.snack.domain;
    import com.graduation.snack.common.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
    @Entity @Table(name="group_order")
    public class GroupOrder extends BaseEntity {
      @Column(nullable=false) private Long campaignId;
  @Column(nullable=false) private Long userId;
  private int quantity;
  @Column(nullable=false,precision=12,scale=2) private BigDecimal totalAmount;
  @Column(nullable=false) private String status;
      public Long getCampaignId(){return campaignId;}
  public void setCampaignId(Long v){campaignId=v;}
  public Long getUserId(){return userId;}
  public void setUserId(Long v){userId=v;}
  public int getQuantity(){return quantity;}
  public void setQuantity(int v){quantity=v;}
  public BigDecimal getTotalAmount(){return totalAmount;}
  public void setTotalAmount(BigDecimal v){totalAmount=v;}
  public String getStatus(){return status;}
  public void setStatus(String v){status=v;}
    }
