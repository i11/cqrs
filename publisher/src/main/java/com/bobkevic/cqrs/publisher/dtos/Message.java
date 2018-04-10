package com.bobkevic.cqrs.publisher.dtos;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableMessage.class)
@JsonDeserialize(as = ImmutableMessage.class)
@Value.Style(forceJacksonPropertyNames = false)
public interface Message {

  /**
   * Message name
   */
  String name();

  /**
   * Serialized message body
   */
  Optional<String> message();

  /**
   * Serialized message attributes
   */
  Optional<String> attributes();
}
