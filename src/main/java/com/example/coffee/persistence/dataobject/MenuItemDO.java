package com.example.coffee.persistence.dataobject;

import java.time.Instant;

/** Row from menu_item; numeric IDs are internal and are not public HTTP identifiers. */
public class MenuItemDO {
  /** id: Auto-increment internal primary key. */
  private Long id;
  /** item_code: Stable public item identifier, e.g. cappuccino. */
  private String itemCode;
  /** category_id: Internal category ID; logical relationship to category.id. */
  private Long categoryId;
  /** name: Display name. */
  private String name;
  /** price_minor: Unit price in the smallest currency unit (cents for USD). */
  private Long priceMinor;
  /** active: Enabled/sellable flag: 0 = disabled, 1 = enabled. */
  private Boolean active;
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

  public String getItemCode() {
    return itemCode;
  }

  public void setItemCode(String itemCode) {
    this.itemCode = itemCode;
  }

  public Long getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(Long categoryId) {
    this.categoryId = categoryId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Long getPriceMinor() {
    return priceMinor;
  }

  public void setPriceMinor(Long priceMinor) {
    this.priceMinor = priceMinor;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
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
