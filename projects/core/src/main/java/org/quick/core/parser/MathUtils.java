package org.quick.core.parser;

import java.util.Objects;

import org.observe.ObservableValue;

import com.google.common.reflect.TypeToken;

public class MathUtils {
	public static final TypeToken<Double> DOUBLE = TypeToken.of(Double.TYPE);
	public static final TypeToken<Float> FLOAT = TypeToken.of(Float.TYPE);
	public static final TypeToken<Long> LONG = TypeToken.of(Long.TYPE);
	public static final TypeToken<Integer> INT = TypeToken.of(Integer.TYPE);
	public static final TypeToken<Short> SHORT = TypeToken.of(Short.TYPE);
	public static final TypeToken<Byte> BYTE = TypeToken.of(Byte.TYPE);
	public static final TypeToken<Character> CHAR = TypeToken.of(Character.TYPE);
	public static final TypeToken<Boolean> BOOLEAN = TypeToken.of(Boolean.TYPE);

	public static boolean isMathable(TypeToken<?> type) {
		TypeToken<?> prim = type.unwrap();
		return prim == DOUBLE || prim == FLOAT || prim == LONG || prim == INT || prim == SHORT || prim == BYTE || prim == CHAR;
	}

	public static boolean isIntMathable(TypeToken<?> type) {
		TypeToken<?> prim = type.unwrap();
		return prim == LONG || prim == INT || prim == SHORT || prim == BYTE || prim == CHAR;
	}

	public static ObservableValue<? extends Number> unaryOp(String op, ObservableValue<?> value) {
		switch (op) {
		case "+":
			return posit(value);
		case "-":
			return negate(value);
		case "~":
			return complement(value);
		}
		throw new IllegalArgumentException("Unrecognized operator: " + op);
	}

	public static ObservableValue<?> binaryMathOp(String op, ObservableValue<?> value1, ObservableValue<?> value2) {
		switch (op) {
		case "+":
			return add(value1, value2);
		case "-":
			return subtract(value1, value2);
		case "*":
			return multiply(value1, value2);
		case "/":
			return divide(value1, value2);
		case "%":
			return modulo(value1, value2);
		case "<<":
			return leftShift(value1, value2);
		case ">>":
			return rightShift(value1, value2);
		case ">>>":
			return rightShiftUnsigned(value1, value2);
		case "==":
			return value1.combineV(BOOLEAN, (v1, v2) -> Objects.equals(v1, v2), value2, true);
		case "!=":
			return value1.combineV(BOOLEAN, (v1, v2) -> Objects.equals(v1, v2), value2, true);
		case ">":
		case ">=":
		case "<":
		case "<=":
			return compare(op, value1, value2);
		case "&":
			return binaryAnd(value1, value2);
		case "|":
			return binaryOr(value1, value2);
		case "^":
			return binaryXor(value1, value2);
		case "&&":
			return and(value1, value2);
		case "||":
			return or(value1, value2);
		}
		throw new IllegalArgumentException("Unrecognized operator: " + op);
	}

	public static ObservableValue<Boolean> compare(String op, ObservableValue<?> v1, ObservableValue<?> v2) {
		ObservableValue<Integer> comp = compare(v1, v2);
		switch (op) {
		case ">":
			return comp.mapV(c -> c > 0);
		case ">=":
			return comp.mapV(c -> c >= 0);
		case "<":
			return comp.mapV(c -> c < 0);
		case "<=":
			return comp.mapV(c -> c <= 0);
		case "==":
			return comp.mapV(c -> c == 0);
		case "!=":
			return comp.mapV(c -> c != 0);
		}
		throw new IllegalArgumentException("Unrecognized comparison operatior: " + op);
	}

