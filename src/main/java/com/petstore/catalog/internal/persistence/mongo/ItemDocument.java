package com.petstore.catalog.internal.persistence.mongo;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

/**
 * An item, one document in {@code items}.
 *
 * <p>Holds what the legacy tables {@code item} and {@code item_details} held for one language, plus
 * two values copied from the item's product: its name and its category. An item is always shown
 * with its product's name and is searched by both, so with the copies every read of an item is one
 * document and needs no join. Catalog data is loaded once and has no rename operation; if one were
 * added, it would have to update the copies.
 *
 * <p>The legacy columns {@code attr1} to {@code attr5} are one list here. Prices are stored as
 * decimal numbers, never as binary floating point.
 */
@Document("items")
record ItemDocument(
    @Id String id,
    String productId,
    String productName,
    String categoryId,
    String image,
    String description,
    List<String> attributes,
    @Field(targetType = FieldType.DECIMAL128) BigDecimal listPrice,
    @Field(targetType = FieldType.DECIMAL128) BigDecimal unitCost) {}
