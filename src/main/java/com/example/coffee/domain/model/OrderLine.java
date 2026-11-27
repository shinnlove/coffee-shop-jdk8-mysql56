package com.example.coffee.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import javax.validation.constraints.*;

@JsonPropertyOrder({"itemId", "name", "unitPriceMinor", "quantity", "lineTotalMinor"})
public final class OrderLine {
  // Numeric internal relationship; the public itemId remains the original item code.
  private final Long internalItemId;
  private final String itemId;
  private final String name;
  private final long unitPriceMinor;
  private final int quantity;
  private final long lineTotalMinor;

  @JsonCreator
  public OrderLine(
      @JsonProperty("itemId") String itemId,
      @JsonProperty("name") String name,
      @JsonProperty("unitPriceMinor") long unitPriceMinor,
      @JsonProperty("quantity") int quantity,
      @JsonProperty("lineTotalMinor") long lineTotalMinor) {
    this(null, itemId, name, unitPriceMinor, quantity, lineTotalMinor);
  }

  public OrderLine(
      Long internalItemId,
      String itemId,
      String name,
      long unitPriceMinor,
      int quantity,
      long lineTotalMinor) {
    this.internalItemId = internalItemId;
    this.itemId = itemId;
    this.name = name;
    this.unitPriceMinor = unitPriceMinor;
    this.quantity = quantity;
    this.lineTotalMinor = lineTotalMinor;
  }

  public String getItemId() {
    return itemId;
  }

  public String getName() {
    return name;
  }

  public long getUnitPriceMinor() {
    return unitPriceMinor;
  }

  public int getQuantity() {
    return quantity;
  }

  public long getLineTotalMinor() {
    return lineTotalMinor;
  }

  @JsonIgnore
  public Long getInternalItemId() {
    return internalItemId;
  }
}
