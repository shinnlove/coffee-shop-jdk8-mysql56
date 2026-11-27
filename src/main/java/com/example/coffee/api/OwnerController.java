package com.example.coffee.api;

import com.example.coffee.api.response.PaidOrdersResponse;
import com.example.coffee.common.error.BusinessException;
import com.example.coffee.common.error.ErrorCode;
import com.example.coffee.domain.model.OrderStatus;
import com.example.coffee.ordering.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Owner API; SecurityConfiguration authenticates and checks OWNER before dispatching here. */
@RestController
@RequestMapping("/api/v1/owner")
public class OwnerController {
  private final OrderService orderService;

  public OwnerController(OrderService orderService) {
    this.orderService = orderService;
  }

  @GetMapping("/orders")
  public PaidOrdersResponse listPaidOrders(
      @RequestParam(defaultValue = OrderStatus.PAID_API_VALUE) String status) {
    // 1. Only the paid queue is supported; other filters retain the existing 400 error contract.
    if (!OrderStatus.PAID.name().equals(status)) {
      throw new BusinessException(ErrorCode.UNSUPPORTED_STATUS);
    }

    // 2. Authentication is complete; fetch by paidAt and a stable order-number tie-breaker.
    return orderService.listPaidOrders();
  }
}
