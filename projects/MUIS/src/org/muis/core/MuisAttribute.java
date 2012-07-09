package org.muis.core;

import java.awt.Color;
import java.net.URL;

/**
 * A MuisAttribute represents an option that may or must be specified in a MUIS element either from the document(XML) or from code. A
 * MuisAttribute must be created in java (preferably in the form of a static constant) and given to the element to tell it that the
 * attribute is {@link MuisElement#acceptAttribute(MuisAttribute) accepted} or {@link MuisElement#requireAttribute(MuisAttribute) required}.
 * This allows the element to properly parse the attribute value specified in the document so that the option is available to java code to
 * properly interpret the value.
 *
 * @param <T> The java type of the attribute
 */
public final class MuisAttribute<T>
{
	/**
	 * A type of attribute that may be specified on an element
	 *
	 * @param <T> The java type of the attribute
	 */
	public static interface AttributeType<T>
	{
		/** @return The java type that this attribute type parses strings into instances of */
		Class<T> getType();

		/**
		 * Validates the attribute
		 *
		 * @param classView The class view to use in parsing, if needed
		 * @param value The value set for the attribute
		 * @return null if the value is valid for this type; otherwise a message saying why the value is invalid, e.g.
		 *         "must be of type <type>"
		 */
		String validate(MuisClassView classView, String value);

		/**
		 * Parses an attribute value from a string representation
		 *
		 * @param classView The class view to use in parsing, if needed
		 * @param value The string representation to parse
		 * @return The parsed attribute value
		 * @throws MuisException If the value cannot be parsed for any reason
		 */
		T parse(MuisClassView classView, String value) throws MuisException;

		/**
		 * Casts any object to an appropriate value of this type, or returns null if the given value cannot be interpreted as an instance of
		 * this attribute's type. This method may choose to convert liberally by creating new instances of this type corresponding to
		 * instances of other types, or it may choose to be conservative, only returning non-null for instances of this type.
		 *
		 * @param value The value to cast
		 * @return An instance of this type whose value matches the parameter in some sense, or null if the conversion cannot be made
		 */
		T cast(Object value);
	}

	/** A string attribute type--this type validates anything */
	public static final AttributeType<String> stringAttr = new AttributeType<String>() {
		@Override
		public String validate(MuisClassView classView, String value)
		{
			return null;
		}

		@Override
		public String parse(MuisClassView classView, String value)
		{
			return value;
		}

		@Override
		public String cast(Object value)
		{
			if(value instanceof String)
				return (String) value;
			else
				return null;
		}

		@Override
		public Class<String> getType()
		{
			return String.class;
		}
	};

	/** A boolean attribute type--values must be either true or false */
	public static final AttributeType<Boolean> boolAttr = new AttributeType<Boolean>() {
		@Override
		public String validate(MuisClassView classView, String value)
		{
			if(value.equals("true") || value.equals("false"))
				return null;
			return "must be a either \"true\" or \"false\": " + value + " is invalid";
		}

		@Override
		public Boolean parse(MuisClassView classView, String value) throws MuisException
		{
			if(value == null)
				return null;
			else if("true".equals(value))
				return Boolean.TRUE;
			else if("false".equals(value))
				return Boolean.FALSE;
			else
				throw new MuisException("Value " + value + " is not a boolean representation");
		}

		@Override
		public Boolean cast(Object value)
		{
			if(value instanceof Boolean)
				return (Boolean) value;
			else
				return null;
		}

		@Override
		public Class<Boolean> getType()
		{
			return Boolean.class;
		}
	};

