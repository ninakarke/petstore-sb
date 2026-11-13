-- Orders.
--
-- The legacy storefront kept no record of an order: it sent the order as a
-- message to the order-processing application and forgot it. Here the order
-- is stored first, together with the state of its hand-off, so that it can
-- never be lost between the two. These tables are therefore new, apart from
-- "counter", which replaces the table the legacy application server
-- generated for the Counter entity bean used to number orders.

create table counter (
  name    varchar(255) not null,
  counter integer      not null,
  constraint pk_counter primary key (name)
);

-- Order ids are the prefix "1001" followed by this counter, as in legacy.
insert into counter (name, counter) values ('1001', 0);

create table purchase_order (
  order_id                varchar(40)   not null,
  user_id                 varchar(25)   not null,
  email                   varchar(255),
  placed_at               timestamp(6) with time zone not null,
  total_price             decimal(12,2) not null,

  ship_given_name         varchar(255),
  ship_family_name        varchar(255),
  ship_street_name1       varchar(255),
  ship_street_name2       varchar(255),
  ship_city               varchar(255),
  ship_state              varchar(255),
  ship_zip_code           varchar(255),
  ship_country            varchar(255),
  ship_email              varchar(255),
  ship_telephone          varchar(255),

  bill_given_name         varchar(255),
  bill_family_name        varchar(255),
  bill_street_name1       varchar(255),
  bill_street_name2       varchar(255),
  bill_city               varchar(255),
  bill_state              varchar(255),
  bill_zip_code           varchar(255),
  bill_country            varchar(255),
  bill_email              varchar(255),
  bill_telephone          varchar(255),

  card_number             varchar(255),
  card_type               varchar(255),
  card_expiry_date        varchar(255),

  -- Hand-off to order processing: written in the same statement as the order.
  handoff_status          varchar(10)   not null,
  handoff_attempts        integer       not null,
  handoff_last_attempt_at timestamp(6) with time zone,

  -- Lets a client of the JSON API repeat a request without ordering twice.
  idempotency_key         varchar(100),

  constraint pk_purchase_order primary key (order_id),
  constraint uq_purchase_order_idempotency unique (user_id, idempotency_key)
);

create index ix_purchase_order_user on purchase_order (user_id, placed_at);
create index ix_purchase_order_handoff on purchase_order (handoff_status, placed_at);

create table order_line (
  order_id    varchar(40)   not null,
  line_number integer       not null,
  category_id varchar(10),
  product_id  varchar(10),
  item_id     varchar(10)   not null,
  quantity    integer       not null,
  unit_price  decimal(10,2) not null,
  constraint pk_order_line primary key (order_id, line_number),
  constraint fk_order_line_order foreign key (order_id) references purchase_order (order_id)
);
