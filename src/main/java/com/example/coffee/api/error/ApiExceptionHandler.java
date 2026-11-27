package com.example.coffee.api.error;

import com.example.coffee.api.response.ApiError;
import com.example.coffee.common.error.BusinessException;
import com.example.coffee.common.error.ErrorCode;
import com.example.coffee.common.error.InternalException;
import com.example.coffee.persistence.DatabaseFailureClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
  private static final String RETRY_AFTER_SECONDS = "1";

  @ExceptionHandler(BusinessException.class)
  ResponseEntity<ApiError> handleBusinessException(BusinessException e) {
    return ResponseEntity.status(e.getErrorCode().getHttpStatus())
        .body(new ApiError(e.getErrorCode().getCode(), e.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiError> handleValidationException() {
    return buildErrorResponse(ErrorCode.VALIDATION_ERROR);
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  ResponseEntity<ApiError> handleMalformedRequest() {
    return buildErrorResponse(ErrorCode.BAD_REQUEST);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<ApiError> handleUnsupportedMethod() {
    return buildErrorResponse(ErrorCode.METHOD_NOT_ALLOWED);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  ResponseEntity<ApiError> handleUnsupportedMediaType() {
    return buildErrorResponse(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
  }

  @ExceptionHandler(InternalException.class)
  ResponseEntity<ApiError> handleInternalException(InternalException failure) {
    // Factories allow only controlled codes and numeric IDs; no arbitrary cause can be attached.
    log.error(
        "event=internal_error internalCode={} identifiers={} requestId={}",
        failure.getInternalCode(),
        failure.getIdentifiers(),
        MDC.get("requestId"),
        failure);
    return buildErrorResponse(ErrorCode.INTERNAL_ERROR);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiError> handleUnexpectedException(Exception e) {
    // 1. Delegate database classification; this handler chooses HTTP status and retry guidance.
    if (DatabaseFailureClassifier.isRetryable(e)) {
      log.warn(
          "event=database_retryable type={} requestId={} frames={}",
          e.getClass().getName(),
          MDC.get("requestId"),
          safeStackLocations(e));
      return ResponseEntity.status(ErrorCode.TEMPORARILY_UNAVAILABLE.getHttpStatus())
          .header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
          .body(new ApiError(ErrorCode.TEMPORARILY_UNAVAILABLE));
    }

    // 2. Transaction misuse and unexpected failures return 500, without encouraging blind retries.
    // Driver messages may contain SQL parameters or credentials; log only safe stack locations.
    log.error(
        "event=request_failed type={} requestId={} frames={}",
        e.getClass().getName(),
        MDC.get("requestId"),
        safeStackLocations(e));
    return buildErrorResponse(ErrorCode.INTERNAL_ERROR);
  }

  // Never stringify a third-party Throwable: its message can contain SQL parameters or secrets.
  private static String safeStackLocations(Throwable failure) {
    StringBuilder locations = new StringBuilder();
    StackTraceElement[] frames = failure.getStackTrace();
    for (int index = 0; index < Math.min(frames.length, 8); index++) {
      if (index > 0) {
        locations.append(" | ");
      }
      StackTraceElement frame = frames[index];
      locations
          .append(frame.getClassName())
          .append('.')
          .append(frame.getMethodName())
          .append(':')
          .append(frame.getLineNumber());
    }
    return locations.toString();
  }

  private static ResponseEntity<ApiError> buildErrorResponse(ErrorCode errorCode) {
    return ResponseEntity.status(errorCode.getHttpStatus()).body(new ApiError(errorCode));
  }
}
