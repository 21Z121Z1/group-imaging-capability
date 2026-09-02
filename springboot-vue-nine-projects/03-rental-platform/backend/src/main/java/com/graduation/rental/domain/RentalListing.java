package com.graduation.rental.domain;
    import com.graduation.rental.common.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
    @Entity @Table(name="rental_listing")
    public class RentalListing extends BaseEntity {
      @Column(nullable=false) private String title;
  private String city;
  private String district;
  @Column(nullable=false) private String address;
  @Column(nullable=false,precision=12,scale=2) private BigDecimal monthlyRent;
  private int bedrooms;
  @Column(precision=10,scale=2) private BigDecimal areaSqm;
  @Column(length=3000) private String description;
  private String contact;
  @Column(nullable=false) private Long ownerUserId;
  @Column(nullable=false) private String status;
      public String getTitle(){return title;}
  public void setTitle(String v){title=v;}
  public String getCity(){return city;}
  public void setCity(String v){city=v;}
  public String getDistrict(){return district;}
  public void setDistrict(String v){district=v;}
  public String getAddress(){return address;}
  public void setAddress(String v){address=v;}
  public BigDecimal getMonthlyRent(){return monthlyRent;}
  public void setMonthlyRent(BigDecimal v){monthlyRent=v;}
  public int getBedrooms(){return bedrooms;}
  public void setBedrooms(int v){bedrooms=v;}
  public BigDecimal getAreaSqm(){return areaSqm;}
  public void setAreaSqm(BigDecimal v){areaSqm=v;}
  public String getDescription(){return description;}
  public void setDescription(String v){description=v;}
  public String getContact(){return contact;}
  public void setContact(String v){contact=v;}
  public Long getOwnerUserId(){return ownerUserId;}
  public void setOwnerUserId(Long v){ownerUserId=v;}
  public String getStatus(){return status;}
  public void setStatus(String v){status=v;}
    }
