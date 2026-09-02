package com.graduation.club.domain;
import com.graduation.club.common.ApiException;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/activities")
public class ClubActivityController {
  private final ClubActivityRepository repo;
  public ClubActivityController(ClubActivityRepository repo) { this.repo = repo; }

  public record Upsert(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long clubId, @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=255) String title, @jakarta.validation.constraints.Size(max=255) String location, @jakarta.validation.constraints.NotNull java.time.LocalDateTime startTime, @jakarta.validation.constraints.NotNull java.time.LocalDateTime endTime, @jakarta.validation.constraints.Positive int capacity, @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=255) String status) {}

  @GetMapping
  public List<ClubActivity> list() {
    return repo.findAll(PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "id"))).getContent();
  }

  @GetMapping("/{id}")
  public ClubActivity one(@PathVariable Long id) {
    return repo.findById(id).orElseThrow(() -> ApiException.notFound("记录不存在"));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping
  public ClubActivity create(@Valid @RequestBody Upsert v) {
    validate(v);
    var entity = new ClubActivity();
    apply(v, entity);
    return repo.save(entity);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping("/{id}")
  public ClubActivity update(@PathVariable Long id, @Valid @RequestBody Upsert v) {
    validate(v);
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

  private static void validate(Upsert v) { if (!v.endTime().isAfter(v.startTime())) throw ApiException.badRequest("活动结束时间必须晚于开始时间"); }

  private static void apply(Upsert v, ClubActivity t) { t.setClubId(v.clubId()); t.setTitle(v.title()); t.setLocation(v.location()); t.setStartTime(v.startTime()); t.setEndTime(v.endTime()); t.setCapacity(v.capacity()); t.setStatus(v.status()); }
}
