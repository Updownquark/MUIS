package org.quick.core.style;

import org.qommons.ArrayUtils;
import org.quick.core.prop.QuickProperty;
import org.quick.core.prop.QuickPropertyType;

/**
 * A style property that can affect the rendering of Quick elements
 *
 * @param <T> The type of value the property supports
 */
public final class StyleAttribute<T> extends QuickProperty<T> {
	private final StyleDomain theDomain;

	private final T theDefault;

	/**
	 * @param domain The style domain for the attribute
	 * @param name The name for the attribute
	 * @param type The type of the attribute
	 * @param defValue The default value for the attribute
	 * @param validator The validator for the attribute
	 */
	public StyleAttribute(StyleDomain domain, String name, QuickPropertyType<T> type, T defValue, PropertyValidator<T> validator) {
		super(name, type, validator);
		theDomain = domain;
		theDefault = defValue;
	}

	/**
	 * @param domain The style domain for the attribute
	 * @param name The name for the attribute
	 * @param type The type of the attribute
	 * @param defValue The default value for the attribute
	 */
	public StyleAttribute(StyleDomain domain, String name, QuickPropertyType<T> type, T defValue) {
		super(name, type);
		theDomain = domain;
		theDefault = defValue;
	}

	/** @return The style domain that the attribute belongs to */
	public StyleDomain getDomain() {
		return theDomain;
	}

	/** @return The default value for this attribute */
	public T getDefault() {
		return theDefault;
	}

	@Override
	public final String getPropertyTypeName() {
		return "style attribute";
	}

	@Override
	public final boolean equals(Object obj) {
		if(!super.equals(obj))
			return false;
		StyleAttribute<?> attr = (StyleAttribute<?>) obj;
		return attr.theDomain.equals(theDomain) && ArrayUtils.equals(attr.theDefault, theDefault);
	}

	@Override
	public final int hashCode() {
		int ret = super.hashCode();
		ret = ret * 13 + theDomain.hashCode();
		ret = ret * 13 + ArrayUtils.hashCode(theDefault);
		return super.hashCode();
	}
}
