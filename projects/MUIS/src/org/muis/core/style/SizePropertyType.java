package org.muis.core.style;


/** An attribute type that validates and parses instances of {@link Size} from an attribute value */
public class SizePropertyType extends org.muis.core.MuisProperty.AbstractPropertyType<Size> implements
	org.muis.core.MuisProperty.PrintablePropertyType<Size>
{
	/** An instance of this class--prevents having to create multiple instances for no reason */
	public static final SizePropertyType instance = new SizePropertyType();

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
			throw new IllegalArgumentException(propName() + ": No size specified");
		if(c == 1 && neg)
			throw new IllegalArgumentException(propName() + ": No size specified");
		number = number.substring(neg ? 1 : 0, c);
		int lengthVal = Integer.parseInt(number);
		if(c == value.length())
			return new Size(lengthVal, LengthUnit.pixels); // Default unit
		String unitString = value.substring(c);
		for(LengthUnit u : LengthUnit.values())
			if(u.isSize() && u.attrValue.equals(unitString))
				return new Size(lengthVal, u);
		throw new IllegalArgumentException(propName() + ": " + value + " is not a valid size unit");
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

	@Override
	public String toString(Size value) {
		StringBuilder ret = new StringBuilder();
		if(value.getUnit() != LengthUnit.percent || Math.floor(value.getValue()) == value.getValue())
			ret.append(Math.round(value.getValue()));
		else
			ret.append(value.getValue());
		ret.append(value.getUnit().attrValue);
		return ret.toString();
	}
}
