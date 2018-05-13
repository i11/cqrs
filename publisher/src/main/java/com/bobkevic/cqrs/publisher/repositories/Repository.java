package com.bobkevic.cqrs.publisher.repositories;

import com.bobkevic.cqrs.publisher.dtos.Message;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface Repository extends AutoCloseable {

  CompletableFuture<String> store(String destination, Message message);

  CompletableFuture<String> store(String destination, UUID id, Message message);

  CompletableFuture<Iterator<Message>> get(String source, String key, String value, int limit);

}
