package com.example.coffee.api.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import javax.validation.constraints.*;

@JsonPropertyOrder({"itemId", "quantity"})
public final class ItemRequest {
  @NotBlank
  @Size(max = 80)
  private final String itemId;

  @NotNull
  @Min(1)
  @Max(20)
  private final Integer quantity;

  @JsonCreator
  public ItemRequest(
      @JsonProperty("itemId") String itemId, @JsonProperty("quantity") Integer quantity) {
    this.itemId = itemId;
    this.quantity = quantity;
  }

  public String getItemId() {
    return itemId;
  }

  public Integer getQuantity() {
    return quantity;
  }
}
