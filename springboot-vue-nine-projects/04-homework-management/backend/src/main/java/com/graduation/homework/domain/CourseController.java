package com.graduation.homework.domain;
import com.graduation.homework.common.ApiException;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {
  private final CourseRepository repo;
  public CourseController(CourseRepository repo) { this.repo = repo; }

  public record Upsert(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=255) String code, @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=255) String name, @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=255) String teacher, @jakarta.validation.constraints.Size(max=3000) String description, @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=255) String status) {}

  @GetMapping
  public List<Course> list() {
    return repo.findAll(PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "id"))).getContent();
  }

  @GetMapping("/{id}")
  public Course one(@PathVariable Long id) {
    return repo.findById(id).orElseThrow(() -> ApiException.notFound("记录不存在"));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping
  public Course create(@Valid @RequestBody Upsert v) {
    var entity = new Course();
    apply(v, entity);
    return repo.save(entity);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping("/{id}")
  public Course update(@PathVariable Long id, @Valid @RequestBody Upsert v) {
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

  private static void apply(Upsert v, Course t) { t.setCode(v.code()); t.setName(v.name()); t.setTeacher(v.teacher()); t.setDescription(v.description()); t.setStatus(v.status()); }
}
