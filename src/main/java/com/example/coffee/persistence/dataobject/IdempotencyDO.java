package com.example.coffee.persistence.dataobject;

import java.time.Instant;

/** Row from idempotency_record; numeric IDs are internal and are not public HTTP identifiers. */
public class IdempotencyDO {
  /** id: Auto-increment internal primary key. Not the request idempotency identity. */
  private Long id;
  /** scope: Idempotency operation, e.g. orders:create or orders:{UUID}:pay. */
  private String scope;
  /** request_key: Caller key stored as original ASCII bytes. */
  private byte[] requestKey;
  /** request_hash: SHA-256 of normalized content, encoded as hexadecimal. */
  private String requestHash;
  /**
   * http_status: Original HTTP response status; null until the transaction completes business work.
   */
  private Integer httpStatus;
  /** response_body: Original response JSON; null until business work completes. */
  private String responseBody;
  /** create_time: Creation time in UTC. */
  private Instant createTime;
  /** update_time: Last update time in UTC. */
  private Instant updateTime;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  public byte[] getRequestKey() {
    return requestKey;
  }

  public void setRequestKey(byte[] requestKey) {
    this.requestKey = requestKey;
  }

  public String getRequestHash() {
    return requestHash;
  }

  public void setRequestHash(String requestHash) {
    this.requestHash = requestHash;
  }

  public Integer getHttpStatus() {
    return httpStatus;
  }

  public void setHttpStatus(Integer httpStatus) {
    this.httpStatus = httpStatus;
  }

  public String getResponseBody() {
    return responseBody;
  }

  public void setResponseBody(String responseBody) {
    this.responseBody = responseBody;
  }

  public Instant getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Instant createTime) {
    this.createTime = createTime;
  }

  public Instant getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(Instant updateTime) {
    this.updateTime = updateTime;
  }
}
