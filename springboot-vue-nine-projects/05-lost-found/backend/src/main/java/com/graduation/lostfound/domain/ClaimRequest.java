package com.graduation.lostfound.domain;
    import com.graduation.lostfound.common.BaseEntity;
import jakarta.persistence.*;
    @Entity @Table(name="claim_request", uniqueConstraints=@UniqueConstraint(name="uk_claim_post_user", columnNames={"postId","userId"}))
    public class ClaimRequest extends BaseEntity {
      @Column(nullable=false) private Long postId;
  @Column(nullable=false) private Long userId;
  @Column(length=2000) private String proof;
  @Column(nullable=false) private String status;
      public Long getPostId(){return postId;}
  public void setPostId(Long v){postId=v;}
  public Long getUserId(){return userId;}
  public void setUserId(Long v){userId=v;}
  public String getProof(){return proof;}
  public void setProof(String v){proof=v;}
  public String getStatus(){return status;}
  public void setStatus(String v){status=v;}
    }
