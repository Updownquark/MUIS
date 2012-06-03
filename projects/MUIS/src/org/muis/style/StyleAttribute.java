package org.muis.style;

import org.muis.core.MuisException;

/**
 * A style property that can affect the rendering of MUIS elements
 * 
 * @param <T> The type of value the property supports
 */
public final class StyleAttribute<T>
{
	/**
	 * The different types of attributes
	 */
	public enum AttributeType
	{
		/** An attribute whose value can be either true or false */
		BOOLEAN,
		/** An attribute whose value must be an integer */
		INT,
		/** An attribute whose value must be a number */
		FLOAT,
		/** An attribute whose value must be one of a series of choices */
		ENUM,
		/** An attribute whose value must be a color */
		COLOR;
	}

	/** The style domain that the attribute belongs to */
	public final StyleDomain domain;

	/** The name of the attribute */
	public final String name;

	/** The type of the attribute */
	public final AttributeType type;

	/** The java class of which this attribute's values must be instances */
	public final Class<T> javaType;

	/** The default value for this attribute */
	public final T theDefault;

	/**
	 * The minimum value for this number attribute--will be Float#NaN if this attribute is not a
	 * number attribute.
	 */
	public final float min;

	/**
	 * The maximum value for this number attribute--will be Float#NaN if this attribute is not a
	 * number attribute.
	 */
	public final float max;

	private StyleAttribute(StyleDomain aDomain, String aName, AttributeType aType,
		Class<T> aJavaType, T defValue, float aMin, float aMax)
	{
		domain = aDomain;
		name = aName;
		type = aType;
		javaType = aJavaType;
		min = aMin;
		max = aMax;
		theDefault = defValue;
	}

	/**
	 * Parses a value for this attribute
	 * 
	 * @param value The string value to parse
	 * @return A value of this attribute's type (unchecked--may not be valid)
	 * @throws MuisException If the value cannot be parsed
	 */
	public T parse(String value) throws MuisException
	{
		switch(type)
		{
		case BOOLEAN:
			if("true".equals(value))
				return (T) Boolean.TRUE;
			else if("false".equals(value))
				return (T) Boolean.FALSE;
			else
				throw new MuisException("Value \"" + value + "\" for boolean property "
					+ domain.getName() + "." + name + " is not either \"true\" or \"false\"");
		case INT:
			try
			{
				return (T) new Integer(value);
			} catch(NumberFormatException e)
			{
				throw new MuisException("Value \"" + value + "\" for integer property "
					+ domain.getName() + "." + name + " is not an integer value", e);
			}
		case FLOAT:
			try
			{
				return (T) new Float(value);
			} catch(NumberFormatException e)
			{
				throw new MuisException("Value \"" + value + "\" for real number property "
					+ domain.getName() + "." + name + " is not a real number value", e);
			}
		case ENUM:
			for(T val : javaType.getEnumConstants())
				if(((Enum<?>) val).name().equals(value))
					return val;
			throw new MuisException("Value \"" + value + "\" for enumerated property "
				+ domain.getName() + "." + name + " does not match any valid values");
		case COLOR:
			return (T) Colors.parseColor(value);
		}
		throw new MuisException("Unrecognized attribute type: " + type);
	}

