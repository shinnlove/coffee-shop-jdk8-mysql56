package com.example.coffee.persistence.dataobject;

import java.time.Instant;

/** Row from item_inventory; numeric IDs are internal and are not public HTTP identifiers. */
public class ItemInventoryDO {
  /** id: Auto-increment internal primary key. */
  private Long id;
  /** item_id: Internal item ID; logical relationship to menu_item.id and item_inventory.item_id. */
  private Long itemId;
  /** stock_quantity: Current available quantity; no reservation is maintained. */
  private Integer stockQuantity;
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

  public Long getItemId() {
    return itemId;
  }

  public void setItemId(Long itemId) {
    this.itemId = itemId;
  }

  public Integer getStockQuantity() {
    return stockQuantity;
  }

  public void setStockQuantity(Integer stockQuantity) {
    this.stockQuantity = stockQuantity;
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
