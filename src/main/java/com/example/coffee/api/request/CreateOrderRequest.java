package com.example.coffee.api.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.*;

@JsonPropertyOrder({"customerName", "items"})
public final class CreateOrderRequest {
  @NotBlank
  @Size(max = 80)
  private final String customerName;

  @NotEmpty
  @Size(max = 100)
  private final List<@NotNull @Valid ItemRequest> items;

  @JsonCreator
  public CreateOrderRequest(
      @JsonProperty("customerName") String customerName,
      @JsonProperty("items") List<ItemRequest> items) {
    this.customerName = customerName;
    this.items = items == null ? null : Collections.unmodifiableList(new ArrayList<>(items));
  }

  public String getCustomerName() {
    return customerName;
  }

  public List<ItemRequest> getItems() {
    return items;
  }
}
