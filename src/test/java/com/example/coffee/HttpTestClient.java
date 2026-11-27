package com.example.coffee;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Real HTTP transport using only Java 8 APIs; each concurrent call owns its connection. */
final class HttpTestClient {
  private HttpTestClient() {}

  static final class HttpResult {
    private final int status;
    private final String body;
    private final Map<String, List<String>> headers;

    HttpResult(int status, String body, Map<String, List<String>> headers) {
      this.status = status;
      this.body = body;
      this.headers = headers;
    }

    int statusCode() {
      return status;
    }

    String getBody() {
      return body;
    }

    Optional<String> header(String name) {
      return headers.entrySet().stream()
          .filter(e -> name.equalsIgnoreCase(e.getKey()))
          .flatMap(e -> e.getValue().stream())
          .findFirst();
    }
  }

  static HttpResult send(String url, String method, String body, String key, String authorization) {
    HttpURLConnection connection = null;
    try {
      connection = (HttpURLConnection) new URL(url).openConnection(Proxy.NO_PROXY);
      connection.setRequestMethod(method);
      connection.setConnectTimeout(5000);
      connection.setReadTimeout(20000);
      connection.setInstanceFollowRedirects(false);
      if (key != null) {
        connection.setRequestProperty("Idempotency-Key", key);
      }
      if (authorization != null) {
        connection.setRequestProperty("Authorization", authorization);
      }
      if (body != null) {
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(payload.length);
        try (OutputStream out = connection.getOutputStream()) {
          out.write(payload);
        }
      }
      int status = connection.getResponseCode();
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      try (InputStream input =
          status >= 400 ? connection.getErrorStream() : connection.getInputStream()) {
        if (input != null) {
          byte[] buffer = new byte[4096];
          int count;
          while ((count = input.read(buffer)) != -1) {
            bytes.write(buffer, 0, count);
          }
        }
      }
      return new HttpResult(
          status,
          new String(bytes.toByteArray(), StandardCharsets.UTF_8),
          connection.getHeaderFields());
    } catch (Exception e) {
      throw new IllegalStateException("HTTP test request failed", e);
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }
}
