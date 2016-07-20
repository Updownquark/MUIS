package org.quick.core.prop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.observe.ObservableValue;
import org.quick.core.QuickException;

/**
 * Represents a property in Quick
 *
 * @param <T> The type of values that may be associated with the property
 */
public abstract class QuickProperty<T> {
	/**
	 * A property validator places constraints on the value of a property
	 *
	 * @param <T> The type of value that this validator can validate
	 */
	public static interface PropertyValidator<T> {
		/**
		 * @param value The value to check
		 * @return Whether the value is valid by this validator's constraints
		 */
		boolean isValid(T value);

		/**
		 * @param value The value to check
		 * @throws QuickException If the value was not valid by this validator's constraints. The message in this exception will be as
		 *             descriptive and user-friendly as possible.
		 */
		void assertValid(T value) throws QuickException;
	}

	private final String theName;
	private final QuickPropertyType<T> theType;
	private final List<Function<String, ObservableValue<?>>> theValueSuppliers;
	private final PropertyValidator<T> theValidator;

	/**
	 * @param name The name for the property
	 * @param type The type of the property
	 * @param validator The validator for the property
	 */
	protected QuickProperty(String name, QuickPropertyType<T> type, PropertyValidator<T> validator,
		List<Function<String, ObservableValue<?>>> valueSuppliers) {
		theName = name;
		theType = type;
		theValueSuppliers = valueSuppliers == null ? Collections.emptyList() : Collections.unmodifiableList(valueSuppliers);
		theValidator = validator;
	}

	/** @return This property's name */
	public String getName() {
		return theName;
	}

	/** @return This property's type */
	public QuickPropertyType<T> getType() {
		return theType;
	}

	/** @return The validator for this property. May be null. */
	public PropertyValidator<T> getValidator() {
		return theValidator;
	}

	public List<Function<String, ObservableValue<?>>> getValueSuppliers() {
		return theValueSuppliers;
	}

	/** @return What kind of property this is */
	protected abstract String getPropertyTypeName();

	@Override
	public String toString() {
		return getPropertyTypeName() + " " + theName + "(" + theType + ")";
	}

	@Override
	public boolean equals(Object o) {
		if(o == null || !o.getClass().equals(getClass()))
			return false;
		QuickProperty<?> prop = (QuickProperty<?>) o;
		return prop.theName.equals(theName) && prop.theType.equals(theType);
	}

	@Override
	public int hashCode() {
		return theName.hashCode() * 13 + theType.hashCode() * 7;
	}

	public static abstract class Builder<T> {
		private final String theName;
		private final QuickPropertyType<T> theType;
		private PropertyValidator<T> theValidator;
		private final List<Function<String, ObservableValue<?>>> theValueSuppliers;

		public Builder(String name, QuickPropertyType<T> type) {
			theName = name;
			theType = type;
			theValueSuppliers = new ArrayList<>();
		}

		public Builder<T> validate(PropertyValidator<T> validator) {
			theValidator = validator;
			return this;
		}

		public Builder<T> withValues(Function<String, ObservableValue<?>> values) {
			theValueSuppliers.add(values);
			return this;
		}

		protected String getName() {
			return theName;
		}

		protected QuickPropertyType<T> getType() {
			return theType;
		}

		protected PropertyValidator<T> getValidator() {
			return theValidator;
		}

		protected List<Function<String, ObservableValue<?>>> getValueSuppliers() {
			return theValueSuppliers;
		}

		public abstract QuickProperty<T> build();
	}

	/**
	 * A simple validator that compares values to minimum and maximum values
	 *
	 * @param <T> The type of value to validate
	 */
	public static class ComparableValidator<T> implements PropertyValidator<T> {
		private final Comparator<? super T> theCompare;
		private final Comparator<? super T> theInternalCompare;

		private final T theMin;
		private final T theMax;

		private final int theHashCode;

		/**
		 * Shorter constructor for comparable types
		 *
		 * @param min The minimum value for the property
		 * @param max The maximum value for the property
		 */
		public ComparableValidator(T min, T max) {
			this(min, max, null);
		}

		/**
		 * @param min The minimum value for the property
		 * @param max The maximum value for the property
		 * @param compare The comparator to use to compare. May be null if this type is comparable.
		 */
		public ComparableValidator(T min, T max, Comparator<? super T> compare) {
			theCompare = compare;
			theMin = min;
			theMax = max;
			if(compare != null)
				theInternalCompare = compare;
			else {
				if(theMin != null && !(theMin instanceof Comparable))
					throw new IllegalArgumentException("No comparator given, but minimum value is not Comparable");
				if(theMax != null && !(theMax instanceof Comparable))
					throw new IllegalArgumentException("No comparator given, but maximum value is not Comparable");
				theInternalCompare = new Comparator<T>() {
					@Override
					public int compare(T o1, T o2) {
						return ((Comparable<T>) o1).compareTo(o2);
					}
				};
			}

			int hc = 0;
			if(theCompare != null)
				hc += theCompare.getClass().hashCode();
			if(theMin != null)
				hc = hc * 7 + theMin.hashCode();
			if(theMax != null)
				hc = hc * 7 + theMax.hashCode();
			theHashCode = hc;
		}

		/** @return The comparator that is used to validate values. May be null if the type is comparable. */
		public Comparator<? super T> getCompare() {
			return theCompare;
		}

		/** @return The minimum value to validate against */
		public T getMin() {
			return theMin;
		}

		/** @return The maximum value to validate against */
		public T getMax() {
			return theMax;
		}

		@Override
		public boolean isValid(T value) {
			if(theMin != null && theInternalCompare.compare(theMin, value) > 0)
				return false;
			if(theMax != null && theInternalCompare.compare(value, theMax) > 0)
				return false;
			return true;
		}

		@Override
		public void assertValid(T value) throws QuickException {
			if(theMin != null && theInternalCompare.compare(theMin, value) > 0)
				throw new QuickException("Value must be at least " + theMin + ": " + value + " is invalid");
			if(theMax != null && theInternalCompare.compare(value, theMax) > 0)
				throw new QuickException("Value must be at most " + theMax + ": " + value + " is invalid");
		}

		@Override
		public String toString() {
			if(theMin == null && theMax == null)
				return "Empty comparable validator";
			return "Comparable validator: " + (theMin != null ? theMin + " < " : "") + "value" + (theMax != null ? " < " + theMax : "");
		}

		@Override
		public boolean equals(Object o) {
			if(o == null || o.getClass() != getClass())
				return false;
			ComparableValidator<?> val = (ComparableValidator<?>) o;
			if(theCompare != null) {
				if(val.theCompare == null)
					return false;
				if(theCompare.getClass() != val.theCompare.getClass())
					return false;
			} else if(val.theCompare != null)
				return false;
			if(theMin == null ? val.theMin != null : !theMin.equals(val.theMin))
				return false;
			if(theMax == null ? val.theMax != null : !theMax.equals(val.theMax))
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			return theHashCode;
		}
	}
}
