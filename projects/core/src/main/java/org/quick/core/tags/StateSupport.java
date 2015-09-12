package org.quick.core.tags;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Tags a QuickElement as supporting a set of states */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface StateSupport {
	/** The states that the type supports */
	State [] value();
}
