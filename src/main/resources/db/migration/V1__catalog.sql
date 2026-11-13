-- Catalog schema.
--
-- Carried over from the legacy populate/PopulateSQL.xml, table for table and
-- column for column, so that existing data fits unchanged. Text and prices
-- are stored once per locale (en_US, ja_JP, zh_CN) in the *_details tables.
--
-- Differences from the legacy DDL:
--   * varchar instead of fixed-width char for ids and locale. The legacy
--     code had to trim() every id it read because of the padding.
--   * indexes on the foreign keys used to list products and items.
--   * ${binaryCollation} on the name columns. The legacy database compared
--     strings by code point; a database with a language-aware default
--     collation would order names differently and change the page a product
--     appears on. The placeholder pins the legacy ordering where needed.

create table category (
  catid varchar(10) not null,
  constraint pk_category primary key (catid)
);

create table category_details (
  catid  varchar(10)  not null,
  locale varchar(10)  not null,
  name   varchar(80)  ${binaryCollation} not null,
  image  varchar(255),
  descn  varchar(255),
  constraint pk_category_details primary key (catid, locale),
  constraint fk_category_details_1 foreign key (catid) references category (catid)
);

create table product (
  productid varchar(10) not null,
  catid     varchar(10) not null,
  constraint pk_product primary key (productid),
  constraint fk_product_1 foreign key (catid) references category (catid)
);

create index ix_product_catid on product (catid);

create table product_details (
  productid varchar(10)  not null,
  locale    varchar(10)  not null,
  name      varchar(80)  ${binaryCollation} not null,
  image     varchar(255),
  descn     varchar(255),
  constraint pk_product_details primary key (productid, locale),
  constraint fk_product_details_1 foreign key (productid) references product (productid)
);

create table item (
  itemid    varchar(10) not null,
  productid varchar(10) not null,
  constraint pk_item primary key (itemid),
  constraint fk_item_1 foreign key (productid) references product (productid)
);

create index ix_item_productid on item (productid);

create table item_details (
  itemid    varchar(10)   not null,
  locale    varchar(10)   not null,
  listprice decimal(10,2) not null,
  unitcost  decimal(10,2) not null,
  image     varchar(255)  not null,
  descn     varchar(255)  not null,
  attr1     varchar(80),
  attr2     varchar(80),
  attr3     varchar(80),
  attr4     varchar(80),
  attr5     varchar(80),
  constraint pk_item_details primary key (itemid, locale),
  constraint fk_item_details_1 foreign key (itemid) references item (itemid)
);
