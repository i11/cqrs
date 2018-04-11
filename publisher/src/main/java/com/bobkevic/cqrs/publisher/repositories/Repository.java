package com.bobkevic.cqrs.publisher.repositories;

import com.bobkevic.cqrs.publisher.dtos.Message;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

public interface Repository {

  CompletableFuture<String> store(String destination, Message message);

  CompletableFuture<Iterator<Message>> get(String source, String key, String value, int limit);

}
