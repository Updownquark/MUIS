package org.muis.core.style;

import org.muis.core.MuisException;
import org.muis.core.MuisParseEnv;
import org.muis.core.MuisProperty;
import org.observe.ObservableValue;

import prisms.lang.Type;

/** An attribute type that validates and parses instances of {@link Size} from an attribute value */
public class SizePropertyType extends MuisProperty.AbstractPropertyType<Size> implements MuisProperty.PrintablePropertyType<Size>
{
	/** An instance of this class--prevents having to create multiple instances for no reason */
	public static final SizePropertyType instance = new SizePropertyType();

	@Override
	public ObservableValue<? extends Size> parse(MuisParseEnv env, String value) throws org.muis.core.MuisException {
		ObservableValue<?> modelValue = MuisProperty.parseExplicitObservable(env, value, false);
		if(modelValue != null) {
			if(modelValue.getType().canAssignTo(Size.class))
				return (ObservableValue<? extends Size>) modelValue;
			else if(modelValue.getType().canAssignTo(Number.class))
				return ((ObservableValue<? extends Number>) modelValue).mapV(val -> {
					return new Size(val.floatValue(), LengthUnit.pixels);
				});
			else
				throw new MuisException("Model value " + value + ", type " + modelValue.getType() + " is not compatible with position.");
		}
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
			return ObservableValue.constant(new Size(lengthVal, LengthUnit.pixels)); // Default unit
		String unitString = value.substring(c);
		for(LengthUnit u : LengthUnit.values())
			if(u.isSize() && u.attrValue.equals(unitString))
				return ObservableValue.constant(new Size(lengthVal, u));
		throw new IllegalArgumentException(propName() + ": " + value + " is not a valid size unit");
	}

	@Override
	public boolean canCast(Type type) {
		if(type.canAssignTo(Size.class))
			return true;
		if(type.isMathable() && !type.canAssignTo(Character.TYPE))
			return true;
		return false;
	}

	@Override
	public Size cast(Type type, Object value) {
		if(value instanceof Size)
			return (Size) value;
		else if(value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte)
			return new Size(((Number) value).intValue(), LengthUnit.pixels);
		else if(value instanceof Float || value instanceof Double)
			return new Size(Math.round(((Number) value).floatValue()), LengthUnit.pixels);
		return null;
	}

	@Override
	public Type getType() {
		return new Type(Size.class);
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

	@Override
	public String toString() {
		return "size";
	}
}