	/** An integer attribute type--values must be valid integers */
	public static final AttributeType<Long> intAttr = new AttributeType<Long>() {
		@Override
		public String validate(MuisClassView classView, String value)
		{
			try
			{
				Long.parseLong(value);
				return null;
			} catch(NumberFormatException e)
			{
				return "must be a valid integral value: " + value + " is invalid";
			}
		}

		@Override
		public Long parse(MuisClassView classView, String value) throws MuisException
		{
			try
			{
				return Long.valueOf(value);
			} catch(NumberFormatException e)
			{
				throw new MuisException("Value " + value + " is not an integer representation", e);
			}
		}

		@Override
		public Long cast(Object value)
		{
			if(value instanceof Long)
				return (Long) value;
			else if(value instanceof Integer || value instanceof Short || value instanceof Byte)
				return Long.valueOf(((Number) value).longValue());
			else
				return null;
		}

		@Override
		public Class<Long> getType()
		{
			return Long.class;
		}
	};

	/** A floating-point attribute type--values must be valid real numbers */
	public static final AttributeType<Double> floatAttr = new AttributeType<Double>() {
		@Override
		public String validate(MuisClassView classView, String value)
		{
			try
			{
				Double.parseDouble(value);
				return null;
			} catch(NumberFormatException e)
			{
				return "must be a valid floating-point value: " + value + " is invalid";
			}
		}

		@Override
		public Double parse(MuisClassView classView, String value) throws MuisException
		{
			try
			{
				return Double.valueOf(value);
			} catch(NumberFormatException e)
			{
				throw new MuisException("Value " + value + " is not an floating-point representation", e);
			}
		}

		@Override
		public Double cast(Object value)
		{
			if(value instanceof Double)
				return (Double) value;
			else if(value instanceof Float)
				return Double.valueOf(((Number) value).doubleValue());
			else
				return null;
		}

		@Override
		public Class<Double> getType()
		{
			return Double.class;
		}
	};

	/** Represents an amount, typically from 0 to 1. May also be given in percent. */
	public static final AttributeType<Double> amountAttr = new AttributeType<Double>() {
		@Override
		public String validate(MuisClassView classView, String value)
		{
			boolean percent = value.endsWith("%");
			if(percent)
				value = value.substring(0, value.length() - 1);
			try
			{
				Double.parseDouble(value);
				return null;
			} catch(NumberFormatException e)
			{
				return "must be a valid floating-point value: " + value + " is invalid";
			}
		}

		@Override
		public Double parse(MuisClassView classView, String value) throws MuisException
		{
			boolean percent = value.endsWith("%");
			if(percent)
				value = value.substring(0, value.length() - 1);
			try
			{
				double ret = Double.valueOf(value);
				if(percent)
					ret /= 100;
				return ret;
			} catch(NumberFormatException e)
			{
				throw new MuisException("Value " + value + " is not an floating-point representation", e);
			}
		}

		@Override
		public Double cast(Object value)
		{
			if(value instanceof Double)
				return (Double) value;
			else if(value instanceof Float)
				return Double.valueOf(((Number) value).doubleValue());
			else
				return null;
		}

		@Override
		public Class<Double> getType()
		{
			return Double.class;
		}
	};

	/** A color attribute type--values must be parse to colors via {@link org.muis.core.style.Colors#parseColor(String)} */
	public static final AttributeType<Color> colorAttr = new AttributeType<Color>() {
		@Override
		public String validate(MuisClassView classView, String value)
		{
			try
			{
				org.muis.core.style.Colors.parseColor(value);
				return null;
			} catch(MuisException e)
			{
				return "must be a recognized color name or an RGB value in an accepted format: " + value + " is invalid";
			}
		}

		@Override
		public Color parse(MuisClassView classView, String value) throws MuisException
		{
			try
			{
				return org.muis.core.style.Colors.parseColor(value);
			} catch(MuisException e)
			{
				throw new MuisException("Value " + value + " is not a recognized color or RGB value", e);
			}
		}

		@Override
		public Color cast(Object value)
		{
			if(value instanceof Color)
				return (Color) value;
			else
				return null;
		}

		@Override
		public Class<Color> getType()
		{
			return Color.class;
		}
	};

