-- Customers.
--
-- Replaces six tables that the legacy application server generated for the
-- entity beans Customer, Account, ContactInfo, Address, CreditCard and
-- Profile, and dropped again on undeploy. The six-table shape is kept, so
-- the data model can be compared one to one with the legacy one.
--
-- Every relationship between those beans was one-to-one, owned by the
-- customer and deleted with it. The generated tables used surrogate keys
-- and "reverse" foreign-key columns for that. Here each table simply shares
-- the customer's key, which states the same thing directly.
--
-- There is deliberately no foreign key to signon_user: as in the legacy
-- application, a customer and a user are related only by having the same id.

create table customer (
  userid varchar(25) not null,
  constraint pk_customer primary key (userid)
);

create table account (
  userid varchar(25) not null,
  status varchar(20) not null,
  constraint pk_account primary key (userid),
  constraint fk_account_customer foreign key (userid) references customer (userid)
);

create table contact_info (
  userid      varchar(25) not null,
  given_name  varchar(255),
  family_name varchar(255),
  telephone   varchar(255),
  email       varchar(255),
  constraint pk_contact_info primary key (userid),
  constraint fk_contact_info_account foreign key (userid) references account (userid)
);

create table address (
  userid       varchar(25) not null,
  street_name1 varchar(255),
  street_name2 varchar(255),
  city         varchar(255),
  state        varchar(255),
  zip_code     varchar(255),
  country      varchar(255),
  constraint pk_address primary key (userid),
  constraint fk_address_contact_info foreign key (userid) references contact_info (userid)
);

create table credit_card (
  userid      varchar(25) not null,
  card_number varchar(255),
  card_type   varchar(255),
  expiry_date varchar(255),
  constraint pk_credit_card primary key (userid),
  constraint fk_credit_card_account foreign key (userid) references account (userid)
);

create table profile (
  userid             varchar(25) not null,
  preferred_language varchar(255),
  favorite_category  varchar(255),
  my_list_preference boolean not null,
  banner_preference  boolean not null,
  constraint pk_profile primary key (userid),
  constraint fk_profile_customer foreign key (userid) references customer (userid)
);
