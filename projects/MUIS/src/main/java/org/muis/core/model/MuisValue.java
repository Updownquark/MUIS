package org.muis.core.model;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/** Tags a type as a MUIS model value; or tags a field or method in a POJO model as returning a MUIS model value */
@Retention(RUNTIME)
@Target({FIELD, METHOD, TYPE})
public @interface MuisValue {
	/**
	 * @return The setter to set the value for this model value. If unset, a setter will be searched for. If not found, the value will be
	 *         immutable.
	 */
	String setter() default "";
}
