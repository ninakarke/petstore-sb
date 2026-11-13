-- Users who can sign in.
--
-- Replaces the legacy UserEJBTable, which the application server generated
-- for the User entity bean and dropped again on undeploy. The legacy table
-- held the password itself; this one holds a salted hash, so the column is
-- wider than the 25 characters a password may have.

create table signon_user (
  username varchar(25)  not null,
  password varchar(255) not null,
  constraint pk_signon_user primary key (username)
);
