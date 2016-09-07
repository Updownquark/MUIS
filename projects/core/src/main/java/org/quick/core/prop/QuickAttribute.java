package org.quick.core.prop;

import java.util.List;
import java.util.function.Function;

import org.observe.ObservableValue;

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
	/** @see QuickProperty#QuickProperty(String, QuickPropertyType, PropertyValidator, List) */
	protected QuickAttribute(String name, QuickPropertyType<T> type, PropertyValidator<T> validator,
		List<Function<String, ObservableValue<?>>> valueSuppliers) {
		super(name, type, validator, valueSuppliers);
	}

	@Override
	public final String getPropertyTypeName() {
		return "attribute";
	}

	/**
	 * @param <T> The compile-time type of the attribute
	 * @param name The name for the attribute
	 * @param type The type for the attribute
	 * @return A builder to build the attribute
	 */
	public static <T> Builder<T> build(String name, QuickPropertyType<T> type) {
		return new Builder<>(name, type);
	}

	/**
	 * Builds a {@link QuickAttribute}
	 *
	 * @param <T> The type of the attribute
	 */
	public static class Builder<T> extends QuickProperty.Builder<T> {
		/**
		 * @param name The name for the attribute
		 * @param type The type for the attribute
		 */
		protected Builder(String name, QuickPropertyType<T> type) {
			super(name, type);
		}

		@Override
		public Builder<T> validate(org.quick.core.prop.QuickProperty.PropertyValidator<T> validator) {
			return (Builder<T>) super.validate(validator);
		}

		@Override
		public Builder<T> withValues(Function<String, ObservableValue<?>> values) {
			return (Builder<T>) super.withValues(values);
		}

		@Override
		public QuickAttribute<T> build() {
			return new QuickAttribute<>(getName(), getType(), getValidator(), getValueSuppliers());
		}
	}
}
