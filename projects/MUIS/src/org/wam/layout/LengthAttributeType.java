package org.wam.layout;

import org.wam.core.WamElement;

/** An attribute type that validates and parses instances of {@link Length} from an attribute value */
public class LengthAttributeType implements org.wam.core.WamAttribute.AttributeType<Length>
{
	/** An instance of this class--prevents having to create multiple instances for no reason */
	public static final LengthAttributeType instance = new LengthAttributeType();

	@Override
	public String validate(WamElement element, String value)
	{
		value = value.replaceAll("\\w", "");
		int c = 0;
		if(value.charAt(c) == '-')
			c++;
		for(; c < value.length(); c++)
			if(value.charAt(c) < '0' || value.charAt(c) > '9')
				break;
		if(c == 0)
			return "No length specified";
		if(c == 1 && value.charAt(0) == '-')
			return "No length specified";
		if(c == value.length())
			return null;
		String unitString = value.substring(c);
		for(LengthUnit u : LengthUnit.values())
			if(u.attrValue.equals(unitString))
				return null;
		return unitString + " is not a valid length unit";
	}

	@Override
	public Length parse(WamElement element, String value)
	{
		value = value.replaceAll("\\w", "");
		int c = 0;
		if(value.charAt(c) == '-')
			c++;
		for(; c < value.length(); c++)
			if(value.charAt(c) < '0' || value.charAt(c) > '9')
				break;
		int lengthVal = Integer.parseInt(value.substring(0, c));
		LengthUnit lengthUnit = LengthUnit.valueOf(value.substring(c));
		return new Length(lengthVal, lengthUnit);
	}

	@Override
	public Length cast(Object value)
	{
		if(value instanceof Length)
			return (Length) value;
		else if(value instanceof Integer || value instanceof Long || value instanceof Short
			|| value instanceof Byte)
			return new Length(((Number) value).intValue(), LengthUnit.pixels);
		else if(value instanceof Float || value instanceof Double)
			return new Length(Math.round(((Number) value).floatValue()), LengthUnit.pixels);
		return null;
	}
}
