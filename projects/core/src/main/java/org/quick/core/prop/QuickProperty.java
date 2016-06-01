package org.quick.core.prop;

import java.net.URL;
import java.util.Comparator;

import org.observe.ObservableValue;
import org.quick.core.QuickException;
import org.quick.core.QuickParseEnv;
import org.quick.core.QuickToolkit;
import org.quick.core.eval.impl.ObservableEvaluator;
import org.quick.core.parser.QuickParseException;
import org.quick.util.QuickUtils;

import com.google.common.reflect.TypeToken;

/**
 * Represents a property in MUIS
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

	/**
	 * An abstract class to help {@link QuickPropertyType}s and {@link QuickProperty.PropertyValidator}s generate better error
	 * messages
	 *
	 * @param <T> The type of property that this helper is for
	 */
	public static abstract class PropertyHelper<T> {
		private QuickProperty<T> theProperty;

		/** @return The property that this helper is for */
		public QuickProperty<T> getProperty() {
			return theProperty;
		}

		void setProperty(QuickProperty<T> property) {
			theProperty = property;
		}

		/** @return The concatenation of this helper's property's type name and its name. Useful for error messages. */
		protected String propName() {
			if(theProperty == null)
				return "";
			return theProperty.getPropertyTypeName() + " " + theProperty.getName();
		}
	}

	/**
	 * A property type that is helped by {@link QuickProperty.PropertyHelper}
	 *
	 * @param <T> The type of property that this type is for
	 */
	public static abstract class AbstractPropertyType<T> extends PropertyHelper<T> implements QuickPropertyType<T> {
	}

	/**
	 * A property type that is helped by {@link QuickProperty.PropertyHelper}
	 *
	 * @param <T> The type of property that this validator is for
	 */
	public static abstract class AbstractPropertyValidator<T> extends PropertyHelper<T> implements PropertyValidator<T> {
	}

	private static abstract class AbstractPrintablePropertyType<T> extends AbstractPropertyType<T> implements PrintablePropertyType<T> {
	}

	private static final TypeToken<String> STRING_TYPE = TypeToken.of(String.class);
	private static final TypeToken<CharSequence> CHAR_SEQ_TYPE = TypeToken.of(CharSequence.class);

	/**
	 * Parses MUIS properties using a PrismsParser
	 *
	 * @param <T> The type of the property
	 */
	public static class PrismsParsedPropertyType<T> extends AbstractPropertyType<T> {
		private final TypeToken<T> theType;
		private final boolean evalAsType;

		/** @param type The type of the property */
		public PrismsParsedPropertyType(TypeToken<T> type) {
			this(type, false);
		}

		/**
		 * @param type The type of the property
		 * @param asType Whether to parse this property type's values as types or instances
		 */
		public PrismsParsedPropertyType(TypeToken<T> type, boolean asType) {
			theType = type;
			evalAsType = asType;
		}

		@Override
		public TypeToken<T> getType() {
			return theType;
		}

		/** @return Whether this property type evaluates its values as types */
		public boolean asType() {
			return evalAsType;
		}

		@Override
		public ObservableValue<? extends T> parse(QuickParseEnv env, String value) throws QuickException {
			org.quick.core.parser.DefaultModelValueReferenceParser parser;
			try {
				parser = new org.quick.core.parser.DefaultModelValueReferenceParser(env.getValueParser(), null) {
					@Override
					protected void applyModification() {
						super.applyModification();
						try {
							mutate(getParser(), getEvaluator(), getEvaluationEnvironment());
						} catch(QuickException e) {
							throw new org.quick.util.ExceptionWrapper(e);
						}
					}
				};
			} catch(org.quick.util.ExceptionWrapper e) {
				throw (QuickException) e.getCause();
			}

			ObservableValue<?> ret = parser.parse(value, evalAsType);
			if(theType.equals(ret.getType()))
				return (ObservableValue<? extends T>) ret;
			else if(canCast(ret.getType()))
				return ret.mapV(theType, v -> cast((TypeToken<Object>) ret.getType(), v), true);
			else if(new TypeToken<ObservableValue<?>>() {
			}.isAssignableFrom(ret.getType())) {
				ObservableValue<?> contained=(ObservableValue<?>) ret.get();
				if(theType.equals(contained.getType()))
					return ObservableValue.flatten(theType, (ObservableValue<? extends ObservableValue<? extends T>>) ret);
				else if(canCast(contained.getType()))
					return ObservableValue
						.flatten((TypeToken<Object>) contained.getType(), (ObservableValue<? extends ObservableValue<?>>) ret)
						.mapV(v -> cast((TypeToken<Object>) contained.getType(), v));
			}
			throw new QuickException("The given value of type " + ret.getType() + " is not compatible with this property's type (" + theType
				+ ")");
		}

		/**
		 * May be used by subclasses to modify the prisms parsing types
		 *
		 * @param parser The parser
		 * @param eval The evaluator
		 * @param env The evaluation environment
		 * @throws QuickException If an error occurs mutating the parsing types
		 */
		protected void mutate(PrismsParser parser, ObservableEvaluator eval, EvaluationEnvironment env) throws QuickException {
		}

		@Override
		public boolean canCast(TypeToken<?> type) {
			return theType.isAssignableFrom(type);
		}

		@Override
		public <X, V extends T> V cast(TypeToken<X> type, X value) {
			if(value == null)
				return null;
			if(theType.isAssignableFrom(type))
				return (V) theType.wrap().getRawType().cast(value);
			return null;
		}

		@Override
		public boolean equals(Object o) {
			return o != null && o.getClass() == getClass() && ((PrismsParsedPropertyType<?>) o).getType().equals(theType);
		}

		@Override
		public int hashCode() {
			return theType.hashCode();
		}

		@Override
		public String toString() {
			return theType.toString();
		}
	}

	/**
	 * @param env The parsing environment
	 * @param text The text to parse
	 * @param asType Whether to evaluate the result as a type or an instance
	 * @return An observable value, if the text is explicitly marked to be parsed as such. Null otherwise.
	 * @throws QuickParseException If an exception occurs parsing the explicitly-marked observable from the text.
	 */
	public static ObservableValue<?> parseExplicitObservable(QuickParseEnv env, String text, boolean asType) throws QuickParseException {
		if(text.startsWith("${") && text.endsWith("}"))
			return env.getValueParser().parse(text.substring(2, text.length() - 1), asType);
		else
			return null;
	}

	private final String theName;
	private final QuickPropertyType<T> theType;
	private final PropertyValidator<T> theValidator;

	/**
	 * @param name The name for the property
	 * @param type The type of the property
	 * @param validator The validator for the property
	 */
	protected QuickProperty(String name, QuickPropertyType<T> type, PropertyValidator<T> validator) {
		theName = name;
		theType = type;
		theValidator = validator;
		if(theType instanceof PropertyHelper)
			((PropertyHelper<T>) theType).setProperty(this);
		if(theValidator instanceof PropertyHelper)
			((PropertyHelper<T>) theValidator).setProperty(this);
	}

	/**
	 * Creates a MUIS property without a validator or a path acceptor
	 *
	 * @param name The name for the property
	 * @param type The type of the property
	 */
	protected QuickProperty(String name, QuickPropertyType<T> type) {
		this(name, type, null);
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

	/** @return A string describing this sub-type */
	public abstract String getPropertyTypeName();

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


	/**
	 * A resource property--values may be:
	 * <ul>
	 * <li>namespace:tag, where <code>tag</code> maps to a resource in the <code>namespace</code> toolkit</li>
	 * <li>A relative path to a resource that may be resolved from the element's toolkit. A <code>namespace:</code> prefix may be used to
	 * specify a different toolkit</li>
	 * <li>An absolute URL. Permissions will be checked before resources at any external URLs are retrieved. TODO cite specific permission.</li>
	 * </ul>
	 */
	public static final AbstractPropertyType<URL> resourceAttr = new AbstractPropertyType<java.net.URL>() {
		private static final TypeToken<URL> URL_TYPE = TypeToken.of(URL.class);
		@Override
		public ObservableValue<URL> parse(QuickParseEnv env, String value) throws QuickException {
			ObservableValue<?> ret = parseExplicitObservable(env, value, false);
			if(ret != null) {
				if(TypeToken.of(ObservableValue.class).isAssignableFrom(ret.getType()))
					ret = ObservableValue.flatten(null, (ObservableValue<? extends ObservableValue<?>>) ret);
				if(URL_TYPE.isAssignableFrom(ret.getType())) {
				} else if(CHAR_SEQ_TYPE.isAssignableFrom(ret.getType())) {
					ret = ((ObservableValue<? extends CharSequence>) ret).mapV(seq -> {
						try {
							return new URL(seq.toString());
						} catch(java.net.MalformedURLException e) {
							throw new IllegalArgumentException("Malformed URL", e);
						}
					});
				} else
					throw new QuickException("Model value " + value + " is not of type string or URL");
			}
			int sepIdx = value.indexOf(':');
			String namespace = sepIdx < 0 ? null : value.substring(0, sepIdx);
			String content = sepIdx < 0 ? value : value.substring(sepIdx + 1);
			QuickToolkit toolkit = env.cv().getToolkit(namespace);
			if(toolkit == null)
				try {
					return ObservableValue.constant(new URL(value));
				} catch(java.net.MalformedURLException e) {
					throw new QuickException(propName() + ": Resource property is not a valid URL: \"" + value + "\"", e);
				}
			ret = parseExplicitObservable(env, content, false);
			if(ret != null) {
				if(TypeToken.of(ObservableValue.class).isAssignableFrom(ret.getType()))
					ret = ObservableValue.flatten(null, (ObservableValue<? extends ObservableValue<?>>) ret);
				if(CHAR_SEQ_TYPE.isAssignableFrom(ret.getType())) {
					return ((ObservableValue<? extends CharSequence>) ret).mapV(seq -> {
						try {
							return getMappedResource(toolkit, seq.toString());
						} catch(Exception e) {
							throw new IllegalArgumentException(e);
						}
					});
				} else
					throw new QuickException("Model value " + content + " is not of type string");
			}
			return ObservableValue.constant(getMappedResource(toolkit, content));
		}

		private URL getMappedResource(QuickToolkit toolkit, String resource) throws QuickException {
			ResourceMapping mapping = toolkit.getMappedResource(resource);
			if(mapping == null)
				throw new QuickException(propName() + ": Resource property must map to a declared resource: \"" + resource
					+ "\" in toolkit " + toolkit.getName() + " or one of its dependencies");
			if(mapping.getLocation().contains(":"))
				try {
					return new URL(mapping.getLocation());
				} catch(java.net.MalformedURLException e) {
					throw new QuickException(propName() + ": Resource property maps to an invalid URL \"" + mapping.getLocation()
						+ "\" in toolkit " + mapping.getOwner().getName() + ": \"" + resource + "\"");
				}
			try {
				return QuickUtils.resolveURL(mapping.getOwner().getURI(), mapping.getLocation());
			} catch(QuickException e) {
				throw new QuickException(propName() + ": Resource property maps to a resource (" + mapping.getLocation()
					+ ") that cannot be resolved with respect to toolkit \"" + mapping.getOwner().getName() + "\"'s URL: \"" + resource
					+ "\"");
			}
		}

		@Override
		public boolean canCast(TypeToken<?> type) {
			return URL_TYPE.isAssignableFrom(type);
		}

		@Override
		public <X, V extends URL> V cast(TypeToken<X> type, X value) {
			if(value instanceof URL)
				return (V) value;
			else
				return null;
		}

		@Override
		public TypeToken<URL> getType() {
			return URL_TYPE;
		}

		@Override
		public boolean equals(Object o) {
			return o != null && o.getClass() == getClass();
		}

		@Override
		public int hashCode() {
			return getClass().hashCode();
		}

		@Override
		public String toString() {
			return "url";
		}
	};

	/**
	 * A simple validator that compares values to minimum and maximum values
	 *
	 * @param <T> The type of value to validate
	 */
	public static class ComparableValidator<T> extends AbstractPropertyValidator<T> {
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
				throw new QuickException(propName() + "Value must be at least " + theMin + ": " + value + " is invalid");
			if(theMax != null && theInternalCompare.compare(value, theMax) > 0)
				throw new QuickException(propName() + "Value must be at most " + theMax + ": " + value + " is invalid");
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
