package org.muis.core.tags;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Tells {@link org.muis.core.MuisTemplate MuisTemplate} that the element this is tagged on needs to load a template file */
@Retention(RetentionPolicy.RUNTIME)
@Target(TYPE)
public @interface Template {
	/** The location of the file to load the templated widget's definition from */
	String location();
}
