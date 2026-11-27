package com.example.coffee.persistence.dataobject;

import java.time.Instant;

/** Row from receipt; numeric IDs are internal and are not public HTTP identifiers. */
public class ReceiptDO {
  /** id: Auto-increment internal primary key. */
  private Long id;
  /** order_id: Internal order ID; logical relationship to customer_order.id. */
  private Long orderId;
  /** payload: Immutable receipt JSON captured at successful payment. */
  private String payload;
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

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
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
