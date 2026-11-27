package com.example.coffee.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import javax.validation.constraints.*;

@JsonPropertyOrder({"id", "name", "firstItemId", "secondItemId", "bundlePriceMinor", "active"})
public final class ComboOffer {
  private final String id;
  private final String name;
  private final String firstItemId;
  private final String secondItemId;
  private final long bundlePriceMinor;
  private final boolean active;

  @JsonCreator
  public ComboOffer(
      @JsonProperty("id") String id,
      @JsonProperty("name") String name,
      @JsonProperty("firstItemId") String firstItemId,
      @JsonProperty("secondItemId") String secondItemId,
      @JsonProperty("bundlePriceMinor") long bundlePriceMinor,
      @JsonProperty("active") boolean active) {
    this.id = id;
    this.name = name;
    this.firstItemId = firstItemId;
    this.secondItemId = secondItemId;
    this.bundlePriceMinor = bundlePriceMinor;
    this.active = active;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getFirstItemId() {
    return firstItemId;
  }

  public String getSecondItemId() {
    return secondItemId;
  }

  public long getBundlePriceMinor() {
    return bundlePriceMinor;
  }

  public boolean getActive() {
    return active;
  }
}
