package com.bobkevic.cqrs.publisher.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

public final class Json {

  private Json() {
  }


  public static String uncheckedSerialization(final ObjectMapper json, final Object object) {
    try {
      return json.writeValueAsString(object);
    } catch (final JsonProcessingException e) {
      throw new UncheckedIOException("Failed serializing: " + object, e);
    }
  }

  public static <T> T uncheckedDeserialization(final ObjectMapper json,
                                               final String serializedObject,
                                               final Class<T> clazz) {
    try {
      return json.readValue(serializedObject, clazz);
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed deserializing: " + serializedObject + " as " + clazz,
          e);
    }
  }

  public static <T> Optional<T> neglectingDeserialization(final ObjectMapper json,
                                                          final String serializedObject,
                                                          final TypeReference<T> typeReference) {
    try {
      return Optional.of(json.readValue(serializedObject, typeReference));
    } catch (final IOException e) {
      return Optional.empty();
    }
  }
}
