package com.petstore.storefront.internal.api;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Error format of the JSON API: RFC 9457 problem details ({@code application/problem+json}).
 *
 * <p>Applies to the JSON endpoints only. Pages keep the HTML error page.
 */
@RestControllerAdvice(annotations = RestController.class)
class ApiExceptionHandler extends ResponseEntityExceptionHandler {}
