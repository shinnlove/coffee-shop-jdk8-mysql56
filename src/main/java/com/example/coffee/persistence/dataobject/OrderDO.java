package com.example.coffee.persistence.dataobject;

import java.time.Instant;

/** Row from customer_order; numeric IDs are internal and are not public HTTP identifiers. */
public class OrderDO {
  /**
   * id: Auto-increment internal primary key. Also supplies the sequence for the readable order
   * number.
   */
  private Long id;
  /** order_uuid: Public order UUID; internal relationships use the numeric primary key. */
  private String orderUuid;
  /** customer_name: Pickup name; not a customer account identity. */
  private String customerName;
  /** status: Order state: 1 = PENDING_PAYMENT, 2 = PAID. */
  private Integer status;
  /** currency: Currency code (e.g. USD); fixed in order snapshots. */
  private String currency;
  /** subtotal_minor: Sum of line amounts before discounts, in the smallest currency unit. */
  private Long subtotalMinor;
  /** discount_minor: Total discount, in the smallest currency unit. */
  private Long discountMinor;
  /** total_minor: Amount payable, in the smallest currency unit. */
  private Long totalMinor;
  /** paid_at: Successful payment time in UTC; null for pending orders. */
  private Instant paidAt;
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

  public String getOrderUuid() {
    return orderUuid;
  }

  public void setOrderUuid(String orderUuid) {
    this.orderUuid = orderUuid;
  }

  public String getCustomerName() {
    return customerName;
  }

  public void setCustomerName(String customerName) {
    this.customerName = customerName;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public Long getSubtotalMinor() {
    return subtotalMinor;
  }

  public void setSubtotalMinor(Long subtotalMinor) {
    this.subtotalMinor = subtotalMinor;
  }

  public Long getDiscountMinor() {
    return discountMinor;
  }

  public void setDiscountMinor(Long discountMinor) {
    this.discountMinor = discountMinor;
  }

  public Long getTotalMinor() {
    return totalMinor;
  }

  public void setTotalMinor(Long totalMinor) {
    this.totalMinor = totalMinor;
  }

  public Instant getPaidAt() {
    return paidAt;
  }

  public void setPaidAt(Instant paidAt) {
    this.paidAt = paidAt;
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
