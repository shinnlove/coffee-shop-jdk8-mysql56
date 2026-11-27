package com.example.coffee.common.error;

/**
 * Public errors: HTTP status, stable business code and default message. Explicit code strings
 * preserve the API contract if a Java enum constant is renamed.
 */
public enum ErrorCode {
  BAD_REQUEST(400, "BAD_REQUEST", "Malformed JSON, unsupported value, or invalid identifier."),

  INVALID_IDEMPOTENCY_KEY(
      400,
      "INVALID_IDEMPOTENCY_KEY",
      "Provide an 8-128 character printable ASCII Idempotency-Key without spaces."),

  UNSUPPORTED_STATUS(400, "UNSUPPORTED_STATUS", "Only PAID is supported."),

  UNAUTHORIZED(401, "UNAUTHORIZED", "Owner authentication required."),

  FORBIDDEN(403, "FORBIDDEN", "Access denied."),

  ORDER_NOT_FOUND(404, "ORDER_NOT_FOUND", "Order was not found."),

  NOT_FOUND(404, "NOT_FOUND", "Resource was not found."),

  METHOD_NOT_ALLOWED(405, "METHOD_NOT_ALLOWED", "HTTP method is not supported."),

  INSUFFICIENT_STOCK(409, "INSUFFICIENT_STOCK", "Requested quantity is not available."),

  ORDER_NOT_PAID(409, "ORDER_NOT_PAID", "Receipt is available only after confirmed payment."),

  ORDER_ALREADY_PAID(409, "ORDER_ALREADY_PAID", "A paid order cannot be declined."),

  IDEMPOTENCY_KEY_REUSED(
      409, "IDEMPOTENCY_KEY_REUSED", "Key has already been used with a different request body."),

  UNSUPPORTED_MEDIA_TYPE(415, "UNSUPPORTED_MEDIA_TYPE", "Use application/json."),

  VALIDATION_ERROR(
      422,
      "VALIDATION_ERROR",
      "Customer name and item quantities must satisfy the API constraints."),

  DUPLICATE_ITEM(422, "DUPLICATE_ITEM", "Specify each item only once."),

  INVALID_ITEM(422, "INVALID_ITEM", "Item is unknown or not sellable."),

  AMOUNT_TOO_LARGE(422, "AMOUNT_TOO_LARGE", "The requested amount exceeds supported limits."),

  CATALOGUE_CONFIGURATION_ERROR(
      500, "CATALOGUE_CONFIGURATION_ERROR", "Overlapping offers are unsupported."),

  INTERNAL_ERROR(500, "INTERNAL_ERROR", "The request could not be completed."),

  TEMPORARILY_UNAVAILABLE(
      503,
      "TEMPORARILY_UNAVAILABLE",
      "Retry the same request with the same key after a short delay.");

  private final int httpStatus;
  private final String code;
  private final String message;

  ErrorCode(int httpStatus, String code, String message) {
    this.httpStatus = httpStatus;
    this.code = code;
    this.message = message;
  }

  public int getHttpStatus() {
    return httpStatus;
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }
}
