package com.bobkevic.cqrs.publisher;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.immutables.value.Value;

@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
@Value.Style(
    visibility = Value.Style.ImplementationVisibility.PRIVATE,
    overshadowImplementation = true,
    newBuilder = "builder",
    forceJacksonPropertyNames = false
)
public @interface ImmutableStyle {

}
