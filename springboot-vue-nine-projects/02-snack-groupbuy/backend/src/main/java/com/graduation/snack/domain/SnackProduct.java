package com.graduation.snack.domain;
    import com.graduation.snack.common.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
    @Entity @Table(name="snack_product")
    public class SnackProduct extends BaseEntity {
      @Column(nullable=false) private String name;
  private String brand;
  private String category;
  @Column(nullable=false,precision=12,scale=2) private BigDecimal price;
  private String imageUrl;
  private int stock;
  @Column(nullable=false) private String status;
      public String getName(){return name;}
  public void setName(String v){name=v;}
  public String getBrand(){return brand;}
  public void setBrand(String v){brand=v;}
  public String getCategory(){return category;}
  public void setCategory(String v){category=v;}
  public BigDecimal getPrice(){return price;}
  public void setPrice(BigDecimal v){price=v;}
  public String getImageUrl(){return imageUrl;}
  public void setImageUrl(String v){imageUrl=v;}
  public int getStock(){return stock;}
  public void setStock(int v){stock=v;}
  public String getStatus(){return status;}
  public void setStatus(String v){status=v;}
    }