	public static ObservableValue<? extends Number> mathableToNumber(ObservableValue<?> v) {
		if (TypeToken.of(Number.class).isAssignableFrom(v.getType().wrap()))
			return (ObservableValue<? extends Number>) v;
		else if (CHAR.isAssignableFrom(v.getType().unwrap()))
			return ((ObservableValue<Character>) v).mapV(c -> Integer.valueOf(c.charValue()));
		else
			throw new IllegalArgumentException("Type " + v.getClass().getName() + " is not mathable");
	}

	public static ObservableValue<Integer> compare(ObservableValue<?> v1, ObservableValue<?> v2) {
		if (isMathable(v1.getType()) && isMathable(v2.getType())) {
			ObservableValue<? extends Number> n1 = mathableToNumber(v1);
			ObservableValue<? extends Number> n2 = mathableToNumber(v2);
			if (DOUBLE.isAssignableFrom(n1.getType().unwrap()) || DOUBLE.isAssignableFrom(n2.getType().unwrap()))
				return n1.combineV(INT, (num1, num2) -> Double.compare(num1.doubleValue(), num2.doubleValue()), n2, true);
			else if (FLOAT.isAssignableFrom(n1.getType()) || FLOAT.isAssignableFrom(n2.getType().unwrap()))
				return n1.combineV(INT, (num1, num2) -> Float.compare(num1.floatValue(), num2.floatValue()), n2, true);
			else if (LONG.isAssignableFrom(n1.getType().unwrap()) || LONG.isAssignableFrom(n2.getType().unwrap()))
				return n1.combineV(INT, (num1, num2) -> Long.compare(num1.longValue(), num2.longValue()), n2, true);
			else if (INT.isAssignableFrom(n1.getType().unwrap()) || INT.isAssignableFrom(n2.getType().unwrap()))
				return n1.combineV(INT, (num1, num2) -> Integer.compare(num1.intValue(), num2.intValue()), n2, true);
			else if (SHORT.isAssignableFrom(n1.getType().unwrap()) || SHORT.isAssignableFrom(n2.getType().unwrap()))
				return n1.combineV(INT, (num1, num2) -> Short.compare(num1.shortValue(), num2.shortValue()), n2, true);
			else if (BYTE.isAssignableFrom(n1.getType().unwrap()) && BYTE.isAssignableFrom(n2.getType().unwrap()))
				return n1.combineV(INT, (num1, num2) -> Byte.compare(num1.byteValue(), num2.byteValue()), n2, true);
			else
				throw new IllegalArgumentException("Unrecognized number types: " + v1.getType() + ", " + v2.getType());
		} else if (TypeToken.of(Comparable.class).isAssignableFrom(v1.getType())
			&& TypeToken.of(Comparable.class).isAssignableFrom(v2.getType())) {
			if (v1.getType().resolveType(Comparable.class.getTypeParameters()[0]).isAssignableFrom(v2.getType())) {
				return ((ObservableValue<Comparable<Object>>) v1).combineV(INT, (c1, c2) -> c1.compareTo(c2), v2, true);
			} else
				throw new IllegalArgumentException("Comparable type " + v1.getType() + " cannot be applied to " + v2.getType());
		} else
			throw new IllegalArgumentException(
				"MathUtils.compare() cannot be applied to operand types " + v1.getType() + " and " + v2.getType());
	}

	public static ObservableValue<? extends Number> posit(ObservableValue<?> value) {
		if (!isMathable(value.getType()))
			throw new IllegalArgumentException("Posit cannot be applied to operand type " + value.getType());
		return mathableToNumber(value);
	}

	public static ObservableValue<? extends Number> negate(ObservableValue<?> value) {
		if (!isMathable(value.getType()))
			throw new IllegalArgumentException("Negate cannot be applied to operand type " + value.getType());
		ObservableValue<? extends Number> n = mathableToNumber(value);
		if (DOUBLE.isAssignableFrom(n.getType().unwrap()))
			return ((ObservableValue<Double>) n).mapV(v -> -v);
		else if (FLOAT.isAssignableFrom(n.getType().unwrap()))
			return ((ObservableValue<Float>) n).mapV(v -> -v);
		else if (LONG.isAssignableFrom(n.getType().unwrap()))
			return ((ObservableValue<Long>) n).mapV(v -> -v);
		else
			return ((ObservableValue<? extends Number>) n).mapV(v -> -v.intValue());
	}

