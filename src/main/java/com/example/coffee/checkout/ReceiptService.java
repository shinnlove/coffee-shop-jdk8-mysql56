package com.example.coffee.checkout;

import com.example.coffee.common.error.BusinessException;
import com.example.coffee.common.error.ErrorCode;
import com.example.coffee.common.json.JsonCodec;
import com.example.coffee.domain.model.Order;
import com.example.coffee.domain.model.OrderStatus;
import com.example.coffee.domain.model.Receipt;
import com.example.coffee.ordering.OrderService;
import com.example.coffee.persistence.dao.ReceiptDao;
import com.example.coffee.persistence.dataobject.ReceiptDO;
import com.example.coffee.transaction.RequireWriteTransaction;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ReceiptService {
  private final ReceiptDao receiptDao;
  private final JsonCodec jsonCodec;
  private final OrderService orderService;

  public ReceiptService(ReceiptDao receiptDao, JsonCodec jsonCodec, OrderService orderService) {
    this.receiptDao = receiptDao;
    this.jsonCodec = jsonCodec;
    this.orderService = orderService;
  }

  /**
   * Saves the immutable paid receipt in the same transaction as order, inventory and idempotency.
   */
  @RequireWriteTransaction
  public void saveReceipt(Order paidOrder) {
    ReceiptDO receiptRecord = buildReceiptRecord(paidOrder);
    int insertedReceipts = receiptDao.insert(receiptRecord);
    if (insertedReceipts != 1) {
      throw new IllegalStateException("Receipt insert invariant violated");
    }
  }

  public Receipt getReceipt(UUID orderId) {
    Order order = orderService.getOrder(orderId);
    if (order.getStatus() != OrderStatus.PAID) {
      throw new BusinessException(ErrorCode.ORDER_NOT_PAID);
    }
    ReceiptDO receiptRecord = receiptDao.findByOrderId(order.getInternalId());
    if (receiptRecord == null) {
      throw new IllegalStateException("Paid order receipt is missing");
    }
    return jsonCodec.fromJson(receiptRecord.getPayload(), Receipt.class);
  }

  private ReceiptDO buildReceiptRecord(Order paidOrder) {
    ReceiptDO receiptRecord = new ReceiptDO();
    receiptRecord.setOrderId(paidOrder.getInternalId());
    receiptRecord.setPayload(jsonCodec.toJson(Receipt.fromPaidOrder(paidOrder)));
    return receiptRecord;
  }
}
