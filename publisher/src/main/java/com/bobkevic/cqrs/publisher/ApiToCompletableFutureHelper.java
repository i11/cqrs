package com.bobkevic.cqrs.publisher;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface ApiToCompletableFutureHelper {

  static <V> CompletableFuture<V> toCompletableFuture(ApiFuture<V> apiFuture) {
    final CompletableFuture<V> completableFuture = new CompletableFuture<V>() {
      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        boolean result = apiFuture.cancel(mayInterruptIfRunning);
        super.cancel(mayInterruptIfRunning);
        return result;
      }
    };
    ApiFutures.addCallback(apiFuture, new ApiFutureCallback<V>() {
      @Override
      public void onSuccess(final V result) {
        completableFuture.complete(result);
      }

      @Override
      public void onFailure(final Throwable ex) {
        completableFuture.completeExceptionally(ex);
      }
    });
    return completableFuture;
  }

  static <T> CompletionStage<T> exceptionallyComplete(Throwable ex) {
    final CompletableFuture<T> future = new CompletableFuture<>();
    future.completeExceptionally(ex);
    return future;
  }

}