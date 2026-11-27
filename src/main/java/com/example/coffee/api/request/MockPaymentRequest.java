package com.example.coffee.api.request;

import com.example.coffee.domain.model.PaymentIntent;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import javax.validation.constraints.*;

@JsonPropertyOrder({"outcome"})
public final class MockPaymentRequest {
  @NotNull private final PaymentIntent outcome;

  @JsonCreator
  public MockPaymentRequest(@JsonProperty("outcome") PaymentIntent outcome) {
    this.outcome = outcome;
  }

  public PaymentIntent getOutcome() {
    return outcome;
  }
}
