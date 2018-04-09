package com.bobkevic.cqrs.publisher.dtos;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableGreeting.class)
@JsonDeserialize(as = ImmutableGreeting.class)
@Value.Style(forceJacksonPropertyNames = false)
public interface Greeting {

  long id();

  String content();

}