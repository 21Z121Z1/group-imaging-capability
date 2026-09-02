package com.graduation.homework.common;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<Map<String,Object>> api(ApiException ex) { return body(ex.getStatus(), ex.getMessage()); }
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String,Object>> validation(MethodArgumentNotValidException ex) {
    var errors=new LinkedHashMap<String,String>();
    ex.getBindingResult().getFieldErrors().forEach(e -> errors.putIfAbsent(e.getField(), e.getDefaultMessage()));
    var body=new LinkedHashMap<String,Object>(); body.put("timestamp", LocalDateTime.now()); body.put("status",400); body.put("message","参数校验失败"); body.put("errors",errors);
    return ResponseEntity.badRequest().body(body);
  }
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Map<String,Object>> malformed(HttpMessageNotReadableException ex) { return body(HttpStatus.BAD_REQUEST, "请求体格式错误"); }
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Map<String,Object>> integrity(DataIntegrityViolationException ex) { return body(HttpStatus.CONFLICT, "数据约束冲突"); }
  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  public ResponseEntity<Map<String,Object>> optimistic(ObjectOptimisticLockingFailureException ex) { return body(HttpStatus.CONFLICT, "数据已被其他请求修改，请刷新后重试"); }
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String,Object>> other(Exception ex) { return body(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误"); }
  private ResponseEntity<Map<String,Object>> body(HttpStatus status, String message) {
    var body=new LinkedHashMap<String,Object>(); body.put("timestamp", LocalDateTime.now()); body.put("status",status.value()); body.put("message",message);
    return ResponseEntity.status(status).body(body);
  }
}
