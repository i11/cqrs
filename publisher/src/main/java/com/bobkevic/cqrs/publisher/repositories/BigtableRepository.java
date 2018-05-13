package com.bobkevic.cqrs.publisher.repositories;

import static com.bobkevic.cqrs.publisher.ApiToCompletableFutureHelper.toCompletableFuture;
import static com.bobkevic.cqrs.publisher.dtos.Message.STRING_MAP_TYPE_REFERENCE;
import static com.bobkevic.cqrs.publisher.utils.Json.uncheckedSerialization;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.StreamSupport.stream;

import com.bobkevic.cqrs.publisher.cache.AsyncCache;
import com.bobkevic.cqrs.publisher.cache.CompletableFutureCacheBuilder;
import com.bobkevic.cqrs.publisher.dtos.ImmutableMessage;
import com.bobkevic.cqrs.publisher.dtos.Message;
import com.bobkevic.cqrs.publisher.utils.Json;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.gax.rpc.AlreadyExistsException;
import com.google.bigtable.admin.v2.ColumnFamily;
import com.google.bigtable.admin.v2.Table;
import com.google.cloud.ServiceOptions;
import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.models.Query;
import com.google.cloud.bigtable.data.v2.models.Row;
import com.google.cloud.bigtable.data.v2.models.RowMutation;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@org.springframework.stereotype.Repository
public class BigtableRepository implements Repository {

  private final AsyncCache<String, BigtableDataClient> dataClientCache;
  private final BigtableTableAdminClient tableAdminClient;
  private final ObjectMapper json;
  private final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public BigtableRepository(final ObjectMapper json) {
    this.json = json;
    try {
      this.tableAdminClient = BigtableTableAdminClient.create();
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed creating table admin client", e);
    }
    this.dataClientCache =
        CompletableFutureCacheBuilder.<String, BigtableDataClient>builder()
            .function(
                (destination) -> {
                  try {
                    final String[] destinationSplit = sourceSplit(destination);

                    final BigtableTableAdminClient tableAdminClient =
                        BigtableTableAdminClient.create();
                    try {
                      tableAdminClient.createTable(
                          com.google.bigtable.admin.v2.InstanceName
                              .of(ServiceOptions.getDefaultProjectId(), destinationSplit[0]),
                          destinationSplit[1],
                          Table.newBuilder()
                              .putColumnFamilies("messages", ColumnFamily.getDefaultInstance())
                              .build()
                      );
                    } catch (final AlreadyExistsException aee) {
                      log.debug("Table " + destination + " already exists");
                    }
                    tableAdminClient.close();

                    return BigtableDataClient.create(
                        com.google.cloud.bigtable.data.v2.models.InstanceName
                            .of(ServiceOptions.getDefaultProjectId(), destinationSplit[0]));
                  } catch (final Exception e) {
                    throw new RuntimeException(
                        "Failed constructing a publisher for topic: " + destination, e);
                  }
                })
            .duration(10L)
            .timeUnit(MINUTES)
            .maximumSize(100L)
            .removalListener(notification -> {
              try {
                notification.getValue().close();
              } catch (final Exception e) {
                throw new RuntimeException(
                    "Failed shutting down bigtable data client  for: " + notification.getKey(), e);
              }
            })
            .build();

  }

  private String[] sourceSplit(String source) {
    final String[] sourceSplit = source.split("/");

    if (sourceSplit.length < 2) {
      throw new RuntimeException("Current repositry expects the following source format: {}/{}");
    }
    return sourceSplit;
  }

  @Override
  public void close() throws Exception {
    tableAdminClient.close();
    dataClientCache.invalidateAll().get();
  }

  @Override
  public CompletableFuture<String> store(final String destination, final Message message) {
    return store(destination, UUID.randomUUID(), message);
  }

  @Override
  public CompletableFuture<String> store(final String destination,
                                         final UUID id,
                                         final Message message) {
    final UUID uuid = Optional.ofNullable(id).orElseGet(UUID::randomUUID);
    final String[] destinationSplit = sourceSplit(destination);
    return dataClientCache.get(destination)
        .thenCompose(dataClient -> toCompletableFuture(
            dataClient.mutateRowAsync(getRowMutation(destinationSplit[1], uuid, message))))
        .thenApply(ignore -> uuid.toString());
  }

  // TODO: null vs. empty
  private RowMutation getRowMutation(final String table, final UUID key, final Message message) {
    final RowMutation rowMutation = RowMutation.create(table, key.toString())
        .setCell("messages", "name", message.name())
        .setCell("messages", ByteString.copyFromUtf8("data"), message.asTypedByteString(json))
        .setCell("messages", "correlation_id",
            message.correlationId().orElse(key).toString())
        .setCell("messages", "attributes",
            uncheckedSerialization(json, message.attributes().orElse(Collections.emptyMap())));
    return rowMutation;
  }

  @Override
  public CompletableFuture<Iterator<Message>> get(final String source,
                                                  final String key,
                                                  final String value,
                                                  final int limit) {

    final String[] sourceSplit = sourceSplit(source);

    return dataClientCache.get(source)
        .thenApply(client ->
            stream(client.readRows(Query.create(sourceSplit[1]).rowKey(value)).spliterator(), false)
                .map(this::toMessage)
                .collect(toList())
                .iterator());
  }

  private Message toMessage(final Row row) {
    final Map<String, String> rowMap = row.getCells().stream()
        .collect(toMap(cell -> cell.getQualifier().toStringUtf8(),
            cell -> cell.getValue().toStringUtf8()));
    return ImmutableMessage.builder()
        .name(rowMap.get("name"))
        .message(Json.uncheckedDeserialization(json, rowMap.get("data"), ObjectNode.class))
        .correlationId(UUID.fromString(rowMap.get("correlation_id")))
        .attributes(Json.uncheckedDeserialization(json, rowMap.get("attributes"),
            STRING_MAP_TYPE_REFERENCE))
        .build();
  }
}
