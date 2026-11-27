package com.example.coffee.api;

import com.example.coffee.api.request.CreateOrderRequest;
import com.example.coffee.api.request.MockPaymentRequest;
import com.example.coffee.catalog.MenuService;
import com.example.coffee.checkout.PaymentService;
import com.example.coffee.checkout.ReceiptService;
import com.example.coffee.common.error.BusinessException;
import com.example.coffee.common.error.ErrorCode;
import com.example.coffee.domain.model.Menu;
import com.example.coffee.domain.model.Order;
import com.example.coffee.domain.model.Receipt;
import com.example.coffee.idempotency.IdempotencyService;
import com.example.coffee.idempotency.StoredResponse;
import com.example.coffee.ordering.OrderService;
import com.example.coffee.ordering.Pricing;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Customer API for menus, orders, mock payment and receipts. */
@RestController
@RequestMapping("/api/v1")
public class CustomerController {
  private final MenuService menuService;
  private final OrderService orderService;
  private final PaymentService paymentService;
  private final ReceiptService receiptService;
  private final IdempotencyService idempotencyService;

  public CustomerController(
      MenuService menuService,
      OrderService orderService,
      PaymentService paymentService,
      ReceiptService receiptService,
      IdempotencyService idempotencyService) {
    this.menuService = menuService;
    this.orderService = orderService;
    this.paymentService = paymentService;
    this.receiptService = receiptService;
    this.idempotencyService = idempotencyService;
  }

  @GetMapping("/menu")
  public Menu getMenu() {
    return menuService.getMenu();
  }

  @PostMapping("/orders")
  public ResponseEntity<String> createOrder(
      @Valid @RequestBody CreateOrderRequest request, HttpServletRequest httpRequest) {
    // 1. Normalize content before hashing; item order and surrounding name whitespace are
    // insignificant.
    CreateOrderRequest normalizedRequest = Pricing.normalizeCreateOrderRequest(request);
    String idempotencyKey = extractIdempotencyKey(httpRequest);

    // 2. The server selects scope; callers select keys. A new key allows another identical
    // purchase.
    // orderService.createOrder executes inside IdempotencyService's TransactionTemplate callback.
    return toHttpResponse(
        idempotencyService.executeIdempotentRequest(
            "orders:create",
            idempotencyKey,
            normalizedRequest,
            HttpStatus.CREATED.value(),
            () -> orderService.createOrder(normalizedRequest)));
  }

  @GetMapping("/orders/{orderId}")
  public Order getOrder(@PathVariable UUID orderId) {
    return orderService.getOrder(orderId);
  }

  @PostMapping("/orders/{orderId}/payments/mock")
  public ResponseEntity<String> simulatePayment(
      @PathVariable UUID orderId,
      @Valid @RequestBody MockPaymentRequest request,
      HttpServletRequest httpRequest) {
    String idempotencyKey = extractIdempotencyKey(httpRequest);

    // Different keys represent different attempts; retry the same attempt with the same key and
    // outcome.
    // Scope includes the order UUID; the order lock prevents duplicate deduction across different
    // keys.
    return toHttpResponse(
        idempotencyService.executeIdempotentRequest(
            "orders:" + orderId + ":pay",
            idempotencyKey,
            request,
            HttpStatus.OK.value(),
            () -> paymentService.processMockPayment(orderId, request.getOutcome())));
  }

  @GetMapping("/orders/{orderId}/receipt")
  public Receipt getReceipt(@PathVariable UUID orderId) {
    return receiptService.getReceipt(orderId);
  }

  /** Extracts exactly one header; IdempotencyService validates its character range and length. */
  private static String extractIdempotencyKey(HttpServletRequest request) {
    List<String> idempotencyKeys = Collections.list(request.getHeaders("Idempotency-Key"));
    if (idempotencyKeys.size() != 1) {
      throw new BusinessException(
          ErrorCode.INVALID_IDEMPOTENCY_KEY, "Exactly one Idempotency-Key header is required.");
    }
    return idempotencyKeys.get(0);
  }

  private static ResponseEntity<String> toHttpResponse(StoredResponse storedResponse) {
    // Replayed means a stored response was used; a new key returning an already-paid order is not a
    // replay.
    return ResponseEntity.status(storedResponse.getHttpStatus())
        .contentType(MediaType.APPLICATION_JSON)
        .header("Idempotency-Replayed", Boolean.toString(storedResponse.getReplayed()))
        .body(storedResponse.getResponseBody());
  }
}
