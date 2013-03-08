package org.muis.core.tags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Tells {@link org.muis.core.MuisTemplate2 MuisTemplate} that the element this is tagged on needs to load a template file */
@Retention(RetentionPolicy.RUNTIME)
public @interface Template {
	/** The location of the file to load the templated widget's definition from */
	String location();
}
