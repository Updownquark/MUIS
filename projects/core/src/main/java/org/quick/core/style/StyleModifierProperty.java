package org.quick.core.style;

import java.util.List;
import java.util.function.Function;

import org.observe.ObservableValue;
import org.quick.core.prop.QuickProperty;
import org.quick.core.prop.QuickPropertyType;

public class StyleModifierProperty<T> extends QuickProperty<T> {
	public static final String PRESENT_PROPERTY_NAME = "present";

	private final Class<? extends StyleModifier> theModifierType;
	private final T theDefault;

	private StyleModifierProperty(Class<? extends StyleModifier> modifierType, String name, QuickPropertyType<T> type, T defValue,
		PropertyValidator<T> validator, List<Function<String, ObservableValue<?>>> valueSuppliers) {
		super(name, type, validator, valueSuppliers);
		theModifierType = modifierType;
		theDefault = defValue;
	}

	@Override
	protected String getPropertyTypeName() {
		return "style modifier property";
	}

	/** @return The default value for this property */
	public T getDefault() {
		return theDefault;
	}

	@Override
	public final boolean equals(Object obj) {
		if (!super.equals(obj))
			return false;
		StyleModifierProperty<?> modProp = (StyleModifierProperty<?>) obj;
		return modProp.theModifierType.equals(theModifierType);
	}

	@Override
	public final int hashCode() {
		int ret = super.hashCode();
		ret = ret * 13 + theModifierType.hashCode();
		return super.hashCode();
	}

	@Override
	public String toString() {
		return theModifierType.getSimpleName() + "." + getName();
	}

	/**
	 * @param <T> The type of the property to build
	 * @param modifierType The type of modifier the property is for
	 * @param name The name of the property
	 * @param type The type of the property
	 * @param defValue The default value for the property
	 * @return The builder for the property
	 */
	public static <T> Builder<T> build(Class<? extends StyleModifier> modifierType, String name, QuickPropertyType<T> type, T defValue) {
		return new Builder<>(modifierType, name, type, defValue);
	}

	/**
	 * Builds style modifier properties
	 *
	 * @param <T> The type of property to build
	 */
	public static class Builder<T> extends QuickProperty.Builder<T> {
		private final Class<? extends StyleModifier> theModifierType;
		private final T theDefValue;

		/**
		 * @param domain The type of modifier this property is for
		 * @param name The name of the attribute
		 * @param type The type of the attribute
		 * @param defValue The default value for the attribute
		 */
		protected Builder(Class<? extends StyleModifier> domain, String name, QuickPropertyType<T> type, T defValue) {
			super(name, type);
			theModifierType = domain;
			theDefValue = defValue;
		}

		@Override
		public Builder<T> validate(PropertyValidator<T> validator) {
			return (Builder<T>) super.validate(validator);
		}

		@Override
		public Builder<T> withValues(Function<String, ObservableValue<?>> values) {
			return (Builder<T>) super.withValues(values);
		}

		@Override
		public StyleModifierProperty<T> build() {
			return new StyleModifierProperty<>(theModifierType, getName(), getType(), theDefValue, getValidator(), getValueSuppliers());
		}
	}
}
