package com.petstore.order.internal.api;

import com.petstore.customer.ContactInfo;
import com.petstore.customer.CustomerField;
import com.petstore.order.Handoff;
import com.petstore.order.InvalidOrderException;
import com.petstore.order.NoAccountException;
import com.petstore.order.Order;
import com.petstore.order.OrderLine;
import com.petstore.order.OrderProblems;
import com.petstore.order.OrderService;
import com.petstore.order.Placement;
import com.petstore.order.RequestedLine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.net.URI;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * The caller's own orders as JSON: placing one, listing them, reading one.
 *
 * <p>The legacy application had no such interface, and placing an order without the website is new.
 * It goes through the same {@link OrderService} as the checkout page, so an order placed here is
 * numbered, stored and handed off to order processing in exactly the same way.
 *
 * <p>What the cart guarantees on the website is checked by the service for these requests: items
 * must exist, quantities must be sensible, and prices come from the catalog, never from the
 * request. A client that is not sure whether a request arrived can repeat it with the same {@code
 * Idempotency-Key} header and will get the original order back instead of a second one.
 */
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "The caller's own orders. Requires a bearer token.")
@SecurityRequirement(name = "bearer-jwt")
class OrderApiController {

  static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
  private static final int MAX_KEY_LENGTH = 100;

  private final OrderService orders;

  OrderApiController(OrderService orders) {
    this.orders = orders;
  }

  @GetMapping
  @Operation(summary = "List the caller's orders, newest first")
  List<OrderResponse> list(Principal caller) {
    return orders.ordersOf(caller.getName()).stream().map(OrderResponse::of).toList();
  }

  @GetMapping("/{orderId}")
  @Operation(summary = "Get one of the caller's orders, with the state of its hand-off")
  OrderResponse get(@PathVariable String orderId, Principal caller) {
    return orders
        .find(caller.getName(), orderId)
        .map(OrderResponse::of)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @PostMapping
  @Operation(
      summary = "Place an order",
      description =
          "Prices are taken from the catalog. Billing and shipping details may be omitted; the"
              + " contact details of the account are then used. Answers 201 for a new order and"
              + " 200 when the request repeats an earlier one with the same Idempotency-Key.")
  ResponseEntity<OrderResponse> place(
      @RequestBody OrderRequest request,
      @Parameter(description = "Chosen by the client, at most 100 characters. Makes a retry safe.")
          @RequestHeader(name = IDEMPOTENCY_HEADER, required = false)
          String idempotencyKey,
      Principal caller) {
    if (idempotencyKey != null
        && (idempotencyKey.isBlank() || idempotencyKey.length() > MAX_KEY_LENGTH)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          IDEMPOTENCY_HEADER + " must be 1 to " + MAX_KEY_LENGTH + " characters");
    }
    Placement placement =
        orders.placeOrder(
            caller.getName(), request.lines(), request.billTo(), request.shipTo(), idempotencyKey);
    OrderResponse body = OrderResponse.of(placement.order());
    if (placement.replayed()) {
      return ResponseEntity.ok(body);
    }
    return ResponseEntity.created(URI.create("/api/v1/orders/" + body.orderId())).body(body);
  }

  /** Everything that is wrong with the request is reported at once, under {@code problems}. */
  @ExceptionHandler(InvalidOrderException.class)
  ProblemDetail invalid(InvalidOrderException exception) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "The order cannot be accepted");
    problem.setProperty("problems", ProblemsResponse.of(exception.problems()));
    return problem;
  }

  @ExceptionHandler(NoAccountException.class)
  ProblemDetail noAccount(NoAccountException exception) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT,
        "Enter account details first (PUT /api/v1/account); an order needs the account's card");
  }

  // ---- request -----------------------------------------------------------------------------

  /**
   * @param lines what to order; an item may not carry a price
   * @param billTo billing details; optional
   * @param shipTo shipping details; optional
   */
  record OrderRequest(List<RequestedLine> lines, ContactInfo billTo, ContactInfo shipTo) {}

  // ---- response ----------------------------------------------------------------------------

  record OrderResponse(
      String orderId,
      Instant placedAt,
      String email,
      BigDecimal totalPrice,
      List<LineResponse> lines,
      ContactInfo billTo,
      ContactInfo shipTo,
      CardResponse card,
      HandoffResponse handoff) {

    static OrderResponse of(Order order) {
      return new OrderResponse(
          order.orderId(),
          order.placedAt(),
          order.email(),
          order.totalPrice(),
          order.lines().stream().map(LineResponse::of).toList(),
          order.billTo(),
          order.shipTo(),
          new CardResponse(order.creditCard().cardType(), order.creditCard().maskedNumber()),
          HandoffResponse.of(order.handoff()));
    }
  }

  record LineResponse(
      int lineNumber,
      String itemId,
      String productId,
      String categoryId,
      int quantity,
      BigDecimal unitPrice,
      BigDecimal total) {

    static LineResponse of(OrderLine line) {
      return new LineResponse(
          line.lineNumber(),
          line.itemId(),
          line.productId(),
          line.categoryId(),
          line.quantity(),
          line.unitPrice(),
          line.total());
    }
  }

  /** The card the order is paid with, its number masked to the last four characters. */
  record CardResponse(String cardType, String maskedNumber) {}

  /**
   * @param status {@code SENT} once the order has been delivered to order processing, {@code
   *     PENDING} while delivery is still being tried
   */
  record HandoffResponse(String status, int attempts, Instant lastAttemptAt) {

    static HandoffResponse of(Handoff handoff) {
      return new HandoffResponse(
          handoff.status().name(), handoff.attempts(), handoff.lastAttemptAt());
    }
  }

  record ProblemsResponse(
      List<String> billTo,
      List<String> shipTo,
      List<String> unknownItems,
      List<String> invalidQuantities,
      boolean noLines) {

    static ProblemsResponse of(OrderProblems problems) {
      return new ProblemsResponse(
          problems.billTo().stream().map(CustomerField::name).sorted().toList(),
          problems.shipTo().stream().map(CustomerField::name).sorted().toList(),
          problems.unknownItems(),
          problems.invalidQuantities(),
          problems.noLines());
    }
  }
}
