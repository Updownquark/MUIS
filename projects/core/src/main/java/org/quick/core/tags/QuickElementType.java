package org.quick.core.tags;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/** Tags a type as a QuickElement type */
@Retention(RUNTIME)
@Target(TYPE)
public @interface QuickElementType {
	/** The attributes that this type accepts */
	AcceptAttribute[] attributes() default {};

	/** The states that the type supports */
	State[] states() default {};
}
