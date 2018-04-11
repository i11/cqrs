package com.bobkevic.cqrs.publisher.cache;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.Test;

public class CompletableFutureCacheTest {

  private CompletableFutureCache<String, String> cache;

  @Before
  public void setUp() {
    cache = CompletableFutureCacheBuilder.<String, String>builder()
        .duration(1L)
        .timeUnit(MINUTES)
        .function(this::reverseString)
        .build();
  }

  @Test
  public void checkDerivedProvidesSameInstance() {
    assertThat(cache.cache(), is(cache.cache()));
  }

  @Test
  public void checkGetPopulatesCache() throws ExecutionException, InterruptedException {
    assertThat(cache.cache().asMap().size(), is(0));
    assertThat(cache.get("key").get(), is("yek"));
    assertThat(cache.cache().asMap().size(), is(1));
    assertThat(cache.get("key").get(), is("yek"));
    assertThat(cache.cache().asMap().size(), is(1));
  }

  @Test
  public void checkInvalidatePurgesCache() throws ExecutionException, InterruptedException {
    cache.get("key").get();
    assertThat(cache.cache().asMap().size(), is(1));
    cache.invalidate("key").get();
    assertThat(cache.cache().asMap().size(), is(0));
  }

  @Test
  public void testRemovalNotification() throws ExecutionException, InterruptedException {
    final AtomicBoolean removed = new AtomicBoolean(false);
    cache = CompletableFutureCacheBuilder.<String, String>builder()
        .duration(1L)
        .timeUnit(MINUTES)
        .function(this::reverseString)
        .removalListener(notification -> {
          if ("key".equals(notification.getKey())) {
            removed.set(true);
            synchronized (removed) {
              removed.notify();
            }
          }
        })
        .build();

    cache.get("key").get();
    assertThat(cache.cache().asMap().size(), is(1));
    synchronized (removed) {
      cache.invalidate("key");
      removed.wait(10000);
    }
    assertThat(cache.cache().asMap().size(), is(0));
    assertThat(removed.get(), is(true));
  }

  private String reverseString(final String input) {
    return Optional.ofNullable(input)
        .map(str -> {
          final StringBuilder reverse = new StringBuilder();
          for (int i = str.length() - 1; i >= 0; i--) {
            reverse.append(str.charAt(i));
          }
          return reverse.toString().trim();
        })
        .orElse(null);
  }
}