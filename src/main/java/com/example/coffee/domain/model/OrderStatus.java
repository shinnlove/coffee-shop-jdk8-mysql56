package com.example.coffee.domain.model;

/** Numeric database values; HTTP preserves PENDING_PAYMENT / PAID enum names. */
public enum OrderStatus {
  PENDING_PAYMENT(1),
  PAID(2);

  /** Compile-time constant for annotation defaults; matches the public enum value. */
  public static final String PAID_API_VALUE = "PAID";

  private final int databaseCode;

  OrderStatus(int databaseCode) {
    this.databaseCode = databaseCode;
  }

  public int getDatabaseCode() {
    return databaseCode;
  }

  public static OrderStatus fromDatabaseCode(Integer code) {
    for (OrderStatus status : values()) {
      if (code != null && status.databaseCode == code) {
        return status;
      }
    }
    throw new IllegalStateException("Unknown persisted order status: " + code);
  }
}
