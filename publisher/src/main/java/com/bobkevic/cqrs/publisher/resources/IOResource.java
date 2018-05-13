package com.bobkevic.cqrs.publisher.resources;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;

import com.bobkevic.cqrs.publisher.dtos.Message;
import com.bobkevic.cqrs.publisher.repositories.Repository;
import com.google.common.collect.ImmutableList;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.HandlerMapping;

@RestController
public class IOResource {

  private final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
//  @Qualifier(value = "pubsubRepository")
  @Qualifier(value = "bigtableRepository")
  private Repository repository;

  @RequestMapping("/in/**")
  public Future<String> in(final HttpServletRequest request,
                           @RequestParam(value = "id", required = false) final UUID id,
                           @RequestBody final Message message) {
    final String uri =
        request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE).toString()
            .replaceAll("^/in/", "");

    log.info("Got request on: {}", uri);
    return repository.store(uri, id, message)
        .thenApply(v -> "Received: " + message + ". Stored: " + v);
  }


  @RequestMapping("/out/**")
  public Future<List<Message>> out(final HttpServletRequest request,
                                   @RequestParam("key") final String key,
                                   @RequestParam("value") final String value,
                                   @RequestParam(value = "limit", defaultValue = "100") final int limit) {
    final String uri =
        request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE).toString()
            .replaceAll("^/out"
                        + "/", "");

    return repository.get(uri, key, value, limit)
        .thenApply(iterator -> {
          final List<Message> messages = newArrayList();
          iterator.forEachRemaining(messages::add);
          return messages.stream().limit(limit).collect(toList());
        });
  }
}