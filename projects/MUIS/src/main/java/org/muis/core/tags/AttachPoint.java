package org.muis.core.tags;

/**
 * Represents an attach point in a templated widget
 *
 * @see org.muis.core.MuisTemplate
 * @see Template
 */
public @interface AttachPoint {
	/** The name of the attach point */
	String name();

	/** The type of widget that may be on the attach point */
	Class<? extends org.muis.core.MuisElement> type() default org.muis.core.MuisElement.class;

	/** Whether the widget may be specified in the source XML that invokes the templated widget */
	boolean external() default false;

	/** Whether the widget may be specified multiple times */
	boolean multiple() default false;

	/**
	 * Whether this attach point is the default, such that widgets in the source XML whose attach point is not specified will be attached at
	 * this point
	 */
	boolean def() default false;
}
