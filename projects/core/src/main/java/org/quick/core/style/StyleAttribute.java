package org.quick.core.style;

import java.util.List;
import java.util.function.Function;

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
	 * @param valueSuppliers The value suppliers for parsing the property
	 */
	protected StyleAttribute(StyleDomain domain, String name, QuickPropertyType<T> type, T defValue, PropertyValidator<T> validator,
		List<Function<String, ?>> valueSuppliers) {
		super(name, type, validator, valueSuppliers);
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
		if (!super.equals(obj))
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

	/**
	 * @param <T> The type of the attribute to build
	 * @param domain The domain for the attribute
	 * @param name The name of the attribute
	 * @param type The type of the attribute
	 * @param defValue The default value for the attribute
	 * @return The builder for the attribute
	 */
	public static <T> Builder<T> build(StyleDomain domain, String name, QuickPropertyType<T> type, T defValue) {
		return new Builder<>(domain, name, type, defValue);
	}

	/**
	 * Builds style attributes
	 * 
	 * @param <T> The type of attribute to build
	 */
	public static class Builder<T> extends QuickProperty.Builder<T> {
		private final StyleDomain theDomain;
		private final T theDefValue;

		/**
		 * @param domain The style domain for the attribute
		 * @param name The name of the attribute
		 * @param type The type of the attribute
		 * @param defValue The default value for the attribute
		 */
		protected Builder(StyleDomain domain, String name, QuickPropertyType<T> type, T defValue) {
			super(name, type);
			theDomain = domain;
			theDefValue = defValue;
		}

		@Override
		public StyleAttribute<T> build() {
			return new StyleAttribute<>(theDomain, getName(), getType(), theDefValue, getValidator(), getValueSuppliers());
		}
	}
}
