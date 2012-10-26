package org.muis.core.tags;

/** Tells {@link org.muis.core.MuisTemplate MuisTemplate} that the element this is tagged on needs to load a template file */
public @interface Template {
	/** The location of the file to load the templated widget's definition from */
	String location();

	/** All attach points for this widget */
	AttachPoint [] attachPoints();

	/** Overrides for super class attach points */
	AttachPointOverride [] attachPointOverrides() default {};
}
