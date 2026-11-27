package com.example.coffee;

import static org.assertj.core.api.Assertions.*;

import com.example.coffee.HttpTestClient.HttpResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

class PersistenceRestartTest {
  @Test
  void committedOrderReceiptInventoryAndIdempotencySurviveApplicationRestart() throws Exception {
    {
      TestDatabase db = TestDatabase.get();
      String original, paid, id;
      try (ServletWebServerApplicationContext app = start(db)) {
        TestDatabase.reset(app.getBean(JdbcTemplate.class));
        HttpResult created =
            call(app, "POST", "/api/v1/orders", HttpApiTest.CART, "persist-create");
        assertThat(created.statusCode()).isEqualTo(201);
        original = created.getBody();
        id = app.getBean(ObjectMapper.class).readTree(original).path("id").asText();
        HttpResult payment =
            call(
                app,
                "POST",
                "/api/v1/orders/" + id + "/payments/mock",
                "{\"outcome\":\"APPROVE\"}",
                "persist-payment");
        assertThat(payment.statusCode()).isEqualTo(200);
        paid = payment.getBody();
      }
      try (ServletWebServerApplicationContext app = start(db)) {
        assertThat(
                call(app, "POST", "/api/v1/orders", HttpApiTest.CART, "persist-create").getBody())
            .isEqualTo(original);
        assertThat(
                call(
                        app,
                        "POST",
                        "/api/v1/orders/" + id + "/payments/mock",
                        "{\"outcome\":\"APPROVE\"}",
                        "persist-payment")
                    .getBody())
            .isEqualTo(paid);
        assertThat(call(app, "GET", "/api/v1/orders/" + id + "/receipt", null, null).statusCode())
            .isEqualTo(200);
        JsonNode menu =
            app.getBean(ObjectMapper.class)
                .readTree(call(app, "GET", "/api/v1/menu", null, null).getBody());
        for (JsonNode item : menu.path("items")) {
          if (item.path("id").asText().equals("cappuccino")) {
            assertThat(item.path("availableStock").asInt()).isEqualTo(8);
          }
        }
      }
    }
  }

  ServletWebServerApplicationContext start(TestDatabase db) {
    return (ServletWebServerApplicationContext)
        new SpringApplicationBuilder(CoffeeApplication.class)
            .profiles("demo")
            .run(
                "--server.port=0",
                "--spring.datasource.url=" + db.url,
                "--spring.datasource.username=" + db.username,
                "--spring.datasource.password=" + db.password);
  }

  HttpResult call(
      ServletWebServerApplicationContext app, String method, String path, String body, String key) {
    return HttpTestClient.send(
        "http://127.0.0.1:" + app.getWebServer().getPort() + path, method, body, key, null);
  }
}