	public static ObservableValue<? extends Number> complement(ObservableValue<?> value) {
		if (!isIntMathable(value.getType()))
			throw new IllegalArgumentException("Complement cannot be applied to operand type " + value.getType());
		ObservableValue<? extends Number> n = mathableToNumber(value);
		if (LONG.isAssignableFrom(n.getType().unwrap()))
			return ((ObservableValue<Long>) n).mapV(v -> ~v);
		else
			return ((ObservableValue<? extends Number>) n).mapV(v -> ~v.intValue());
	}

	public static ObservableValue<? extends Number> add(ObservableValue<?> v1, ObservableValue<?> v2) {
		if (!isMathable(v1.getType()) || !isMathable(v2.getType()))
			throw new IllegalArgumentException("Add cannot be applied to operand types " + v1.getType() + " and " + v2.getType());
		ObservableValue<? extends Number> n1 = mathableToNumber(v1);
		ObservableValue<? extends Number> n2 = mathableToNumber(v2);
		if (DOUBLE.isAssignableFrom(n1.getType().unwrap()) || DOUBLE.isAssignableFrom(n2.getType().unwrap()))
			return n1.combineV(DOUBLE, (num1, num2) -> Double.valueOf(num1.doubleValue() + num2.doubleValue()), n2, true);
		else if (FLOAT.isAssignableFrom(n1.getType()) || FLOAT.isAssignableFrom(n2.getType().unwrap()))
			return n1.combineV(FLOAT, (num1, num2) -> Float.valueOf(num1.floatValue() + num2.floatValue()), n2, true);
		else if (LONG.isAssignableFrom(n1.getType().unwrap()) || LONG.isAssignableFrom(n2.getType().unwrap()))
			return n1.combineV(LONG, (num1, num2) -> Long.valueOf(num1.longValue() + num2.longValue()), n2, true);
		else
			return n1.combineV(INT, (num1, num2) -> Integer.valueOf(num1.intValue() + num2.intValue()), n2, true);
	}

	public static ObservableValue<? extends Number> subtract(ObservableValue<?> v1, ObservableValue<?> v2) {
		if (!isMathable(v1.getType()) || !isMathable(v2.getType()))
			throw new IllegalArgumentException("Subtract cannot be applied to operand types " + v1.getType() + " and " + v2.getType());
		ObservableValue<? extends Number> n1 = mathableToNumber(v1);
		ObservableValue<? extends Number> n2 = mathableToNumber(v2);
		if (DOUBLE.isAssignableFrom(n1.getType().unwrap()) || DOUBLE.isAssignableFrom(n2.getType().unwrap()))
			return n1.combineV(DOUBLE, (num1, num2) -> Double.valueOf(num1.doubleValue() - num2.doubleValue()), n2, true);
		else if (FLOAT.isAssignableFrom(n1.getType()) || FLOAT.isAssignableFrom(n2.getType().unwrap()))
			return n1.combineV(FLOAT, (num1, num2) -> Float.valueOf(num1.floatValue() - num2.floatValue()), n2, true);
		else if (LONG.isAssignableFrom(n1.getType().unwrap()) || LONG.isAssignableFrom(n2.getType().unwrap()))
			return n1.combineV(LONG, (num1, num2) -> Long.valueOf(num1.longValue() - num2.longValue()), n2, true);
		else
			return n1.combineV(INT, (num1, num2) -> Integer.valueOf(num1.intValue() - num2.intValue()), n2, true);
	}

