package com.example.coffee.persistence.dataobject;

import java.time.Instant;

/** Row from inventory_change_log; numeric IDs are internal and are not public HTTP identifiers. */
public class InventoryChangeLogDO {
  /** id: Auto-increment internal primary key. */
  private Long id;
  /** order_id: Internal order ID; logical relationship to customer_order.id. */
  private Long orderId;
  /** item_id: Internal item ID; logical relationship to menu_item.id and item_inventory.item_id. */
  private Long itemId;
  /** quantity: Positive quantity deducted by this sale. */
  private Integer quantity;
  /** create_time: Creation time in UTC. */
  private Instant createTime;
  /** update_time: Last update time in UTC. */
  private Instant updateTime;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getOrderId() {
    return orderId;
  }

  public void setOrderId(Long orderId) {
    this.orderId = orderId;
  }

  public Long getItemId() {
    return itemId;
  }

  public void setItemId(Long itemId) {
    this.itemId = itemId;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  public Instant getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Instant createTime) {
    this.createTime = createTime;
  }

  public Instant getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(Instant updateTime) {
    this.updateTime = updateTime;
  }
}
