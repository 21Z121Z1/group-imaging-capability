package com.graduation.flower.domain;
    import com.graduation.flower.common.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
    @Entity @Table(name="flower_product")
    public class FlowerProduct extends BaseEntity {
      @Column(nullable=false) private String name;
  private String category;
  private String meaning;
  private String color;
  @Column(nullable=false,precision=12,scale=2) private BigDecimal price;
  private int stock;
  private String imageUrl;
  @Column(nullable=false) private String status;
      public String getName(){return name;}
  public void setName(String v){name=v;}
  public String getCategory(){return category;}
  public void setCategory(String v){category=v;}
  public String getMeaning(){return meaning;}
  public void setMeaning(String v){meaning=v;}
  public String getColor(){return color;}
  public void setColor(String v){color=v;}
  public BigDecimal getPrice(){return price;}
  public void setPrice(BigDecimal v){price=v;}
  public int getStock(){return stock;}
  public void setStock(int v){stock=v;}
  public String getImageUrl(){return imageUrl;}
  public void setImageUrl(String v){imageUrl=v;}
  public String getStatus(){return status;}
  public void setStatus(String v){status=v;}
    }
