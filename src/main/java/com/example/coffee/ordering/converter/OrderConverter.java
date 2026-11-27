package com.example.coffee.ordering.converter;

import com.example.coffee.domain.model.Order;
import com.example.coffee.domain.model.OrderLine;
import com.example.coffee.domain.model.OrderStatus;
import com.example.coffee.domain.model.Quote;
import com.example.coffee.persistence.dataobject.OrderDO;
import com.example.coffee.persistence.dataobject.OrderLineDO;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Pure object construction and field mapping; no database calls or transaction control. */
public final class OrderConverter {
  private OrderConverter() {}

  public static OrderDO toOrderDO(
      UUID orderUuid, String customerName, Quote quote, Instant createdAt) {
    OrderDO orderRecord = new OrderDO();
    orderRecord.setOrderUuid(orderUuid.toString());
    orderRecord.setCustomerName(customerName);
    orderRecord.setStatus(OrderStatus.PENDING_PAYMENT.getDatabaseCode());
    orderRecord.setCurrency(quote.getCurrency());
    orderRecord.setSubtotalMinor(quote.getSubtotalMinor());
    orderRecord.setDiscountMinor(quote.getDiscountMinor());
    orderRecord.setTotalMinor(quote.getTotalMinor());
    orderRecord.setCreateTime(createdAt);
    return orderRecord;
  }

  public static OrderLineDO toOrderLineDO(Long orderId, OrderLine orderLine) {
    OrderLineDO orderLineRecord = new OrderLineDO();
    orderLineRecord.setOrderId(orderId);
    orderLineRecord.setItemId(
        Objects.requireNonNull(orderLine.getInternalItemId(), "Internal item ID is required"));
    orderLineRecord.setName(orderLine.getName());
    orderLineRecord.setUnitPriceMinor(orderLine.getUnitPriceMinor());
    orderLineRecord.setQuantity(orderLine.getQuantity());
    orderLineRecord.setLineTotalMinor(orderLine.getLineTotalMinor());
    return orderLineRecord;
  }

  public static Order toOrder(OrderDO orderRecord, List<OrderLineDO> orderLineRecords) {
    List<OrderLine> orderLines = new ArrayList<>();
    for (OrderLineDO orderLineRecord : orderLineRecords) {
      orderLines.add(toOrderLine(orderLineRecord));
    }
    return new Order(
        orderRecord.getId(),
        UUID.fromString(orderRecord.getOrderUuid()),
        "C-" + orderRecord.getId(),
        orderRecord.getCustomerName(),
        OrderStatus.fromDatabaseCode(orderRecord.getStatus()),
        orderRecord.getCurrency(),
        orderLines,
        orderRecord.getSubtotalMinor(),
        orderRecord.getDiscountMinor(),
        orderRecord.getTotalMinor(),
        orderRecord.getCreateTime(),
        orderRecord.getPaidAt());
  }

  public static OrderLine toOrderLine(OrderLineDO orderLineRecord) {
    return new OrderLine(
        orderLineRecord.getItemId(),
        Objects.requireNonNull(
            orderLineRecord.getItemCode(), "Referenced menu item identity is missing"),
        orderLineRecord.getName(),
        orderLineRecord.getUnitPriceMinor(),
        orderLineRecord.getQuantity(),
        orderLineRecord.getLineTotalMinor());
  }
}
