package org.muis.core.style;


/** An attribute type that validates and parses instances of {@link Position} from an attribute value */
public class PositionPropertyType extends org.muis.core.MuisProperty.AbstractPropertyType<Position>
{
	/** An instance of this class--prevents having to create multiple instances for no reason */
	public static final PositionPropertyType instance = new PositionPropertyType();

	@Override
	public Position parse(org.muis.core.MuisClassView classView, String value)
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
			throw new IllegalArgumentException(propName() + ": No position specified");
		if(c == 1 && neg)
			throw new IllegalArgumentException(propName() + ": No position specified");
		number = number.substring(neg ? 1 : 0, c);
		int lengthVal = Integer.parseInt(number);
		if(c == value.length())
			return new Position(lengthVal, LengthUnit.pixels); // Default unit
		String unitString = value.substring(c);
		for(LengthUnit u : LengthUnit.values())
			if(u.attrValue.equals(unitString))
				return new Position(lengthVal, u);
		throw new IllegalArgumentException(propName() + ": " + value + " is not a valid position unit");
	}

	@Override
	public Position cast(Object value)
	{
		if(value instanceof Position)
			return (Position) value;
		else if(value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte)
			return new Position(((Number) value).intValue(), LengthUnit.pixels);
		else if(value instanceof Float || value instanceof Double)
			return new Position(Math.round(((Number) value).floatValue()), LengthUnit.pixels);
		return null;
	}

	@Override
	public Class<Position> getType()
	{
		return Position.class;
	}
}
