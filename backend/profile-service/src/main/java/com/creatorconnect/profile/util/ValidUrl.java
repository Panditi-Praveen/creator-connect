package com.creatorconnect.profile.util;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bean Validation constraint that verifies a string is a well-formed
 * {@code http(s)} URL with a resolvable host.
 *
 * <p>Implemented as a custom constraint because Hibernate Validator's built-in
 * {@code org.hibernate.validator.constraints.URL} is deprecated since
 * Hibernate Validator 8 — this annotation keeps URL checks working with no
 * deprecation warnings and full control over the accepted schemes.
 *
 * <p>{@code null} and blank values pass validation (optional fields are handled
 * by their own {@code @NotBlank}/{@code @Size} constraints where required).
 */
@Documented
@Constraint(validatedBy = UrlValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUrl {

    /**
     * The default error message.
     *
     * @return the validation message
     */
    String message() default "must be a valid http(s) URL";

    /**
     * Constraint groups.
     *
     * @return the groups
     */
    Class<?>[] groups() default {};

    /**
     * Constraint payload.
     *
     * @return the payload
     */
    Class<? extends Payload>[] payload() default {};
}
