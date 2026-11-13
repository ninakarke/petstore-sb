package com.petstore.order.internal.persistence.mongo;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import com.mongodb.client.result.UpdateResult;
import com.petstore.order.Handoff;
import com.petstore.order.HandoffStatus;
import com.petstore.order.Order;
import com.petstore.order.internal.OrderStore;
import com.petstore.order.internal.persistence.mongo.OrderDocument.HandoffPart;
import com.petstore.shared.DocumentStore;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * MongoDB adapter of the order port.
 *
 * <p>Every method is one operation on one document, which MongoDB applies atomically. Nothing here
 * needs a transaction, so the adapter works the same on a single server, a replica set and Atlas.
 *
 * <p>The uniqueness rules of the relational schema are indexes here: the order id is the document
 * key, and a user's idempotency key is a unique index. Breaking either is reported as the same
 * {@code DataIntegrityViolationException} the port promises.
 */
@Repository
@DocumentStore
class MongoOrderStore implements OrderStore, InitializingBean {

  private static final String PENDING = HandoffStatus.PENDING.name();

  private final MongoOperations mongo;

  MongoOrderStore(MongoOperations mongo) {
    this.mongo = mongo;
  }

  @Override
  public void afterPropertiesSet() {
    IndexOperations indexes = mongo.indexOps(OrderDocument.class);
    // A customer's orders, newest first.
    indexes.createIndex(
        new Index()
            .named("by_user_newest_first")
            .on("userId", Sort.Direction.ASC)
            .on("placedAt", Sort.Direction.DESC)
            .on("_id", Sort.Direction.DESC));
    // One order per user and idempotency key. Orders placed without a key are not in this index,
    // so any number of them may exist.
    indexes.createIndex(
        new Index()
            .named("idempotency_key_per_user")
            .on("userId", Sort.Direction.ASC)
            .on("idempotencyKey", Sort.Direction.ASC)
            .unique()
            .partial(PartialIndexFilter.of(where("idempotencyKey").exists(true))));
    // The retry job's work list, oldest first. It contains only orders that are not delivered
    // yet, so it stays small however many orders there are.
    indexes.createIndex(
        new Index()
            .named("pending_handoff")
            .on("placedAt", Sort.Direction.ASC)
            .on("_id", Sort.Direction.ASC)
            .partial(PartialIndexFilter.of(where("handoff.status").is(PENDING))));
  }

  /** One insert: the order, its lines and its pending hand-off appear together or not at all. */
  @Override
  public void save(Order order) {
    mongo.insert(OrderDocument.of(order));
  }

  @Override
  public Optional<Order> find(String orderId) {
    return Optional.ofNullable(mongo.findById(orderId, OrderDocument.class))
        .map(OrderDocument::toOrder);
  }

  @Override
  public Optional<Order> findByIdempotencyKey(String userId, String idempotencyKey) {
    Query query = query(where("userId").is(userId).and("idempotencyKey").is(idempotencyKey));
    return Optional.ofNullable(mongo.findOne(query, OrderDocument.class))
        .map(OrderDocument::toOrder);
  }

  @Override
  public List<Order> findByUser(String userId) {
    Query query =
        query(where("userId").is(userId))
            .with(Sort.by(Sort.Order.desc("placedAt"), Sort.Order.desc("_id")));
    return mongo.find(query, OrderDocument.class).stream().map(OrderDocument::toOrder).toList();
  }

  @Override
  public List<Order> findPendingHandoff(int limit) {
    Query query =
        query(where("handoff.status").is(PENDING)).with(Sort.by("placedAt", "_id")).limit(limit);
    return mongo.find(query, OrderDocument.class).stream().map(OrderDocument::toOrder).toList();
  }

  @Override
  public List<Order> ordersFrom(String startOrderId, int limit) {
    Query query = startOrderId == null ? new Query() : query(where("_id").gte(startOrderId));
    return mongo.find(query.with(Sort.by("_id")).limit(limit), OrderDocument.class).stream()
        .map(OrderDocument::toOrder)
        .toList();
  }

  /** Replaces the hand-off part of the document and nothing else. */
  @Override
  public void updateHandoff(String orderId, Handoff handoff) {
    UpdateResult result =
        mongo.updateFirst(
            query(where("_id").is(orderId)),
            Update.update("handoff", HandoffPart.of(handoff)),
            OrderDocument.class);
    if (result.getMatchedCount() == 0) {
      throw new IllegalArgumentException("No such order: " + orderId);
    }
  }
}
