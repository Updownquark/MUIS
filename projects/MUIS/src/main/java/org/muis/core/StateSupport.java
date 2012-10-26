package org.muis.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Tags a MuisElement as supporting a set of states */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface StateSupport {
	/** The states that the type supports */
	State [] value();
}
