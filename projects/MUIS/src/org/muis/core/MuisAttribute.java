package org.muis.core;


/**
 * A MuisAttribute represents an option that may or must be specified in a MUIS element either from the document(XML) or from code. A
 * MuisAttribute must be created in java (preferably in the form of a static constant) and given to the element to tell it that the
 * attribute is {@link org.muis.core.mgr.AttributeManager#accept(Object, MuisAttribute) accepted} or
 * {@link org.muis.core.mgr.AttributeManager#require(Object, MuisAttribute) required}. This allows the element to properly parse the
 * attribute value specified in the document so that the option is available to java code to properly interpret the value.
 *
 * @param <T> The java type of the attribute
 */
public class MuisAttribute<T> extends MuisProperty<T> {
	/** Allows hierarchical attributes */
	public static interface PropertyPathAccepter {
		/**
		 * @param element The element to accept or reject the pathed attribute on
		 * @param path The path to check
		 * @return Whether a property with the given path off of a root property can be instantiated
		 */
		boolean accept(MuisElement element, String... path);
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
	public MuisAttribute(String name, PropertyType<T> type, PropertyValidator<T> validator, PropertyPathAccepter accepter) {
		super(name, type, validator);
		thePathAccepter = accepter;
	}

	/**
	 * Creates a new attribute for a MUIS element
	 *
	 * @see MuisProperty#MuisProperty(String, org.muis.core.MuisProperty.PropertyType)
	 */
	public MuisAttribute(String name, PropertyType<T> type) {
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