	/**
	 * A resource attribute--values may be:
	 * <ul>
	 * <li>namespace:tag, where <code>tag</code> maps to a resource in the <code>namespace</code> toolkit</li>
	 * <li>A relative path to a resource that may be resolved from the element's toolkit. A <code>namespace:</code> prefix may be used to
	 * specify a different toolkit</li>
	 * <li>An absolute URL. Permissions will be checked before resources at any external URLs are retrieved. TODO cite specific permission.</li>
	 * </ul>
	 */
	public static final AttributeType<URL> resourceAttr = new AttributeType<java.net.URL>() {
		@Override
		public String validate(MuisClassView classView, String value)
		{
			try
			{
				parse(classView, value);
			} catch(MuisException e)
			{
				return e.getMessage().substring("Resource attribute ".length());
			}
			return null;
		}

		@Override
		public URL parse(MuisClassView classView, String value) throws MuisException
		{
			int sepIdx = value.indexOf(':');
			String namespace = sepIdx < 0 ? null : value.substring(0, sepIdx);
			String content = sepIdx < 0 ? value : value.substring(sepIdx + 1);
			MuisToolkit toolkit = classView.getToolkit(namespace);
			if(toolkit == null)
				try
				{
					return new URL(value);
				} catch(java.net.MalformedURLException e)
				{
					throw new MuisException("Resource attribute is not a valid URL: \"" + value + "\"", e);
				}
			ResourceMapping mapping = toolkit.getMappedResource(content);
			if(mapping == null)
				throw new MuisException("Resource attribute must map to a declared resource: \"" + value + "\" in toolkit "
					+ toolkit.getName() + " or one of its dependencies");
			if(mapping.getLocation().contains(":"))
				try
				{
					return new URL(mapping.getLocation());
				} catch(java.net.MalformedURLException e)
				{
					throw new MuisException("Resource attribute maps to an invalid URL \"" + mapping.getLocation() + "\" in toolkit "
						+ mapping.getOwner().getName() + ": \"" + value + "\"");
				}
			try
			{
				return MuisUtils.resolveURL(mapping.getOwner().getURI(), mapping.getLocation());
			} catch(MuisException e)
			{
				throw new MuisException("Resource attribute maps to a resource (" + mapping.getLocation()
					+ ") that cannot be resolved with respect to toolkit \"" + mapping.getOwner().getName() + "\"'s URL: \"" + value + "\"");
			}
		}

		@Override
		public URL cast(Object value)
		{
			if(value instanceof URL)
				return (URL) value;
			else
				return null;
		}

		@Override
		public Class<URL> getType()
		{
			return URL.class;
		}
	};

	/**
	 * A MUIS-type attribute type--values must be valid types mapped under MUIS
	 *
	 * @param <T> The subtype that the value must map to
	 */
	public static class MuisTypeAttribute<T> implements AttributeType<Class<? extends T>>
	{
		/** The subtype that the value must map to */
		public final Class<T> type;

		/** @param aType The subtype that the value must map to */
		public MuisTypeAttribute(Class<T> aType)
		{
			type = aType;
		}

		@Override
		public String validate(MuisClassView classView, String value)
		{
			MuisToolkit toolkit = classView.getToolkitForQName(value);
			if(toolkit == null)
				return "must be a valid MUIS type: no such toolkit to load " + value;
			String className = toolkit.getMappedClass(value);
			if(className == null)
				return "must be a valid MUIS type: " + value + " is not mapped to a class";
			Class<?> valueClass;
			try
			{
				valueClass = toolkit.loadClass(className, null);
			} catch(MuisException e)
			{
				return "must be a valid MUIS type: " + value + " maps to " + className + ", which could not be loaded: " + e.getMessage();
			}
			if(!type.isAssignableFrom(valueClass))
				return "must be a MUIS subtype of " + type.getName() + ": " + value + " maps to " + valueClass.getName();
			return null;
		}

