package com.bobkevic.cqrs.publisher.resources.errors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
@RestController
public class CustomizedResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

//  @ExceptionHandler(Exception.class)
//  public final ResponseEntity<ApiError> handleUserNotFoundException(final Exception ex,
//                                                                    final WebRequest request) {
//
//    final HttpStatus status = HttpStatus.BAD_REQUEST;
//    final ApiError apiError = ApiErrorBuilder.builder()
//        .status(status.value())
//        .message(ex.getLocalizedMessage())
//        .path(request.getDescription(false))
//        .build();
//
//    return new ResponseEntity<>(apiError, status);
//  }

  @ExceptionHandler({MethodArgumentTypeMismatchException.class})
  public ResponseEntity<ApiError> handleMethodArgumentTypeMismatch(
      final MethodArgumentTypeMismatchException ex,
      final WebRequest request) {
    final String error = ex.getName() + " should be of type " + ex.getRequiredType().getName();

    final HttpStatus status = HttpStatus.BAD_REQUEST;
    final ApiError apiError = ApiErrorBuilder.builder()
        .status(status.value())
        .message(ex.getLocalizedMessage())
        .path(request.getDescription(false))
        .addErrors(error)
        .build();
    return new ResponseEntity<>(apiError, status);
  }
}