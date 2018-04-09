package com.bobkevic.cqrs.publisher.resources;

import com.bobkevic.cqrs.publisher.dtos.Greeting;
import com.bobkevic.cqrs.publisher.dtos.ImmutableGreeting;
import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingResource {

  private static final String template = "Hello, {0}!";
  private final AtomicLong counter = new AtomicLong();

  @RequestMapping("/greeting")
  public Greeting greeting(@RequestParam(value = "name", defaultValue = "World") final String name) {
    return ImmutableGreeting.builder()
        .id(counter.incrementAndGet())
        .content(MessageFormat.format(template, name))
        .build();
  }
}