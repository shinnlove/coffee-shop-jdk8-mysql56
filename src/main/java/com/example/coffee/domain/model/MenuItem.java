package com.example.coffee.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import javax.validation.constraints.*;

@JsonPropertyOrder({
  "id",
  "categoryId",
  "name",
  "priceMinor",
  "currency",
  "availableStock",
  "available"
})
public final class MenuItem {
  // Numeric internal relationship; the public item ID remains the original item code.
  private final Long internalId;
  private final String id;
  private final String categoryId;
  private final String name;
  private final long priceMinor;
  private final String currency;
  private final int availableStock;
  private final boolean available;

  @JsonCreator
  public MenuItem(
      @JsonProperty("id") String id,
      @JsonProperty("categoryId") String categoryId,
      @JsonProperty("name") String name,
      @JsonProperty("priceMinor") long priceMinor,
      @JsonProperty("currency") String currency,
      @JsonProperty("availableStock") int availableStock,
      @JsonProperty("available") boolean available) {
    this(null, id, categoryId, name, priceMinor, currency, availableStock, available);
  }

  public MenuItem(
      Long internalId,
      String id,
      String categoryId,
      String name,
      long priceMinor,
      String currency,
      int availableStock,
      boolean available) {
    this.internalId = internalId;
    this.id = id;
    this.categoryId = categoryId;
    this.name = name;
    this.priceMinor = priceMinor;
    this.currency = currency;
    this.availableStock = availableStock;
    this.available = available;
  }

  public String getId() {
    return id;
  }

  public String getCategoryId() {
    return categoryId;
  }

  public String getName() {
    return name;
  }

  public long getPriceMinor() {
    return priceMinor;
  }

  public String getCurrency() {
    return currency;
  }

  public int getAvailableStock() {
    return availableStock;
  }

  public boolean getAvailable() {
    return available;
  }

  @JsonIgnore
  public Long getInternalId() {
    return internalId;
  }
}
