package com.example.coffee.checkout;

import com.example.coffee.api.response.PaymentResult;
import com.example.coffee.common.error.BusinessException;
import com.example.coffee.common.error.ErrorCode;
import com.example.coffee.domain.model.Order;
import com.example.coffee.domain.model.OrderStatus;
import com.example.coffee.domain.model.PaymentIntent;
import com.example.coffee.domain.model.PaymentOutcome;
import com.example.coffee.ordering.OrderService;
import com.example.coffee.transaction.RequireWriteTransaction;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {
  private final OrderService orderService;
  private final InventoryService inventoryService;
  private final ReceiptService receiptService;
  private final Clock clock;

  public PaymentService(
      OrderService orderService,
      InventoryService inventoryService,
      ReceiptService receiptService,
      Clock clock) {
    this.orderService = orderService;
    this.inventoryService = inventoryService;
    this.receiptService = receiptService;
    this.clock = clock;
  }

  /**
   * APPROVE deducts stock, marks PAID and saves a receipt atomically; DECLINE leaves the order
   * pending.
   */
  @RequireWriteTransaction
  public PaymentResult processMockPayment(UUID orderId, PaymentIntent paymentIntent) {
    // 1. The annotation rejects absent/read-only transactions before entering this method.

    // 2. SELECT ... FOR UPDATE serializes payments for one order even when request keys differ.
    // The outer TransactionTemplate owns the lock until commit/rollback, not just until this method
    // returns.
    Order order = orderService.lockOrder(orderId);

    // 3. Check state after locking; a waiter sees committed PAID and cannot deduct inventory again.
    if (order.getStatus() == OrderStatus.PAID) {
      // A new decline attempt must not move a paid order back to pending.
      if (paymentIntent == PaymentIntent.DECLINE) {
        throw new BusinessException(ErrorCode.ORDER_ALREADY_PAID);
      }

      // A new APPROVE key returns the paid result without deducting stock or creating another
      // receipt.
      return new PaymentResult(PaymentOutcome.APPROVED, order);
    }

    // 4. DECLINE is a normal result: keep pending and leave stock unchanged; the outer layer saves
    // it.
    if (paymentIntent == PaymentIntent.DECLINE) {
      return new PaymentResult(PaymentOutcome.DECLINED, order);
    }

    // 5. Lock and validate all stock, then deduct it and insert inventory change logs.
    inventoryService.deductStockAndRecordChanges(order.getInternalId(), order.getLines());

    // 6. Transition PENDING_PAYMENT to PAID; SQL checks the old state and the service checks the
    // row count.
    orderService.markOrderPaid(
        order.getInternalId(), clock.instant().truncatedTo(ChronoUnit.MICROS));

    // 7. Save the unique paid receipt; failure rolls back the order transition and inventory
    // together.
    Order paidOrder = orderService.getOrder(orderId);
    receiptService.saveReceipt(paidOrder);

    // 8. Return for idempotent response storage; business steps are done but commit happens
    // outside.
    return new PaymentResult(PaymentOutcome.APPROVED, paidOrder);
  }
}
