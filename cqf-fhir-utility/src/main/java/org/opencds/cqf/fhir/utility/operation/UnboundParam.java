package org.opencds.cqf.fhir.utility.operation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to denote the parameters that are not bound by the operation definition.
 * The absence of this annotation on a method implies that it is strict, and will
 * only accept parameters that are defined in the operation definition. Additional
 * parameters will result in an error.
 *
 * If this annotation is present on a method, it must be the last parameter.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.PARAMETER)
public @interface UnboundParam {}
