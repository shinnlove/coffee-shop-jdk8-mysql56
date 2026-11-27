package com.example.coffee.api.response;

import com.example.coffee.domain.model.Order;
import com.example.coffee.domain.model.PaymentOutcome;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import javax.validation.constraints.*;

@JsonPropertyOrder({"outcome", "order"})
public final class PaymentResult {
  private final PaymentOutcome outcome;
  private final Order order;

  @JsonCreator
  public PaymentResult(
      @JsonProperty("outcome") PaymentOutcome outcome, @JsonProperty("order") Order order) {
    this.outcome = outcome;
    this.order = order;
  }

  public PaymentOutcome getOutcome() {
    return outcome;
  }

  public Order getOrder() {
    return order;
  }
}
