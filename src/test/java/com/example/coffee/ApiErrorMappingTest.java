package com.example.coffee;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.coffee.api.OwnerController;
import com.example.coffee.api.error.ApiExceptionHandler;
import com.example.coffee.common.error.BusinessException;
import com.example.coffee.common.error.ErrorCode;
import com.example.coffee.ordering.OrderService;
import java.sql.SQLException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.UnexpectedRollbackException;

/**
 * Isolated MVC error mapping; HttpApiTest covers real authentication, transactions and inventory.
 */
class ApiErrorMappingTest {
  private final OrderService orderService = mock(OrderService.class);
  private final MockMvc mvc =
      MockMvcBuilders.standaloneSetup(new OwnerController(orderService))
          .setControllerAdvice(new ApiExceptionHandler())
          .build();

  static Stream<Arguments> failures() {
    return Stream.of(
        Arguments.of(new IllegalTransactionStateException("no write transaction"), 500),
        Arguments.of(new UnexpectedRollbackException("caught inner failure"), 500),
        Arguments.of(
            new CannotAcquireLockException(
                "wrapped misuse", new IllegalTransactionStateException("read-only transaction")),
            500),
        Arguments.of(new CannotAcquireLockException("busy"), 503),
        Arguments.of(new CannotCreateTransactionException("connection unavailable"), 503),
        Arguments.of(new RuntimeException(new SQLException("hidden", null, 1205)), 503),
        Arguments.of(new RuntimeException(new SQLException("hidden", null, 1213)), 503),
        Arguments.of(new RuntimeException(new SQLException("hidden", "40001", 0)), 503),
        Arguments.of(new RuntimeException(new SQLException("hidden", "08006", 0)), 503),
        Arguments.of(new RuntimeException(new SQLException("hidden", "HYT00", 0)), 503),
        Arguments.of(new RuntimeException(new SQLException("constraint", "23000", 0)), 500),
        Arguments.of(
            new RuntimeException(new SQLException("deadlock words are not a code", "22001", 0)),
            500));
  }

  @ParameterizedTest
  @MethodSource("failures")
  void classifiesFailuresWithoutExposingInternalMessages(
      RuntimeException failure, int expectedStatus) throws Exception {
    when(orderService.listPaidOrders()).thenThrow(failure);

    ResultActions response =
        mvc.perform(get("/api/v1/owner/orders")).andExpect(status().is(expectedStatus));
    if (expectedStatus == 503) {
      response
          .andExpect(header().string("Retry-After", "1"))
          .andExpect(
              content()
                  .json(
                      "{\"code\":\"TEMPORARILY_UNAVAILABLE\","
                          + "\"message\":\"Retry the same request with the same key after a short delay.\"}",
                      true));
    } else {
      response
          .andExpect(header().doesNotExist("Retry-After"))
          .andExpect(
              content()
                  .json(
                      "{\"code\":\"INTERNAL_ERROR\","
                          + "\"message\":\"The request could not be completed.\"}",
                      true));
    }
  }

  @Test
  void businessFailureKeepsOriginalStatusCodeAndBody() throws Exception {
    when(orderService.listPaidOrders())
        .thenThrow(new BusinessException(ErrorCode.INSUFFICIENT_STOCK));

    mvc.perform(get("/api/v1/owner/orders"))
        .andExpect(status().isConflict())
        .andExpect(header().doesNotExist("Retry-After"))
        .andExpect(
            content()
                .json(
                    "{\"code\":\"INSUFFICIENT_STOCK\","
                        + "\"message\":\"Requested quantity is not available.\"}",
                    true));
  }

  @Test
  void ownerStatusValidationUsesTheSameErrorContract() throws Exception {
    mvc.perform(get("/api/v1/owner/orders").param("status", "PENDING_PAYMENT"))
        .andExpect(status().isBadRequest())
        .andExpect(
            content()
                .json(
                    "{\"code\":\"UNSUPPORTED_STATUS\","
                        + "\"message\":\"Only PAID is supported.\"}",
                    true));
  }
}
