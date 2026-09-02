package com.graduation.flower.domain;
    import com.graduation.flower.common.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
    @Entity @Table(name="flower_order")
    public class FlowerOrder extends BaseEntity {
      @Column(nullable=false) private Long userId;
  @Column(nullable=false) private Long productId;
  private int quantity;
  @Column(nullable=false,precision=12,scale=2) private BigDecimal totalAmount;
  @Column(nullable=false) private String recipientName;
  @Column(nullable=false) private String recipientPhone;
  @Column(nullable=false) private String deliveryAddress;
  @Column(nullable=false) private LocalDate deliveryDate;
  @Column(length=1000) private String message;
  @Column(nullable=false) private String status;
      public Long getUserId(){return userId;}
  public void setUserId(Long v){userId=v;}
  public Long getProductId(){return productId;}
  public void setProductId(Long v){productId=v;}
  public int getQuantity(){return quantity;}
  public void setQuantity(int v){quantity=v;}
  public BigDecimal getTotalAmount(){return totalAmount;}
  public void setTotalAmount(BigDecimal v){totalAmount=v;}
  public String getRecipientName(){return recipientName;}
  public void setRecipientName(String v){recipientName=v;}
  public String getRecipientPhone(){return recipientPhone;}
  public void setRecipientPhone(String v){recipientPhone=v;}
  public String getDeliveryAddress(){return deliveryAddress;}
  public void setDeliveryAddress(String v){deliveryAddress=v;}
  public LocalDate getDeliveryDate(){return deliveryDate;}
  public void setDeliveryDate(LocalDate v){deliveryDate=v;}
  public String getMessage(){return message;}
  public void setMessage(String v){message=v;}
  public String getStatus(){return status;}
  public void setStatus(String v){status=v;}
    }
