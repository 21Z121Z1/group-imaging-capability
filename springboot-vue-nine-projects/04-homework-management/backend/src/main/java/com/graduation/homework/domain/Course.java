package com.graduation.homework.domain;
    import com.graduation.homework.common.BaseEntity;
import jakarta.persistence.*;
    @Entity @Table(name="course_info")
    public class Course extends BaseEntity {
      @Column(nullable=false,unique=true) private String code;
  @Column(nullable=false) private String name;
  private String teacher;
  @Column(length=2000) private String description;
  @Column(nullable=false) private String status;
      public String getCode(){return code;}
  public void setCode(String v){code=v;}
  public String getName(){return name;}
  public void setName(String v){name=v;}
  public String getTeacher(){return teacher;}
  public void setTeacher(String v){teacher=v;}
  public String getDescription(){return description;}
  public void setDescription(String v){description=v;}
  public String getStatus(){return status;}
  public void setStatus(String v){status=v;}
    }
