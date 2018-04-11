package com.bobkevic.cqrs.publisher.dtos;

import com.bobkevic.cqrs.publisher.utils.Json;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableMessage.class)
@JsonDeserialize(as = ImmutableMessage.class)
@Value.Style(forceJacksonPropertyNames = false)
public interface Message {

  TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE =
      new TypeReference<Map<String, Object>>() {
      };

  /**
   * Message name
   */
  String name();

  /**
   * Generally deserialized message body
   */
  @JsonIgnore
  @Value.Auxiliary
  default Optional<Map<String, Object>> messageAsMap(final ObjectMapper json) {
    return Json.neglectingDeserialization(json, message().toString(), MAP_TYPE_REFERENCE);
  }

  /**
   * Serialized message body
   */
  ObjectNode message();

  /**
   * Generally deserialized message attributes
   */
  Optional<Map<String, String>> attributes();

  Optional<UUID> correlationId();
}
