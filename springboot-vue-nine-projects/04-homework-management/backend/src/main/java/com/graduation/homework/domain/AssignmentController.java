package com.graduation.homework.domain;
import com.graduation.homework.common.ApiException;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {
  private final AssignmentRepository repo;
  public AssignmentController(AssignmentRepository repo) { this.repo = repo; }

  public record Upsert(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long courseId, @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=255) String title, @jakarta.validation.constraints.Size(max=3000) String description, @jakarta.validation.constraints.NotNull java.time.LocalDateTime dueAt, @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.DecimalMin("0.0") java.math.BigDecimal maxScore, @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=255) String status) {}

  @GetMapping
  public List<Assignment> list() {
    return repo.findAll(PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "id"))).getContent();
  }

  @GetMapping("/{id}")
  public Assignment one(@PathVariable Long id) {
    return repo.findById(id).orElseThrow(() -> ApiException.notFound("记录不存在"));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping
  public Assignment create(@Valid @RequestBody Upsert v) {
    var entity = new Assignment();
    apply(v, entity);
    return repo.save(entity);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping("/{id}")
  public Assignment update(@PathVariable Long id, @Valid @RequestBody Upsert v) {
    var entity = repo.findById(id).orElseThrow(() -> ApiException.notFound("记录不存在"));
    apply(v, entity);
    return repo.save(entity);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    if (!repo.existsById(id)) throw ApiException.notFound("记录不存在");
    repo.deleteById(id);
  }

  private static void apply(Upsert v, Assignment t) { t.setCourseId(v.courseId()); t.setTitle(v.title()); t.setDescription(v.description()); t.setDueAt(v.dueAt()); t.setMaxScore(v.maxScore()); t.setStatus(v.status()); }
}
