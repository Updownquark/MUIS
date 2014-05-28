package org.muis.core;

import java.awt.Color;
import java.net.URL;
import java.util.Comparator;
import java.util.Map;

import org.muis.core.model.MuisModelValue;
import org.muis.util.MuisUtils;

/**
 * Represents a property in MUIS
 *
 * @param <T> The type of values that may be associated with the property
 */
public abstract class MuisProperty<T> {
	/**
	 * A property type understands how to produce items of a certain type from parseable strings and other types
	 *
	 * @param <T> The type of value that this property type produces TODO Get rid of all the V types
	 */
	public static interface PropertyType<T> {
		/**
		 * @param <V> The type of this property type's value
		 * @return The java type that this property type parses strings into instances of
		 */
		<V extends T> Class<V> getType();

		/**
		 * Parses an property value from a string representation
		 *
		 * @param <V> The type of value parsed by this property type
		 * @param env The parsing environment
		 * @param value The string representation to parse
		 * @return The parsed property value
		 * @throws MuisException If a fatal parsing error occurs
		 */
		<V extends T> V parse(MuisParseEnv env, String value) throws MuisException;

		/**
		 * Casts any object to an appropriate value of this type, or returns null if the given value cannot be interpreted as an instance of
		 * this property's type. This method may choose to convert liberally by creating new instances of this type corresponding to
		 * instances of other types, or it may choose to be conservative, only returning non-null for instances of this type.
		 *
		 * @param <V> The type of value cast by this property type
		 * @param value The value to cast
		 * @return An instance of this type whose value matches the parameter in some sense, or null if the conversion cannot be made
		 */
		<V extends T> V cast(Object value);
	}

