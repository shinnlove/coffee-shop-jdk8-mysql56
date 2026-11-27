package com.example.coffee.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.validation.constraints.*;

@JsonPropertyOrder({
  "orderNumber",
  "customerName",
  "currency",
  "subtotalMinor",
  "discountMinor",
  "totalMinor",
  "paidAt",
  "lines"
})
public final class Receipt {
  private final String orderNumber;
  private final String customerName;
  private final String currency;
  private final long subtotalMinor;
  private final long discountMinor;
  private final long totalMinor;
  /** Paid instant; UTC ISO-8601 on HTTP and microsecond-precision paid_at in MySQL. */
  private final Instant paidAt;

  private final List<OrderLine> lines;

  @JsonCreator
  public Receipt(
      @JsonProperty("orderNumber") String orderNumber,
      @JsonProperty("customerName") String customerName,
      @JsonProperty("currency") String currency,
      @JsonProperty("subtotalMinor") long subtotalMinor,
      @JsonProperty("discountMinor") long discountMinor,
      @JsonProperty("totalMinor") long totalMinor,
      @JsonProperty("paidAt") Instant paidAt,
      @JsonProperty("lines") List<OrderLine> lines) {
    this.orderNumber = orderNumber;
    this.customerName = customerName;
    this.currency = currency;
    this.subtotalMinor = subtotalMinor;
    this.discountMinor = discountMinor;
    this.totalMinor = totalMinor;
    this.paidAt = paidAt;
    this.lines = lines == null ? null : Collections.unmodifiableList(new ArrayList<>(lines));
  }

  public String getOrderNumber() {
    return orderNumber;
  }

  public String getCustomerName() {
    return customerName;
  }

  public String getCurrency() {
    return currency;
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

  public Instant getPaidAt() {
    return paidAt;
  }

  public List<OrderLine> getLines() {
    return lines;
  }

  public static Receipt fromPaidOrder(Order paidOrder) {
    return new Receipt(
        paidOrder.getOrderNumber(),
        paidOrder.getCustomerName(),
        paidOrder.getCurrency(),
        paidOrder.getSubtotalMinor(),
        paidOrder.getDiscountMinor(),
        paidOrder.getTotalMinor(),
        paidOrder.getPaidAt(),
        paidOrder.getLines());
  }
}
