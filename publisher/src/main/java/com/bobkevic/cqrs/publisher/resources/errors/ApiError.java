package com.bobkevic.cqrs.publisher.resources.errors;

import com.bobkevic.cqrs.publisher.ImmutableStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@ImmutableStyle
public interface ApiError {

  int getStatus();

  Optional<String> getMessage();

  List<String> getErrors();

  String getPath();

  @Value.Default
  default long getTimestamp() {
    return Instant.now().toEpochMilli();
  }

}
