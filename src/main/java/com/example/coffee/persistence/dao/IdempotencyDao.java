package com.example.coffee.persistence.dao;

import com.example.coffee.persistence.dataobject.IdempotencyDO;
import org.apache.ibatis.annotations.Param;

/**
 * Idempotency SQL/mappings live in mapper/IdempotencyDao.xml; writes and locks share one
 * transaction.
 */
public interface IdempotencyDao {
  /**
   * Inserts a request record, retaining the original on UNIQUE(scope, request_key) conflict.
   *
   * <p>ON DUPLICATE KEY UPDATE is a no-op; concurrent same-key requests may wait for a conflicting
   * transaction. The digest and response are not overwritten. A subsequent locking read decides
   * between a content conflict, response replay and first execution.
   *
   * @param idempotencyRecord scope, key and digest; the database generates the internal ID
   * @return JDBC affected-row count; driver settings vary, so do not infer first execution from it
   */
  int preInsertIdempotencyRecord(IdempotencyDO idempotencyRecord);

  /**
   * Reads the complete record by request identity and takes an exclusive row lock.
   *
   * @param operationScope the same scope used by the pre-insert
   * @param requestKey original ASCII key bytes, matching request_key
   * @return locked record, or null if missing; the lock lasts until commit or rollback
   */
  IdempotencyDO selectByScopeAndKeyForUpdate(
      @Param("scope") String operationScope, @Param("requestKey") byte[] requestKey);

  /**
   * Stores the original HTTP status and JSON response by internal primary key.
   *
   * @param idempotencyRecord locked record containing its ID, HTTP status and response body
   * @return affected rows; the caller requires exactly one, otherwise an invariant has failed
   */
  int updateResponseById(IdempotencyDO idempotencyRecord);
}
