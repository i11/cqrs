package com.bobkevic.cqrs.publisher.repositories;

import static com.bobkevic.cqrs.publisher.ApiToCompletableFutureHelper.toCompletableFuture;
import static com.bobkevic.cqrs.publisher.dtos.Message.OBJECT_MAP_TYPE_REFERENCE;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toMap;

import com.bobkevic.cqrs.publisher.cache.AsyncCache;
import com.bobkevic.cqrs.publisher.cache.CompletableFutureCacheBuilder;
import com.bobkevic.cqrs.publisher.dtos.ImmutableMessage;
import com.bobkevic.cqrs.publisher.dtos.Message;
import com.bobkevic.cqrs.publisher.utils.Json;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.ServiceOptions;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.stub.GrpcSubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStubSettings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.pubsub.v1.AcknowledgeRequest;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.PullRequest;
import com.google.pubsub.v1.PullResponse;
import com.google.pubsub.v1.ReceivedMessage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@org.springframework.stereotype.Repository
public class PubsubRepository implements Repository, AutoCloseable {

  private final ObjectMapper json;
  private final AsyncCache<String, Publisher> publisherCache;

  @Autowired
  public PubsubRepository(final ObjectMapper json,
                          @Qualifier("pubsubRetry") final RetrySettings retrySettings) {
    this.json = json;
    this.publisherCache =
        CompletableFutureCacheBuilder.<String, Publisher>builder()
            .function(
                (destination) -> {
                  try {
                    final ProjectTopicName topicName =
                        ProjectTopicName.of(ServiceOptions.getDefaultProjectId(), destination);
                    return Publisher.newBuilder(topicName)
                        .setRetrySettings(retrySettings)
                        .build();
                  } catch (final IOException e) {
                    throw new UncheckedIOException(
                        "Failed constructing a publisher for topic: " + destination, e);
                  }
                })
            .duration(10L)
            .timeUnit(MINUTES)
            .maximumSize(100L)
            .removalListener(notification -> {
              try {
                notification.getValue().shutdown();
              } catch (final Exception e) {
                throw new RuntimeException(
                    "Failed shutting down publisher for: " + notification.getKey(), e);
              }
            })
            .build();
  }

  @Override
  public CompletableFuture<String> store(final String destination, final Message message) {
    return store(destination, UUID.randomUUID(), message);
  }

  @Override
  public CompletableFuture<String> store(final String destination,
                                         final UUID id,
                                         final Message message) {
    return supplyAsync(() ->
        PubsubMessage.newBuilder()
            .setMessageId(id.toString())
            .setData(message.asTypedByteString(json))
            .putAllAttributes(getMessageAttributes(message, id))
            .build())
        .thenCompose(pubsubMessage -> publisherCache.get(destination)
            .thenCompose(publisher -> toCompletableFuture(publisher.publish(pubsubMessage))));
  }

  Map<String, String> getMessageAttributes(final Message message, final UUID id) {
    final ImmutableMap.Builder<String, String> attributesBuilder =
        ImmutableMap.<String, String>builder()
            .put("correlation_id", message.correlationId().orElse(id).toString());

    message.attributes().ifPresent(attributesBuilder::putAll);
    return attributesBuilder.build();
  }

  @Override
  public CompletableFuture<Iterator<Message>> get(final String source,
                                                  final String key,
                                                  final String value,
                                                  final int limit) {
    return supplyAsync(() -> new Iterator<Message>() {

      private Collection<Message> nextPage;
      private Iterator<Message> nextPageIterator;
      private final AtomicInteger scanCount = new AtomicInteger(0);

      private Collection<Message> fetch() {
        try (final SubscriberStub subscriber = GrpcSubscriberStub
            .create(uncheckedSubscriberStubSettings())) {
          final String subscriptionName =
              ProjectSubscriptionName.format(ServiceOptions.getDefaultProjectId(), source);
          final PullRequest pullRequest =
              PullRequest.newBuilder()
                  .setMaxMessages(limit)
                  .setReturnImmediately(false) // return immediately if messages are not available
                  .setSubscription(subscriptionName)
                  .build();

          final PullResponse pullResponse = subscriber.pullCallable().call(pullRequest);

          final Map<String, Message> ackIdToMessage =
              pullResponse.getReceivedMessagesList().stream()
                  .filter(ReceivedMessage::hasMessage)
                  .map(message -> new AbstractMap.SimpleEntry<>(message.getAckId(),
                      message.getMessage()))
                  .filter(entry -> {
                    final PubsubMessage message = entry.getValue();
                    final Map<String, Object> deserializedData =
                        Json.neglectingDeserialization(json,
                            message.getData().toStringUtf8(),
                            OBJECT_MAP_TYPE_REFERENCE)
                            .orElse(emptyMap());
                    return value.equals(message.getAttributesMap().get(key))
                           || value.equals(message.getMessageId())
                           || Optional.ofNullable(deserializedData.get(key))
                               .map(Object::toString)
                               .filter(value::equals)
                               .isPresent();
                  })
                  .collect(toMap(AbstractMap.SimpleEntry::getKey,
                      entry -> fromPubsubMessage(entry.getValue())));

          final AcknowledgeRequest acknowledgeRequest =
              AcknowledgeRequest.newBuilder()
                  .setSubscription(subscriptionName)
                  .addAllAckIds(ackIdToMessage.keySet())
                  .build();
          subscriber.acknowledgeCallable().futureCall(acknowledgeRequest);

          return ackIdToMessage.values();
        } catch (final Exception e) {
          throw new RuntimeException(
              "Failed getting messages matching: " + key + " with " + value, e);
        }
      }

      @Override
      public boolean hasNext() {
        sync();
        return nextPageIterator.hasNext();
      }

      @Override
      public Message next() {
        sync();
        return nextPageIterator.next();
      }

      private void sync() {
        synchronized (scanCount) {
          if ((null == nextPage || (!nextPageIterator.hasNext() && nextPage.size() > 0))
              && scanCount.get() < limit) {
            nextPage = fetch();
            nextPageIterator = nextPage.iterator();
            scanCount.incrementAndGet();
          }
        }
      }
    });
  }

  @Override
  public void close() throws Exception {
    publisherCache.invalidateAll().get();
  }

  private SubscriberStubSettings uncheckedSubscriberStubSettings() {
    try {
      return SubscriberStubSettings.newBuilder().build();
    } catch (IOException e) {
      throw new UncheckedIOException("Failed building subscriber stub settings", e);
    }
  }

  private Message fromPubsubMessage(final PubsubMessage message) {
    final ObjectNode messageData =
        Json.uncheckedDeserialization(json, message.getData().toStringUtf8(), ObjectNode.class);
    return ImmutableMessage.builder()
        .name(messageData.get("@type").asText())
        .message(messageData.without(ImmutableList.of("@type")))
        .attributes(message.getAttributesMap())
        .correlationId(
            Optional
                .ofNullable(message.getAttributesMap().get("correlation_id"))
                .map(UUID::fromString))
        .build();
  }

}