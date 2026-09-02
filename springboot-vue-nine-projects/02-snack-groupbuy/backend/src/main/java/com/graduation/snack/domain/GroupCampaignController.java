package com.graduation.snack.domain;
import com.graduation.snack.common.ApiException;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/campaigns")
public class GroupCampaignController {
  private final GroupCampaignRepository repo;
  public GroupCampaignController(GroupCampaignRepository repo) { this.repo = repo; }

  public record Upsert(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long productId, @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=255) String title, @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.DecimalMin("0.0") java.math.BigDecimal groupPrice, @jakarta.validation.constraints.Positive int targetQuantity, @jakarta.validation.constraints.NotNull java.time.LocalDateTime startTime, @jakarta.validation.constraints.NotNull java.time.LocalDateTime endTime, @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=255) String status) {}

  @GetMapping
  public List<GroupCampaign> list() {
    return repo.findAll(PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "id"))).getContent();
  }

  @GetMapping("/{id}")
  public GroupCampaign one(@PathVariable Long id) {
    return repo.findById(id).orElseThrow(() -> ApiException.notFound("记录不存在"));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping
  public GroupCampaign create(@Valid @RequestBody Upsert v) {
    validate(v);
    var entity = new GroupCampaign();
    apply(v, entity);
    entity.setSoldQuantity(0);
    return repo.save(entity);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping("/{id}")
  public GroupCampaign update(@PathVariable Long id, @Valid @RequestBody Upsert v) {
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

  private static void validate(Upsert v) { if (!v.endTime().isAfter(v.startTime())) throw ApiException.badRequest("团购结束时间必须晚于开始时间"); }

  private static void apply(Upsert v, GroupCampaign t) { t.setProductId(v.productId()); t.setTitle(v.title()); t.setGroupPrice(v.groupPrice()); t.setTargetQuantity(v.targetQuantity()); t.setStartTime(v.startTime()); t.setEndTime(v.endTime()); t.setStatus(v.status()); }
}
