# Java Pet Store storefront, migrated to Spring Boot 4 on Java 21

This is the `petstore` storefront of the Java Pet Store Demo 1.3.1_02, a J2EE 1.3
application (JSP, a home-grown MVC framework, EJB 2.0, JMS, Ant, Cloudscape), migrated to
**Spring Boot 4.1.2 on Java 21**.

It runs from one build on three data stores, chosen when it starts:

| Data store | What it is for |
| --- | --- |
| Embedded H2 | Zero setup: one command, nothing to install but a JDK |
| PostgreSQL | The relational target, with the legacy schema |
| **MongoDB** | The stretch goal: a document model, not the tables copied one to one |

The legacy storefront had 194 Java files (22,054 lines), 67 JSPs and no automated tests. The
migrated application has 704 tests, which run against all three data stores, and a job that copies
existing relational data into MongoDB and checks that the two match.

The migration was done the way it would be done on a codebase a hundred times this size: measured
first, cut into slices, every slice proven against the legacy source, every difference recorded.
That method is described in [How the migration was approached](#how-the-migration-was-approached).

## Contents

- [Quick start](#quick-start)
- [Run modes](#run-modes)
- [Moving existing data to MongoDB](#moving-existing-data-to-mongodb)
- [Build and test](#build-and-test)
- [A tour of the application](#a-tour-of-the-application)
- [What was migrated](#what-was-migrated)
- [How the migration was approached](#how-the-migration-was-approached)
- [How correctness is shown](#how-correctness-is-shown)
- [Before and after: four components](#before-and-after-four-components)
- [MongoDB](#mongodb)
- [Differences from the legacy application](#differences-from-the-legacy-application)
- [Key decisions](#key-decisions)
- [Risks and what was done about them](#risks-and-what-was-done-about-them)
- [Limits, and what a larger migration would add](#limits-and-what-a-larger-migration-would-add)
- [Lessons learned](#lessons-learned)
- [Configuration reference](#configuration-reference)
- [History, tools and attribution](#history-tools-and-attribution)

## Quick start

You need **JDK 21 or newer**. Maven is not needed; the wrapper downloads it.

```bash
git clone <this repository>
cd petstore
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

Then open <http://localhost:8080>. The application creates an in-memory database, loads the
original catalog and demo accounts, and is ready in a few seconds.

| What | Where |
| --- | --- |
| Storefront | <http://localhost:8080> |
| Demo account | user `j2ee`, password `j2ee` (also `shopper` / `j2ee`) |
| API documentation (Swagger UI) | <http://localhost:8080/swagger-ui.html> |
| Health | <http://localhost:8080/actuator/health> |
| Module structure, as verified by the build | <http://localhost:8080/actuator/modulith> |

To build a runnable jar instead:

```bash
./mvnw verify                               # build, all tests, all quality gates
java -jar target/petstore-*.jar             # same as spring-boot:run
```

## Run modes

The data store is chosen by a Spring profile. Nothing else changes: the same pages, the same API,
the same services.

| Mode | Start the database | Start the application | Needs |
| --- | --- | --- | --- |
| Embedded (default) | nothing | `./mvnw spring-boot:run` | JDK |
| PostgreSQL 18 | `docker compose up -d postgres` | `./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres` | JDK, Docker |
| MongoDB 8 | `docker compose up -d mongo` | `./mvnw spring-boot:run -Dspring-boot.run.profiles=mongo` | JDK, Docker |
| MongoDB elsewhere (for example Atlas) | nothing | `PETSTORE_MONGODB_URI='mongodb+srv://…/petstore' ./mvnw spring-boot:run -Dspring-boot.run.profiles=mongo` | JDK |
| Everything in containers, PostgreSQL | | `docker compose --profile app up --build` | Docker only |
| Everything in containers, MongoDB | | `docker compose --profile app-mongo up --build` | Docker only |

With the jar, the profile is given as `--spring.profiles.active=postgres` or `mongo`.

In every mode an empty database is filled with the seed data at start-up, and a database that
already holds data is left alone. PostgreSQL gets its schema from versioned Flyway migrations;
MongoDB needs no schema, and the application creates its indexes.

Stop and remove the containers and their data with `docker compose --profile app --profile app-mongo down -v`.

**Original pictures (optional).** The legacy pet pictures may not be redistributed, so the
repository contains simple placeholder graphics. To see the originals locally, copy the files from
`src/apps/petstore/src/docroot/images/` of the legacy distribution into `local/legacy-images/`.
That folder is ignored by git and is kept out of the container image.

## Moving existing data to MongoDB

A real switch to MongoDB starts with a relational database full of data. The `migrate-to-mongo`
profile turns the application into a one-off job that copies everything, compares the two stores
record by record, prints the result and exits with code 0 (match) or 1 (mismatch).

```bash
docker compose up -d postgres mongo
./mvnw verify
java -jar target/petstore-*.jar --spring.profiles.active=postgres,migrate-to-mongo
```

Output of a real run, after two orders had been placed on PostgreSQL:

```text
Copy to MongoDB finished. Source and target compared:
  kind            in source   copied  already there  in target  different  result
  categories              5        5              0          5          0  match
  products               16       16              0         16          0  match
  items                  28       28              0         28          0  match
  users                   4        4              0          4          0  match
  customers               4        4              0          4          0  match
  orders                  2        2              0          2          0  match
  order counter           2        0              0          2          0  match
Result: the target holds exactly what the source holds.
```

Then start the application with the `mongo` profile. In the rehearsal of this switch-over the
orders taken on PostgreSQL were listed by the application on MongoDB, sign-in worked with the
copied password hashes, a repeated API request with an `Idempotency-Key` from before the switch was
recognised, the next order got the next number, and no order was delivered twice. After a customer
was changed, an item deleted and an order added directly in MongoDB, the job reported all three and
exited with 1.

The job can be run again: records already copied are compared, not copied twice. It is an offline
copy, so run it while the application is not taking orders. How it works is described under
[MongoDB](#mongodb).

## Build and test

```bash
./mvnw verify
```

That one command is the whole quality gate, locally and in CI. It fails if any of these fails:

| Gate | Tool | Fails the build when |
| --- | --- | --- |
| Toolchain | Maven Enforcer | Java is older than 21, or a legacy `javax.*` enterprise API is on the classpath |
| Tests | JUnit 5, Testcontainers | any of the 704 tests fails |
| Module boundaries | Spring Modulith | a module uses another module's internals, or there is a dependency cycle |
| Architecture rules | ArchUnit | for example: database technology used outside a persistence adapter |
| Coverage | JaCoCo | line coverage is below 80 % (it is 98.6 %; branches 90.1 %) |
| Formatting | Spotless (google-java-format) | a file is not formatted (`./mvnw spotless:apply` fixes it) |

**Docker and the tests.** The tests that need a real PostgreSQL or MongoDB server start one
themselves in a container (images `postgres:18` and `mongo:8`). With Docker running, all 704 tests
run. Without Docker those tests are skipped, 531 tests run, coverage is 82.4 % and the build still
passes.

Useful while working:

```bash
./mvnw test -Dtest=ShoppingJourneyMongoTests    # one test class
./mvnw spotless:apply                           # format
docker build -t petstore .                      # the container image on its own
```

Reports after a build: coverage in `target/site/jacoco/index.html`, generated module diagrams in
`target/spring-modulith-docs/`.

CI is defined in [.github/workflows/ci.yml](.github/workflows/ci.yml): `./mvnw verify`, then the
container image build, on every push and pull request. Everything it runs was run locally; the
workflow itself could not be exercised before the repository was first pushed.

## A tour of the application

A sequence that shows every migrated feature, usable as a demo script.

1. **Browse.** Home page, a category (two products per page, as in the original, with "next"), a
   product, an item. Search for `Angel`, then `ANGEL`: the legacy search found nothing for a
   keyword with a capital letter.
2. **Legacy links still work.** <http://localhost:8080/petstore/category.screen?category_id=FISH>
   answers with a permanent redirect to `/categories/FISH`.
3. **Cart.** Add two items, change a quantity, set one to zero (the line is removed). Adding an
   item that is already in the cart sets its quantity to one; that is the legacy rule, kept.
4. **Checkout needs an account.** "Check Out" sends an anonymous visitor to the sign-in page.
   Register a new user there; the account form follows, and after it the visitor is returned to
   the checkout they were on their way to, cart intact.
5. **Order.** Billing and shipping details are prefilled from the account. Submit: the
   confirmation page shows the order number (`10011`, `10012`, … in the legacy format). The log
   shows the stand-in for the order-processing system receiving the purchase order from the JMS
   queue.
6. **Account.** The account page shows the card number masked; "My List" and the pet-tips banner
   follow the profile's favourite category.
7. **API.** In Swagger UI, or with curl:

   ```bash
   B=http://localhost:8080
   curl "$B/api/v1/catalog/search?keywords=parrot"

   TOKEN=$(curl -s -H 'Content-Type: application/json' \
     -d '{"username":"j2ee","password":"j2ee"}' $B/api/v1/auth/token | sed 's/.*"accessToken":"\([^"]*\)".*/\1/')

   curl -H "Authorization: Bearer $TOKEN" $B/api/v1/account

   # Place an order. Send it twice: the second answer is 200 with the same order, not a second order.
   curl -i -H "Authorization: Bearer $TOKEN" -H 'Idempotency-Key: demo-1' \
     -H 'Content-Type: application/json' \
     -d '{"lines":[{"itemId":"EST-1","quantity":2}]}' $B/api/v1/orders

   curl -H "Authorization: Bearer $TOKEN" $B/api/v1/orders
   ```

8. **The same on another data store.** Stop, start with `-Dspring-boot.run.profiles=mongo`, repeat.

## What was migrated

### The legacy application

Java Pet Store is four applications that talk over JMS. Only the storefront is in scope; it hands
each order to the order-processing application as an XML message and keeps no record of it.

```text
                       in scope                         out of scope
 Browser ──HTTP──▶ ┌──────────────────┐   JMS queue   ┌─────┐   ┌──────────┐
                   │ petstore         │──────────────▶│ opc │──▶│ supplier │
                   │ (storefront)     │ purchase-order└─────┘   └──────────┘
                   └────────┬─────────┘     XML          ▲
                            │                         ┌───────┐
                       PetStoreDB                     │ admin │
                       (Cloudscape)                   └───────┘
```

"Just petstore" was defined as what is packaged into `petstore.ear`, read from the legacy build
files: the storefront itself, the web framework (WAF) it is built on, and seven EJB components
(catalog, cart, sign-on, customer, unique-id generator, asynchronous sender, and the purchase-order
value classes).

| Measured in the legacy code in scope | |
| --- | --- |
| Java files / lines | 194 / 22,054 |
| JSP files | 67 (25 English, the rest Japanese and Chinese copies) |
| EJB deployment descriptors | 13 |
| Files using `javax.ejb` | 66, a third of the code |
| Automated tests | 0 |

**Scope of this version: the English storefront.** The legacy application also shipped Japanese
and Chinese copies of its pages. The relational schema is kept as the legacy one, including its
per-language layout (the `locale` column of the catalog detail tables, the preferred language of a
customer), and all page text is in one message file. Further languages are therefore an addition
for a later version (the catalog rows for the language and a message file), not a rework.

These numbers come from a script, [tools/legacy-inventory.sh](tools/legacy-inventory.sh), not from
reading. The headline is the third line from the bottom: a third of the files exist only to satisfy
the EJB 2.0 component contract (home and local interfaces, bean classes, descriptors). They have
nothing to be converted into. They are deleted.

A state-changing request in the legacy application passed through nine hops: two servlet filters, a
front servlet, a request processor that looked the URL up in XML and created an "HTML action" by
reflection, an event object, a web controller with a single application-wide `synchronized` method,
a stateful session bean, a state machine that created an "EJB action" by class name, a screen-flow
manager, and a template servlet that assembled the page from JSP fragments listed in more XML.

### The migrated application

One deployable application, divided into modules that mirror the legacy components. A module may
use only the public API of the modules it is allowed to depend on; the build fails otherwise.

```text
                         ┌──────────────┐
                         │  storefront  │  page layout, home, error pages,
                         │  (web shell) │  pictures, legacy URL redirects
                         └──────┬───────┘
        ┌──────────┬────────────┼────────────┬───────────┐
        ▼          ▼            ▼            ▼           ▼
   ┌─────────┐ ┌──────┐    ┌─────────┐  ┌──────────┐ ┌──────────┐
   │ catalog │◀│ cart │◀───│  order  │─▶│ customer │ │ identity │
   └─────────┘ └──────┘    └────┬────┘  └──────────┘ └──────────┘
                                ▼
                  JMS queue (legacy purchase-order XML)
```

Every module has the same shape:

```text
com.petstore.<module>                     public API: services and the domain model
com.petstore.<module>.internal            the persistence port (an interface) and wiring
com.petstore.<module>.internal.persistence.jpa     relational adapter
com.petstore.<module>.internal.persistence.mongo   MongoDB adapter
com.petstore.<module>.internal.web        server-rendered pages (Thymeleaf)
com.petstore.<module>.internal.api        JSON endpoints
```

| Module | Replaces (legacy) | Contains |
| --- | --- | --- |
| [catalog](src/main/java/com/petstore/catalog) | `catalog` component, SQL-in-XML DAO, `CatalogHelper` in JSPs, populate servlet | categories, products, items, search, paging, seed loader |
| [cart](src/main/java/com/petstore/cart) | `cart` stateful session bean | the session cart and its rules |
| [identity](src/main/java/com/petstore/identity) | `signon` component, `SignOnFilter`, `signon-config.xml` | users, sign-in and sign-out, registration, protected pages, API tokens |
| [customer](src/main/java/com/petstore/customer) | `customer`, `contactinfo`, `address`, `creditcard` entity beans | the customer aggregate, account pages, personalisation |
| [order](src/main/java/com/petstore/order) | `uidgen`, `asyncsender`, `purchaseorder`, order actions | order numbers, checkout, order store, hand-off to order processing |
| [storefront](src/main/java/com/petstore/storefront) | WAF template servlet, screen definitions, custom tags | layout, home page, error pages, pictures, redirects from legacy URLs |
| [shared](src/main/java/com/petstore/shared) | | the few types every module needs |

| Technology | Legacy | Now |
| --- | --- | --- |
| Runtime | Sun J2EE 1.3 reference implementation | Spring Boot 4.1.2, embedded Tomcat, Java 21 |
| Pages | JSP, custom tag libraries, template servlet | Thymeleaf 3.1 layout and fragments |
| Request handling | custom front controller, XML mappings, actions and events | Spring MVC controllers |
| Business components | stateless and stateful session beans | services and a session-scoped cart |
| Persistence | CMP 2.0 entity beans, DAO with SQL in XML | ports with adapters: JPA (Hibernate 7) and Spring Data MongoDB 5 |
| Schema | container-generated tables, DDL run by a servlet | Flyway migrations |
| Security | custom servlet filter, plain-text passwords | Spring Security 7: form login, CSRF, hashed passwords, JWT for the API |
| Messaging | JMS through JNDI | Spring JMS with an embedded ActiveMQ Artemis broker |
| API | none | JSON under `/api/v1`, OpenAPI documentation |
| Build | layered Ant scripts, EAR | Maven wrapper, one executable jar, a container image |
| Tests | none | JUnit 5, AssertJ, Testcontainers, ArchUnit |

The resulting code: 147 source files and about 8,000 lines of application code, and about 9,600
lines of tests in 86 test classes.

## How the migration was approached

The storefront is small enough to rewrite in one sitting. That approach does not survive a larger
codebase, so it was not used. What was used instead is meant to be the method for the large case,
applied here end to end.

### Principles

| # | Principle | Why it matters when the codebase is far larger |
| --- | --- | --- |
| 1 | **Measure before moving.** Inventory the legacy code with a script. | Nobody can read two million lines. Scope and progress have to come from numbers that can be regenerated. |
| 2 | **Change one thing at a time.** Platform first, on a relational schema shaped like the original. Data store second. | When something breaks there is one suspect. |
| 3 | **Migrate in vertical slices, in dependency order.** Each slice goes from page to database and ends in a working, tagged application. | Slices can be given to different teams, and one that goes wrong is small. |
| 4 | **Build the safety net with the slice.** The legacy code has no tests, so each slice starts by pinning legacy behaviour in tests that cite the legacy source. | Tests are the only definition of "same behaviour" that scales. |
| 5 | **Preserve by default, deviate on record.** Every intentional difference is written down with its reason and its test. | "While we were in there" changes are how migrations lose their baseline. |
| 6 | **Make boundaries enforceable.** The build fails if a module reaches into another's internals. | Conventions erode with every new contributor; a failing build does not. |
| 7 | **Put persistence behind ports.** One contract test suite per port, run against every data store. | Changing the database becomes adding an adapter and passing an existing suite. |
| 8 | **Preserve contracts with systems you do not own.** The purchase-order message keeps its legacy format and is validated against the legacy DTD. | The neighbours of a migrated system are usually not migrated with it. |
| 9 | **Automate the gates.** Formatting, tests, coverage and boundary checks run on every build. | Review capacity does not scale; machines do. |
| 10 | **Write decisions down,** with the alternatives. | The people who made a decision are rarely there when it is questioned. |

### Breaking the work up

The dependency direction between the legacy components gives the order: a component is migrated
after everything it depends on.

```text
catalog ◀── cart ◀──┐
                    ├── order ──▶ (JMS) order processing
identity   customer ┘
```

| Stage | Slice | Why here | Git tag |
| --- | --- | --- | --- |
| 0 | Assess: inventory, scope, findings, plan | nothing can be planned before it is measured | |
| 1 | Scaffold: build, gates, module rules, CI, page layout | every later slice needs them | `stage-1-scaffold` |
| 2 | Catalog | no dependencies, largest read surface | `stage-2-catalog` |
| 3 | Cart | depends only on the catalog | `stage-3-cart` |
| 4 | Identity | needed by customer and order | `stage-4-identity` |
| 5 | Customer | the account behind every order | `stage-5-customer` |
| 6 | Order and hand-off | depends on everything above | `stage-6-order` |
| 7 | Other languages (Japanese, Chinese) | not part of this version, which is English only; a later addition | |
| 8 | Relational parity: whole journeys on H2 and PostgreSQL, container image | proves the platform change before the data store changes | `stage-8-relational-parity` |
| 9 | MongoDB: adapters, whole journeys, data copy | the second change, made alone | `stage-9-mongodb` |
| 10 | Finish: this document | | `v1.0.0` |

`git checkout stage-3-cart` gives a working application with the catalog and the cart and nothing
else. Each tag is a point at which the work could have been reviewed, released or handed over.

### The slice recipe

Every slice was done in the same eight steps. On a larger codebase this recipe is what each team is
handed.

1. **Read** the legacy component and list its rules, with file and line.
2. **Decide** keep, fix or drop for each rule, by a stated policy; record every fix.
3. **Model** the domain and define the persistence port.
4. **Pin** the rules with tests that name the legacy source they come from.
5. **Implement** the adapter and run the port's contract suite against it.
6. **Build** the pages and the JSON endpoints on the service.
7. **Verify** by starting the application and using the slice over real HTTP.
8. **Update** the feature list, then tag.

A slice was done when every legacy rule in it was either covered by a test or recorded as a
deliberate difference, its legacy URLs redirected, and the build was green.

### Scaffolding and infrastructure, built before the first slice

- A **scripted inventory** of the legacy code ([tools/legacy-inventory.sh](tools/legacy-inventory.sh)).
- A **translation table** from J2EE constructs to their replacements (stateless session bean to
  service, CMP entity bean to aggregate behind a port, JNDI lookup to constructor injection, and so
  on), so that each slice applies rules instead of redesigning. The rows that repeat most are the
  candidates for automated refactoring on a larger codebase.
- The **build with its gates** (see [Build and test](#build-and-test)), so that the first line of
  migrated code was already subject to them.
- **Module boundary verification** and **architecture rules** written before the code they
  constrain. The rule that keeps MongoDB classes out of everything but the MongoDB adapters existed
  in stage 1, eight stages before the first such class.
- A **feature list** (61 entries, each mapped to its legacy files, its new home and the tests that
  prove it; 55 are done and 6 were dropped, with the reason) and a **register of differences**,
  both kept up to date at every stage.
- **Run modes and containers**: an embedded database for zero setup, real databases through Docker
  Compose and Testcontainers, a container image built from source.
- **CI** running the same single command a developer runs.

## How correctness is shown

The legacy application has no tests and cannot practically be run today (it needs Sun's J2EE 1.3
reference implementation and a Cloudscape database). "It behaves the same" therefore has to be
established from the legacy source itself. These are the mechanisms, from the most direct to the
most general.

| Mechanism | What it shows | Where |
| --- | --- | --- |
| **Legacy SQL run verbatim.** The original `CatalogDAOSQL.xml` is executed, unchanged, against the same seeded database, and its rows are compared with what the new code returns. | Catalog reads return what the legacy queries return. Also documents where the legacy result was wrong (a keyword with a capital letter found nothing). | [CatalogLegacySqlDifferentialTests](src/test/java/com/petstore/catalog/internal/CatalogLegacySqlDifferentialTests.java), on H2 and PostgreSQL |
| **Legacy DTD validation.** The purchase-order message is validated against the original DTDs. | The neighbouring system can still read what the storefront sends. | [PurchaseOrderXmlTests](src/test/java/com/petstore/order/internal/handoff/PurchaseOrderXmlTests.java) |
| **Characterization tests.** Rules read from the legacy source, each test named `legacy: …` for behaviour that is kept and `fixed: …` for a defect that is not. | Business rules are the legacy ones, and every difference is deliberate. | in every module, for example [CartServiceTests](src/test/java/com/petstore/cart/CartServiceTests.java) |
| **Contract suites.** One abstract test suite per persistence port, run unchanged against four implementations: in-memory, H2, PostgreSQL, MongoDB. | Every data store behaves the same behind the port, including under concurrency (order numbers) and on bad data (duplicates, missing parents). | [CatalogStoreContract](src/test/java/com/petstore/catalog/internal/CatalogStoreContract.java) and four more |
| **Cross-store equivalence.** Every listing, page and search over the complete seed catalog, compared answer by answer with a reference implementation. | H2, PostgreSQL and MongoDB return the same pages in the same order. | [CatalogEquivalenceTests](src/test/java/com/petstore/catalog/internal/CatalogEquivalenceTests.java) |
| **Whole journeys.** A shopper from the first page to the order confirmation, a returning customer, an API client, a legacy link; the same test class run on each store. | The parts fit together, on every data store. | [ShoppingJourneyTests](src/test/java/com/petstore/journey/ShoppingJourneyTests.java) |
| **Run-mode tests.** | Each run mode starts exactly one database stack and five adapters of one kind. | [runmode](src/test/java/com/petstore/runmode) |
| **Copy and reconcile.** The data copy reads every record back from the target and compares it with the source. | Moving to MongoDB loses and changes nothing. | [CopyToMongoTests](src/test/java/com/petstore/migration/CopyToMongoTests.java), from H2 and from PostgreSQL |
| **Structure tests.** Module boundaries and layering rules. | The design stays what it is claimed to be. | [architecture](src/test/java/com/petstore/architecture) |
| **Use over real HTTP** at the end of every stage: the packaged application, a browser-like client, the database inspected afterwards. | It works outside the test harness. | recorded per stage; summarised in this document |

The chain of evidence for MongoDB is: legacy SQL equals the relational adapter (differential
tests); the relational adapter and the MongoDB adapter both pass the same contract suite and both
equal the same reference over the full catalog; the same journeys pass on both.

**What is not proven.** There is no recording of the running legacy application to compare pages
with, so page layout and wording were ported by reading the JSPs, not checked against a golden
master. On a larger migration it would be worth containerising the old runtime once to capture one.

## Before and after: four components

### Catalog: SQL in XML, read from the JSPs

*Before.* JSPs called `CatalogHelper`, which called `GenericCatalogDAO` directly from the web tier.
The DAO downloaded its own SQL from `http://localhost:8000/petstore/CatalogDAOSQL.xml`, picked the
block for the database vendor, assembled the search statement from fragments, opened a scrollable
cursor over the whole result and skipped to the requested row. Item lists had no `ORDER BY`, so
what a page contained was undefined.

*After.* [CatalogStore](src/main/java/com/petstore/catalog/internal/CatalogStore.java) is an
interface whose methods correspond one to one to the legacy statements.
[JpaCatalogStore](src/main/java/com/petstore/catalog/internal/persistence/jpa/JpaCatalogStore.java)
holds the same queries as JPQL, with an order and a real page limit;
[MongoCatalogStore](src/main/java/com/petstore/catalog/internal/persistence/mongo/MongoCatalogStore.java)
answers the same questions from documents.
[CatalogService](src/main/java/com/petstore/catalog/CatalogService.java) keeps the legacy paging
(two per page, "is there a next page") and is the only thing pages and endpoints talk to; a rule in
the build forbids them to reach a store.

### Cart: a stateful session bean

*Before.* `ShoppingCartLocalEJB`, a stateful session bean holding a map of item id to quantity,
reached through a facade bean, a component manager stored in the HTTP session, and the nine-hop
request path; totals in `double`; changes made by GET links.

*After.* [SessionCart](src/main/java/com/petstore/cart/internal/SessionCart.java), a small
serialisable session-scoped object, and
[CartService](src/main/java/com/petstore/cart/CartService.java). The legacy rules are kept and
pinned by tests: adding sets the quantity to one, a quantity of zero or less removes the line, the
price is the list price. Money is `BigDecimal`. Changes are POSTs with a CSRF token.

### Sign-on: a custom filter and plain-text passwords

*Before.* `SignOnFilter` read the protected URLs from `signon-config.xml`, compared the password
with the one stored in clear text by the `User` entity bean, and kept the originally requested URL
in the session. `order.do`, the URL that actually places the order, was not in the protected list.

*After.* A Spring Security filter chain
([SecurityConfiguration](src/main/java/com/petstore/identity/internal/SecurityConfiguration.java)):
protected paths still come from configuration, the form fields keep their legacy names
(`j_username`, `j_password`), the visitor is still returned to the page they asked for. Passwords
are stored as salted hashes; the seed passwords are hashed when they are loaded. The order endpoint
is protected. The API has its own stateless chain with short-lived JWTs.

### Order: built in the request, sent once, never stored

*Before.* `OrderEJBAction` asked the unique-id bean for a number (read, add one, write back, with
nothing to stop two requests getting the same number), built a `PurchaseOrder`, serialised it to
XML, sent it to a JMS queue and emptied the cart. Nothing was stored. If the sender could not be
found, the failure was printed and ignored: the order was lost and the cart emptied anyway. Every
order carried the same hard-coded credit card.

*After.* [OrderService](src/main/java/com/petstore/order/OrderService.java) stores the order first,
together with the fact that it still has to be handed off, in one atomic write.
[OrderHandoffRelay](src/main/java/com/petstore/order/internal/handoff/OrderHandoffRelay.java) then
delivers it at once and retries later if that fails, so an order is delivered at least once and
never lost. The message
([PurchaseOrderXml](src/main/java/com/petstore/order/internal/handoff/PurchaseOrderXml.java)) is the
legacy "PurchaseOrder 1.1" document, sent to a queue with the legacy name on an embedded broker; a
small listener stands in for the out-of-scope order-processing application. Order numbers keep the
legacy format and are safe under concurrency. The order carries the customer's own card.

## MongoDB

### The document model

The relational schema is the legacy one: six catalog tables, six customer tables, an order table
with its lines. Copying those tables into collections would work and would use MongoDB as a
relational database. The model below was chosen instead; the alternatives considered are listed
under [Key decisions](#key-decisions).

| Data | Relational | MongoDB | Reason |
| --- | --- | --- | --- |
| Catalog | 6 tables (`category`, `product`, `item`, and a details table for each) | 3 collections: `categories`, `products`, `items`. An item carries a copy of its product's name and category. | An item is always shown with its product's name and searched by both, so every read is one document and there are no joins. |
| Customer | 6 tables sharing the customer's key | 1 document in `customers`, with account, contact, address, card and profile inside | A customer is always read and saved whole and shares nothing with other customers. One read, one atomic write. |
| Sign-in user | 1 table | 1 document in `users` | Kept apart from the customer: credentials belong to another module, and no page that shows a customer loads a password hash. |
| Order | 2 tables | 1 document in `orders`, with lines, both addresses, the card and the hand-off state inside | Storing an order and marking it "to be sent" is one insert. No transaction is needed. |
| Order counter | 1 row, locked while incremented | 1 document in `counters`, incremented with `$inc` | "Add one and tell me the result" is atomic on the server. |

An item and an order as stored (from a real run):

```js
// items
{ _id: "EST-6", productId: "K9-BD-01", productName: "Bulldog", categoryId: "DOGS",
  image: "dog2.gif", description: "Friendly dog from England", attributes: ["Male Adult"],
  listPrice: NumberDecimal("18.50"), unitCost: NumberDecimal("12.00") }

// orders
{ _id: "10012", userId: "j2ee", email: "…", placedAt: ISODate("2026-11-13T15:50:07.894Z"),
  shipTo: { … }, billTo: { … }, creditCard: { … },
  lines: [ { lineNumber: 0, itemId: "EST-1", productId: "FI-SW-01", categoryId: "FISH",
             quantity: 2, unitPrice: NumberDecimal("16.50") } ],
  totalPrice: NumberDecimal("33.00"),
  handoff: { status: "SENT", attempts: 1, lastAttemptAt: ISODate("2026-11-13T15:50:07.895Z") },
  idempotencyKey: "mongo-check-1" }
```

Details that matter:

- **Money is `Decimal128`**, never a string or a binary float.
- **No `_class` field.** Documents do not name the Java classes they were written from, so
  renaming a class is not a data migration.
- **Document classes are separate from domain classes.** The records in each
  `persistence.mongo` package are the schema of the collections; renaming something in the domain
  cannot silently rename a stored field.
- **No multi-document transactions anywhere.** Every port call is one operation on one document.
  That is why the services were written without `@Transactional` from the first slice on, and why a
  single standalone server, a replica set and Atlas all work.
- **Indexes are created by the adapters** and are part of the tests: listings are read in index
  order; the idempotency key is a unique partial index per user; the retry job reads from a partial
  index that contains only orders not yet delivered, and a test asserts from the query plan that it
  is used.
- **What the relational schema got from the database, the adapter does itself**: a product must
  name an existing category and an item an existing product. The contract suite checks this on
  every store.
- **Search gives the legacy results**: a case-insensitive "contains" on product name, category id
  and item description. That cannot use an index, so it scans the items; see
  [Limits](#limits-and-what-a-larger-migration-would-add).

### One build, two database stacks

Both stacks are in the application. A profile decides which one is switched on, and the other one's
auto-configuration is excluded, so it creates no client, no connection pool and no health check.
Each adapter is marked `@RelationalStore` or `@DocumentStore`, and tests check that each run mode
has exactly five adapters, all of one kind.

### How the data copy works

Every port has one "read everything in key order, starting at this key" method per kind of record.
On top of that, [StoreCopy](src/main/java/com/petstore/shared/StoreCopy.java) is fewer than a
hundred lines of code that know nothing about any store or record: read the source a batch at a time, continuing
after the last key seen (never by offset, so the work per batch does not grow with the size of the
store); write what the target does not have; read every record back from the target and compare it
with the source; finally count the target, which shows records it has that the source does not.
Orders keep their hand-off state, so a delivered order is not delivered again, and the order counter
is carried over, so numbering continues.

Because it works through the ports, the copied documents are by construction the documents the
application itself writes, and the same code would copy in the other direction.

## Differences from the legacy application

The policy, decided before any code was written: **security defects are always fixed; functional
defects that are clearly bugs are fixed; behaviour that looks intentional is kept, even if it is
unusual; everything else stays as it was.** Improvements are a different project. Each difference
has a test, named `fixed: …`, or `legacy: …` where the old behaviour was kept on purpose.

### Security defects, fixed

| Legacy | Now |
| --- | --- |
| Passwords stored and compared in plain text | Salted hashes; seed passwords hashed when loaded |
| `/Populate`, reachable by anyone, dropped and reloaded the database | No such URL. Seed data is loaded once at start-up, only into an empty store |
| Java objects posted by the browser were deserialised (client-state tag) | The feature that used it is not carried over; nothing sent by a browser is deserialised |
| The URL that places an order was not protected | Protected, and tested |
| Cart and sign-out changed state by GET; no CSRF protection | POST with a CSRF token everywhere |
| Full card number shown on the account page and sent back in the edit form | Masked; the edit form starts empty and keeps the stored number if left empty |
| Sign-in form prefilled with the demo user and password | Empty |
| Redirect target taken from a request parameter | No endpoint does that |
| Database and JMS credentials in a deployment descriptor | Environment variables |
| The error page printed the exception and stack trace | A message; details go to the log |
| No protective attributes on the session cookie | `HttpOnly`, `SameSite=Lax` |

### Bugs, fixed

| Legacy | Now |
| --- | --- |
| Search found nothing if the keyword contained a capital letter | Case is ignored |
| `%` and `_` in a search keyword acted as wildcards | Taken literally |
| Item lists and search results had no defined order, so pages were arbitrary | Ordered by item id |
| The password confirmation was read and never compared | Compared |
| Every order carried the same hard-coded credit card | The customer's own card |
| If the message sender could not be found, the order was lost and the cart emptied anyway | The order is stored first, then delivered, with retries |
| Two requests could be given the same order number | Atomic on every store, tested with concurrent requests |
| Money computed in `double` and `float` (`68.0`) | `BigDecimal` (`68.00`) |
| Card and profile checks in the account form tested the wrong variable and were never used | Enforced |
| The account form offered card expiry years 2001 to 2005 | The current year and the next ten |
| The account edit form silently changed a stored value that was not among its choices | The stored value is kept |

### Legacy behaviour kept on purpose

| Behaviour | Why it might look like a bug |
| --- | --- |
| Adding an item to the cart sets its quantity to one | It does not increment |
| A quantity of zero or less, or a non-number, removes the line | |
| Catalog and search pages show two results | Small, but it is the legacy page size (configurable) |
| The cart uses the list price | |
| Order numbers are `1001` followed by a counter: `10011`, `10012`, …, `100110` | Plain concatenation, not a number |
| User name and password are limited to 25 characters; `%` and `*` are not allowed in a user name | |
| The purchase-order message: element names and order, date format, empty elements for missing values | Kept exactly; it is another system's input |
| The queue is still called `jms/opc/OrderQueue` | |
| Sessions, and with them carts, end after 15 idle minutes | |
| The pet-tips banner appears on the home and cart pages only | |
| After creating an account the visitor returns to the page they first asked for | |

### Changed by design

| Legacy | Now | Why |
| --- | --- | --- |
| `category.screen?category_id=FISH`, `cart.do?action=…` | `/categories/FISH`, `POST /cart/items`; every legacy URL redirects | Resource-style URLs without breaking old links |
| The order was sent and never stored | Stored, then handed off | An order must not exist only in a message |
| No programmatic interface | JSON API under `/api/v1` with OpenAPI documentation; orders can be placed with an `Idempotency-Key` | The same services, a second way in |
| One database product, fixed by descriptors | Relational or MongoDB from one build | The stretch goal |
| Three languages, as three copies of every JSP | English in this version. One template per page, all text in one message file; the relational schema keeps the legacy per-language layout | Another language is then a message file and its catalog rows, not another copy of every page |
| Deployed as an EAR into an application server | An executable jar and a container image that runs as a non-root user | |

Not in this version: the Japanese and Chinese pages and the language switch (future work, see
the scope above). Not migrated: the cart as a JSON endpoint
(the API is stateless and the cart lives in the browser session); the order-processing, supplier
and admin applications; and legacy code that was already dead (an unused DAO, a servlet with no
mapping, a screen whose JSP does not exist).

## Key decisions

Each was taken from options with their trade-offs; the alternative and the reason are given.

| Decision | Instead of | Reason |
| --- | --- | --- |
| **Spring Boot 4.1.2** (the latest stable release when the work was done; 4.2 was a release candidate), Java 21, Maven | Quarkus | The legacy concepts map most directly: session beans to services, JMS, declarative security, and one data-access family (Spring Data) that covers both JPA and MongoDB. Spring Modulith gives enforceable modules. |
| **Modular monolith**, boundaries verified by the build | Microservices; or one unstructured application | Changing the platform and the architecture style at once would be two changes with one baseline. The modules are where service boundaries would later go. |
| **Server-rendered pages (Thymeleaf) plus a JSON API** on the same services | A single-page application | The legacy UI is server-rendered. Keeping it so is the smallest change in behaviour and keeps page-level comparison possible; the API is added, not substituted. |
| **Persistence behind ports; relational first, MongoDB second** | Going straight to MongoDB | On the legacy-shaped schema the legacy SQL can be run for comparison. Only once the platform change is proven does the data store change. |
| **Store the order, hand it off through an outbox kept inside the order, over a real JMS queue** | Sending from the request as legacy did; or only logging | Nothing is lost if the broker is down, and the contract with the neighbouring system is exercised for real. Keeping the hand-off state inside the order makes the write atomic without a transaction. |
| **Fix security defects and clear bugs, keep the rest, record every difference** | Faithful copy including defects; or free improvement | A migration needs a baseline. Carrying known security holes forward is not defensible; redesigning features is another project. |
| **Original pictures are not redistributed** | Committing them | Their licence does not allow it. Placeholders are committed; originals are read from an ignored local folder. |
| **English in this version, with the legacy per-language schema kept** | Porting the Japanese and Chinese page copies as well | Languages are additive: with one template per page and the schema unchanged, adding one later needs no data migration and no page rework. They add volume to a migration, not risk. |
| **Zero-setup default, real databases through profiles and Docker** | Requiring a database to start | A reviewer can run it with one command; the real databases are one more command. |
| **JWT for the API, session for the pages, no cart in the API** | Session cookies for the API | A stateless API needs no CSRF handling and no sticky sessions. The cart is session state, so it stays with the pages. |
| **The orders API can place orders, with an `Idempotency-Key`** | Read-only API | A retried request must not create a second order; the key is a unique index per user. |
| **MongoDB: three flat catalog collections** | Mirroring the six tables (joins on every page); one nested document per category (paging and search inside nested arrays, unbounded growth) | One read per page, no joins, bounded documents. |
| **MongoDB: one customer document, users separate** | User inside the customer document (two modules writing one collection); separate collections with references (needs multi-document transactions) | One atomic write per customer, module boundaries kept. |
| **MongoDB: one order document with the hand-off inside** | A separate outbox collection (needs a transaction, so a replica set) | One insert; works on any deployment. |
| **MongoDB: search matches as the legacy search did** | A text index (matches whole words: `angel` would no longer find `Angelfish`) | Same results on every store, proven by the same tests. The cost is a collection scan. |
| **Both stacks in one build, chosen by profile; data moved by a job that works through the ports** | Two builds; or a dump-and-import with a database tool | One artefact to test and release. A tool-based copy would bypass the adapters, so the copied documents would not be guaranteed to be what the application writes. |

## Risks and what was done about them

| Risk | What was done | Outcome |
| --- | --- | --- |
| Behaviour changes unnoticed because the legacy code has no tests | Tests written from the legacy source with each slice; a feature list tracking what is covered | 55 features done, each with named tests; 6 dropped with reasons |
| The legacy application cannot be run for comparison | Legacy SQL run verbatim; legacy DTDs used for validation; rules extracted with file and line | In place; no golden master (stated above) |
| A rule lives in configuration, not code | `mappings.xml`, the screen definitions, `signon-config.xml` and the descriptors were read as part of the assessment | This is how the unprotected order URL was found |
| Fixing defects blurs the baseline | A written policy; every fix recorded with its test | Every difference is listed in this document |
| Module boundaries erode as slices are added | Verified on every build | Enforced by every build since stage 1 |
| The hand-off to the out-of-scope system breaks | Format preserved and validated; order stored with its hand-off state; retry tested | In place |
| Moving to MongoDB changes results or loses data | Ports, one contract suite on every store, equivalence over the full catalog, copy with reconciliation | The MongoDB adapters passed the existing suites; the copy job reports match or mismatch |
| The embedded database behaves differently from a real one | The same tests run on PostgreSQL in a container | It did: see the first lesson below |
| The newest framework version differs from what documentation describes | Class, starter and property names were checked against the actual 4.1.2 artefacts at each step | Several had moved in Spring Boot 4 |
| Licensed assets published by accident | The picture folder is git-ignored and excluded from the container build context | No picture from the legacy distribution is in the repository or the image |
| Session state prevents running more than one instance | The cart is a small serialisable session object | Needs a shared session store to scale out; see limits |

## Limits, and what a larger migration would add

Stated plainly, so that nobody has to find them.

- **No golden master.** Parity is shown from the legacy source, its SQL and its DTDs, not from
  recordings of the running legacy application.
- **Sessions are in memory.** Carts are lost on restart, and more than one instance needs sticky
  sessions or a shared session store.
- **The message broker is embedded,** one per instance and not persistent. An order whose hand-off
  is not confirmed stays pending in the database and is retried, so nothing is lost, but a real
  deployment would point at an external broker (a configuration change).
- **Delivery is at least once.** A crash between sending and recording can send an order twice; the
  receiver has to tolerate that, as with any outbox.
- **Search on MongoDB scans the items collection.** That is the price of legacy-identical results.
  A large catalog needs a search index (Atlas Search), which matches by word and would be a recorded
  change of behaviour.
- **Card numbers are stored as the legacy application stored them,** in clear text, on both stores.
  They are masked on output. A real system would tokenise them or encrypt the field.
- **The data copy is offline.** A switch without downtime needs dual writes or change data capture;
  the comparison step of the job would remain as the reconciliation.
- **Indexes are created at start-up.** At scale they would be rolled out as migration steps.
- **The API token secret is random per start** unless `PETSTORE_TOKEN_SECRET` is set; several
  instances need the same one.
- **Not tried:** a real Atlas cluster (the connection string is configuration and the driver is the
  same, but it was not run), and the CI workflow before the first push.

What would be added for a codebase a hundred times larger:

- **Automated refactoring recipes** (OpenRewrite) for the mechanical part: `javax` to `jakarta`,
  EJB annotations, JNDI lookups, the repeating rows of the translation table.
- **A golden master** captured from the legacy system running in a container, and **parallel run**
  in production with comparison of results.
- **Strangler routing**: a proxy sends each URL to the old or the new application, so slices go
  live one at a time. The legacy-URL redirects in this project are the small version of that.
- **Contract tests agreed with the neighbouring systems' teams,** in place of validating against a
  DTD alone.
- **Feature flags per slice,** and a dashboard fed from the feature list.
- **Change data capture** for the bulk of the data move, with reconciliation as here.

## Lessons learned

1. **An embedded database is not a stand-in until a real one has run the same tests.** Catalog
   lists are ordered by name and paged. PostgreSQL sorts text by language rules, H2 by code point,
   so the same data can land on different pages. The contract test for this passes on H2 and fails
   on PostgreSQL unless the comparison is pinned in the schema, which it now is. Found any later,
   this would have been "some products moved to another page".
2. **A third of the legacy code needed deleting, not converting.** Counting files per API before
   starting showed that the EJB component contract was the largest single consumer of code. Effort
   estimates from line counts would have been wrong by that third.
3. **Configuration is code.** The most serious finding, an order URL that needed no sign-in, was
   not in any Java file. It was a missing line in an XML list.
4. **Reading found defects that no test would have looked for:** the hard-coded credit card on
   every order, the password confirmation that was never compared, validation that tested the wrong
   variable. A migration is the one time someone reads all of it.
5. **Decide the second data store's constraints while building the first.** The services never used
   a transaction that spans calls, and each aggregate was written in one call, from the first slice.
   Because of that the MongoDB adapters passed the existing contract suites on their first run and
   the journey tests found nothing store-specific. Retrofitting that would have meant touching
   every service.
6. **A contract suite improves the adapters that already pass it.** Writing the MongoDB adapter
   raised questions the relational one had answered by accident (what if a product names a missing
   category; how fine is a timestamp). Each became a contract test, and all stores are now held to
   it.
7. **Tests must run under real conditions somewhere.** Request encoding, the session cookie and the
   error pages are decided by the server, not by application code, and are tested on a real port.
   A whole journey failed on one store and not another because the test's generated user name broke
   the legacy 25-character rule: the legacy rule was working, and the journey test is what met it.
8. **Check the new platform against its artefacts, not from memory or older guides.** Spring Boot 4
   split its auto-configuration into one module per technology, renamed starters and moved test
   annotations to new packages. Every such name used here was looked up in the jars actually on the
   classpath before it was written down. On a large migration that check belongs in the scaffold
   stage, once, so that every slice starts from names that are known to be right.
9. **Keep "same" and "better" apart.** The register of differences was the most useful document in
   the project: every time something looked wrong in the legacy code, the question "keep, fix or
   drop?" had to be answered in writing, which stopped improvements from leaking in.

## Configuration reference

| Setting | Environment variable or property | Default |
| --- | --- | --- |
| Data store | profile: none, `postgres`, `mongo` | embedded H2 |
| PostgreSQL connection | `PETSTORE_DB_URL`, `PETSTORE_DB_USER`, `PETSTORE_DB_PASSWORD` | the Compose service on `localhost:5432` |
| MongoDB connection | `PETSTORE_MONGODB_URI` | `mongodb://localhost:27017/petstore` |
| API token signing key | `PETSTORE_TOKEN_SECRET` (Base64, at least 32 bytes) | random at each start |
| API token lifetime | `petstore.security.token.lifetime` | 15 minutes |
| Pages that need sign-in | `petstore.security.protected-paths` | `/account/**`, `/checkout/**`, `/orders/**` |
| Results per page | `petstore.catalog.page-size` | 2 (legacy) |
| Order queue | `petstore.order.handoff.queue` | `jms/opc/OrderQueue` (legacy) |
| Hand-off without a broker | `petstore.order.handoff.transport=log` | `jms` |
| Stand-in for order processing | `petstore.order.handoff.stub-consumer` | on |
| Hand-off retry interval | `petstore.order.handoff.retry-interval` | 30 seconds |
| External broker | `spring.artemis.mode=native` and its address | embedded |
| Seed data | `petstore.seed.enabled` | on (only ever loads into an empty store) |
| Local pictures | `petstore.images.local-directory` | `local/legacy-images` |
| Data copy batch size | `petstore.migration.batch-size` | 500 |
| Port | `server.port` | 8080 |

All defaults are in [application.yml](src/main/resources/application.yml); the profiles are
[application-postgres.yml](src/main/resources/application-postgres.yml),
[application-mongo.yml](src/main/resources/application-mongo.yml) and
[application-migrate-to-mongo.yml](src/main/resources/application-migrate-to-mongo.yml).

## History, tools and attribution

**History.** One branch, small commits, each made when a step was finished and the build was green,
and one tag per stage (see [Breaking the work up](#breaking-the-work-up)). `git log --oneline`
reads as the order in which the work was done.

**How it was built.** The challenge explicitly allows AI assistance, and it was used throughout;
the commit trailers say so. The decisions listed above were taken by the author from options with
their trade-offs, and nothing in this document asks to be taken on trust: every claim of "same
behaviour" points at a test that can be run.

**Legacy inventory.** To regenerate the numbers quoted here:
`tools/legacy-inventory.sh <path to the petstore1.3.1_02 distribution>`.

**Attribution.** Java Pet Store is a sample application originally published by Sun Microsystems.
The application code here is new. The seed data and the purchase-order DTDs are derived from the
original distribution and keep its notice; see [NOTICE](NOTICE). The original pet pictures are not
redistributed. This project is not affiliated with or endorsed by Sun Microsystems or Oracle.
