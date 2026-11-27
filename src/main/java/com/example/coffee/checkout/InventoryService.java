package com.example.coffee.checkout;

import com.example.coffee.common.error.BusinessException;
import com.example.coffee.common.error.InternalException;
import com.example.coffee.domain.model.OrderLine;
import com.example.coffee.persistence.dao.InventoryChangeLogDao;
import com.example.coffee.persistence.dao.ItemInventoryDao;
import com.example.coffee.persistence.dataobject.InventoryChangeLogDO;
import com.example.coffee.persistence.dataobject.ItemInventoryDO;
import com.example.coffee.transaction.RequireWriteTransaction;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {
  private final ItemInventoryDao itemInventoryDao;
  private final InventoryChangeLogDao inventoryChangeLogDao;

  public InventoryService(
      ItemInventoryDao itemInventoryDao, InventoryChangeLogDao inventoryChangeLogDao) {
    this.itemInventoryDao = itemInventoryDao;
    this.inventoryChangeLogDao = inventoryChangeLogDao;
  }

  /**
   * Locks inventory in internal item ID order, validates all stock, deducts and records each sale.
   */
  @RequireWriteTransaction
  public void deductStockAndRecordChanges(Long orderId, List<OrderLine> orderLines) {
    // 1. The annotation requires a write transaction; the caller already holds the order lock.

    // 2. Use one numeric item ID lock order to avoid reversed-order inventory lock cycles.
    List<OrderLine> sortedOrderLines =
        orderLines.stream()
            .sorted(Comparator.comparing(OrderLine::getInternalItemId))
            .collect(Collectors.toList());

    // 3. Lock and check every item before deducting; all locks remain held until transaction
    // completion.
    // A waiter sees the preceding committed stock and returns 409 if another order bought the last
    // unit.
    for (OrderLine orderLine : sortedOrderLines) {
      ItemInventoryDO itemInventoryRecord =
          itemInventoryDao.selectByItemIdForUpdate(orderLine.getInternalItemId());
      if (itemInventoryRecord == null) {
        throw InternalException.menuItemInventoryMissing(orderLine.getInternalItemId());
      }
      if (itemInventoryRecord.getStockQuantity() < orderLine.getQuantity()) {
        throw BusinessException.insufficientStock();
      }
    }

    // 4. Deduct all items and append their inventory change logs in the same transaction.
    for (OrderLine orderLine : sortedOrderLines) {
      // UPDATE also requires stock_quantity >= quantity; an unexpected row count rolls back
      // payment.
      int updatedInventoryRows =
          itemInventoryDao.deductStock(orderLine.getInternalItemId(), orderLine.getQuantity());
      if (updatedInventoryRows != 1) {
        throw new IllegalStateException("Locked inventory invariant violated");
      }

      // UNIQUE(order_id, item_id) adds duplicate-sale protection; a failed log insert rolls back
      // deduction.
      InventoryChangeLogDO inventoryChangeLog = buildInventoryChangeLog(orderId, orderLine);
      int insertedChangeLogs = inventoryChangeLogDao.insert(inventoryChangeLog);
      if (insertedChangeLogs != 1) {
        throw new IllegalStateException("Inventory change log insert failed");
      }
    }
  }

  private InventoryChangeLogDO buildInventoryChangeLog(Long orderId, OrderLine orderLine) {
    InventoryChangeLogDO inventoryChangeLog = new InventoryChangeLogDO();
    inventoryChangeLog.setOrderId(orderId);
    inventoryChangeLog.setItemId(orderLine.getInternalItemId());
    inventoryChangeLog.setQuantity(orderLine.getQuantity());
    return inventoryChangeLog;
  }
}
