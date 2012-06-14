package org.muis.base.layout;

import org.muis.core.MuisElement;

/** An attribute type that validates and parses instances of {@link Length} from an attribute value */
public class LengthAttributeType implements org.muis.core.MuisAttribute.AttributeType<Length>
{
	/** An instance of this class--prevents having to create multiple instances for no reason */
	public static final LengthAttributeType instance = new LengthAttributeType();

	@Override
	public String validate(MuisElement element, String value)
	{
		String number = value;
		number = number.replaceAll("\\s", "");
		if(number.length() == 0)
			return "No length specified";
		int c = 0;
		if(number.charAt(c) == '-')
			c++;
		for(; c < number.length(); c++)
			if(number.charAt(c) < '0' || number.charAt(c) > '9')
				break;
		if(c == 0)
			return "No length specified";
		if(c == 1 && value.charAt(0) == '-')
			return "No length specified";
		if(c == value.length())
			return null; // Default unit
		String unitString = value.substring(c);
		for(LengthUnit u : LengthUnit.values())
			if(u.attrValue.equals(unitString))
				return null;
		return unitString + " is not a valid length unit";
	}

	@Override
	public Length parse(MuisElement element, String value)
	{
		String number = value;
		number = number.replaceAll("\\s", "");
		int c = 0;
		boolean neg = number.charAt(c) == '-';
		if(neg)
			c++;
		for(; c < number.length(); c++)
			if(number.charAt(c) < '0' || number.charAt(c) > '9')
				break;
		if(c == 0)
			throw new IllegalArgumentException("No length specified");
		if(c == 1 && neg)
			throw new IllegalArgumentException("No length specified");
		number = number.substring(neg ? 1 : 0, c);
		int lengthVal = Integer.parseInt(number);
		if(c == value.length())
			return new Length(lengthVal, LengthUnit.pixels); // Default unit
		String unitString = value.substring(c);
		for(LengthUnit u : LengthUnit.values())
			if(u.attrValue.equals(unitString))
				return new Length(lengthVal, u);
		throw new IllegalArgumentException(value + " is not a valid length unit");
	}

	@Override
	public Length cast(Object value)
	{
		if(value instanceof Length)
			return (Length) value;
		else if(value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte)
			return new Length(((Number) value).intValue(), LengthUnit.pixels);
		else if(value instanceof Float || value instanceof Double)
			return new Length(Math.round(((Number) value).floatValue()), LengthUnit.pixels);
		return null;
	}
}
