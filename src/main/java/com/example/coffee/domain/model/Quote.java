package com.example.coffee.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Quote {
  private final String currency;
  private final List<OrderLine> lines;
  private final long subtotalMinor;
  private final long discountMinor;
  private final long totalMinor;

  public Quote(
      String currency,
      List<OrderLine> lines,
      long subtotalMinor,
      long discountMinor,
      long totalMinor) {
    this.currency = currency;
    this.lines = lines == null ? null : Collections.unmodifiableList(new ArrayList<>(lines));
    this.subtotalMinor = subtotalMinor;
    this.discountMinor = discountMinor;
    this.totalMinor = totalMinor;
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
}
