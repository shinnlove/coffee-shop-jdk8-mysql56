package com.example.coffee;

import static org.assertj.core.api.Assertions.*;

import com.example.coffee.HttpTestClient.HttpResult;
import com.example.coffee.api.request.CreateOrderRequest;
import com.example.coffee.api.request.ItemRequest;
import com.example.coffee.checkout.InventoryService;
import com.example.coffee.checkout.PaymentService;
import com.example.coffee.checkout.ReceiptService;
import com.example.coffee.domain.model.PaymentIntent;
import com.example.coffee.ordering.OrderService;
import com.example.coffee.persistence.dao.ItemInventoryDao;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("demo")
class HttpApiTest {
  static final TestDatabase DB = TestDatabase.get();
  static final String CART =
      "{\"customerName\":\"Alex\",\"items\":[{\"itemId\":\"cappuccino\",\"quantity\":2},{\"itemId\":\"croissant\",\"quantity\":1}]}\n";
  static final String COOKIE =
      "{\"customerName\":\"Alex\",\"items\":[{\"itemId\":\"cookie\",\"quantity\":1}]}";
  static final String AUTH =
      "Basic "
          + Base64.getEncoder()
              .encodeToString("owner:coffee-demo-2026".getBytes(StandardCharsets.UTF_8));
  @LocalServerPort int port;
  @Autowired ObjectMapper mapper;
  @Autowired JdbcTemplate jdbc;
  @Autowired OrderService orderService;
  @Autowired PaymentService paymentService;
  @Autowired InventoryService inventoryService;
  @Autowired ReceiptService receiptService;
  @Autowired ItemInventoryDao itemInventoryDao;
  @Autowired PlatformTransactionManager transactionManager;
  @Autowired DataSource dataSource;

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", () -> DB.url);
    registry.add("spring.datasource.username", () -> DB.username);
    registry.add("spring.datasource.password", () -> DB.password);
  }

  @BeforeEach
  void reset() {
    TestDatabase.reset(jdbc);
  }

  @Test
  void writeServicesRejectMissingOrReadOnlyTransaction() {
    CreateOrderRequest request =
        new CreateOrderRequest("Alex", Collections.singletonList(new ItemRequest("cookie", 1)));
    UUID id = UUID.randomUUID();
    assertThatThrownBy(() -> orderService.createOrder(request))
        .isInstanceOf(IllegalTransactionStateException.class);
    assertThatThrownBy(() -> orderService.lockOrder(id))
        .isInstanceOf(IllegalTransactionStateException.class);
    assertThatThrownBy(() -> orderService.markOrderPaid(1001L, Instant.now()))
        .isInstanceOf(IllegalTransactionStateException.class);
    assertThatThrownBy(() -> paymentService.processMockPayment(id, PaymentIntent.APPROVE))
        .isInstanceOf(IllegalTransactionStateException.class);
    assertThatThrownBy(
            () -> inventoryService.deductStockAndRecordChanges(1001L, Collections.emptyList()))
        .isInstanceOf(IllegalTransactionStateException.class);
    assertThatThrownBy(() -> receiptService.saveReceipt(null))
        .isInstanceOf(IllegalTransactionStateException.class);
    TransactionTemplate readOnly = new TransactionTemplate(transactionManager);
    readOnly.setReadOnly(true);
    readOnly.execute(
        tx -> {
          assertThatThrownBy(() -> orderService.createOrder(request))
              .isInstanceOf(IllegalTransactionStateException.class);
          assertThatThrownBy(() -> orderService.lockOrder(id))
              .isInstanceOf(IllegalTransactionStateException.class);
          assertThatThrownBy(() -> orderService.markOrderPaid(1001L, Instant.now()))
              .isInstanceOf(IllegalTransactionStateException.class);
          assertThatThrownBy(() -> paymentService.processMockPayment(id, PaymentIntent.APPROVE))
              .isInstanceOf(IllegalTransactionStateException.class);
          assertThatThrownBy(
                  () ->
                      inventoryService.deductStockAndRecordChanges(1001L, Collections.emptyList()))
              .isInstanceOf(IllegalTransactionStateException.class);
          assertThatThrownBy(() -> receiptService.saveReceipt(null))
              .isInstanceOf(IllegalTransactionStateException.class);
          return null;
        });
    assertThat(count("customer_order")).isZero();
    assertThat(count("idempotency_record")).isZero();
    assertThat(count("inventory_change_log")).isZero();
    assertThat(count("receipt")).isZero();
    assertThat(stock("cappuccino")).isEqualTo(10);
  }

  @Test
  void caughtMandatoryFailureStillRollsBackTheOuterTransaction() {
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    // A failing MANDATORY participant marks the outer transaction rollback-only, even if caught.
    assertThatThrownBy(
            () ->
                transaction.execute(
                    tx -> {
                      orderService.createOrder(
                          new CreateOrderRequest(
                              "Alex", Collections.singletonList(new ItemRequest("cookie", 1))));
                      assertThatThrownBy(
                              () ->
                                  paymentService.processMockPayment(
                                      UUID.randomUUID(), PaymentIntent.APPROVE))
                          .isInstanceOf(com.example.coffee.common.error.BusinessException.class);
                      return null;
                    }))
        .isInstanceOf(UnexpectedRollbackException.class);

    assertThat(count("customer_order")).isZero();
    assertThat(count("order_line")).isZero();
    assertThat(count("receipt")).isZero();
    assertThat(stock("cookie")).isEqualTo(12);
  }

  @Test
  void mapperReadsObserveOtherCommittedWritesWithinReadCommittedTransaction() {
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    transaction.execute(
        tx -> {
          assertThat(
                  itemInventoryDao.findAll().stream()
                      .filter(row -> row.getItemId().equals(itemId("cappuccino")))
                      .findFirst()
                      .get()
                      .getStockQuantity())
              .isEqualTo(10);
          // A different JDBC connection commits without flushing the first session's MyBatis cache.
          try (Connection other = dataSource.getConnection();
              PreparedStatement update =
                  other.prepareStatement(
                      "UPDATE item_inventory SET stock_quantity=7 WHERE item_id=(SELECT id FROM menu_item WHERE item_code='cappuccino')")) {
            assertThat(other.getAutoCommit()).isTrue();
            assertThat(update.executeUpdate()).isEqualTo(1);
          } catch (SQLException e) {
            throw new IllegalStateException(e);
          }
          assertThat(
                  itemInventoryDao.findAll().stream()
                      .filter(row -> row.getItemId().equals(itemId("cappuccino")))
                      .findFirst()
                      .get()
                      .getStockQuantity())
              .isEqualTo(7);
          return null;
        });
  }

  @Test
  void referenceFlowMatchesProvidedFixtures() throws Exception {
    JsonNode fixtures =
        mapper.readTree(
            Files.readAllBytes(java.nio.file.Paths.get("reference/reference_fixtures.json")));
    assertThat(get("/actuator/health").statusCode()).isEqualTo(200);
    assertThat(json(get("/api/v1/menu")).path("currency").asText()).isEqualTo("USD");
    assertThat(get("/openapi.yaml").statusCode()).isEqualTo(200);
    HttpResult created = create("reference-create", CART);
    assertThat(created.statusCode()).isEqualTo(201);
    JsonNode order = json(created);
    assertThat(order.path("lines")).isEqualTo(fixtures.path("orderPending").path("lines"));
    for (String field :
        Arrays.asList(
            "customerName", "status", "currency", "subtotalMinor", "discountMinor", "totalMinor")) {
      assertThat(order.path(field)).as(field).isEqualTo(fixtures.path("orderPending").path(field));
    }
    assertThat(stock("cappuccino")).isEqualTo(10);
    String id = order.path("id").asText();
    assertError(get("/api/v1/orders/" + id + "/receipt"), 409, "ORDER_NOT_PAID");
    HttpResult paid = pay(id, "reference-pay", "APPROVE");
    assertThat(paid.statusCode()).isEqualTo(200);
    assertThat(json(paid).path("order").path("status").asText()).isEqualTo("PAID");
    assertThat(stock("cappuccino")).isEqualTo(8);
    assertThat(stock("croissant")).isEqualTo(7);
    JsonNode receipt = json(get("/api/v1/orders/" + id + "/receipt"));
    assertThat(receipt.path("totalMinor").asLong()).isEqualTo(1150);
    assertThat(receipt.path("paidAt")).isEqualTo(json(paid).path("order").path("paidAt"));
    assertThat(json(owner("PAID", AUTH)).path("orders").size()).isEqualTo(1);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{\"customerName\":\"\",\"items\":[{\"itemId\":\"cookie\",\"quantity\":1}]}",
        "{\"customerName\":\"  \",\"items\":[{\"itemId\":\"cookie\",\"quantity\":1}]}",
        "{\"customerName\":\"\u3000\",\"items\":[{\"itemId\":\"cookie\",\"quantity\":1}]}",
        "{\"customerName\":\" \u2003\u3000 \",\"items\":[{\"itemId\":\"cookie\",\"quantity\":1}]}",
        "{\"customerName\":\"Alex\",\"items\":[]}",
        "{\"customerName\":\"Alex\",\"items\":[null]}",
        "{\"customerName\":\"Alex\",\"items\":[{\"itemId\":\"cookie\",\"quantity\":0}]}",
        "{\"customerName\":\"Alex\",\"items\":[{\"itemId\":\"cookie\",\"quantity\":-1}]}",
        "{\"customerName\":\"Alex\",\"items\":[{\"itemId\":\"cookie\",\"quantity\":21}]}",
        "{\"customerName\":\"Alex\",\"items\":[{\"itemId\":\"cookie\"}]}"
      })
  void invalidInputDoesNotPersist(String body) {
    assertError(create("invalid-input", body), 422, "VALIDATION_ERROR");
    assertThat(count("customer_order")).isZero();
    assertThat(count("idempotency_record")).isZero();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{",
        "null",
        "{\"customerName\":\"A\",\"customerName\":\"B\",\"items\":[]}",
        "{\"customerName\":\"Alex\",\"priceMinor\":1,\"items\":[]}",
        "{\"customerName\":\"Alex\",\"items\":[{\"itemId\":\"cookie\",\"quantity\":1.5}]}",
        "{\"customerName\":\"Alex\",\"items\":[{\"itemId\":\"cookie\",\"quantity\":\"1\"}]}"
      })
  void strictJsonRejectsMalformedOrAmbiguousRequests(String body) {
    assertError(create("bad-json-key", body), 400, "BAD_REQUEST");
    assertThat(count("customer_order")).isZero();
  }

  @Test
  void unsupportedAndDuplicateItemsAreRejected() {
    assertError(create("unknown-item", COOKIE.replace("cookie", "unknown")), 422, "INVALID_ITEM");
    assertError(
        create(
            "duplicate-item",
            "{\"customerName\":\"Alex\",\"items\":[{\"itemId\":\"cookie\",\"quantity\":1},{\"itemId\":\"cookie\",\"quantity\":1}]}"),
        422,
        "DUPLICATE_ITEM");
    jdbc.execute("UPDATE menu_item SET active=FALSE WHERE item_code='cookie'");
    assertError(create("inactive-item", COOKIE), 422, "INVALID_ITEM");
    assertThat(count("customer_order")).isZero();
  }

  @Test
  void stockCheckOnCreationIsAdvisoryAndDoesNotReserve() {
    jdbc.execute(
        "UPDATE item_inventory SET stock_quantity=1 WHERE item_id=(SELECT id FROM menu_item WHERE item_code='cookie')");
    assertThat(create("pending-one", COOKIE).statusCode()).isEqualTo(201);
    assertThat(create("pending-two", COOKIE).statusCode()).isEqualTo(201);
    assertThat(stock("cookie")).isEqualTo(1);
    assertError(
        create("too-many-cookies", COOKIE.replace("\"quantity\":1", "\"quantity\":2")),
        409,
        "INSUFFICIENT_STOCK");
  }

  @Test
  void idempotencyHeaderRequiredAndBounded() {
    assertError(create(null, CART), 400, "INVALID_IDEMPOTENCY_KEY");
    assertError(create("short", CART), 400, "INVALID_IDEMPOTENCY_KEY");
    assertError(
        create(String.join("", Collections.nCopies(129, "a")), CART),
        400,
        "INVALID_IDEMPOTENCY_KEY");
    assertError(create("has space", CART), 400, "INVALID_IDEMPOTENCY_KEY");
    assertThat(count("customer_order")).isZero();
  }

  @Test
  void caseDistinctKeysDoNotCollideInMySql() {
    String first = id(create("Case-Request-001", COOKIE));
    String second = id(create("case-request-001", COOKIE));
    assertThat(first).isNotEqualTo(second);
    assertThat(count("customer_order")).isEqualTo(2);
    assertThat(count("idempotency_record")).isEqualTo(2);
  }

  @Test
  void schemaUsesInnoDbAndRejectsNegativeInventoryInStrictMode() {
    assertThat(
            jdbc.queryForList(
                "SELECT DISTINCT ENGINE FROM information_schema.tables WHERE table_schema=DATABASE()",
                String.class))
        .containsOnly("InnoDB");
    assertThatThrownBy(
            () ->
                jdbc.execute(
                    "UPDATE item_inventory SET stock_quantity=-1 WHERE item_id=(SELECT id FROM menu_item WHERE item_code='cookie')"))
        .isInstanceOf(DataAccessException.class);
    assertThat(stock("cookie")).isEqualTo(12);
  }

  @Test
  void unicodeNamesAndUtcTimestampsSurviveMySqlRoundTrip() {
    Instant before = Instant.now().minusSeconds(1);
    HttpResult created = create("unicode-time-key", COOKIE.replace("Alex", "张三 😀"));
    String orderId = id(created);
    JsonNode order = json(created);
    assertThat(order.path("customerName").asText()).isEqualTo("张三 😀");
    Instant stored =
        jdbc.queryForObject(
            "SELECT create_time FROM customer_order WHERE order_uuid=?",
            (rs, i) ->
                rs.getTimestamp(1, Calendar.getInstance(TimeZone.getTimeZone("UTC"))).toInstant(),
            orderId);
    assertThat(Instant.parse(order.path("createdAt").asText())).isEqualTo(stored);
    assertThat(stored).isBetween(before, Instant.now().plusSeconds(1));
  }

  @Test
  void createReplayIsOriginalEvenAfterPaymentAndCanonicalizesOrder() {
    HttpResult original = create("stable-create-key", CART);
    String id = json(original).path("id").asText();
    pay(id, "stable-pay-key", "APPROVE");
    HttpResult retry =
        create(
            "stable-create-key",
            "{\"items\":[{\"quantity\":1,\"itemId\":\"croissant\"},{\"quantity\":2,\"itemId\":\"cappuccino\"}],\"customerName\":\" Alex \"}");
    assertThat(retry.getBody()).isEqualTo(original.getBody());
    assertThat(retry.header("Idempotency-Replayed")).contains("true");
    assertThat(json(get("/api/v1/orders/" + id)).path("status").asText()).isEqualTo("PAID");
    assertError(
        create("stable-create-key", CART.replace("Alex", "Sam")), 409, "IDEMPOTENCY_KEY_REUSED");
    assertThat(count("customer_order")).isEqualTo(1);
  }

  @Test
  void paymentDeclineReplayAndLaterApprovalHaveExplicitSemantics() {
    String id = id(create("decline-create", CART));
    HttpResult declined = pay(id, "decline-attempt", "DECLINE");
    assertThat(declined.statusCode()).isEqualTo(200);
    assertThat(json(declined).path("outcome").asText()).isEqualTo("DECLINED");
    assertThat(stock("cappuccino")).isEqualTo(10);
    assertThat(count("receipt")).isZero();
    assertError(get("/api/v1/orders/" + id + "/receipt"), 409, "ORDER_NOT_PAID");
    assertError(pay(id, "decline-attempt", "APPROVE"), 409, "IDEMPOTENCY_KEY_REUSED");
    HttpResult approved = pay(id, "approve-attempt", "APPROVE");
    assertThat(approved.statusCode()).isEqualTo(200);
    assertThat(pay(id, "approve-attempt", "APPROVE").getBody()).isEqualTo(approved.getBody());
    assertThat(pay(id, "approve-new-key", "APPROVE").getBody()).isEqualTo(approved.getBody());
    assertThat(pay(id, "decline-attempt", "DECLINE").getBody()).isEqualTo(declined.getBody());
    assertError(pay(id, "new-decline-key", "DECLINE"), 409, "ORDER_ALREADY_PAID");
    assertThat(stock("cappuccino")).isEqualTo(8);
    assertThat(count("receipt")).isEqualTo(1);
    assertThat(count("inventory_change_log")).isEqualTo(2);
  }

  @Test
  void paymentKeyIsScopedToOrder() {
    String first = id(create("scope-create-one", COOKIE)),
        second = id(create("scope-create-two", COOKIE));
    assertThat(pay(first, "same-payment-key", "APPROVE").statusCode()).isEqualTo(200);
    assertThat(pay(second, "same-payment-key", "APPROVE").statusCode()).isEqualTo(200);
    assertThat(stock("cookie")).isEqualTo(10);
  }

  @Test
  void existingPriceAndReceiptDoNotChangeWhenCatalogueChanges() {
    String id = id(create("snapshot-create", CART));
    jdbc.execute(
        "UPDATE menu_item SET price_minor=999,name='Changed',active=FALSE WHERE item_code='cappuccino'");
    jdbc.execute("UPDATE combo_offer SET bundle_price_minor=1");
    JsonNode paid = json(pay(id, "snapshot-approve", "APPROVE")).path("order");
    assertThat(paid.path("totalMinor").asLong()).isEqualTo(1150);
    assertThat(paid.path("lines").get(0).path("name").asText()).isEqualTo("Cappuccino");
    assertThat(paid.path("lines").get(0).path("unitPriceMinor").asLong()).isEqualTo(450);
    JsonNode receipt = json(get("/api/v1/orders/" + id + "/receipt"));
    assertThat(receipt.path("subtotalMinor").asLong()).isEqualTo(1250);
    assertThat(receipt.path("discountMinor").asLong()).isEqualTo(100);
    assertThat(receipt.path("totalMinor").asLong()).isEqualTo(1150);
    assertThat(receipt.path("lines")).isEqualTo(paid.path("lines"));
    assertThat(receipt.path("paidAt")).isEqualTo(paid.path("paidAt"));
    jdbc.execute("UPDATE menu_item SET price_minor=1 WHERE item_code='cappuccino'");
    assertThat(json(get("/api/v1/orders/" + id + "/receipt"))).isEqualTo(receipt);
  }

  @Test
  void multiItemInsufficientStockDoesNotPartiallyDeductAndCanBeRetried() {
    String id = id(create("multi-item-create", CART));
    jdbc.execute(
        "UPDATE item_inventory SET stock_quantity=0 WHERE item_id=(SELECT id FROM menu_item WHERE item_code='croissant')");
    assertError(pay(id, "multi-item-pay", "APPROVE"), 409, "INSUFFICIENT_STOCK");
    assertThat(stock("cappuccino")).isEqualTo(10);
    assertThat(count("inventory_change_log")).isZero();
    assertThat(count("receipt")).isZero();
    assertThat(json(get("/api/v1/orders/" + id)).path("status").asText())
        .isEqualTo("PENDING_PAYMENT");
    jdbc.execute(
        "UPDATE item_inventory SET stock_quantity=1 WHERE item_id=(SELECT id FROM menu_item WHERE item_code='croissant')");
    assertThat(pay(id, "multi-item-pay", "APPROVE").statusCode()).isEqualTo(200);
  }

  @Test
  void failureAfterDeductionRollsBackOrderStockReceiptAndKeyTogether() {
    String id = id(create("rollback-create", CART));
    jdbc.execute("ALTER TABLE receipt MODIFY payload VARCHAR(1) NOT NULL");
    try {
      assertError(pay(id, "rollback-payment", "APPROVE"), 500, "INTERNAL_ERROR");
    } finally {
      jdbc.execute("ALTER TABLE receipt MODIFY payload MEDIUMTEXT NOT NULL");
    }
    assertThat(stock("cappuccino")).isEqualTo(10);
    assertThat(count("inventory_change_log")).isZero();
    assertThat(count("receipt")).isZero();
    assertThat(count("idempotency_record")).isEqualTo(1);
    assertThat(json(get("/api/v1/orders/" + id)).path("status").asText())
        .isEqualTo("PENDING_PAYMENT");
    assertThat(pay(id, "rollback-payment", "APPROVE").statusCode()).isEqualTo(200);
  }

  @Test
  void concurrentSameKeyCreateProducesOneOrderAndIdenticalResponses() throws Exception {
    List<HttpResult> results =
        race(
            IntStream.range(0, 8)
                .mapToObj(i -> (Supplier<HttpResult>) () -> create("racing-create-key", CART))
                .collect(Collectors.toList()));
    assertThat(results).allSatisfy(r -> assertThat(r.statusCode()).isEqualTo(201));
    assertThat(results.stream().map(HttpResult::getBody).distinct().count()).isEqualTo(1);
    assertThat(count("customer_order")).isEqualTo(1);
    assertThat(count("idempotency_record")).isEqualTo(1);
  }

  @Test
  void concurrentConflictingPayloadsChooseOneWinner() throws Exception {
    List<HttpResult> results =
        race(
            Arrays.asList(
                () -> create("conflicting-race", CART), () -> create("conflicting-race", COOKIE)));
    assertThat(results.stream().map(HttpResult::statusCode)).containsExactlyInAnyOrder(201, 409);
    assertThat(count("customer_order")).isEqualTo(1);
  }

  @Test
  void concurrentPaymentsWithDifferentKeysStillDeductExactlyOnce() throws Exception {
    String id = id(create("same-order-create", CART));
    List<HttpResult> results =
        race(
            IntStream.range(0, 8)
                .mapToObj(i -> (Supplier<HttpResult>) () -> pay(id, "parallel-pay-" + i, "APPROVE"))
                .collect(Collectors.toList()));
    assertThat(results).allSatisfy(r -> assertThat(r.statusCode()).isEqualTo(200));
    assertThat(results.stream().map(HttpResult::getBody).distinct().count()).isEqualTo(1);
    assertThat(stock("cappuccino")).isEqualTo(8);
    assertThat(count("receipt")).isEqualTo(1);
    assertThat(count("inventory_change_log")).isEqualTo(2);
  }

  @Test
  void lastItemRaceAllowsExactlyOnePaidOrder() throws Exception {
    jdbc.execute(
        "UPDATE item_inventory SET stock_quantity=1 WHERE item_id=(SELECT id FROM menu_item WHERE item_code='cookie')");
    List<String> ids =
        IntStream.range(0, 8)
            .mapToObj(i -> id(create("last-item-create-" + i, COOKIE)))
            .collect(Collectors.toList());
    List<HttpResult> results =
        race(
            ids.stream()
                .map(id -> (Supplier<HttpResult>) () -> pay(id, "last-item-pay", "APPROVE"))
                .collect(Collectors.toList()));
    assertThat(results.stream().filter(r -> r.statusCode() == 200).count()).isEqualTo(1);
    assertThat(results.stream().filter(r -> r.statusCode() == 409).count()).isEqualTo(7);
    results.stream()
        .filter(r -> r.statusCode() == 409)
        .forEach(r -> assertError(r, 409, "INSUFFICIENT_STOCK"));
    assertThat(stock("cookie")).isZero();
    assertThat(count("receipt")).isEqualTo(1);
    assertThat(count("inventory_change_log")).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM customer_order WHERE status=2", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void oppositeInputItemOrdersDoNotDeadlock() throws Exception {
    String first = id(create("lock-order-first", CART));
    String second =
        id(
            create(
                "lock-order-second",
                "{\"customerName\":\"Sam\",\"items\":[{\"itemId\":\"croissant\",\"quantity\":1},{\"itemId\":\"cappuccino\",\"quantity\":2}]}"));
    assertThat(
            race(
                Arrays.asList(
                    () -> pay(first, "lock-order-pay", "APPROVE"),
                    () -> pay(second, "lock-order-pay", "APPROVE"))))
        .allSatisfy(r -> assertThat(r.statusCode()).isEqualTo(200));
    assertThat(stock("cappuccino")).isEqualTo(6);
  }

  @Test
  void boundedLockWaitReturnsRetryableErrorAndLeavesNoPartialWrites() throws Exception {
    String id = id(create("timeout-create", COOKIE));
    try (Connection connection = jdbc.getDataSource().getConnection()) {
      connection.setAutoCommit(false);
      try (Statement statement = connection.createStatement()) {
        statement
            .executeQuery(
                "SELECT * FROM item_inventory WHERE item_id=(SELECT id FROM menu_item WHERE item_code='cookie') FOR UPDATE")
            .close();
        HttpResult result = pay(id, "timeout-payment", "APPROVE");
        assertError(result, 503, "TEMPORARILY_UNAVAILABLE");
        assertThat(result.header("Retry-After")).contains("1");
      } finally {
        connection.rollback();
      }
    }
    assertThat(stock("cookie")).isEqualTo(12);
    assertThat(count("receipt")).isZero();
    assertThat(count("idempotency_record")).isEqualTo(1);
    assertThat(pay(id, "timeout-payment", "APPROVE").statusCode()).isEqualTo(200);
  }

  @Test
  void ownerAuthenticationOrderingAndStatusFilter() {
    assertError(owner("PAID", null), 401, "UNAUTHORIZED");
    assertThat(owner("PAID", null).header("WWW-Authenticate")).isPresent();
    assertError(
        owner(
            "PAID",
            "Basic "
                + Base64.getEncoder()
                    .encodeToString("owner:wrong".getBytes(StandardCharsets.UTF_8))),
        401,
        "UNAUTHORIZED");
    String first = id(create("queue-first-create", COOKIE));
    String second = id(create("queue-second-create", COOKIE));
    create("queue-pending-create", COOKIE);
    pay(second, "queue-second-payment", "APPROVE");
    pay(first, "queue-first-payment", "APPROVE");
    JsonNode queue = json(owner("PAID", AUTH)).path("orders");
    assertThat(queue.get(0).path("id").asText()).isEqualTo(second);
    assertThat(queue.size()).isEqualTo(2);
    jdbc.execute("UPDATE customer_order SET paid_at='2026-11-03 08:00:00.000000' WHERE status=2");
    queue = json(owner("PAID", AUTH)).path("orders");
    assertThat(queue.get(0).path("orderNumber").asText()).isEqualTo("C-1001");
    assertThat(queue.get(1).path("orderNumber").asText()).isEqualTo("C-1002");
    assertError(owner("PENDING_PAYMENT", AUTH), 400, "UNSUPPORTED_STATUS");
  }

  @Test
  void invalidIdentifiersUnknownRoutesAndPaymentIntentHaveStableErrors() {
    String missing = UUID.randomUUID().toString();
    assertError(get("/api/v1/orders/" + missing), 404, "ORDER_NOT_FOUND");
    assertError(pay(missing, "missing-order-key", "APPROVE"), 404, "ORDER_NOT_FOUND");
    assertError(get("/api/v1/orders/bad-id"), 400, "BAD_REQUEST");
    assertError(get("/no-such-resource"), 404, "NOT_FOUND");
    assertError(pay(missing, "bad-intent-key", "MAYBE"), 400, "BAD_REQUEST");
    assertThat(count("idempotency_record")).isZero();
  }

  HttpResult get(String path) {
    return send("GET", path, null, null, null);
  }

  HttpResult owner(String status, String auth) {
    return send("GET", "/api/v1/owner/orders?status=" + status, null, null, auth);
  }

  HttpResult create(String key, String body) {
    return send("POST", "/api/v1/orders", body, key, null);
  }

  HttpResult pay(String id, String key, String outcome) {
    return send(
        "POST",
        "/api/v1/orders/" + id + "/payments/mock",
        "{\"outcome\":\"" + outcome + "\"}",
        key,
        null);
  }

  HttpResult send(String method, String path, String body, String key, String auth) {
    return HttpTestClient.send("http://127.0.0.1:" + port + path, method, body, key, auth);
  }

  JsonNode json(HttpResult response) {
    try {
      return mapper.readTree(response.getBody());
    } catch (Exception e) {
      throw new AssertionError(response.getBody(), e);
    }
  }

  String id(HttpResult response) {
    assertThat(response.statusCode()).isEqualTo(201);
    return json(response).path("id").asText();
  }

  void assertError(HttpResult response, int status, String code) {
    assertThat(response.statusCode()).as(response.getBody()).isEqualTo(status);
    assertThat(json(response).path("code").asText()).isEqualTo(code);
  }

  long itemId(String code) {
    return jdbc.queryForObject("SELECT id FROM menu_item WHERE item_code=?", Long.class, code);
  }

  int stock(String item) {
    return jdbc.queryForObject(
        "SELECT stock_quantity FROM item_inventory WHERE item_id=(SELECT id FROM menu_item WHERE item_code=?)",
        Integer.class,
        item);
  }

  int count(String table) {
    return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
  }

  List<HttpResult> race(List<Supplier<HttpResult>> actions) throws Exception {
    CountDownLatch ready = new CountDownLatch(actions.size());
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(actions.size());
    try {
      List<Future<HttpResult>> futures = new ArrayList<>();
      for (Supplier<HttpResult> action : actions) {
        futures.add(
            pool.submit(
                () -> {
                  ready.countDown();
                  if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("race barrier timed out");
                  }
                  return action.get();
                }));
      }
      try {
        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      } finally {
        start.countDown();
      }
      List<HttpResult> responses = new ArrayList<>();
      for (Future<HttpResult> future : futures) {
        responses.add(future.get(25, TimeUnit.SECONDS));
      }
      return responses;
    } finally {
      start.countDown();
      pool.shutdownNow();
      if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Race workers did not terminate");
      }
    }
  }
}
