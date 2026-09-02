package com.graduation.library.domain;

import com.graduation.library.auth.AuthService;
import com.graduation.library.common.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/borrows")
public class BorrowController {
  private final BorrowRecordRepository borrows;
  private final BookRepository books;
  private final AuthService auth;

  public BorrowController(BorrowRecordRepository borrows, BookRepository books, AuthService auth) {
    this.borrows = borrows;
    this.books = books;
    this.auth = auth;
  }

  public record Create(@NotNull @Positive Long bookId) {}

  @PostMapping
  @Transactional
  public BorrowRecord borrow(@Valid @RequestBody Create request) {
    var user = auth.currentUser();
    var book = books.findLockedById(request.bookId()).orElseThrow(() -> ApiException.notFound("图书不存在"));
    if (!"AVAILABLE".equals(book.getStatus())) throw ApiException.badRequest("图书当前不可借阅");
    if (!borrows.findActiveLocked(book.getId(), user.getId()).isEmpty()) throw ApiException.badRequest("不可重复借阅");
    if (book.getAvailableCopies() <= 0) throw ApiException.badRequest("暂无可借副本");
    book.setAvailableCopies(book.getAvailableCopies() - 1);
    var now = LocalDateTime.now();
    var record = new BorrowRecord();
    record.setBookId(book.getId());
    record.setUserId(user.getId());
    record.setBorrowedAt(now);
    record.setDueAt(now.plusDays(30));
    record.setStatus("BORROWED");
    return borrows.save(record);
  }

  @PostMapping("/{id}/return")
  @Transactional
  public BorrowRecord returnBook(@PathVariable @Positive Long id) {
    var user = auth.currentUser();
    var snapshot = borrows.findById(id).orElseThrow(() -> ApiException.notFound("借阅记录不存在"));
    var book = books.findLockedById(snapshot.getBookId()).orElseThrow(() -> ApiException.notFound("图书不存在"));
    var record = borrows.findLockedById(id).orElseThrow(() -> ApiException.notFound("借阅记录不存在"));
    if (!Objects.equals(record.getUserId(), user.getId()) && !"ADMIN".equals(user.getRole()))
      throw new ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "无权限");
    if (!"BORROWED".equals(record.getStatus())) throw ApiException.badRequest("当前状态不可归还");
    if (!Objects.equals(book.getId(), record.getBookId()))
      throw new ApiException(org.springframework.http.HttpStatus.CONFLICT, "借阅记录发生变化，请重试");
    book.setAvailableCopies(book.getAvailableCopies() + 1);
    record.setStatus("RETURNED");
    record.setReturnedAt(LocalDateTime.now());
    return record;
  }

  @GetMapping("/mine")
  public List<BorrowRecord> mine() { return borrows.findTop100ByUserIdOrderByCreatedAtDesc(auth.currentUser().getId()); }

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/admin")
  public List<BorrowRecord> all() { return borrows.findAllByOrderByCreatedAtDesc(PageRequest.of(0,100)); }
}
