package com.example.coffee.common.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class JsonCodec {
  private final ObjectMapper objectMapper;

  public JsonCodec(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public String toJson(Object jsonValue) {
    try {
      return objectMapper.writeValueAsString(jsonValue);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Cannot encode persisted response", e);
    }
  }

  public <T> T fromJson(String jsonValue, Class<T> targetType) {
    try {
      return objectMapper.readValue(jsonValue, targetType);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Cannot decode persisted response", e);
    }
  }
}
