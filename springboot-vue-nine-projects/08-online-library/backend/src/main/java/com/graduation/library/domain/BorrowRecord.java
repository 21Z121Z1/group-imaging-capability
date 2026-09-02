package com.graduation.library.domain;
    import com.graduation.library.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
    @Entity @Table(name="borrow_record")
    public class BorrowRecord extends BaseEntity {
      @Column(nullable=false) private Long bookId;
  @Column(nullable=false) private Long userId;
  @Column(nullable=false) private LocalDateTime borrowedAt;
  @Column(nullable=false) private LocalDateTime dueAt;
  private LocalDateTime returnedAt;
  @Column(nullable=false) private String status;
      public Long getBookId(){return bookId;}
  public void setBookId(Long v){bookId=v;}
  public Long getUserId(){return userId;}
  public void setUserId(Long v){userId=v;}
  public LocalDateTime getBorrowedAt(){return borrowedAt;}
  public void setBorrowedAt(LocalDateTime v){borrowedAt=v;}
  public LocalDateTime getDueAt(){return dueAt;}
  public void setDueAt(LocalDateTime v){dueAt=v;}
  public LocalDateTime getReturnedAt(){return returnedAt;}
  public void setReturnedAt(LocalDateTime v){returnedAt=v;}
  public String getStatus(){return status;}
  public void setStatus(String v){status=v;}
    }
