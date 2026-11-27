package com.example.coffee;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/** Uses an explicitly selected *_test schema, otherwise an isolated Docker MySQL 5.6.30. */
final class TestDatabase {
  private static final TestDatabase INSTANCE = start();
  final String url;
  final String username;
  final String password;

  private TestDatabase(String url, String username, String password) {
    this.url = url;
    this.username = username;
    this.password = password;
  }

  static TestDatabase get() {
    return INSTANCE;
  }

  private static TestDatabase start() {
    String url = System.getenv("TEST_DB_URL");
    TestDatabase result;
    if (url != null && !url.trim().isEmpty()) {
      result =
          new TestDatabase(
              url,
              env("TEST_DB_USERNAME", "coffee_test"),
              env("TEST_DB_PASSWORD", "coffee-test-only"));
    } else {
      MySQLContainer<?> mysql =
          new MySQLContainer<>(
                  DockerImageName.parse(env("TEST_MYSQL_IMAGE", "coffee-shop-mysql:5.6.30"))
                      .asCompatibleSubstituteFor("mysql"))
              .withDatabaseName("coffee_shop_test")
              .withUsername("coffee_test")
              .withPassword("coffee-test-only")
              .withUrlParam("useSSL", "false")
              .withUrlParam("serverTimezone", "UTC")
              .withUrlParam("characterEncoding", "UTF-8");
      mysql.start();
      result = new TestDatabase(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
    }
    Matcher match =
        Pattern.compile("jdbc:mysql://[^/]+/([A-Za-z0-9_]+)(?:\\?.*)?").matcher(result.url);
    if (!match.matches() || !match.group(1).endsWith("_test")) {
      throw new IllegalArgumentException(
          "TEST_DB_URL must explicitly select a dedicated database ending in _test.");
    }
    try (Connection connection =
        DriverManager.getConnection(result.url, result.username, result.password)) {
      if (!match.group(1).equals(connection.getCatalog())) {
        throw new IllegalStateException("Unexpected test database catalog");
      }
    } catch (Exception e) {
      throw new IllegalStateException("Cannot connect to the dedicated test database", e);
    }
    return result;
  }

  private static String env(String name, String fallback) {
    String value = System.getenv(name);
    return value == null ? fallback : value;
  }

  static void reset(JdbcTemplate jdbc) {
    for (String table :
        Arrays.asList(
            "idempotency_record",
            "receipt",
            "inventory_change_log",
            "order_line",
            "customer_order")) {
      jdbc.execute("DELETE FROM " + table);
    }
    jdbc.execute("ALTER TABLE customer_order AUTO_INCREMENT=1001");
    jdbc.execute(
        "UPDATE menu_item SET active=TRUE, price_minor=CASE item_code WHEN 'cappuccino' THEN 450 WHEN 'croissant' THEN 350 ELSE 250 END");
    jdbc.execute("UPDATE menu_item SET name='Cappuccino' WHERE item_code='cappuccino'");
    jdbc.execute(
        "UPDATE item_inventory inv JOIN menu_item mi ON mi.id=inv.item_id SET inv.stock_quantity=CASE mi.item_code WHEN 'cappuccino' THEN 10 WHEN 'croissant' THEN 8 ELSE 12 END");
    jdbc.execute("UPDATE combo_offer SET active=TRUE, bundle_price_minor=700");
  }
}