	/**
	 * Validates a value for this attribute
	 * 
	 * @param value The value that might be set as a value for this attribute
	 * @return An error to display if the value is not valid. Null if the value is valid.
	 */
	public String validate(Object value)
	{
		/* Eclipse and other IDEs have a feature that warns the developer when switch statements are
		 * used on enums but don't have cases for every value. Putting a default in the switch
		 * defeats that feature.  But I need to catch the case at run time as well that the constant
		 * isn't covered, so I use the switchHit boolean. */
		boolean switchHit = false;
		switch(type)
		{
		case BOOLEAN:
			switchHit = true;
			if(!(value instanceof Boolean))
				return value + " is not a boolean value";
			break;
		case INT:
			switchHit = true;
			if(value instanceof Integer)
			{
				int iVal = ((Integer) value).intValue();
				if(!Float.isNaN(min) && iVal < min)
					return "The value for property " + domain.getName() + "." + name
						+ " must be at least " + min;
				if(!Float.isNaN(max) && iVal > max)
					return "The value for property " + domain.getName() + "." + name
						+ " must be at most " + max;
				break;
			}
			if(value instanceof Byte || value instanceof Short)
				return "The value for an integer property must be of type Integer--"
					+ "re-cast bytes or shorts";
			if(value instanceof Long)
				return "The value for an integer property must be int, not long";
			if(value instanceof Float || value instanceof Double)
				return "The value for an integer property must be of type Integer--"
					+ "re-cast floating-point values";
			return value + " is not an integer value";
		case FLOAT:
			switchHit = true;
			if(value instanceof Float)
			{
				float fVal = ((Float) value).floatValue();
				if(!Float.isNaN(min) && fVal < min)
					return "The value for property " + domain.getName() + "." + name
						+ " must be at least " + min;
				if(!Float.isNaN(max) && fVal > max)
					return "The value for property " + domain.getName() + "." + name
						+ " must be at most " + max;
				break;
			}
			if(value instanceof Byte || value instanceof Short || value instanceof Integer
				|| value instanceof Long)
				return "The value for a real number property must be of type Float--"
					+ "re-cast integer values";
			if(value instanceof Double)
				return "The value for a real number property must be float, not double";
			return value + " is not an real number value";
		case ENUM:
			switchHit = true;
			if(!javaType.isInstance(value))
				return value + " is not an instance of enumeration " + javaType.getName();
			break;
		case COLOR:
			switchHit = true;
			if(!(value instanceof java.awt.Color))
				return value + " is not a color";
			break;
		}
		if(!switchHit)
			return "Unrecognized attribute type " + type;
		return null;
	}

	/**
	 * Creates a style attribute of a boolean type
	 * 
	 * @param domain The style domain to create the attribute for
	 * @param name The name for the new attribute
	 * @param def The default value for the attribute
	 * @return The new attribute
	 */
	public static StyleAttribute<Boolean> createBooleanStyle(StyleDomain domain, String name,
		boolean def)
	{
		return new StyleAttribute<Boolean>(domain, name, AttributeType.BOOLEAN, Boolean.class, def,
			Float.NaN, Float.NaN);
	}

	/**
	 * Creates a style attribute of a integer type
	 * 
	 * @param domain The style domain to create the attribute for
	 * @param name The name for the new attribute
	 * @param min The minimum for the attribute's value
	 * @param max The maximum for the attribute's value
	 * @param def The default value for the attribute
	 * @return The new attribute
	 */
	public static StyleAttribute<Integer> createIntStyle(StyleDomain domain, String name, int min,
		int max, int def)
	{
		return new StyleAttribute<Integer>(domain, name, AttributeType.INT, Integer.class, def,
			min, max);
	}

	/**
	 * Creates a style attribute of a real number type
	 * 
	 * @param domain The style domain to create the attribute for
	 * @param name The name for the new attribute
	 * @param min The minimum for the attribute's value
	 * @param max The maximum for the attribute's value
	 * @param def The default value for the attribute
	 * @return The new attribute
	 */
	public static StyleAttribute<Float> createFloatStyle(StyleDomain domain, String name,
		float min, float max, float def)
	{
		return new StyleAttribute<Float>(domain, name, AttributeType.FLOAT, Float.class, def, min,
			max);
	}

	/**
	 * Creates a style attribute of a enumerated type
	 * 
	 * @param <T> The type of enumeration to create the attribute for
	 * @param domain The style domain to create the attribute for
	 * @param name The name for the new attribute
	 * @param javaType The enum class that the style's values may be from
	 * @param def The default value for the attribute
	 * @return The new attribute
	 */
	public static <T extends Enum<T>> StyleAttribute<T> createEnumStyle(StyleDomain domain,
		String name, Class<T> javaType, T def)
	{
		if(!Enum.class.isAssignableFrom(javaType))
			throw new IllegalArgumentException(
				"Cannot create an enumeration style for non-enum class " + javaType.getName());
		return new StyleAttribute<T>(domain, name, AttributeType.ENUM, javaType, def, Float.NaN,
			Float.NaN);
	}

	/**
	 * Creates a style attribute of a color type
	 * 
	 * @param domain The style domain to create the attribute for
	 * @param name The name for the new attribute
	 * @param def The default value for the attribute
	 * @return The new attribute
	 */
	public static StyleAttribute<java.awt.Color> createColorStyle(StyleDomain domain, String name,
		java.awt.Color def)
	{
		return new StyleAttribute<java.awt.Color>(domain, name, AttributeType.COLOR,
			java.awt.Color.class, def, Float.NaN, Float.NaN);
	}
	/* TODO: Need more types of style attributes:
	 * URL (maybe) for background images (format?)
	 * dimensioned attributes (with units), e.g. width=100px or 1in
	 */
}
