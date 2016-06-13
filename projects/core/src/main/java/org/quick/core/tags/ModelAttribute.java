package org.quick.core.tags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** An attribute that may be specified on a templated element to affect its model */
@Retention(RetentionPolicy.RUNTIME)
public @interface ModelAttribute {
	/** The name of the attribute */
	String name();
	/** The type of the attribute */
	Class<?> type();
}
