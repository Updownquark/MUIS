package org.quick.core.style;

import java.util.List;
import java.util.function.Function;

import org.observe.ObservableValue;
import org.observe.util.TypeTokens;
import org.quick.core.prop.QuickProperty;
import org.quick.core.prop.QuickPropertyType;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;

/**
 * A style property that can affect the rendering of Quick elements
 *
 * @param <T> The type of value the property supports
 */
public final class StyleAttribute<T> extends QuickProperty<T> {
	@SuppressWarnings("rawtypes")
	public static final TypeToken<StyleAttribute<?>> TYPE = TypeTokens.get().keyFor(StyleAttribute.class)
		.enableCompoundTypes(new TypeTokens.UnaryCompoundTypeCreator<StyleAttribute>() {
			@Override
			public <P> TypeToken<? extends StyleAttribute> createCompoundType(TypeToken<P> param) {
				return new TypeToken<StyleAttribute<P>>() {}.where(new TypeParameter<P>() {}, param);
			}
		}).parameterized();

	private final StyleDomain theDomain;
	private final boolean isInherited;
	private final T theDefault;

	/**
	 * @param domain The style domain for the attribute
	 * @param name The name for the attribute
	 * @param type The type of the attribute
	 * @param inherited Whether an element's style can inherit values from its parent for this attribute
	 * @param defValue The default value for the attribute
	 * @param validator The validator for the attribute
	 * @param valueSuppliers The value suppliers for parsing the property
	 */
	protected StyleAttribute(StyleDomain domain, String name, QuickPropertyType<T> type, boolean inherited, T defValue,
		PropertyValidator<T> validator,
		List<Function<String, ObservableValue<?>>> valueSuppliers) {
		super(name, type, validator, valueSuppliers);
		theDomain = domain;
		isInherited = inherited;
		theDefault = defValue;
	}

	/** @return The style domain that the attribute belongs to */
	public StyleDomain getDomain() {
		return theDomain;
	}

	/** @return Whether an element's style can inherit values from its parent for this attribute */
	public boolean isInherited() {
		return isInherited;
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
		return attr.theDomain.equals(theDomain);
	}

	@Override
	public final int hashCode() {
		int ret = super.hashCode();
		ret = ret * 13 + theDomain.hashCode();
		return super.hashCode();
	}

	@Override
	public String toString() {
		return theDomain.getName() + "." + getName();
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
		private boolean isInherited;
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

		/**
		 * Marks the style created by this builder as inherited, meaning element styles can inherit values for this attribute from their
		 * parent or ancestors
		 *
		 * @return This builder
		 */
		public Builder<T> inherited() {
			isInherited = true;
			return this;
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
		public StyleAttribute<T> build() {
			return new StyleAttribute<>(theDomain, getName(), getType(), isInherited, theDefValue, getValidator(), getValueSuppliers());
		}
	}
}
