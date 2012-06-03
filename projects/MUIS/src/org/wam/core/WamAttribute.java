package org.wam.core;

/**
 * A WamAttribute represents an option that may or must be specified in a WAM element either from
 * the document(XML) or from code. A WamAttribute must be created in java (prefereably in the form
 * of a static constant) and given to the element to tell it that the attribute is
 * {@link WamElement#acceptAttribute(WamAttribute) accepted} or
 * {@link WamElement#requireAttribute(WamAttribute) required}. This allows the element to properly
 * parse the attribute value specified in the document so that the option is available to java code
 * to properly interpret the value.
 * 
 * @param <T> The java type of the attribute
 */
public final class WamAttribute<T>
{
	/**
	 * A type of attribute that may be specified on an element
	 * 
	 * @param <T> The java type of the attribute
	 */
	public static interface AttributeType<T>
	{
		/**
		 * Validates the attribute
		 * 
		 * @param element The element on which the attribute has been set
		 * @param value The value set for the attribute
		 * @return null if the value is valid for this type; otherwise a message saying why the
		 *         value is invalid, e.g. "must be of type <type>"
		 */
		String validate(WamElement element, String value);

		/**
		 * Parses an attribute value from a string representation
		 * 
		 * @param element The element to parse the value for
		 * @param value The string representation to parse
		 * @return The parsed attribute value
		 * @throws WamException If the value cannot be parsed for any reason
		 */
		T parse(WamElement element, String value) throws WamException;

		/**
		 * Casts any object to an appropriate value of this type. This method may choose to convert
		 * liberally by creating new instances of this type corresponding to instances of other
		 * types, or it may choose to be conservative, only returning non-null for instances of this
		 * type.
		 * 
		 * @param value The value to cast
		 * @return An instance of this type whose value matches the parameter in some sense, or null
		 *         if the conversion cannot be made
		 */
		T cast(Object value);
	}

	/** A string attribute type--this type validates anything */
	public static final WamAttribute.AttributeType<String> stringAttr = new AttributeType<String>()
	{
		public String validate(WamElement element, String value)
		{
			return null;
		}

		public String parse(WamElement element, String value)
		{
			return value;
		}

		public String cast(Object value)
		{
			if(value instanceof String)
				return (String) value;
			else
				return null;
		}
	};

	/** A boolean attribute type--values must be either true or false */
	public static final WamAttribute.AttributeType<Boolean> boolAttr = new AttributeType<Boolean>()
	{
		public String validate(WamElement element, String value)
		{
			if(value.equals("true") || value.equals("false"))
				return null;
			return "must be a either \"true\" or \"false\": " + value + " is invalid";
		}

		public Boolean parse(WamElement element, String value) throws WamException
		{
			if(value == null)
				return null;
			else if("true".equals(value))
				return Boolean.TRUE;
			else if("false".equals(value))
				return Boolean.FALSE;
			else
				throw new WamException("Value " + value + " is not a boolean representation");
		}

		public Boolean cast(Object value)
		{
			if(value instanceof Boolean)
				return (Boolean) value;
			else
				return null;
		}
	};

	/** An integer attribute type--values must be valid integers */
	public static final WamAttribute.AttributeType<Long> intAttr = new AttributeType<Long>()
	{
		public String validate(WamElement element, String value)
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

		public Long parse(WamElement element, String value) throws WamException
		{
			try
			{
				return Long.valueOf(value);
			} catch(NumberFormatException e)
			{
				throw new WamException("Value " + value + " is not an integer representation", e);
			}
		}

		public Long cast(Object value)
		{
			if(value instanceof Long)
				return (Long) value;
			else if(value instanceof Integer || value instanceof Short || value instanceof Byte)
				return Long.valueOf(((Number) value).longValue());
			else
				return null;
		}
	};

	/** A floating-point attribute type--values must be valid real numbers */
	public static final WamAttribute.AttributeType<Double> floatAttr = new AttributeType<Double>()
	{
		public String validate(WamElement element, String value)
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

		public Double parse(WamElement element, String value) throws WamException
		{
			try
			{
				return Double.valueOf(value);
			} catch(NumberFormatException e)
			{
				throw new WamException("Value " + value
					+ " is not an floating-point representation", e);
			}
		}

		public Double cast(Object value)
		{
			if(value instanceof Double)
				return (Double) value;
			else if(value instanceof Float)
				return Double.valueOf(((Number) value).doubleValue());
			else
				return null;
		}
	};

	/**
	 * A WAM-type attribute type--values must be valid types mapped under WAM
	 * 
	 * @param <T> The subtype that the value must map to
	 */
	public static class WamTypeAttribute<T> implements AttributeType<Class<? extends T>>
	{
		/**
		 * The subtype that the value must map to
		 */
		public final Class<T> type;

