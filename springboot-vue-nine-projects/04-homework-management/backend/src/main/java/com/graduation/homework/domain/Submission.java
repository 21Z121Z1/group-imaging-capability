package com.graduation.homework.domain;
    import com.graduation.homework.common.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
    @Entity @Table(name="submission_info", uniqueConstraints=@UniqueConstraint(name="uk_submission_assignment_user", columnNames={"assignmentId","userId"}))
    public class Submission extends BaseEntity {
      @Column(nullable=false) private Long assignmentId;
  @Column(nullable=false) private Long userId;
  @Column(length=8000) private String content;
  private String attachmentUrl;
  @Column(nullable=false) private LocalDateTime submittedAt;
  @Column(precision=8,scale=2) private BigDecimal score;
  @Column(length=2000) private String feedback;
  @Column(nullable=false) private String status;
      public Long getAssignmentId(){return assignmentId;}
  public void setAssignmentId(Long v){assignmentId=v;}
  public Long getUserId(){return userId;}
  public void setUserId(Long v){userId=v;}
  public String getContent(){return content;}
  public void setContent(String v){content=v;}
  public String getAttachmentUrl(){return attachmentUrl;}
  public void setAttachmentUrl(String v){attachmentUrl=v;}
  public LocalDateTime getSubmittedAt(){return submittedAt;}
  public void setSubmittedAt(LocalDateTime v){submittedAt=v;}
  public BigDecimal getScore(){return score;}
  public void setScore(BigDecimal v){score=v;}
  public String getFeedback(){return feedback;}
  public void setFeedback(String v){feedback=v;}
  public String getStatus(){return status;}
  public void setStatus(String v){status=v;}
    }
