package com.example.coffee.api.response;

import com.example.coffee.domain.model.Order;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.validation.constraints.*;

@JsonPropertyOrder({"orders"})
public final class PaidOrdersResponse {
  private final List<Order> orders;

  @JsonCreator
  public PaidOrdersResponse(@JsonProperty("orders") List<Order> orders) {
    this.orders = orders == null ? null : Collections.unmodifiableList(new ArrayList<>(orders));
  }

  public List<Order> getOrders() {
    return orders;
  }
}