	public static ObservableValue<? extends Number> multiply(ObservableValue<?> v1, ObservableValue<?> v2) {
		if (!isMathable(v1.getType()) || !isMathable(v2.getType()))
			throw new IllegalArgumentException("Multiply cannot be applied to operand types " + v1.getType() + " and " + v2.getType());
		ObservableValue<? extends Number> n1 = mathableToNumber(v1);
		ObservableValue<? extends Number> n2 = mathableToNumber(v2);
		if (DOUBLE.isAssignableFrom(n1.getType().unwrap()) || DOUBLE.isAssignableFrom(n2.getType().unwrap()))
			return n1.combineV(DOUBLE, (num1, num2) -> Double.valueOf(num1.doubleValue() * num2.doubleValue()), n2, true);
		else if (FLOAT.isAssignableFrom(n1.getType()) || FLOAT.isAssignableFrom(n2.getType().unwrap()))
			return n1.combineV(FLOAT, (num1, num2) -> Float.valueOf(num1.floatValue() * num2.floatValue()), n2, true);
		else if (LONG.isAssignableFrom(n1.getType().unwrap()) || LONG.isAssignableFrom(n2.getType().unwrap()))
			return n1.combineV(LONG, (num1, num2) -> Long.valueOf(num1.longValue() * num2.longValue()), n2, true);
		else
			return n1.combineV(INT, (num1, num2) -> Integer.valueOf(num1.intValue() * num2.intValue()), n2, true);
	}

	public static ObservableValue<? extends Number> divide(ObservableValue<?> v1, ObservableValue<?> v2) {
		if (!isMathable(v1.getType()) || !isMathable(v2.getType()))
			throw new IllegalArgumentException("Divide cannot be applied to operand types " + v1.getType() + " and " + v2.getType());
		ObservableValue<? extends Number> n1 = mathableToNumber(v1);
		ObservableValue<? extends Number> n2 = mathableToNumber(v2);
		if (DOUBLE.isAssignableFrom(n1.getType().unwrap()) || DOUBLE.isAssignableFrom(n2.getType().unwrap()))
			return n1.combineV(DOUBLE, (num1, num2) -> Double.valueOf(num1.doubleValue() / num2.doubleValue()), n2, true);
		else if (FLOAT.isAssignableFrom(n1.getType()) || FLOAT.isAssignableFrom(n2.getType().unwrap()))
			return n1.combineV(FLOAT, (num1, num2) -> Float.valueOf(num1.floatValue() / num2.floatValue()), n2, true);
		else if (LONG.isAssignableFrom(n1.getType().unwrap()) || LONG.isAssignableFrom(n2.getType().unwrap()))
			return n1.combineV(LONG, (num1, num2) -> Long.valueOf(num1.longValue() / num2.longValue()), n2, true);
		else
			return n1.combineV(INT, (num1, num2) -> Integer.valueOf(num1.intValue() / num2.intValue()), n2, true);
	}

	public static ObservableValue<? extends Number> modulo(ObservableValue<?> v1, ObservableValue<?> v2) {
		if (!isMathable(v1.getType()) || !isMathable(v2.getType()))
			throw new IllegalArgumentException("Modulus cannot be applied to operand types " + v1.getType() + " and " + v2.getType());
		ObservableValue<? extends Number> n1 = mathableToNumber(v1);
		ObservableValue<? extends Number> n2 = mathableToNumber(v2);
		if (DOUBLE.isAssignableFrom(n1.getType().unwrap()) || DOUBLE.isAssignableFrom(n2.getType().unwrap()))
			return n1.combineV(DOUBLE, (num1, num2) -> Double.valueOf(num1.doubleValue() % num2.doubleValue()), n2, true);
		else if (FLOAT.isAssignableFrom(n1.getType()) || FLOAT.isAssignableFrom(n2.getType().unwrap()))
			return n1.combineV(FLOAT, (num1, num2) -> Float.valueOf(num1.floatValue() % num2.floatValue()), n2, true);
		else if (LONG.isAssignableFrom(n1.getType().unwrap()) || LONG.isAssignableFrom(n2.getType().unwrap()))
			return n1.combineV(LONG, (num1, num2) -> Long.valueOf(num1.longValue() % num2.longValue()), n2, true);
		else
			return n1.combineV(INT, (num1, num2) -> Integer.valueOf(num1.intValue() % num2.intValue()), n2, true);
	}

