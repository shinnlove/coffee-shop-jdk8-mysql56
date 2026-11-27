package com.example.coffee.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.validation.constraints.*;

@JsonPropertyOrder({
  "id",
  "orderNumber",
  "customerName",
  "status",
  "currency",
  "lines",
  "subtotalMinor",
  "discountMinor",
  "totalMinor",
  "createdAt",
  "paidAt"
})
public final class Order {
  // Numeric internal relationship; the public order ID remains a UUID.
  private final Long internalId;
  private final UUID id;
  private final String orderNumber;
  private final String customerName;
  private final OrderStatus status;
  private final String currency;
  private final List<OrderLine> lines;
  private final long subtotalMinor;
  private final long discountMinor;
  private final long totalMinor;
  private final Instant createdAt;
  /** Paid instant, null while pending; UTC ISO-8601 on HTTP and microsecond paid_at in MySQL. */
  private final Instant paidAt;

  @JsonCreator
  public Order(
      @JsonProperty("id") UUID id,
      @JsonProperty("orderNumber") String orderNumber,
      @JsonProperty("customerName") String customerName,
      @JsonProperty("status") OrderStatus status,
      @JsonProperty("currency") String currency,
      @JsonProperty("lines") List<OrderLine> lines,
      @JsonProperty("subtotalMinor") long subtotalMinor,
      @JsonProperty("discountMinor") long discountMinor,
      @JsonProperty("totalMinor") long totalMinor,
      @JsonProperty("createdAt") Instant createdAt,
      @JsonProperty("paidAt") Instant paidAt) {
    this(
        null,
        id,
        orderNumber,
        customerName,
        status,
        currency,
        lines,
        subtotalMinor,
        discountMinor,
        totalMinor,
        createdAt,
        paidAt);
  }

  public Order(
      Long internalId,
      UUID id,
      String orderNumber,
      String customerName,
      OrderStatus status,
      String currency,
      List<OrderLine> lines,
      long subtotalMinor,
      long discountMinor,
      long totalMinor,
      Instant createdAt,
      Instant paidAt) {
    this.internalId = internalId;
    this.id = id;
    this.orderNumber = orderNumber;
    this.customerName = customerName;
    this.status = status;
    this.currency = currency;
    this.lines = lines == null ? null : Collections.unmodifiableList(new ArrayList<>(lines));
    this.subtotalMinor = subtotalMinor;
    this.discountMinor = discountMinor;
    this.totalMinor = totalMinor;
    this.createdAt = createdAt;
    this.paidAt = paidAt;
  }

  public UUID getId() {
    return id;
  }

  public String getOrderNumber() {
    return orderNumber;
  }

  public String getCustomerName() {
    return customerName;
  }

  public OrderStatus getStatus() {
    return status;
  }

  public String getCurrency() {
    return currency;
  }

  public List<OrderLine> getLines() {
    return lines;
  }

  public long getSubtotalMinor() {
    return subtotalMinor;
  }

  public long getDiscountMinor() {
    return discountMinor;
  }

  public long getTotalMinor() {
    return totalMinor;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getPaidAt() {
    return paidAt;
  }

  @JsonIgnore
  public Long getInternalId() {
    return internalId;
  }
}
