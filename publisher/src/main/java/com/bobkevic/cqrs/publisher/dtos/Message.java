package com.bobkevic.cqrs.publisher.dtos;

import static com.bobkevic.cqrs.publisher.utils.Json.uncheckedSerialization;

import com.bobkevic.cqrs.publisher.utils.Json;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableMessage.class)
@JsonDeserialize(as = ImmutableMessage.class)
@Value.Style(forceJacksonPropertyNames = false)
public interface Message {

  TypeReference<Map<String, Object>> OBJECT_MAP_TYPE_REFERENCE =
      new TypeReference<Map<String, Object>>() {
      };

  TypeReference<Map<String, String>> STRING_MAP_TYPE_REFERENCE =
      new TypeReference<Map<String, String>>() {
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
    return Json.neglectingDeserialization(json, message().toString(), OBJECT_MAP_TYPE_REFERENCE);
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

  @JsonIgnore
  @Value.Auxiliary
  default ByteString asTypedByteString(final ObjectMapper json) {
    final ImmutableMap.Builder<String, Object> messageDataBuilder =
        ImmutableMap.<String, Object>builder().put("@type", name());
    return messageAsMap(json)
        .map(msg -> messageDataBuilder.putAll(msg).build())
        .map(map -> Json.uncheckedSerialization(json, map))
        .map(ByteString::copyFromUtf8)
        .orElseGet(() ->
            ByteString
                .copyFromUtf8(uncheckedSerialization(json, messageDataBuilder.build())));
  }
}