	public static ObservableValue<? extends Number> leftShift(ObservableValue<?> v1, ObservableValue<?> v2)
		throws IllegalArgumentException {
		if (!isIntMathable(v1.getType()) || !isIntMathable(v2.getType()))
			throw new IllegalArgumentException("Left shift cannot be applied to operand types " + v1.getType() + " and " + v2.getType());
		ObservableValue<? extends Number> n1 = mathableToNumber(v1);
		ObservableValue<? extends Number> n2 = mathableToNumber(v2);
		if (LONG.isAssignableFrom(n1.getType().unwrap()) || LONG.isAssignableFrom(n2.getType().unwrap()))
			return n1.combineV(LONG, (num1, num2) -> Long.valueOf(num1.longValue() << num2.longValue()), n2, true);
		else
			return n1.combineV(INT, (num1, num2) -> Integer.valueOf(num1.intValue() << num2.intValue()), n2, true);
	}

	public static ObservableValue<? extends Number> rightShift(ObservableValue<?> v1, ObservableValue<?> v2) {
		if (!isIntMathable(v1.getType()) || !isIntMathable(v2.getType()))
			throw new IllegalArgumentException("Right shift cannot be applied to operand types " + v1.getType() + " and " + v2.getType());
		ObservableValue<? extends Number> n1 = mathableToNumber(v1);
		ObservableValue<? extends Number> n2 = mathableToNumber(v2);
		if (LONG.isAssignableFrom(n1.getType().unwrap()) || LONG.isAssignableFrom(n2.getType().unwrap()))
			return n1.combineV(LONG, (num1, num2) -> Long.valueOf(num1.longValue() >> num2.longValue()), n2, true);
		else
			return n1.combineV(INT, (num1, num2) -> Integer.valueOf(num1.intValue() >> num2.intValue()), n2, true);
	}

	public static ObservableValue<? extends Number> rightShiftUnsigned(ObservableValue<?> v1, ObservableValue<?> v2) {
		if (!isIntMathable(v1.getType()) || !isIntMathable(v2.getType()))
			throw new IllegalArgumentException(
				"Unsigned right shift cannot be applied to operand types " + v1.getType() + " and " + v2.getType());
		ObservableValue<? extends Number> n1 = mathableToNumber(v1);
		ObservableValue<? extends Number> n2 = mathableToNumber(v2);
		if (LONG.isAssignableFrom(n1.getType().unwrap()) || LONG.isAssignableFrom(n2.getType().unwrap()))
			return n1.combineV(LONG, (num1, num2) -> Long.valueOf(num1.longValue() >>> num2.longValue()), n2, true);
		else
			return n1.combineV(INT, (num1, num2) -> Integer.valueOf(num1.intValue() >>> num2.intValue()), n2, true);
	}

	public static ObservableValue<?> binaryAnd(ObservableValue<?> v1, ObservableValue<?> v2){
		if (BOOLEAN.isAssignableFrom(v1.getType().unwrap()) && BOOLEAN.isAssignableFrom(v2.getType().unwrap())) {
			return ((ObservableValue<Boolean>) v1).combineV(BOOLEAN, (b1, b2) -> b1 & b2, (ObservableValue<Boolean>) v2, true);
		} else if (isIntMathable(v1.getType()) && isIntMathable(v2.getType())) {
			ObservableValue<? extends Number> n1 = mathableToNumber(v1);
			ObservableValue<? extends Number> n2 = mathableToNumber(v2);
			if (LONG.isAssignableFrom(n1.getType().unwrap()) || LONG.isAssignableFrom(n2.getType().unwrap()))
				return n1.combineV(LONG, (num1, num2) -> Long.valueOf(num1.longValue() & num2.longValue()), n2, true);
			else
				return n1.combineV(INT, (num1, num2) -> Integer.valueOf(num1.intValue() & num2.intValue()), n2, true);
		} else
			throw new IllegalArgumentException("Binary and cannot be applied to operand types " + v1.getType() + " and " + v2.getType());
	}

