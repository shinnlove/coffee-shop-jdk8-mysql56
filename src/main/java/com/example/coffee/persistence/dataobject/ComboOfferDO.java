package com.example.coffee.persistence.dataobject;

import java.time.Instant;

/** Row from combo_offer; numeric IDs are internal and are not public HTTP identifiers. */
public class ComboOfferDO {
  /** id: Auto-increment internal primary key. */
  private Long id;
  /** offer_code: Stable public promotion identifier, e.g. coffee-pastry. */
  private String offerCode;
  /** name: Display name. */
  private String name;
  /** first_item_id: First component ID; logical relationship to menu_item.id. */
  private Long firstItemId;
  /** second_item_id: Second component ID; logical relationship to menu_item.id. */
  private Long secondItemId;
  /** bundle_price_minor: Price of one 1+1 bundle in the smallest currency unit. */
  private Long bundlePriceMinor;
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

  public String getOfferCode() {
    return offerCode;
  }

  public void setOfferCode(String offerCode) {
    this.offerCode = offerCode;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Long getFirstItemId() {
    return firstItemId;
  }

  public void setFirstItemId(Long firstItemId) {
    this.firstItemId = firstItemId;
  }

  public Long getSecondItemId() {
    return secondItemId;
  }

  public void setSecondItemId(Long secondItemId) {
    this.secondItemId = secondItemId;
  }

  public Long getBundlePriceMinor() {
    return bundlePriceMinor;
  }

  public void setBundlePriceMinor(Long bundlePriceMinor) {
    this.bundlePriceMinor = bundlePriceMinor;
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
