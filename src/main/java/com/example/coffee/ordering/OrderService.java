package com.example.coffee.ordering;

import com.example.coffee.api.request.CreateOrderRequest;
import com.example.coffee.api.response.PaidOrdersResponse;
import com.example.coffee.catalog.MenuService;
import com.example.coffee.common.error.BusinessException;
import com.example.coffee.domain.model.Menu;
import com.example.coffee.domain.model.Order;
import com.example.coffee.domain.model.OrderLine;
import com.example.coffee.domain.model.Quote;
import com.example.coffee.ordering.converter.OrderConverter;
import com.example.coffee.persistence.dao.OrderDao;
import com.example.coffee.persistence.dao.OrderLineDao;
import com.example.coffee.persistence.dataobject.OrderDO;
import com.example.coffee.persistence.dataobject.OrderLineDO;
import com.example.coffee.transaction.RequireWriteTransaction;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
  private final MenuService menuService;
  private final OrderDao orderDao;
  private final OrderLineDao orderLineDao;
  private final Clock clock;

  public OrderService(
      MenuService menuService, OrderDao orderDao, OrderLineDao orderLineDao, Clock clock) {
    this.menuService = menuService;
    this.orderDao = orderDao;
    this.orderLineDao = orderLineDao;
    this.clock = clock;
  }

  /**
   * Creates a pending order and price snapshots without reserving stock; joins the caller
   * transaction.
   */
  @RequireWriteTransaction
  public Order createOrder(CreateOrderRequest request) {
    // 1. The annotation requires the write transaction that also saves the idempotent response.

    // 2. Price against the current menu and check stock; payment must check stock again under
    // locks.
    Menu menu = menuService.getMenu();
    Quote quote = Pricing.calculate(request, menu);

    // 3. Freeze customer, currency and totals so later menu changes cannot reprice this order.
    OrderDO orderRecord =
        OrderConverter.toOrderDO(
            UUID.randomUUID(),
            request.getCustomerName(),
            quote,
            clock.instant().truncatedTo(ChronoUnit.MICROS));

    // 4. Insert the order header and obtain its internal ID and readable order number.
    int insertedOrders = orderDao.insert(orderRecord);
    if (insertedOrders != 1) {
      throw new IllegalStateException("Order insert invariant violated");
    }

    // 5. Persist each item snapshot; a failed detail insert rolls back the header as well.
    for (OrderLine orderLine : quote.getLines()) {
      OrderLineDO orderLineRecord = OrderConverter.toOrderLineDO(orderRecord.getId(), orderLine);
      int insertedOrderLines = orderLineDao.insert(orderLineRecord);
      if (insertedOrderLines != 1) {
        throw new IllegalStateException("Order line insert failed");
      }
    }

    // 6. Return the pending snapshot for the outer service to store as the original response.
    return getOrder(UUID.fromString(orderRecord.getOrderUuid()));
  }

  public Order getOrder(UUID orderId) {
    OrderDO orderRecord = orderDao.findByUuid(orderId.toString());
    return toOrder(orderRecord);
  }

  /**
   * Locks the header by public UUID, then reads details; the caller holds the lock until
   * completion.
   */
  @RequireWriteTransaction
  public Order lockOrder(UUID orderId) {
    // A write transaction is required so auto-commit cannot release the lock immediately.

    // The locking read returns the header; its lock remains held after this method returns.
    OrderDO orderRecord = orderDao.selectByUuidForUpdate(orderId.toString());
    return toOrder(orderRecord);
  }

  @RequireWriteTransaction
  public void markOrderPaid(Long orderId, Instant paidAt) {
    int updatedOrders = orderDao.markPaid(orderId, paidAt);
    if (updatedOrders != 1) {
      throw new IllegalStateException("Order transition invariant violated");
    }
  }

  public PaidOrdersResponse listPaidOrders() {
    List<Order> paidOrders = new ArrayList<>();
    List<OrderDO> paidOrderRecords = orderDao.findPaid();
    for (OrderDO orderRecord : paidOrderRecords) {
      paidOrders.add(toOrder(orderRecord));
    }
    return new PaidOrdersResponse(paidOrders);
  }

  private Order toOrder(OrderDO orderRecord) {
    if (orderRecord == null) {
      throw BusinessException.orderNotFound();
    }
    List<OrderLineDO> orderLineRecords = orderLineDao.findByOrderId(orderRecord.getId());
    return OrderConverter.toOrder(orderRecord, orderLineRecords);
  }
}
