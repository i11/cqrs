package com.bobkevic.cqrs.publisher.cache;

import java.util.concurrent.CompletableFuture;

public interface AsyncCache<T, R> {

  CompletableFuture<R> get(final T key);

  CompletableFuture<Void> invalidate(final T key);

  CompletableFuture<Void> invalidateAll();
}