	public static ObservableValue<?> binaryOr(ObservableValue<?> v1, ObservableValue<?> v2){
		if (BOOLEAN.isAssignableFrom(v1.getType().unwrap()) && BOOLEAN.isAssignableFrom(v2.getType().unwrap())) {
			return ((ObservableValue<Boolean>) v1).combineV(BOOLEAN, (b1, b2) -> b1 | b2, (ObservableValue<Boolean>) v2, true);
		} else if (isIntMathable(v1.getType()) && isIntMathable(v2.getType())) {
			ObservableValue<? extends Number> n1 = mathableToNumber(v1);
			ObservableValue<? extends Number> n2 = mathableToNumber(v2);
			if (LONG.isAssignableFrom(n1.getType().unwrap()) || LONG.isAssignableFrom(n2.getType().unwrap()))
				return n1.combineV(LONG, (num1, num2) -> Long.valueOf(num1.longValue() | num2.longValue()), n2, true);
			else
				return n1.combineV(INT, (num1, num2) -> Integer.valueOf(num1.intValue() | num2.intValue()), n2, true);
		} else
			throw new IllegalArgumentException("Binary or cannot be applied to operand types " + v1.getType() + " and " + v2.getType());
	}

	public static ObservableValue<?> binaryXor(ObservableValue<?> v1, ObservableValue<?> v2){
		if (BOOLEAN.isAssignableFrom(v1.getType().unwrap()) && BOOLEAN.isAssignableFrom(v2.getType().unwrap())) {
			return ((ObservableValue<Boolean>) v1).combineV(BOOLEAN, (b1, b2) -> b1 ^ b2, (ObservableValue<Boolean>) v2, true);
		} else if (isIntMathable(v1.getType()) && isIntMathable(v2.getType())) {
			ObservableValue<? extends Number> n1 = mathableToNumber(v1);
			ObservableValue<? extends Number> n2 = mathableToNumber(v2);
			if (LONG.isAssignableFrom(n1.getType().unwrap()) || LONG.isAssignableFrom(n2.getType().unwrap()))
				return n1.combineV(LONG, (num1, num2) -> Long.valueOf(num1.longValue() ^ num2.longValue()), n2, true);
			else
				return n1.combineV(INT, (num1, num2) -> Integer.valueOf(num1.intValue() ^ num2.intValue()), n2, true);
		} else
			throw new IllegalArgumentException("Binary xor cannot be applied to operand types " + v1.getType() + " and " + v2.getType());
	}

	public static ObservableValue<Boolean> and(ObservableValue<?> v1, ObservableValue<?> v2){
		if (BOOLEAN.isAssignableFrom(v1.getType().unwrap()) && BOOLEAN.isAssignableFrom(v2.getType().unwrap())) {
			return ((ObservableValue<Boolean>) v1).combineV(BOOLEAN, (b1, b2) -> b1 && b2, (ObservableValue<Boolean>) v2, true);
		} else
			throw new IllegalArgumentException("And cannot be applied to operand types " + v1.getType() + " and " + v2.getType());
	}

	public static ObservableValue<Boolean> or(ObservableValue<?> v1, ObservableValue<?> v2){
		if (BOOLEAN.isAssignableFrom(v1.getType().unwrap()) && BOOLEAN.isAssignableFrom(v2.getType().unwrap())) {
			return ((ObservableValue<Boolean>) v1).combineV(BOOLEAN, (b1, b2) -> b1 || b2, (ObservableValue<Boolean>) v2, true);
		} else
			throw new IllegalArgumentException("Or cannot be applied to operand types " + v1.getType() + " and " + v2.getType());
	}
}
