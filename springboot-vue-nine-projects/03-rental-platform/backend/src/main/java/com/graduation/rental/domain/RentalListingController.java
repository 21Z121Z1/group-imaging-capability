package com.graduation.rental.domain;
import com.graduation.rental.auth.AuthService;
import com.graduation.rental.common.ApiException;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/listings")
public class RentalListingController {
  private final RentalListingRepository repo; private final AuthService auth;
  public RentalListingController(RentalListingRepository repo, AuthService auth) { this.repo=repo; this.auth=auth; }
  public record Create(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=255) String title,
                       @jakarta.validation.constraints.Size(max=128) String city,
                       @jakarta.validation.constraints.Size(max=128) String district,
                       @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=512) String address,
                       @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.DecimalMin("0.0") BigDecimal monthlyRent,
                       @jakarta.validation.constraints.PositiveOrZero int bedrooms,
                       @jakarta.validation.constraints.DecimalMin("0.0") BigDecimal areaSqm,
                       @jakarta.validation.constraints.Size(max=3000) String description,
                       @jakarta.validation.constraints.Size(max=255) String contact) {}
  @GetMapping public List<RentalListing> list() { return repo.findAll(PageRequest.of(0,200,Sort.by(Sort.Direction.DESC,"id"))).getContent(); }
  @PostMapping public RentalListing create(@Valid @RequestBody Create v) {
    var x=new RentalListing(); x.setTitle(v.title()); x.setCity(v.city()); x.setDistrict(v.district()); x.setAddress(v.address());
    x.setMonthlyRent(v.monthlyRent()); x.setBedrooms(v.bedrooms()); x.setAreaSqm(v.areaSqm()); x.setDescription(v.description()); x.setContact(v.contact());
    x.setOwnerUserId(auth.currentUser().getId()); x.setStatus("PUBLISHED"); return repo.save(x);
  }
  @DeleteMapping("/{id}") public void delete(@PathVariable Long id) { var x=repo.findById(id).orElseThrow(()->ApiException.notFound("房源不存在")); var u=auth.currentUser(); if(!"ADMIN".equals(u.getRole())&&!u.getId().equals(x.getOwnerUserId())) throw ApiException.forbidden("只能删除自己的房源"); repo.delete(x); }
}
