package com.bobkevic.cqrs.publisher.cache;

import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.bobkevic.cqrs.publisher.ImmutableStyle;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.immutables.value.Value;

@Value.Immutable
@ImmutableStyle
public abstract class CompletableFutureCache<T, R> implements AsyncCache<T, R> {

  abstract Optional<RemovalListener<T, R>> removalListener();

  abstract Function<T, R> function();

  abstract Optional<Long> maximumSize();

  abstract Long duration();

  abstract TimeUnit timeUnit();

  @Value.Derived
  LoadingCache<T, R> cache() {
    final CacheBuilder<T, R> cacheBuilder = (CacheBuilder<T, R>) CacheBuilder.newBuilder()
        .expireAfterWrite(duration(), timeUnit());

    maximumSize().ifPresent(cacheBuilder::maximumSize);

    return removalListener()
        .map(cacheBuilder::removalListener)
        .orElse(cacheBuilder)
        .build(CacheLoader.from(function()::apply));
  }

  public CompletableFuture<R> get(T key) {
    return supplyAsync(() -> {
      try {
        return cache().get(key);
      } catch (final ExecutionException e) {
        throw new RuntimeException("Failed getting cache for key: " + key, e);
      }
    });
  }

  public CompletableFuture<Void> invalidate(T key) {
    return runAsync(() -> cache().invalidate(key));
  }

  public CompletableFuture<Void> invalidateAll() {
    return runAsync(() -> cache().invalidateAll());
  }
}