		/**
		 * @param aType The subtype that the value must map to
		 */
		public WamTypeAttribute(Class<T> aType)
		{
			type = aType;
		}

		public String validate(WamElement element, String value)
		{
			WamToolkit toolkit = element.getClassView().getToolkitForQName(value);
			if(toolkit == null)
				return "must be a valid WAM type: no such toolkit to load " + value;
			String className = toolkit.getMappedClass(value);
			if(className == null)
				return "must be a valid WAM type: " + value + " is not mapped to a class";
			Class<?> valueClass;
			try
			{
				valueClass = toolkit.loadClass(className, null);
			} catch(WamException e)
			{
				return "must be a valid WAM type: " + value + " maps to " + className
					+ ", which could not be loaded: " + e.getMessage();
			}
			if(!type.isAssignableFrom(valueClass))
				return "must be a WAM subtype of " + type.getName() + ": " + value + " maps to "
					+ valueClass.getName();
			return null;
		}

		public Class<? extends T> parse(WamElement element, String value) throws WamException
		{
			if(value == null)
				return null;
			WamToolkit toolkit = element.getClassView().getToolkitForQName(value);
			if(toolkit == null)
				throw new WamException("Value " + value
					+ " refers to a toolkit that is inaccessible from its element");
			String className = toolkit.getMappedClass(value);
			if(className == null)
				throw new WamException("Value " + value
					+ " refers to a type that is not mapped within toolkit " + toolkit.getName());
			Class<?> valueClass;
			try
			{
				valueClass = toolkit.loadClass(className, null);
			} catch(WamException e)
			{
				throw new WamException("Value " + value + " refers to a type that failed to load",
					e);
			}
			if(!type.isAssignableFrom(valueClass))
				throw new WamException("Value " + value + " refers to a type ("
					+ valueClass.getName() + ") that is not a subtype of " + type);
			return (Class<? extends T>) valueClass;
		}

		public Class<? extends T> cast(Object value)
		{
			if(!(value instanceof Class<?>))
				return null;
			if(!type.isAssignableFrom((Class<?>) value))
				return null;
			return (Class<? extends T>) value;
		}
	}

	/** A WamTypeAttribute for a generic WamElement */
	public static final WamTypeAttribute<WamElement> elementTypeAttr = new WamTypeAttribute<WamElement>(
		WamElement.class);

	/**
	 * An enumeration attribute type--validates elements whose value matches any of the values given
	 * in the constructor
	 * 
	 * @param <T> The enumeration type
	 */
	public static final class WamEnumAttribute<T extends Enum<T>> implements AttributeType<T>
	{
		/** The enumeration that this attribute represents */
		public final Class<T> enumType;

		/**
		 * Creates an enumeration attribute from an enumerated type. The options will be all the
		 * type's constants in lower-case.
		 * 
		 * @param enumClass The enumerated type
		 */
		public WamEnumAttribute(Class<T> enumClass)
		{
			enumType = enumClass;
		}

		public String validate(WamElement element, String value)
		{
			T [] consts = enumType.getEnumConstants();
			for(T e : consts)
				if(e.name().equals(value))
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

		public T parse(WamElement element, String value) throws WamException
		{
			if(value == null)
				return null;
			T [] consts = enumType.getEnumConstants();
			for(T e : consts)
				if(e.name().equals(value))
					return e;
			throw new WamException("Value " + value
				+ " does not match any of the allowable values for type " + enumType.getName());
		}

		public T cast(Object value)
		{
			if(enumType.isInstance(value))
				return (T) value;
			else
				return null;
		}
	}

	/**
	 * Parses an enumerated value from a validated attribute value
	 * 
	 * @param <T> The type of the enumeration
	 * @param value The validated attribute value
	 * @param def The default to return if the value does not match any of the enumeration's
	 *        constants. This cannot be null.
	 * @return The parsed value
	 */
	public static <T extends Enum<T>> T parseEnum(String value, T def)
	{
		Enum<T> [] consts = def.getClass().getEnumConstants();
		for(Enum<T> val : consts)
			if(val.name().toLowerCase().equals(value))
				return (T) val;
		return def;
	}

	/** The name of the attribute */
	public final String name;

	/** The type of the attribute */
	public final AttributeType<T> type;

	/**
	 * Creates a new attribute for a WAM element
	 * 
	 * @param aName The name for the attribute
	 * @param aType The type for the attribute
	 */
	public WamAttribute(String aName, AttributeType<T> aType)
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