		@Override
		public Class<? extends T> parse(MuisClassView classView, String value) throws MuisException
		{
			if(value == null)
				return null;
			int sep = value.indexOf(':');
			String ns, tag;
			if(sep >= 0)
			{
				ns = value.substring(0, sep);
				tag = value.substring(sep + 1);
			}
			else
			{
				ns = null;
				tag = value;
			}
			MuisToolkit toolkit = classView.getToolkit(ns);
			if(toolkit == null)
				throw new MuisException("Value " + value + " refers to a toolkit \"" + ns + "\" that is inaccessible from its element");
			String className = toolkit.getMappedClass(tag);
			if(className == null)
				throw new MuisException("Value " + value + " refers to a type \"" + tag + "\" that is not mapped within toolkit "
					+ toolkit.getName());
			Class<?> valueClass;
			try
			{
				valueClass = toolkit.loadClass(className, null);
			} catch(MuisException e)
			{
				throw new MuisException("Value " + value + " refers to a type that failed to load", e);
			}
			if(!type.isAssignableFrom(valueClass))
				throw new MuisException("Value " + value + " refers to a type (" + valueClass.getName() + ") that is not a subtype of "
					+ type);
			return (Class<? extends T>) valueClass;
		}

		@Override
		public Class<? extends T> cast(Object value)
		{
			if(!(value instanceof Class<?>))
				return null;
			if(!type.isAssignableFrom((Class<?>) value))
				return null;
			return (Class<? extends T>) value;
		}

		@Override
		public String toString()
		{
			return type.isPrimitive() ? type.getSimpleName() : type.getName();
		}

		@Override
		public Class<Class<? extends T>> getType()
		{
			return (Class<Class<? extends T>>) type.getClass();
		}
	}

	/**
	 * An attribute type that creates actual instances of a specified type
	 *
	 * @param <T> The type of value to create
	 */
	public static final class MuisTypeInstanceAttribute<T> implements AttributeType<T>
	{
		/** The subtype that the value must map to */
		public final Class<T> type;

		/** @param aType The subtype that the value must map to */
		public MuisTypeInstanceAttribute(Class<T> aType)
		{
			type = aType;
		}

		@Override
		public String validate(MuisClassView classView, String value)
		{
			MuisToolkit toolkit = classView.getToolkitForQName(value);
			if(toolkit == null)
				return "must be a valid MUIS type: no such toolkit to load " + value;
			String className = toolkit.getMappedClass(value);
			if(className == null)
				return "must be a valid MUIS type: " + value + " is not mapped to a class";
			Class<?> valueClass;
			try
			{
				valueClass = toolkit.loadClass(className, null);
			} catch(MuisException e)
			{
				return "must be a valid MUIS type: " + value + " maps to " + className + ", which could not be loaded: " + e.getMessage();
			}
			if(!type.isAssignableFrom(valueClass))
				return "must be a MUIS subtype of " + type.getName() + ": " + value + " maps to " + valueClass.getName();
			try
			{
				valueClass.getDeclaredConstructor();
			} catch(NoSuchMethodException e)
			{
				return "must be a type with a default constructor: " + value + " maps to " + valueClass.getName();
			}
			return null;
		}

		@Override
		public T parse(MuisClassView classView, String value) throws MuisException
		{
			if(value == null)
				return null;
			int sep = value.indexOf(':');
			String ns, tag;
			if(sep >= 0)
			{
				ns = value.substring(0, sep);
				tag = value.substring(sep + 1);
			}
			else
			{
				ns = null;
				tag = value;
			}
			MuisToolkit toolkit = classView.getToolkit(ns);
			if(toolkit == null)
				throw new MuisException("Value " + value + " refers to a toolkit \"" + ns + "\" that is inaccessible from its element");
			String className = toolkit.getMappedClass(tag);
			if(className == null)
				throw new MuisException("Value " + value + " refers to a type \"" + tag + "\" that is not mapped within toolkit "
					+ toolkit.getName());
			Class<?> valueClass;
			try
			{
				valueClass = toolkit.loadClass(className, null);
			} catch(MuisException e)
			{
				throw new MuisException("Value " + value + " refers to a type that failed to load", e);
			}
			if(!type.isAssignableFrom(valueClass))
				throw new MuisException("Value " + value + " refers to a type (" + valueClass.getName() + ") that is not a subtype of "
					+ type);
			try
			{
				return (T) valueClass.newInstance();
			} catch(InstantiationException e)
			{
				throw new MuisException("Could not instantiate type " + valueClass.getName(), e);
			} catch(IllegalAccessException e)
			{
				throw new MuisException("Could not access default constructor of type " + valueClass.getName(), e);
			}
		}

