package com.example.coffee.persistence.dataobject;

import java.time.Instant;

/** Row from order_line; numeric IDs are internal and are not public HTTP identifiers. */
public class OrderLineDO {
  /** id: Auto-increment internal primary key. */
  private Long id;
  /** order_id: Internal order ID; logical relationship to customer_order.id. */
  private Long orderId;
  /** item_id: Internal item ID; logical relationship to menu_item.id and item_inventory.item_id. */
  private Long itemId;
  /** name: Item name snapshot at order creation. */
  private String name;
  /** unit_price_minor: Unit price snapshot at order creation, in the smallest currency unit. */
  private Long unitPriceMinor;
  /** quantity: Purchased quantity. */
  private Integer quantity;
  /** line_total_minor: Quantity times unit price, before order-level discounts. */
  private Long lineTotalMinor;
  /** create_time: Creation time in UTC. */
  private Instant createTime;
  /** update_time: Last update time in UTC. */
  private Instant updateTime;
  // Read-only join projection from menu_item.item_code; not stored in order_line.
  /** item_code: Stable public item identifier, e.g. cappuccino. */
  private String itemCode;

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

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Long getUnitPriceMinor() {
    return unitPriceMinor;
  }

  public void setUnitPriceMinor(Long unitPriceMinor) {
    this.unitPriceMinor = unitPriceMinor;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  public Long getLineTotalMinor() {
    return lineTotalMinor;
  }

  public void setLineTotalMinor(Long lineTotalMinor) {
    this.lineTotalMinor = lineTotalMinor;
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

  public String getItemCode() {
    return itemCode;
  }

  public void setItemCode(String itemCode) {
    this.itemCode = itemCode;
  }
}
