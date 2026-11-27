package com.example.coffee.persistence.dataobject;

import java.time.Instant;

/** Row from shop; numeric IDs are internal and are not public HTTP identifiers. */
public class ShopDO {
  /** id: Auto-increment internal primary key. */
  private Long id;
  /** currency: Currency code (e.g. USD); fixed in order snapshots. */
  private String currency;
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

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
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
