package org.quick.core.prop;

import org.quick.core.QuickElement;

/**
 * A QuickAttribute represents an option that may or must be specified in a Quick element either from the document(XML) or from code. A
 * QuickAttribute must be created in java (preferably in the form of a static constant) and given to the element to tell it that the
 * attribute is {@link org.quick.core.mgr.AttributeManager#accept(Object, QuickAttribute) accepted} or
 * {@link org.quick.core.mgr.AttributeManager#require(Object, QuickAttribute) required}. This allows the element to properly parse the
 * attribute value specified in the document so that the option is available to java code to properly interpret the value.
 *
 * @param <T> The java type of the attribute
 */
public class QuickAttribute<T> extends QuickProperty<T> {
	/** Allows hierarchical attributes */
	public static interface PropertyPathAccepter {
		/**
		 * @param element The element to accept or reject the pathed attribute on
		 * @param path The path to check
		 * @return Whether a property with the given path off of a root property can be instantiated
		 */
		boolean accept(QuickElement element, String... path);
	}

	private PropertyPathAccepter thePathAccepter;

	/**
	 * Creates a new attribute for a MUIS element
	 *
	 * @param name The name for this attribute
	 * @param type The type of the attribute
	 * @param validator The validator for this attribute's values
	 * @param accepter The path accepter to accept hierarchical properties with this property as the root
	 */
	public QuickAttribute(String name, QuickPropertyType<T> type, PropertyValidator<T> validator, PropertyPathAccepter accepter) {
		super(name, type, validator);
		thePathAccepter = accepter;
	}

	/**
	 * Creates a new attribute for a MUIS element
	 *
	 * @see QuickProperty#QuickProperty(String, org.quick.core.QuickProperty.QuickPropertyType)
	 */
	public QuickAttribute(String name, QuickPropertyType<T> type) {
		super(name, type);
	}

	/** @return The path accepter to accept hierarchical properties with this property as the root */
	public PropertyPathAccepter getPathAccepter() {
		return thePathAccepter;
	}

	@Override
	public final String getPropertyTypeName() {
		return "attribute";
	}
}
