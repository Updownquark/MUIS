package org.quick.core.tags;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/** Declares a type as a Quick element type. Unused as of right now. */
@Retention(RUNTIME)
@Target(TYPE)
public @interface QuickType {
	/** The tag name of the type */
	String name();
}
