package org.quick.core.tags;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;

@Retention(RUNTIME)
public @interface AcceptAttribute {
	/** The class that has the static field or method providing the attribute */
	Class<?> declaringClass();

	String field() default "";

	String method() default "";

	boolean required() default false;

	String defaultValue() default "";
}
