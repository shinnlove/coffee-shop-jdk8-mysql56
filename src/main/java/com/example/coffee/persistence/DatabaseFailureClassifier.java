package com.example.coffee.persistence;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionUsageException;
import org.springframework.transaction.UnexpectedRollbackException;

/**
 * Classifies retryable database failures without inspecting messages or exposing SQL and
 * credentials.
 */
public final class DatabaseFailureClassifier {
  private static final int MYSQL_LOCK_WAIT_TIMEOUT = 1205;
  private static final int MYSQL_DEADLOCK = 1213;
  private static final String CONNECTION_EXCEPTION_CLASS = "08";
  private static final Set<String> RETRYABLE_SQL_STATES =
      Collections.unmodifiableSet(
          new HashSet<>(
              Arrays.asList(
                  "40001", // serialization failure / deadlock
                  "41000", // MySQL lock-timeout SQLSTATE used by some driver versions
                  "HYT00", // timeout expired
                  "HYT01" // connection timeout
                  )));

  private DatabaseFailureClassifier() {}

  public static boolean isRetryable(Throwable failure) {
    // 1. Transaction misuse is a programming error, not temporary database contention.
    // Inspect the cause chain so a wrapper cannot turn transaction misuse into a retryable failure.
    Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
    boolean retryable = false;
    for (Throwable cause = failure; cause != null && visited.add(cause); cause = cause.getCause()) {
      if (cause instanceof TransactionUsageException
          || cause instanceof UnexpectedRollbackException) {
        return false;
      }

      // 2. Recognize Spring transient database failures and connection acquisition failures.
      if (cause instanceof TransientDataAccessException
          || cause instanceof CannotCreateTransactionException) {
        retryable = true;
      }

      // 3. Use SQLSTATE and MySQL error numbers when the driver wraps contention or connection
      // failures.
      if (cause instanceof SQLException && isRetryableSql((SQLException) cause)) {
        retryable = true;
      }
    }
    return retryable;
  }

  private static boolean isRetryableSql(SQLException failure) {
    String state = failure.getSQLState();
    int vendorCode = failure.getErrorCode();
    return (state != null
            && (RETRYABLE_SQL_STATES.contains(state)
                || state.startsWith(CONNECTION_EXCEPTION_CLASS)))
        || vendorCode == MYSQL_LOCK_WAIT_TIMEOUT
        || vendorCode == MYSQL_DEADLOCK;
  }
}
