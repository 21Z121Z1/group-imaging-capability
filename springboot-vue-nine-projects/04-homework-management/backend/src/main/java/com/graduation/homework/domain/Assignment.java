package com.graduation.homework.domain;
    import com.graduation.homework.common.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
    @Entity @Table(name="assignment_info")
    public class Assignment extends BaseEntity {
      @Column(nullable=false) private Long courseId;
  @Column(nullable=false) private String title;
  @Column(length=3000) private String description;
  @Column(nullable=false) private LocalDateTime dueAt;
  @Column(nullable=false,precision=8,scale=2) private BigDecimal maxScore;
  @Column(nullable=false) private String status;
      public Long getCourseId(){return courseId;}
  public void setCourseId(Long v){courseId=v;}
  public String getTitle(){return title;}
  public void setTitle(String v){title=v;}
  public String getDescription(){return description;}
  public void setDescription(String v){description=v;}
  public LocalDateTime getDueAt(){return dueAt;}
  public void setDueAt(LocalDateTime v){dueAt=v;}
  public BigDecimal getMaxScore(){return maxScore;}
  public void setMaxScore(BigDecimal v){maxScore=v;}
  public String getStatus(){return status;}
  public void setStatus(String v){status=v;}
    }
