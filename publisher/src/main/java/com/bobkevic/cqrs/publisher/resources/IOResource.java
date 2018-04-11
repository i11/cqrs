package com.bobkevic.cqrs.publisher.resources;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;

import com.bobkevic.cqrs.publisher.dtos.Message;
import com.bobkevic.cqrs.publisher.repositories.Repository;
import java.util.List;
import java.util.concurrent.Future;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IOResource {

  @Autowired
  private Repository pubsubRepository;

  @RequestMapping("/in/{destination}")
  public Future<String> in(@PathVariable("destination") final String destination,
                           @RequestBody final Message message) {
    return pubsubRepository.store(destination, message)
        .thenApply((id) -> "Received: " + message + ". Stored: " + id);
  }

  @RequestMapping("/out/{source}")
  public Future<List<Message>> out(@PathVariable("source") final String source,
                                   @RequestParam("key") final String key,
                                   @RequestParam("value") final String value,
                                   @RequestParam(value = "limit", defaultValue = "100") final int limit) {
    return pubsubRepository.get(source, key, value, limit)
        .thenApply(iterator -> {
          final List<Message> messages = newArrayList();
          iterator.forEachRemaining(messages::add);
          return messages.stream().limit(limit).collect(toList());
        });
  }
}