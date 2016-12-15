package org.quick.core.tags;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;

/** Describes the acceptance of an attribute on an element */
@Retention(RUNTIME)
public @interface AcceptAttribute {
	/** The class that has the static field or method providing the attribute */
	Class<?> declaringClass();

	/** The name of the static field on the #declaringClass() holding the attribute */
	String field() default "";

	/** The name of the static method on the {@link #declaringClass()} providing the attribute */
	String method() default "";

	/** Whether the attribute is required */
	boolean required() default false;

	/** The default value for the attribute if none is provided. Incompatible with {@link #required()}. */
	String defaultValue() default "";
}
