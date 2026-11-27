package com.example.coffee.idempotency;

/** Shared rules for the API's caller-supplied operation identity. */
public final class IdempotencyConstants {
  /** Eight to 128 printable ASCII characters, excluding whitespace. */
  public static final String IDEMPOTENCY_KEY_REGEX = "[\\x21-\\x7E]{8,128}";

  private IdempotencyConstants() {}
}
