package com.bobkevic.cqrs.publisher.resources;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.bobkevic.cqrs.publisher.dtos.Message;
import java.util.concurrent.Future;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/publish")
public class PublishResource {

  @RequestMapping("/action")
  public Future<String> publishAction(@RequestBody final Message message) {
    return supplyAsync(() -> "Received: " + message);
  }

  @RequestMapping("/event")
  public Future<String> publishEvent(@RequestBody final Message message) {
    return supplyAsync(() -> "Received: " + message);
  }
}