package com.graduation.snack.domain;

import com.graduation.snack.auth.AuthService;
import com.graduation.snack.common.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
  private final GroupOrderRepository orders;
  private final GroupCampaignRepository campaigns;
  private final SnackProductRepository products;
  private final AuthService auth;

  public OrderController(GroupOrderRepository orders, GroupCampaignRepository campaigns, SnackProductRepository products, AuthService auth) {
    this.orders = orders;
    this.campaigns = campaigns;
    this.products = products;
    this.auth = auth;
  }

  public record Create(@NotNull @Positive Long campaignId, @Positive int quantity) {}

  @PostMapping
  @Transactional
  public GroupOrder create(@Valid @RequestBody Create request) {
    var user = auth.currentUser();
    var campaign = campaigns.findLockedById(request.campaignId()).orElseThrow(() -> ApiException.notFound("团购不存在"));
    var now = LocalDateTime.now();
    if (!"ACTIVE".equals(campaign.getStatus()) || now.isBefore(campaign.getStartTime()) || now.isAfter(campaign.getEndTime()))
      throw ApiException.badRequest("团购当前不可下单");

    var product = products.findLockedById(campaign.getProductId()).orElseThrow(() -> ApiException.notFound("商品不存在"));
    if (product.getStock() < request.quantity()) throw ApiException.badRequest("库存不足");

    product.setStock(product.getStock() - request.quantity());
    campaign.setSoldQuantity(campaign.getSoldQuantity() + request.quantity());

    var order = new GroupOrder();
    order.setCampaignId(campaign.getId());
    order.setUserId(user.getId());
    order.setQuantity(request.quantity());
    order.setTotalAmount(campaign.getGroupPrice().multiply(java.math.BigDecimal.valueOf(request.quantity())));
    order.setStatus("PAID");
    return orders.save(order);
  }

  @GetMapping
  public List<GroupOrder> list() {
    var user = auth.currentUser();
    return "ADMIN".equals(user.getRole())
      ? orders.findAll(org.springframework.data.domain.PageRequest.of(0, 200, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"))).getContent()
      : orders.findTop200ByUserIdOrderByIdDesc(user.getId());
  }
}
