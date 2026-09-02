package com.graduation.club.domain;
import com.graduation.club.common.ApiException;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/clubs")
public class ClubController {
  private final ClubRepository repo;
  public ClubController(ClubRepository repo) { this.repo = repo; }

  public record Upsert(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=255) String name, @jakarta.validation.constraints.Size(max=255) String category, @jakarta.validation.constraints.Size(max=3000) String description, @jakarta.validation.constraints.Size(max=255) String president, @jakarta.validation.constraints.Size(max=255) String contact, @jakarta.validation.constraints.Positive int memberLimit, @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=255) String status) {}

  @GetMapping
  public List<Club> list() {
    return repo.findAll(PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "id"))).getContent();
  }

  @GetMapping("/{id}")
  public Club one(@PathVariable Long id) {
    return repo.findById(id).orElseThrow(() -> ApiException.notFound("记录不存在"));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping
  public Club create(@Valid @RequestBody Upsert v) {
    var entity = new Club();
    apply(v, entity);
    return repo.save(entity);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping("/{id}")
  public Club update(@PathVariable Long id, @Valid @RequestBody Upsert v) {
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

  private static void apply(Upsert v, Club t) { t.setName(v.name()); t.setCategory(v.category()); t.setDescription(v.description()); t.setPresident(v.president()); t.setContact(v.contact()); t.setMemberLimit(v.memberLimit()); t.setStatus(v.status()); }
}
