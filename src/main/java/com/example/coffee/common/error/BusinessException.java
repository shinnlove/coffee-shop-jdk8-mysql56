package com.example.coffee.common.error;

/** A business failure mapped by ApiExceptionHandler to the existing HTTP error contract. */
public class BusinessException extends RuntimeException {
  private final ErrorCode errorCode;

  public BusinessException(ErrorCode errorCode) {
    this(errorCode, errorCode.getMessage());
  }

  /** Adds a controlled explanation, e.g. distinguishing a missing header from an invalid key. */
  public BusinessException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }

  public int getHttpStatus() {
    return errorCode.getHttpStatus();
  }

  public String getCode() {
    return errorCode.getCode();
  }

  public static BusinessException insufficientStock() {
    return new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
  }

  public static BusinessException orderNotFound() {
    return new BusinessException(ErrorCode.ORDER_NOT_FOUND);
  }
}
