//package com.bobkevic.cqrs.publisher.resources;
//
//import java.util.UUID;
//import org.springframework.validation.Errors;
//import org.springframework.validation.ValidationUtils;
//import org.springframework.validation.Validator;
//
//public class IdValidator implements Validator {
//
//  @Override
//  public boolean supports(final Class clazz) {
//    return UUID.class.isAssignableFrom(clazz);
//  }
//
//  @Override
//  public void validate(final Object target, final Errors errors) {
//    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "firstName", "field.required");
//    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "surname", "field.required");
//    Customer customer = (Customer) target;
//    try {
//      errors.pushNestedPath("address");
//      ValidationUtils.invokeValidator(this.addressValidator, customer.getAddress(), errors);
//    } finally {
//      errors.popNestedPath();
//    }
//  }
//}
