package com.graduation.library.domain;
import com.graduation.library.common.ApiException;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {
  private final BookRepository repo;
  public BookController(BookRepository repo) { this.repo = repo; }

  public record Upsert(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=255) String isbn, @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=255) String title, @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=255) String author, @jakarta.validation.constraints.Size(max=255) String publisher, @jakarta.validation.constraints.Size(max=255) String category, @jakarta.validation.constraints.Positive int totalCopies, @jakarta.validation.constraints.Size(max=3000) String description, @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=255) String status) {}

  @GetMapping
  public List<Book> list() {
    return repo.findAll(PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "id"))).getContent();
  }

  @GetMapping("/{id}")
  public Book one(@PathVariable Long id) {
    return repo.findById(id).orElseThrow(() -> ApiException.notFound("记录不存在"));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping
  public Book create(@Valid @RequestBody Upsert v) {
    var entity = new Book();
    apply(v, entity);
    entity.setAvailableCopies(v.totalCopies());
    return repo.save(entity);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping("/{id}")
  public Book update(@PathVariable Long id, @Valid @RequestBody Upsert v) {
    var entity = repo.findById(id).orElseThrow(() -> ApiException.notFound("记录不存在"));
    int borrowed = entity.getTotalCopies() - entity.getAvailableCopies();
    if (v.totalCopies() < borrowed) throw ApiException.badRequest("总副本数不能小于当前借出数");
    apply(v, entity);
    entity.setAvailableCopies(v.totalCopies() - borrowed);
    return repo.save(entity);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    if (!repo.existsById(id)) throw ApiException.notFound("记录不存在");
    repo.deleteById(id);
  }

  private static void apply(Upsert v, Book t) { t.setIsbn(v.isbn()); t.setTitle(v.title()); t.setAuthor(v.author()); t.setPublisher(v.publisher()); t.setCategory(v.category()); t.setTotalCopies(v.totalCopies()); t.setDescription(v.description()); t.setStatus(v.status()); }
}
