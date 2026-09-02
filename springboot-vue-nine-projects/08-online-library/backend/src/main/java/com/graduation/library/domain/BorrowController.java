package com.graduation.library.domain;

import com.graduation.library.auth.AuthService;
import com.graduation.library.common.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
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

  @PostMapping
  @Transactional
  public BorrowRecord borrow(@RequestParam Long bookId) {
    var user = auth.currentUser();
    var book = books.findLockedById(bookId).orElseThrow(() -> ApiException.notFound("图书"));
    if (!borrows.findActiveLocked(bookId, user.getId()).isEmpty()) throw ApiException.badRequest("不可重复借阅");
    if (book.getAvailableCopies() <= 0) throw ApiException.badRequest("暂无可借副本");

    book.setAvailableCopies(book.getAvailableCopies() - 1);
    var record = new BorrowRecord();
    record.setBookId(bookId);
    record.setUserId(user.getId());
    record.setBorrowDate(LocalDate.now());
    record.setDueDate(LocalDate.now().plusDays(30));
    return borrows.save(record);
  }

  @PostMapping("/{id}/return")
  @Transactional
  public BorrowRecord returnBook(@PathVariable Long id) {
    var user = auth.currentUser();
    var snapshot = borrows.findById(id).orElseThrow(() -> ApiException.notFound("借阅记录"));
    books.findLockedById(snapshot.getBookId()).orElseThrow(() -> ApiException.notFound("图书"));
    var record = borrows.findLockedById(id).orElseThrow(() -> ApiException.notFound("借阅记录"));

    if (!Objects.equals(record.getUserId(), user.getId()) && !user.isAdmin())
      throw new ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "无权限");
    if (!"BORROWED".equals(record.getStatus())) throw ApiException.badRequest("当前状态不可归还");

    var book = books.findLockedById(record.getBookId()).orElseThrow(() -> ApiException.notFound("图书"));
    book.setAvailableCopies(book.getAvailableCopies() + 1);
    record.setStatus("RETURNED");
    record.setReturnDate(LocalDate.now());
    return record;
  }

  @GetMapping("/mine")
  public List<BorrowRecord> mine() {
    return borrows.findTop100ByUserIdOrderByCreatedAtDesc(auth.currentUser().getId());
  }

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/admin")
  public List<BorrowRecord> all() {
    return borrows.findAllByOrderByCreatedAtDesc(PageRequest.of(0,100));
  }
}
