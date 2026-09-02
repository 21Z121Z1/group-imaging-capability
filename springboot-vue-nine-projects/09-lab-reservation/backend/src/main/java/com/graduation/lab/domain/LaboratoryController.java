package com.graduation.lab.domain;
import com.graduation.lab.common.ApiException;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/labs")
public class LaboratoryController {
  private final LaboratoryRepository repo;
  public LaboratoryController(LaboratoryRepository repo) { this.repo = repo; }

  public record Upsert(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=255) String name, @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=255) String building, @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=255) String room, @jakarta.validation.constraints.Positive int capacity, @jakarta.validation.constraints.Size(max=1000) String equipment, @jakarta.validation.constraints.Size(max=255) String openTime, @jakarta.validation.constraints.Size(max=255) String closeTime, @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=255) String status) {}

  @GetMapping
  public List<Laboratory> list() {
    return repo.findAll(PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "id"))).getContent();
  }

  @GetMapping("/{id}")
  public Laboratory one(@PathVariable Long id) {
    return repo.findById(id).orElseThrow(() -> ApiException.notFound("记录不存在"));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping
  public Laboratory create(@Valid @RequestBody Upsert v) {
    var entity = new Laboratory();
    apply(v, entity);
    return repo.save(entity);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping("/{id}")
  public Laboratory update(@PathVariable Long id, @Valid @RequestBody Upsert v) {
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

  private static void apply(Upsert v, Laboratory t) { t.setName(v.name()); t.setBuilding(v.building()); t.setRoom(v.room()); t.setCapacity(v.capacity()); t.setEquipment(v.equipment()); t.setOpenTime(v.openTime()); t.setCloseTime(v.closeTime()); t.setStatus(v.status()); }
}
