package com.graduation.library.domain;
    import com.graduation.library.common.BaseEntity;
import jakarta.persistence.*;
    @Entity @Table(name="library_book")
    public class Book extends BaseEntity {
      @Column(nullable=false,unique=true) private String isbn;
  @Column(nullable=false) private String title;
  private String author;
  private String publisher;
  private String category;
  private int totalCopies;
  private int availableCopies;
  @Column(length=3000) private String description;
  @Column(nullable=false) private String status;
      public String getIsbn(){return isbn;}
  public void setIsbn(String v){isbn=v;}
  public String getTitle(){return title;}
  public void setTitle(String v){title=v;}
  public String getAuthor(){return author;}
  public void setAuthor(String v){author=v;}
  public String getPublisher(){return publisher;}
  public void setPublisher(String v){publisher=v;}
  public String getCategory(){return category;}
  public void setCategory(String v){category=v;}
  public int getTotalCopies(){return totalCopies;}
  public void setTotalCopies(int v){totalCopies=v;}
  public int getAvailableCopies(){return availableCopies;}
  public void setAvailableCopies(int v){availableCopies=v;}
  public String getDescription(){return description;}
  public void setDescription(String v){description=v;}
  public String getStatus(){return status;}
  public void setStatus(String v){status=v;}
    }
