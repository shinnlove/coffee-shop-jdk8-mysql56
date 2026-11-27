package com.example.coffee;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.coffee.api.OwnerController;
import com.example.coffee.api.error.ApiExceptionHandler;
import com.example.coffee.catalog.converter.MenuConverter;
import com.example.coffee.common.error.InternalErrorCode;
import com.example.coffee.common.error.InternalException;
import com.example.coffee.ordering.OrderService;
import com.example.coffee.persistence.dataobject.CategoryDO;
import com.example.coffee.persistence.dataobject.MenuItemDO;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class InternalDiagnosticsTest {
  private final Logger logger = (Logger) LoggerFactory.getLogger(ApiExceptionHandler.class);
  private final ListAppender<ILoggingEvent> logCapture = new ListAppender<>();
  private final OrderService orderService = mock(OrderService.class);
  private final MockMvc mvc =
      MockMvcBuilders.standaloneSetup(new OwnerController(orderService))
          .setControllerAdvice(new ApiExceptionHandler())
          .build();

  @BeforeEach
  void captureLogs() {
    logCapture.start();
    logger.addAppender(logCapture);
    MDC.put("requestId", "diagnostic-test-request");
  }

  @AfterEach
  void releaseLogs() {
    logger.detachAppender(logCapture);
    logCapture.stop();
    MDC.remove("requestId");
  }

  @Test
  void missingCategoryAndMissingInventoryHaveDifferentInternalCodes() {
    MenuItemDO item = new MenuItemDO();
    item.setId(12L);
    item.setCategoryId(3L);
    assertThatThrownBy(() -> MenuConverter.toMenuItem(item, null, 1, "USD"))
        .isInstanceOfSatisfying(
            InternalException.class,
            failure -> {
              assertThat(failure.getInternalCode())
                  .isEqualTo(InternalErrorCode.MENU_ITEM_CATEGORY_MISSING);
              assertThat(failure.getIdentifiers())
                  .containsEntry("itemId", 12L)
                  .containsEntry("categoryId", 3L);
            });
    assertThatThrownBy(() -> MenuConverter.toMenuItem(item, new CategoryDO(), null, "USD"))
        .isInstanceOfSatisfying(
            InternalException.class,
            failure ->
                assertThat(failure.getInternalCode())
                    .isEqualTo(InternalErrorCode.MENU_ITEM_INVENTORY_MISSING));
  }

  @Test
  void controlledFaultLogsCodeIdsAndStackButKeepsThePublic500Contract() throws Exception {
    InternalException failure = InternalException.menuItemCategoryMissing(12L, 3L);
    when(orderService.listPaidOrders()).thenThrow(failure);
    mvc.perform(get("/api/v1/owner/orders"))
        .andExpect(status().isInternalServerError())
        .andExpect(
            content()
                .json(
                    "{\"code\":\"INTERNAL_ERROR\",\"message\":\"The request could not be completed.\"}",
                    true));
    ILoggingEvent event = logCapture.list.get(0);
    assertThat(event.getFormattedMessage())
        .contains(
            "MENU_ITEM_CATEGORY_MISSING",
            "itemId=12",
            "categoryId=3",
            "requestId=diagnostic-test-request");
    assertThat(event.getThrowableProxy()).isNotNull();
    assertThatThrownBy(() -> failure.initCause(new RuntimeException("secret")))
        .isInstanceOf(IllegalStateException.class);
    failure.addSuppressed(new RuntimeException("secret"));
    assertThat(failure.getSuppressed()).isEmpty();
  }

  @Test
  void driverMessagesAndCausesNeverReachDiagnosticLogsOrResponse() throws Exception {
    when(orderService.listPaidOrders())
        .thenThrow(
            new RuntimeException(
                "password=secret-value",
                new SQLException("SQL parameter: private-customer", "23000")));
    mvc.perform(get("/api/v1/owner/orders"))
        .andExpect(status().isInternalServerError())
        .andExpect(
            content()
                .json(
                    "{\"code\":\"INTERNAL_ERROR\",\"message\":\"The request could not be completed.\"}",
                    true));
    ILoggingEvent event = logCapture.list.get(0);
    assertThat(event.getFormattedMessage())
        .contains("requestId=diagnostic-test-request", "frames=")
        .doesNotContain("secret-value", "private-customer", "SQL parameter");
    assertThat(event.getThrowableProxy()).isNull();
  }
}