	/**
	 * An extension of PropertyType that knows how to convert values of the type back to strings for printing
	 *
	 * @param <T> The type of the value that this property type produces
	 */
	public static interface PrintablePropertyType<T> extends PropertyType<T> {
		/**
		 * @param value The value to print
		 * @return The user-friendly printed value
		 */
		public String toString(T value);
	}

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
		 * @throws MuisException If the value was not valid by this validator's constraints. The message in this exception will be as
		 *             descriptive and user-friendly as possible.
		 */
		void assertValid(T value) throws MuisException;
	}

	/**
	 * An abstract class to help {@link MuisProperty.PropertyType}s and {@link MuisProperty.PropertyValidator}s generate better error
	 * messages
	 *
	 * @param <T> The type of property that this helper is for
	 */
	public static abstract class PropertyHelper<T> {
		private MuisProperty<T> theProperty;

		/** @return The property that this helper is for */
		public MuisProperty<T> getProperty() {
			return theProperty;
		}

		void setProperty(MuisProperty<T> property) {
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
	 * A property type that is helped by {@link MuisProperty.PropertyHelper}
	 *
	 * @param <T> The type of property that this type is for
	 */
	public static abstract class AbstractPropertyType<T> extends PropertyHelper<T> implements PropertyType<T> {
	}

	/**
	 * A property type that is helped by {@link MuisProperty.PropertyHelper}
	 *
	 * @param <T> The type of property that this validator is for
	 */
	public static abstract class AbstractPropertyValidator<T> extends PropertyHelper<T> implements PropertyValidator<T> {
	}

	private final String theName;

	private final PropertyType<T> theType;

	private final PropertyValidator<T> theValidator;

	/**
	 * @param name The name for the property
	 * @param type The type of the property
	 * @param validator The validator for the property
	 */
	protected MuisProperty(String name, PropertyType<T> type, PropertyValidator<T> validator) {
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
	protected MuisProperty(String name, PropertyType<T> type) {
		this(name, type, null);
	}

	/** @return This property's name */
	public String getName() {
		return theName;
	}

	/** @return This property's type */
	public PropertyType<T> getType() {
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
		MuisProperty<?> prop = (MuisProperty<?>) o;
		return prop.theName.equals(theName) && prop.theType.equals(theType);
	}

	@Override
	public int hashCode() {
		return theName.hashCode() * 13 + theType.hashCode() * 7 + (theValidator == null ? 0 : theValidator.hashCode());
	}

	private static abstract class AbstractPrintablePropertyType<T> extends AbstractPropertyType<T> implements PrintablePropertyType<T> {
	}

	/** A string property type--this type validates anything */
	public static final AbstractPropertyType<String> stringAttr = new AbstractPrintablePropertyType<String>() {
		@Override
		public String parse(MuisParseEnv env, String value) throws MuisException {
			if(env.getModelParser().getNextMVR(value, 0) == 0) {
				org.muis.core.model.MuisModelValue<? extends CharSequence> modelValue = (org.muis.core.model.MuisModelValue<? extends CharSequence>) env
					.getModelParser().parseMVR(value);
				if(modelValue.getType().canAssignTo(CharSequence.class))
					return modelValue.get().toString();
				else
					throw new MuisException("Model value " + value + " is not of type string");
			}
			return value;
		}

		@Override
		public String cast(Object value) {
			if(value instanceof String)
				return (String) value;
			else
				return null;
		}

		@Override
		public Class<String> getType() {
			return String.class;
		}

		@Override
		public String toString(String value) {
			return value;
		}

		@Override
		public String toString() {
			return "string";
		}
	};

	/** A boolean property type--values must be either true or false */
	public static final AbstractPropertyType<Boolean> boolAttr = new AbstractPropertyType<Boolean>() {
		@Override
		public Boolean parse(MuisParseEnv env, String value) throws MuisException {
			if(value == null)
				return null;
			if(env.getModelParser().getNextMVR(value, 0) == 0) {
				org.muis.core.model.MuisModelValue<Boolean> modelValue = (org.muis.core.model.MuisModelValue<Boolean>) env.getModelParser()
					.parseMVR(value);
				if(Boolean.class.equals(modelValue.getType()) || Boolean.TYPE.equals(modelValue.getType()))
					return modelValue.get();
				else
					throw new MuisException("Model value " + value + " is not compatible with float");
			}
			else if("true".equals(value))
				return Boolean.TRUE;
			else if("false".equals(value))
				return Boolean.FALSE;
			else
				throw new MuisException(propName() + ": Value " + value + " is not a boolean representation");
		}

		@Override
		public Boolean cast(Object value) {
			if(value instanceof Boolean)
				return (Boolean) value;
			else
				return null;
		}

		@Override
		public Class<Boolean> getType() {
			return Boolean.class;
		}

		@Override
		public String toString() {
			return "boolean";
		}
	};

	/** An integer property type--values must be valid integers */
	public static final AbstractPropertyType<Long> intAttr = new AbstractPropertyType<Long>() {
		@Override
		public Long parse(MuisParseEnv env, String value) throws MuisException {
			if(env.getModelParser().getNextMVR(value, 0) == 0) {
				org.muis.core.model.MuisModelValue<Number> modelValue = (org.muis.core.model.MuisModelValue<Number>) env.getModelParser()
					.parseMVR(value);
				if(Long.class.equals(modelValue.getType()) || Long.TYPE.equals(modelValue.getType())
					|| Integer.class.equals(modelValue.getType()) || Integer.TYPE.equals(modelValue.getType())
					|| Short.class.equals(modelValue.getType()) || Short.TYPE.equals(modelValue.getType())
					|| Byte.class.equals(modelValue.getType()) || Byte.TYPE.equals(modelValue.getType()))
					return modelValue.get().longValue();
				else
					throw new MuisException("Model value " + value + " is not compatible with int");
			}
			try {
				return Long.valueOf(value);
			} catch(NumberFormatException e) {
				throw new MuisException(propName() + ": Value " + value + " is not an integer representation", e);
			}
		}

		@Override
		public Long cast(Object value) {
			if(value instanceof Long)
				return (Long) value;
			else if(value instanceof Integer || value instanceof Short || value instanceof Byte)
				return Long.valueOf(((Number) value).longValue());
			else
				return null;
		}

		@Override
		public Class<Long> getType() {
			return Long.class;
		}

		@Override
		public String toString() {
			return "int";
		}
	};

	/** A floating-point property type--values must be valid real numbers */
	public static final AbstractPropertyType<Double> floatAttr = new AbstractPropertyType<Double>() {
		@Override
		public Double parse(MuisParseEnv env, String value) throws MuisException {
			if(env.getModelParser().getNextMVR(value, 0) == 0) {
				org.muis.core.model.MuisModelValue<Number> modelValue = (org.muis.core.model.MuisModelValue<Number>) env.getModelParser()
					.parseMVR(value);
				if(Double.class.equals(modelValue.getType()) || Double.TYPE.equals(modelValue.getType())
					|| Float.class.equals(modelValue.getType()) || Float.TYPE.equals(modelValue.getType()))
					return modelValue.get().doubleValue();
				else
					throw new MuisException("Model value " + value + " is not compatible with float");
			}
			try {
				return Double.valueOf(value);
			} catch(NumberFormatException e) {
				throw new MuisException(propName() + ": Value " + value + " is not an floating-point representation", e);
			}
		}

		@Override
		public Double cast(Object value) {
			if(value instanceof Double)
				return (Double) value;
			else if(value instanceof Float)
				return Double.valueOf(((Number) value).doubleValue());
			else
				return null;
		}

		@Override
		public Class<Double> getType() {
			return Double.class;
		}

		@Override
		public String toString() {
			return "float";
		}
	};

	/** Represents an amount, typically from 0 to 1. May also be given in percent. */
	public static final AbstractPropertyType<Double> amountAttr = new AbstractPropertyType<Double>() {
		@Override
		public Double parse(MuisParseEnv env, String value) throws MuisException {
			if(env.getModelParser().getNextMVR(value, 0) == 0) {
				org.muis.core.model.MuisModelValue<Number> modelValue = (org.muis.core.model.MuisModelValue<Number>) env.getModelParser()
					.parseMVR(value);
				if(Double.class.equals(modelValue.getType()) || Double.TYPE.equals(modelValue.getType())
					|| Float.class.equals(modelValue.getType()) || Float.TYPE.equals(modelValue.getType()))
					return modelValue.get().doubleValue();
				else
					throw new MuisException("Model value " + value + " is not compatible with amount");
			}
			boolean percent = value.endsWith("%");
			if(percent)
				value = value.substring(0, value.length() - 1);
			try {
				double ret = Double.valueOf(value);
				if(percent)
					ret /= 100;
				return ret;
			} catch(NumberFormatException e) {
				throw new MuisException(propName() + ": Value " + value + " is not an floating-point representation", e);
			}
		}

		@Override
		public Double cast(Object value) {
			if(value instanceof Double)
				return (Double) value;
			else if(value instanceof Float || value instanceof Long || value instanceof Integer || value instanceof Short
				|| value instanceof Byte)
				return Double.valueOf(((Number) value).doubleValue());
			else
				return null;
		}

		@Override
		public Class<Double> getType() {
			return Double.class;
		}

		@Override
		public String toString() {
			return "amount";
		}
	};

	/** Represents a time amount. The value is interpreted in milliseconds. */
	public static final AbstractPropertyType<Long> timeAttr = new AbstractPropertyType<Long>() {
		@Override
		public Class<Long> getType() {
			return Long.class;
		}

		@Override
		public Long parse(MuisParseEnv env, String value) throws MuisException {
			if(env.getModelParser().getNextMVR(value, 0) == 0) {
				org.muis.core.model.MuisModelValue<Long> modelValue = (org.muis.core.model.MuisModelValue<Long>) env.getModelParser()
					.parseMVR(value);
				if(Long.class.equals(modelValue.getType()) || Long.TYPE.equals(modelValue.getType()))
					return modelValue.get();
				else
					throw new MuisException("Model value " + value + " is not compatible with time");
			}
			long ret = 0;
			for(String s : value.trim().split("\\s")) {
				char unit = s.charAt(s.length() - 1);
				if(unit >= '0' && unit <= '9') {
					try {
						ret += Long.parseLong(s);
					} catch(NumberFormatException e) {
						throw new MuisException("Illegal time value: " + value, e);
					}
				} else {
					s = s.substring(0, s.length() - 1);
					if(s.length() == 0)
						throw new MuisException("No value in front of unit for time value: " + value);
					long t;
					try {
						t = Long.parseLong(s) * 1000;
					} catch(NumberFormatException e) {
						throw new MuisException("Illegal time value: " + value, e);
					}
					if(unit == 's') {
						ret += t * 1000;
					} else if(unit == 'm') {
						ret += t * 60000;
					} else if(unit == 'h') {
						ret += t * 60 * 60 * 1000;
					} else if(unit == 'd') {
						ret += t * 24 * 60 * 60 * 1000;
					} else
						throw new MuisException("Unrecognized unit '" + unit + "' in time value: " + value);
				}
			}
			return ret;
		}

		@Override
		public Long cast(Object value) {
			if(value instanceof Long || value instanceof Integer || value instanceof Short || value instanceof Byte)
				return ((Number) value).longValue();
			else if(value instanceof Double || value instanceof Float)
				return (long) Math.round(((Number) value).floatValue() * 1000);
			else
				return null;
		}

		@Override
		public String toString() {
			return "time";
		}
	};

	/** A color property type--values must be parse to colors via {@link org.muis.core.style.Colors#parseColor(String)} */
	public static final AbstractPropertyType<Color> colorAttr = new AbstractPrintablePropertyType<Color>() {
		@Override
		public Color parse(MuisParseEnv env, String value) throws MuisException {
			if(env.getModelParser().getNextMVR(value, 0) == 0) {
				org.muis.core.model.MuisModelValue<Color> modelValue = (org.muis.core.model.MuisModelValue<Color>) env.getModelParser()
					.parseMVR(value);
				if(modelValue.getType().canAssignTo(Color.class))
					return modelValue.get();
				else
					throw new MuisException("Model value " + value + " is not of type color");
			}
			try {
				return org.muis.core.style.Colors.parseColor(value);
			} catch(MuisException e) {
				throw new MuisException(propName() + ": Value " + value + " is not a recognized color or RGB value", e);
			}
		}

		@Override
		public Color cast(Object value) {
			if(value instanceof Color)
				return (Color) value;
			else
				return null;
		}

		@Override
		public Class<Color> getType() {
			return Color.class;
		}

		@Override
		public String toString(Color value) {
			return org.muis.core.style.Colors.toString(value);
		}

		@Override
		public String toString() {
			return "color";
		}
	};

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
		@Override
		public URL parse(MuisParseEnv env, String value) throws MuisException {
			int next = 0;
			while(next >= 0) {
				next = env.getModelParser().getNextMVR(value, next);
				if(next >= 0) {
					String extracted = env.getModelParser().extractMVR(value, next);
					org.muis.core.model.MuisModelValue<String> mv = (MuisModelValue<String>) env.getModelParser().parseMVR(extracted);
					if(!mv.getType().canAssignTo(String.class))
						throw new MuisException("Resource attributes accept properties of type String only");
					String v = mv.get();
					value = value.substring(0, next) + mv.get() + value.substring(next + extracted.length());
					next += v.length();
				}
			}
			int sepIdx = value.indexOf(':');
			String namespace = sepIdx < 0 ? null : value.substring(0, sepIdx);
			String content = sepIdx < 0 ? value : value.substring(sepIdx + 1);
			MuisToolkit toolkit = env.cv().getToolkit(namespace);
			if(toolkit == null)
				try {
					return new URL(value);
				} catch(java.net.MalformedURLException e) {
					throw new MuisException(propName() + ": Resource property is not a valid URL: \"" + value + "\"", e);
				}
			ResourceMapping mapping = toolkit.getMappedResource(content);
			if(mapping == null)
				throw new MuisException(propName() + ": Resource property must map to a declared resource: \"" + value + "\" in toolkit "
					+ toolkit.getName() + " or one of its dependencies");
			if(mapping.getLocation().contains(":"))
				try {
					return new URL(mapping.getLocation());
				} catch(java.net.MalformedURLException e) {
					throw new MuisException(propName() + ": Resource property maps to an invalid URL \"" + mapping.getLocation()
						+ "\" in toolkit " + mapping.getOwner().getName() + ": \"" + value + "\"");
				}
			try {
				return MuisUtils.resolveURL(mapping.getOwner().getURI(), mapping.getLocation());
			} catch(MuisException e) {
				throw new MuisException(propName() + ": Resource property maps to a resource (" + mapping.getLocation()
					+ ") that cannot be resolved with respect to toolkit \"" + mapping.getOwner().getName() + "\"'s URL: \"" + value + "\"");
			}
		}

		@Override
		public URL cast(Object value) {
			if(value instanceof URL)
				return (URL) value;
			else
				return null;
		}

		@Override
		public Class<URL> getType() {
			return URL.class;
		}

		@Override
		public boolean equals(Object o) {
			return o != null && o.getClass() == getClass();
		}

		@Override
		public int hashCode() {
			return getClass().hashCode();
		}
	};

	/**
	 * A MUIS-type property type--values must be valid types mapped under MUIS
	 *
	 * @param <T> The subtype that the value must map to
	 */
	public static class MuisTypeProperty<T> extends AbstractPropertyType<Class<T>> {
		/** The subtype that the value must map to */
		public final Class<T> type;

		/** @param aType The subtype that the value must map to */
		public MuisTypeProperty(Class<T> aType) {
			type = aType;
		}

		@Override
		public Class<? extends T> parse(MuisParseEnv env, String value) throws MuisException {
			if(value == null)
				return null;
			int sep = value.indexOf(':');
			String ns, tag;
			if(sep >= 0) {
				ns = value.substring(0, sep);
				tag = value.substring(sep + 1);
			} else {
				ns = null;
				tag = value;
			}
			MuisToolkit toolkit = null;
			String className = null;
			if(ns != null) {
				toolkit = env.cv().getToolkit(ns);
				if(toolkit == null)
					throw new MuisException(propName() + ": Value " + value + " refers to a toolkit \"" + ns
						+ "\" that is inaccessible from its element");
				className = toolkit.getMappedClass(tag);
				if(className == null)
					throw new MuisException(propName() + ": Value " + value + " refers to a type \"" + tag
						+ "\" that is not mapped within toolkit " + toolkit.getName());
			} else {
				for(MuisToolkit tk : env.cv().getScopedToolkits()) {
					className = tk.getMappedClass(tag);
					if(className != null) {
						toolkit = tk;
						break;
					}
				}
				if(className == null) {
					throw new MuisException(propName() + ": Value " + value + " refers to a type \"" + tag
						+ "\" that is not mapped within a scoped toolkit");
				}
			}
			Class<?> valueClass;
			try {
				valueClass = toolkit.loadClass(className, null);
			} catch(MuisException e) {
				throw new MuisException(propName() + ": Value " + value + " refers to a type that failed to load", e);
			}
			if(!type.isAssignableFrom(valueClass))
				throw new MuisException(propName() + ": Value " + value + " refers to a type (" + valueClass.getName()
					+ ") that is not a subtype of " + type);
			return (Class<? extends T>) valueClass;
		}

		@Override
		public Class<? extends T> cast(Object value) {
			if(!(value instanceof Class<?>))
				return null;
			if(!type.isAssignableFrom((Class<?>) value))
				return null;
			return (Class<? extends T>) value;
		}

		@Override
		public String toString() {
			return type.isPrimitive() ? type.getSimpleName() : type.getName();
		}

		@Override
		public Class<Class<? extends T>> getType() {
			return (Class<Class<? extends T>>) type.getClass();
		}

		@Override
		public boolean equals(Object o) {
			return o != null && o.getClass() == getClass() && ((MuisTypeProperty<?>) o).type.equals(type);
		}

		@Override
		public int hashCode() {
			return getClass().hashCode() * 7 + type.hashCode();
		}
	}

	/**
	 * An property type that creates actual instances of a specified type
	 *
	 * @param <T> The type of value to create
	 */
	public static final class MuisTypeInstanceProperty<T> extends AbstractPropertyType<T> {
		/** The subtype that the value must map to */
		private final MuisTypeProperty<? extends T> theTypeProperty;

		/** @param aType The subtype that the value must map to */
		public MuisTypeInstanceProperty(Class<? extends T> aType) {
			theTypeProperty = new MuisTypeProperty<>(aType);
		}

		@Override
		public <V extends T> V parse(MuisParseEnv env, String value) throws MuisException {
			if(value == null)
				return null;
			if(env.getModelParser().getNextMVR(value, 0) == 0) {
				org.muis.core.model.MuisModelValue<V> modelValue = (org.muis.core.model.MuisModelValue<V>) env.getModelParser().parseMVR(
					value);
				if(modelValue.getType().canAssignTo(theTypeProperty.type))
					return modelValue.get();
				else
					throw new MuisException("Model value " + value + " is not of type " + theTypeProperty.type.getSimpleName());
			}
			Class<V> valueClass = (Class<V>) theTypeProperty.parse(env, value);
			try {
				return valueClass.newInstance();
			} catch(InstantiationException e) {
				throw new MuisException(propName() + ": Could not instantiate type " + valueClass.getName(), e);
			} catch(IllegalAccessException e) {
				throw new MuisException(propName() + ": Could not access default constructor of type " + valueClass.getName(), e);
			}
		}

		@Override
		public <V extends T> V cast(Object value) {
			if(!getType().isInstance(value))
				return null;
			return (V) getType().cast(value);
		}

		@Override
		public String toString() {
			return getType().isPrimitive() ? getType().getSimpleName() : getType().getName();
		}

		@Override
		public <V extends T> Class<V> getType() {
			return (Class<V>) theTypeProperty.type;
		}

		@Override
		public boolean equals(Object o) {
			return o != null && o.getClass() == getClass() && ((MuisTypeInstanceProperty<?>) o).getType().equals(getType());
		}

		@Override
		public int hashCode() {
			return getClass().hashCode() * 7 + getType().hashCode();
		}
	}

	/** A MuisTypeProperty for a generic MuisElement */
	public static final MuisTypeProperty<MuisElement> elementTypeProp = new MuisTypeProperty<>(MuisElement.class);

	/**
	 * An enumeration property type--validates elements whose value matches any of the values given in the constructor
	 *
	 * @param <T> The enumeration type
	 */
	public static final class MuisEnumProperty<T extends Enum<T>> extends AbstractPropertyType<T> {
		/** The enumeration that this property represents */
		public final Class<T> enumType;

		/** Whether all values in this enum are unique case-insensitively. If this is true, property values are case-insensitive. */
		public final boolean ciUnique;

		/**
		 * Creates an enumeration property from an enumerated type. The options will be all the type's constants in lower-case.
		 *
		 * @param enumClass The enumerated type
		 */
		public MuisEnumProperty(Class<T> enumClass) {
			enumType = enumClass;
			java.util.HashSet<String> values = new java.util.HashSet<>();
			boolean unique = true;
			for(T value : enumType.getEnumConstants())
				if(!values.add(value.name().toLowerCase())) {
					unique = false;
					break;
				}
			ciUnique = unique;
		}

		@Override
		public T parse(MuisParseEnv env, String value) throws MuisException {
			if(value == null)
				return null;
			if(env.getModelParser().getNextMVR(value, 0) == 0) {
				org.muis.core.model.MuisModelValue<T> modelValue = (org.muis.core.model.MuisModelValue<T>) env.getModelParser().parseMVR(
					value);
				if(modelValue.getType().canAssignTo(enumType))
					return modelValue.get();
				else
					throw new MuisException("Model value " + value + " is not of type " + enumType.getSimpleName());
			}
			T [] consts = enumType.getEnumConstants();
			for(T e : consts)
				if(ciUnique) {
					if(e.name().equalsIgnoreCase(value))
						return e;
				} else if(e.name().equals(value))
					return e;
			throw new MuisException(propName() + ": Value " + value + " does not match any of the allowable values for type "
				+ enumType.getName());
		}

		@Override
		public T cast(Object value) {
			if(enumType.isInstance(value))
				return (T) value;
			else
				return null;
		}

		@Override
		public Class<T> getType() {
			return enumType;
		}

		@Override
		public boolean equals(Object o) {
			return o != null && o.getClass() == getClass() && ((MuisEnumProperty<?>) o).enumType.equals(enumType);
		}

		@Override
		public int hashCode() {
			return getClass().hashCode() * 7 + enumType.hashCode();
		}
	}

	/**
	 * Wraps another property type, allowing the user to specify predefined values in addition to normal, parseable values
	 *
	 * @param <T> The type of the property
	 */
	public static class NamedValuePropertyType<T> extends AbstractPropertyType<T> {
		private final PropertyType<T> theWrapped;

		private final Map<String, T> theNamedValues;

		private final int theHashCode;

		/**
		 * @param wrap The property type to wrap
		 * @param namedValues Name-value pairs of values that can be specified by name
		 */
		public NamedValuePropertyType(PropertyType<T> wrap, Object... namedValues) {
			this(wrap, compileNamedValues(namedValues, wrap.getType()));
		}

		/**
		 * @param wrap The property type to wrap
		 * @param namedValues Name-value pairs of values that can be specified by name
		 */
		public NamedValuePropertyType(PropertyType<T> wrap, Map<String, T> namedValues) {
			theWrapped = wrap;
			checkTypes(namedValues, wrap.getType());
			java.util.HashMap<String, T> copy = new java.util.HashMap<>(namedValues);
			theNamedValues = java.util.Collections.unmodifiableMap(copy);
			theHashCode = theNamedValues.hashCode();
		}

		private static <T> Map<String, T> compileNamedValues(Object [] nv, Class<T> type) {
			if(nv == null || nv.length == 0)
				return null;
			if(nv.length % 2 != 0)
				throw new IllegalArgumentException("Named values must be pairs in the form name, " + type.getSimpleName() + ", name, "
					+ type.getSimpleName() + "...");
			java.util.HashMap<String, T> ret = new java.util.HashMap<>();
			for(int i = 0; i < nv.length; i += 2) {
				if(!(nv[i] instanceof String) || !type.isInstance(nv[i + 1]))
					throw new IllegalArgumentException("Named values must be pairs in the form name, " + type.getSimpleName() + ", name, "
						+ type.getSimpleName() + "...");
				if(ret.containsKey(nv[i]))
					throw new IllegalArgumentException("Named value \"" + nv[i] + "\" specified multiple times");
				ret.put((String) nv[i], type.cast(nv[i + 1]));
			}
			return ret;
		}

		private static <T> void checkTypes(Map<String, T> map, Class<T> type) {
			if(map == null)
				return;
			for(Map.Entry<?, ?> entry : map.entrySet())
				if(!(entry.getKey() instanceof String) || !type.isInstance(entry.getValue()))
					throw new IllegalArgumentException("name-value pairs must be typed String, " + type.getSimpleName());
		}

		@Override
		public <V extends T> Class<V> getType() {
			return theWrapped.getType();
		}

		@Override
		public <V extends T> V parse(MuisParseEnv env, String value) throws MuisException {
			if(env.getModelParser().getNextMVR(value, 0) == 0) {
				org.muis.core.rx.ObservableValue<?> modelValue = env.getModelParser().parseMVR(value);
				if(modelValue.getType().canAssignTo(String.class)) {
					String mv = (String) modelValue.get();
					if(theNamedValues.containsKey(mv))
						return (V) theNamedValues.get(mv);
					else
						throw new MuisException("Model value " + value + " does not match a predefined value.");
				} else if(modelValue.getType().canAssignTo(theWrapped.getType())) {
					return (V) modelValue.get();
				} else
					throw new MuisException("Model value " + value + " is not of type " + theWrapped.getType().getSimpleName());
			}
			if(theNamedValues.containsKey(value))
				return (V) theNamedValues.get(value);
			return theWrapped.parse(env, value);
		}

		@Override
		public <V extends T> V cast(Object value) {
			return theWrapped.cast(value);
		}

		@Override
		public boolean equals(Object o) {
			if(o.getClass() != getClass())
				return false;
			NamedValuePropertyType<?> nvpt = (NamedValuePropertyType<?>) o;
			return theWrapped.equals(nvpt.theWrapped) && theNamedValues.equals(nvpt.theNamedValues);
		}

		@Override
		public int hashCode() {
			return theHashCode;
		}
	}

	/**
	 * A simple validator that compares values to minimum and maximum values
	 *
	 * @param <T> The type of value to validate
	 */
	public static class ComparableValidator<T> extends AbstractPropertyValidator<T> {
		private final Comparator<T> theCompare;
		private final Comparator<T> theInternalCompare;

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
		public ComparableValidator(T min, T max, Comparator<T> compare) {
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
		public Comparator<T> getCompare() {
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
		public void assertValid(T value) throws MuisException {
			if(theMin != null && theInternalCompare.compare(theMin, value) > 0)
				throw new MuisException(propName() + "Value must be at least " + theMin + ": " + value + " is invalid");
			if(theMax != null && theInternalCompare.compare(value, theMax) > 0)
				throw new MuisException(propName() + "Value must be at most " + theMax + ": " + value + " is invalid");
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
