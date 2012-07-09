package org.muis.core.style;


/** An attribute type that validates and parses instances of {@link Size} from an attribute value */
public class SizeAttributeType implements org.muis.core.MuisAttribute.AttributeType<Size>
{
	/** An instance of this class--prevents having to create multiple instances for no reason */
	public static final SizeAttributeType instance = new SizeAttributeType();

	@Override
	public String validate(org.muis.core.MuisClassView classView, String value)
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
			return "No size specified";
		if(c == 1 && value.charAt(0) == '-')
			return "No size specified";
		if(c == value.length())
			return null; // Default unit
		String unitString = value.substring(c);
		for(LengthUnit u : LengthUnit.values())
			if(u.isSize() && u.attrValue.equals(unitString))
				return null;
		return unitString + " is not a valid size unit";
	}

	@Override
	public Size parse(org.muis.core.MuisClassView classView, String value)
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
			throw new IllegalArgumentException("No size specified");
		if(c == 1 && neg)
			throw new IllegalArgumentException("No size specified");
		number = number.substring(neg ? 1 : 0, c);
		int lengthVal = Integer.parseInt(number);
		if(c == value.length())
			return new Size(lengthVal, LengthUnit.pixels); // Default unit
		String unitString = value.substring(c);
		for(LengthUnit u : LengthUnit.values())
			if(u.isSize() && u.attrValue.equals(unitString))
				return new Size(lengthVal, u);
		throw new IllegalArgumentException(value + " is not a valid size unit");
	}

	@Override
	public Size cast(Object value)
	{
		if(value instanceof Size)
			return (Size) value;
		else if(value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte)
			return new Size(((Number) value).intValue(), LengthUnit.pixels);
		else if(value instanceof Float || value instanceof Double)
			return new Size(Math.round(((Number) value).floatValue()), LengthUnit.pixels);
		return null;
	}

	@Override
	public Class<Size> getType()
	{
		return Size.class;
	}
}
