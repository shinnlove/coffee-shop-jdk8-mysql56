package com.example.coffee.idempotency;

public final class StoredResponse {
  private final int httpStatus;
  private final String responseBody;
  private final boolean replayed;

  public StoredResponse(int httpStatus, String responseBody, boolean replayed) {
    this.httpStatus = httpStatus;
    this.responseBody = responseBody;
    this.replayed = replayed;
  }

  public int getHttpStatus() {
    return httpStatus;
  }

  public String getResponseBody() {
    return responseBody;
  }

  public boolean getReplayed() {
    return replayed;
  }
}
