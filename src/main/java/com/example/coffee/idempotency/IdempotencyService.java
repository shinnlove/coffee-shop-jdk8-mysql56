package com.example.coffee.idempotency;

import com.example.coffee.common.error.BusinessException;
import com.example.coffee.common.error.ErrorCode;
import com.example.coffee.common.json.JsonCodec;
import com.example.coffee.persistence.dao.IdempotencyDao;
import com.example.coffee.persistence.dataobject.IdempotencyDO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Identifies requests by operation scope and caller-supplied key, executing once or replaying a
 * result.
 *
 * <p>The idempotency row, business writes and original response commit or roll back together. The
 * request digest checks content; it is not part of the unique request identity.
 */
@Service
public class IdempotencyService {

  private final IdempotencyDao idempotencyDao;
  private final JsonCodec jsonCodec;
  private final TransactionTemplate transactionTemplate;

  public IdempotencyService(
      IdempotencyDao idempotencyDao,
      JsonCodec jsonCodec,
      PlatformTransactionManager transactionManager) {
    this.idempotencyDao = idempotencyDao;
    this.jsonCodec = jsonCodec;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    transactionTemplate.setTimeout(15);
  }

  /**
   * Executes a new request or replays its original response after verifying identical content.
   *
   * <p>Within one scope, the same key with different content causes a conflict. Different keys
   * represent independent requests, even with identical content. Payments additionally require an
   * order lock and state check to prevent duplicate effects across different keys.
   *
   * <p>The callback runs synchronously in a READ_COMMITTED TransactionTemplate transaction.
   * Propagated exceptions roll back all writes. A normally returned DECLINED result is saved.
   *
   * @param operationScope server-selected operation, such as orders:create or orders:{orderId}:pay
   * @param idempotencyKey caller-supplied header, reused for retries; 8-128 printable ASCII
   *     characters without spaces; this service never generates a replacement key
   * @param normalizedRequest content to hash using SHA-256; the caller must normalize it and pass
   *     the same business content to the callback; this service does not normalize requests
   * @param successHttpStatus HTTP status saved on normal callback return, e.g. 201 or 200; neither
   *     an order state nor an exception status; replay preserves the original status
   * @param businessAction synchronous business callback, skipped when a result already exists
   * @return original HTTP status, JSON body and a flag indicating whether this was a replay
   * @throws BusinessException for an invalid key or conflicting content; business failures
   *     propagate
   */
  public StoredResponse executeIdempotentRequest(
      String operationScope,
      String idempotencyKey,
      Object normalizedRequest,
      int successHttpStatus,
      Supplier<?> businessAction) {
    // 1. Validate the caller identity; retries of one operation must reuse the same key.
    if (idempotencyKey == null
        || !idempotencyKey.matches(IdempotencyConstants.IDEMPOTENCY_KEY_REGEX)) {
      throw new BusinessException(ErrorCode.INVALID_IDEMPOTENCY_KEY);
    }

    // 2. Hash normalized content; identical content with a different key is still a new operation.
    String requestHash = calculateSha256(jsonCodec.toJson(normalizedRequest));

    // 3. Run all following steps in one READ_COMMITTED transaction; propagated failures roll it
    // back.
    // MyBatis joins this transaction. Idempotency, order and inventory locks last until
    // commit/rollback.
    return transactionTemplate.execute(
        transactionStatus -> {
          // 3.1. Compete on the unique request identity; same-key requests may wait on InnoDB
          // conflict locks.
          // The no-op update preserves content and response; affected-row counts do not identify a
          // new request.
          IdempotencyDO pendingIdempotencyRecord =
              buildIdempotencyRecord(operationScope, idempotencyKey, requestHash);
          idempotencyDao.preInsertIdempotencyRecord(pendingIdempotencyRecord);

          // 3.2. Lock and read the record: see a committed result, or proceed if the preceding
          // transaction rolled back.
          IdempotencyDO storedIdempotencyRecord =
              idempotencyDao.selectByScopeAndKeyForUpdate(
                  operationScope, pendingIdempotencyRecord.getRequestKey());
          if (storedIdempotencyRecord == null) {
            throw new IllegalStateException("Idempotency record is missing");
          }

          // 3.3. Reject changed content under the same identity; leave the committed original
          // untouched.
          if (!requestHash.equals(storedIdempotencyRecord.getRequestHash())) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
          }

          // 3.4. Replay the original response for matching content; GET returns the current order
          // state.
          if (storedIdempotencyRecord.getResponseBody() != null) {
            return new StoredResponse(
                storedIdempotencyRecord.getHttpStatus(),
                storedIdempotencyRecord.getResponseBody(),
                true);
          }

          // 3.5. Execute business work here; payments lock the order, then inventory in item ID
          // order.
          // Different payment keys still serialize on the order; the waiter sees PAID and skips
          // deduction.
          // Stock or database failures must propagate, never be swallowed into a successful
          // transaction.
          String responseJson = jsonCodec.toJson(businessAction.get());

          // 3.6. Store the original status/body, including normal DECLINED results; failed writes
          // leave no new claim.
          storedIdempotencyRecord.setHttpStatus(successHttpStatus);
          storedIdempotencyRecord.setResponseBody(responseJson);
          int updatedResponses = idempotencyDao.updateResponseById(storedIdempotencyRecord);
          if (updatedResponses != 1) {
            throw new IllegalStateException("Idempotency completion invariant violated");
          }

          // 3.7. TransactionTemplate commits before the controller is allowed to send this
          // response.
          return new StoredResponse(successHttpStatus, responseJson, false);
        });
  }

  /** Builds an uncompleted record without database access or generating a caller key. */
  private IdempotencyDO buildIdempotencyRecord(
      String operationScope, String idempotencyKey, String requestHash) {
    IdempotencyDO idempotencyRecord = new IdempotencyDO();
    idempotencyRecord.setScope(operationScope);
    idempotencyRecord.setRequestKey(idempotencyKey.getBytes(StandardCharsets.US_ASCII));
    idempotencyRecord.setRequestHash(requestHash);
    return idempotencyRecord;
  }

  private static String calculateSha256(String value) {
    try {
      return toHexString(
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }

  private static String toHexString(byte[] bytes) {
    final char[] alphabet = "0123456789abcdef".toCharArray();
    char[] result = new char[bytes.length * 2];
    for (int i = 0; i < bytes.length; i++) {
      int value = bytes[i] & 0xff;
      result[i * 2] = alphabet[value >>> 4];
      result[i * 2 + 1] = alphabet[value & 0x0f];
    }
    return new String(result);
  }
}
