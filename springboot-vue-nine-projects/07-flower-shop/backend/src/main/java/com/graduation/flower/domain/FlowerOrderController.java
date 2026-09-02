package com.graduation.flower.domain;

import com.graduation.flower.auth.AuthService;
import com.graduation.flower.common.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
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

  public record OrderRequest(@NotNull @Positive Long productId,
      @Min(1) @Max(100) int quantity,
      @NotBlank @Size(max=64) String recipientName,
      @NotBlank @Pattern(regexp="^[0-9+() -]{6,32}$") String recipientPhone,
      @NotBlank @Size(max=255) String deliveryAddress,
      @NotNull @FutureOrPresent LocalDate deliveryDate,
      @Size(max=1000) String message) {}

  @PostMapping
  @Transactional
  public FlowerOrder order(@RequestBody @Valid OrderRequest request) {
    var user = auth.currentUser();
    var product = products.findLockedById(request.productId()).orElseThrow(() -> ApiException.notFound("商品不存在"));
    if (!"ON_SALE".equals(product.getStatus())) throw ApiException.badRequest("商品已下架");
    if (product.getStock() < request.quantity()) throw ApiException.badRequest("库存不足");
    product.setStock(product.getStock() - request.quantity());
    var order = new FlowerOrder();
    order.setUserId(user.getId());
    order.setProductId(product.getId());
    order.setQuantity(request.quantity());
    order.setTotalAmount(product.getPrice().multiply(java.math.BigDecimal.valueOf(request.quantity())));
    order.setRecipientName(request.recipientName());
    order.setRecipientPhone(request.recipientPhone());
    order.setDeliveryAddress(request.deliveryAddress());
    order.setDeliveryDate(request.deliveryDate());
    order.setMessage(request.message());
    order.setStatus("PAID");
    return orders.save(order);
  }

  @GetMapping
  public List<FlowerOrder> list() {
    var user = auth.currentUser();
    return "ADMIN".equals(user.getRole())
        ? orders.findAll(PageRequest.of(0,200, Sort.by(Sort.Direction.DESC,"id"))).getContent()
        : orders.findTop200ByUserIdOrderByIdDesc(user.getId());
  }
}
