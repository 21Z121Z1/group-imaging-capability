package com.graduation.snack.domain;
import com.graduation.snack.common.ApiException;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class SnackProductController {
  private final SnackProductRepository repo;
  public SnackProductController(SnackProductRepository repo) { this.repo = repo; }

  public record Upsert(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=255) String name, @jakarta.validation.constraints.Size(max=255) String brand, @jakarta.validation.constraints.Size(max=255) String category, @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.DecimalMin("0.0") java.math.BigDecimal price, @jakarta.validation.constraints.Size(max=512) String imageUrl, @jakarta.validation.constraints.PositiveOrZero int stock, @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=255) String status) {}

  @GetMapping
  public List<SnackProduct> list() {
    return repo.findAll(PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "id"))).getContent();
  }

  @GetMapping("/{id}")
  public SnackProduct one(@PathVariable Long id) {
    return repo.findById(id).orElseThrow(() -> ApiException.notFound("记录不存在"));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping
  public SnackProduct create(@Valid @RequestBody Upsert v) {
    var entity = new SnackProduct();
    apply(v, entity);
    return repo.save(entity);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping("/{id}")
  public SnackProduct update(@PathVariable Long id, @Valid @RequestBody Upsert v) {
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

  private static void apply(Upsert v, SnackProduct t) { t.setName(v.name()); t.setBrand(v.brand()); t.setCategory(v.category()); t.setPrice(v.price()); t.setImageUrl(v.imageUrl()); t.setStock(v.stock()); t.setStatus(v.status()); }
}
