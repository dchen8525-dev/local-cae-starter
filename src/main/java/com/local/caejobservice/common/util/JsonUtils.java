package com.local.caejobservice.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public final class JsonUtils {

  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private JsonUtils() {}

  public static String writeJson(Map<String, Object> value) {
    try {
      return OBJECT_MAPPER.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Failed to serialize params_json.", exception);
    }
  }

  public static Map<String, Object> readJsonMap(String value) {
    try {
      return OBJECT_MAPPER.readValue(value, MAP_TYPE);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Failed to parse params_json.", exception);
    }
  }
}