		@Override
		public T cast(Object value)
		{
			if(!type.isInstance(value))
				return null;
			return type.cast(value);
		}

		@Override
		public String toString()
		{
			return type.isPrimitive() ? type.getSimpleName() : type.getName();
		}

		@Override
		public Class<T> getType()
		{
			return type;
		}
	}

	/** A MuisTypeAttribute for a generic MuisElement */
	public static final MuisTypeAttribute<MuisElement> elementTypeAttr = new MuisTypeAttribute<MuisElement>(MuisElement.class);

	/**
	 * An enumeration attribute type--validates elements whose value matches any of the values given in the constructor
	 *
	 * @param <T> The enumeration type
	 */
	public static final class MuisEnumAttribute<T extends Enum<T>> implements AttributeType<T>
	{
		/** The enumeration that this attribute represents */
		public final Class<T> enumType;

		/** Whether all values in this enum are unique case-insensitively. If this is true, attribute values are case-insensitive. */
		public final boolean ciUnique;

		/**
		 * Creates an enumeration attribute from an enumerated type. The options will be all the type's constants in lower-case.
		 *
		 * @param enumClass The enumerated type
		 */
		public MuisEnumAttribute(Class<T> enumClass)
		{
			enumType = enumClass;
			java.util.HashSet<String> values = new java.util.HashSet<>();
			boolean unique = true;
			for(T value : enumType.getEnumConstants())
				if(!values.add(value.name().toLowerCase()))
				{
					unique = false;
					break;
				}
			ciUnique = unique;
		}

		@Override
		public String validate(MuisClassView classView, String value)
		{
			if(value == null)
				return null;
			T [] consts = enumType.getEnumConstants();
			for(T e : consts)
				if(ciUnique)
				{
					if(e.name().equalsIgnoreCase(value))
						return null;
				}
				else if(e.name().equals(value))
					return null;
			if(consts.length <= 5)
			{
				StringBuilder ret = new StringBuilder("must be one of [");
				for(int i = 0; i < consts.length; i++)
				{
					ret.append(consts[i].name());
					if(i < consts.length - 1)
						ret.append(',');
				}
				return ret.toString();
			}
			else
				return "does not match any of the allowable values";
		}

		@Override
		public T parse(MuisClassView classView, String value) throws MuisException
		{
			if(value == null)
				return null;
			T [] consts = enumType.getEnumConstants();
			for(T e : consts)
				if(ciUnique)
				{
					if(e.name().equalsIgnoreCase(value))
						return e;
				}
				else if(e.name().equals(value))
					return e;
			throw new MuisException("Value " + value + " does not match any of the allowable values for type " + enumType.getName());
		}

		@Override
		public T cast(Object value)
		{
			if(enumType.isInstance(value))
				return (T) value;
			else
				return null;
		}

		@Override
		public Class<T> getType()
		{
			return enumType;
		}
	}

	/** The name of the attribute */
	public final String name;

	/** The type of the attribute */
	public final AttributeType<T> type;

	/**
	 * Creates a new attribute for a MUIS element
	 *
	 * @param aName The name for the attribute
	 * @param aType The type for the attribute
	 */
	public MuisAttribute(String aName, AttributeType<T> aType)
	{
		name = aName;
		type = aType;
	}

	@Override
	public String toString()
	{
		return name + "(" + type + ")";
	}
}
