package com.graduation.flower.domain;

import com.graduation.flower.auth.AuthService;
import com.graduation.flower.common.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class FlowerOrderController {
  private final FlowerOrderRepository orders;
  private final FlowerProductRepository products;
  private final AuthService auth;

  public FlowerOrderController(FlowerOrderRepository orders, FlowerProductRepository products, AuthService auth) {
    this.orders = orders;
    this.products = products;
    this.auth = auth;
  }

  public record OrderRequest(@NotNull Long productId, @Min(1) @Max(100) int quantity,
                             @NotBlank @Size(max=64) String recipient,
                             @NotBlank @Size(max=64) String phone,
                             @NotBlank @Size(max=255) String address) {}

  @PostMapping
  @Transactional
  public FlowerOrder order(@RequestBody @Valid OrderRequest request) {
    var user = auth.currentUser();
    var product = products.findLockedById(request.productId()).orElseThrow(() -> ApiException.notFound("花卉"));
    if (!product.isEnabled()) throw ApiException.badRequest("商品已下架");
    if (product.getStock() < request.quantity()) throw ApiException.badRequest("库存不足");

    product.setStock(product.getStock() - request.quantity());
    var order = new FlowerOrder();
    order.setUserId(user.getId());
    order.setProductId(product.getId());
    order.setQuantity(request.quantity());
    order.setUnitPrice(product.getPrice());
    order.setTotalAmount(product.getPrice().multiply(java.math.BigDecimal.valueOf(request.quantity())));
    order.setRecipient(request.recipient());
    order.setPhone(request.phone());
    order.setAddress(request.address());
    return orders.save(order);
  }

  @GetMapping("/mine")
  public List<FlowerOrder> mine() {
    return orders.findTop100ByUserIdOrderByCreatedAtDesc(auth.currentUser().getId());
  }

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/admin")
  public List<FlowerOrder> all() {
    return orders.findAllByOrderByCreatedAtDesc(PageRequest.of(0,100));
  }
}
