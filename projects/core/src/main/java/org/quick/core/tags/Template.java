package org.quick.core.tags;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Tells {@link org.quick.core.QuickTemplate QuickTemplate} that the element this is tagged on needs to load a template file */
@Retention(RetentionPolicy.RUNTIME)
@Target(TYPE)
public @interface Template {
	/** The location of the file to load the templated widget's definition from */
	String location();
	/** Attributes that may be specified on the templated widget to affect its data model */
	ModelAttribute[] attributes() default {};
}
