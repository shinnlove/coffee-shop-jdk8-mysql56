package com.example.coffee.api.response;

import com.example.coffee.common.error.ErrorCode;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import javax.validation.constraints.*;

@JsonPropertyOrder({"code", "message"})
public final class ApiError {
  private final String code;
  private final String message;

  public ApiError(ErrorCode errorCode) {
    this(errorCode.getCode(), errorCode.getMessage());
  }

  @JsonCreator
  public ApiError(@JsonProperty("code") String code, @JsonProperty("message") String message) {
    this.code = code;
    this.message = message;
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }
}
