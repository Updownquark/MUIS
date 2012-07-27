package org.muis.core.annotations;

/** Represents an attribute needed or usable by a MUIS type */
public @interface NeededAttr
{
	/** @return The name of the attribute */
	String name();

	/** The type of the attribute */
	MuisAttrType type();

	/**
	 * The attribute's value type (for {@link MuisAttrType#TYPE TYPE}, {@link MuisAttrType#INSTANCE INSTANCE}, and {@link MuisAttrType#ENUM
	 * ENUM} attribute types
	 */
	Class<?> valueType() default Object.class;

	/** @return Whether the attribute should be required or not. This is ignored for style attributes. */
	boolean required() default false;

	/** @return The parseable initial value for the attribute. This MUST be set if {@link #required()} is true. */
	String initValue() default "";

	/** @return The action to perform when one of the attributes changes. Overrides {@link MuisAttrConsumer#action()}. */
	MuisActionType action() default MuisActionType.def;
}
