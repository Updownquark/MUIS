package org.quick.core.model;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/** Tags a method on a POJO model as being an action listener */
@Retention(RUNTIME)
@Target({METHOD})
public @interface QuickAction {
	/** @return The actions that the tagged method listens for. If unset, the name of the method will be used. */
	String [] actions() default {};
}
