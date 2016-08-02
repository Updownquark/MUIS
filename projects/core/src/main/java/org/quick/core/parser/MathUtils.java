package org.quick.core.parser;

import java.util.Objects;

import org.observe.ObservableValue;

import com.google.common.reflect.TypeToken;

/** A utility class for parsing math operations */
public class MathUtils {
	/** Primitive double type */
	public static final TypeToken<Double> DOUBLE = TypeToken.of(Double.TYPE);
	/** Primitive float type */
	public static final TypeToken<Float> FLOAT = TypeToken.of(Float.TYPE);
	/** Primitive long type */
	public static final TypeToken<Long> LONG = TypeToken.of(Long.TYPE);
	/** Primitive int type */
	public static final TypeToken<Integer> INT = TypeToken.of(Integer.TYPE);
	/** Primitive short type */
	public static final TypeToken<Short> SHORT = TypeToken.of(Short.TYPE);
	/** Primitive byte type */
	public static final TypeToken<Byte> BYTE = TypeToken.of(Byte.TYPE);
	/** Primitive char type */
	public static final TypeToken<Character> CHAR = TypeToken.of(Character.TYPE);
	/** Primitive boolean type */
	public static final TypeToken<Boolean> BOOLEAN = TypeToken.of(Boolean.TYPE);

	/**
	 * @param type The type to test
	 * @return Whether math operations like add and multiply can be applied to the given type
	 */
	public static boolean isMathable(TypeToken<?> type) {
		TypeToken<?> prim = type.unwrap();
		return prim == DOUBLE || prim == FLOAT || prim == LONG || prim == INT || prim == SHORT || prim == BYTE || prim == CHAR;
	}

	/**
	 * @param type The type to test
	 * @return Whether integer-specific math operations like bitwise shifts can be applied to the given type
	 */
	public static boolean isIntMathable(TypeToken<?> type) {
		TypeToken<?> prim = type.unwrap();
		return prim == LONG || prim == INT || prim == SHORT || prim == BYTE || prim == CHAR;
	}

	/**
	 * Applies a unary operator by name
	 *
	 * @param op The name of the operator
	 * @param value The operand
	 * @return The result
	 * @throws IllegalArgumentException If the operator cannot be applied to the given operand or if the operator is not recognized
	 */
	public static ObservableValue<? extends Number> unaryOp(String op, ObservableValue<?> value) throws IllegalArgumentException {
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

	/**
	 * Applies a binary operator by name
	 *
	 * @param op The name of the operator
	 * @param value1 The first operand
	 * @param value2 The second operand
	 * @return The result
	 * @throws IllegalArgumentException If the operator cannot be applied to the given operands or if the operator is not recognized
	 */
	public static ObservableValue<?> binaryMathOp(String op, ObservableValue<?> value1, ObservableValue<?> value2)
		throws IllegalArgumentException {
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

	/**
	 * Applies a comparison operator by name
	 *
	 * @param op The name of the operator
	 * @param v1 The first operand
	 * @param v2 The second operand
	 * @return The result
	 * @throws IllegalArgumentException If the operator cannot be applied to the given operands or if the operator is not recognized
	 */
	public static ObservableValue<Boolean> compare(String op, ObservableValue<?> v1, ObservableValue<?> v2)
		throws IllegalArgumentException {
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

	/**
	 * Converts from a value whose type is {@link #isMathable(TypeToken) mathable} to a number
	 *
	 * @param v The mathable value
	 * @return The number
	 * @throws IllegalArgumentException If the given value cannot be converted to a number. Should not happen if the value's type passes
	 *         {@link #isMathable(TypeToken)}.
	 */
	public static ObservableValue<? extends Number> mathableToNumber(ObservableValue<?> v) throws IllegalArgumentException {
		if (TypeToken.of(Number.class).isAssignableFrom(v.getType().wrap()))
			return (ObservableValue<? extends Number>) v;
		else if (CHAR.isAssignableFrom(v.getType().unwrap()))
			return ((ObservableValue<Character>) v).mapV(c -> Integer.valueOf(c.charValue()));
		else
			throw new IllegalArgumentException("Type " + v.getClass().getName() + " is not mathable");
	}

	/**
	 * @param v1 The first value to compare
	 * @param v2 The second value to compare
	 * @return The comparison result (see java.util.Comparator#compare(Object, Object))
	 * @throws IllegalArgumentException If given values cannot be compared
	 */
	public static ObservableValue<Integer> compare(ObservableValue<?> v1, ObservableValue<?> v2) throws IllegalArgumentException {
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

	/**
	 * Adds one to a value
	 *
	 * @param value The value to add to
	 * @return The value
	 * @throws IllegalArgumentException If the operator cannot be applied to the given operand
	 */
	public static <T> ObservableValue<T> addOne(ObservableValue<T> value) throws IllegalArgumentException {
		if (!isMathable(value.getType()))
			throw new IllegalArgumentException("Add one cannot be applied to operand type " + value.getType());
		if (DOUBLE.isAssignableFrom(value.getType().unwrap()))
			return (ObservableValue<T>) ((ObservableValue<Double>) value).mapV(DOUBLE, num -> Double.valueOf(num.doubleValue() + 1), true);
		else if (FLOAT.isAssignableFrom(value.getType()))
			return (ObservableValue<T>) ((ObservableValue<Float>) value).mapV(FLOAT, num -> Float.valueOf(num.floatValue() + 1), true);
		else if (LONG.isAssignableFrom(value.getType().unwrap()))
			return (ObservableValue<T>) ((ObservableValue<Long>) value).mapV(LONG, num -> Long.valueOf(num.longValue() + 1), true);
		else if (INT.isAssignableFrom(value.getType().unwrap()))
			return (ObservableValue<T>) ((ObservableValue<Integer>) value).mapV(INT, num -> Integer.valueOf(num.intValue() + 1), true);
		else if (SHORT.isAssignableFrom(value.getType().unwrap()))
			return (ObservableValue<T>) ((ObservableValue<Short>) value).mapV(SHORT, num -> Short.valueOf((short) (num.shortValue() + 1)),
				true);
		else if (BYTE.isAssignableFrom(value.getType().unwrap()))
			return (ObservableValue<T>) ((ObservableValue<Byte>) value).mapV(BYTE, num -> Byte.valueOf((byte) (num.byteValue() + 1)), true);
		else // if (CHAR.isAssignableFrom(value.getType().unwrap()))
			return (ObservableValue<T>) ((ObservableValue<Character>) value).mapV(CHAR,
				ch -> Character.valueOf((char) (ch.charValue() + 1)), true);
	}

	/**
	 * Subtracts one from a value
	 *
	 * @param value The value to subtract from
	 * @return The value
	 * @throws IllegalArgumentException If the operator cannot be applied to the given operand
	 */
	public static <T> ObservableValue<T> minusOne(ObservableValue<T> value) throws IllegalArgumentException {
		if (!isMathable(value.getType()))
			throw new IllegalArgumentException("Add one cannot be applied to operand type " + value.getType());
		if (DOUBLE.isAssignableFrom(value.getType().unwrap()))
			return (ObservableValue<T>) ((ObservableValue<Double>) value).mapV(DOUBLE, num -> Double.valueOf(num.doubleValue() - 1), true);
		else if (FLOAT.isAssignableFrom(value.getType()))
			return (ObservableValue<T>) ((ObservableValue<Float>) value).mapV(FLOAT, num -> Float.valueOf(num.floatValue() - 1), true);
		else if (LONG.isAssignableFrom(value.getType().unwrap()))
			return (ObservableValue<T>) ((ObservableValue<Long>) value).mapV(LONG, num -> Long.valueOf(num.longValue() - 1), true);
		else if (INT.isAssignableFrom(value.getType().unwrap()))
			return (ObservableValue<T>) ((ObservableValue<Integer>) value).mapV(INT, num -> Integer.valueOf(num.intValue() - 1), true);
		else if (SHORT.isAssignableFrom(value.getType().unwrap()))
			return (ObservableValue<T>) ((ObservableValue<Short>) value).mapV(SHORT, num -> Short.valueOf((short) (num.shortValue() - 1)),
				true);
		else if (BYTE.isAssignableFrom(value.getType().unwrap()))
			return (ObservableValue<T>) ((ObservableValue<Byte>) value).mapV(BYTE, num -> Byte.valueOf((byte) (num.byteValue() - 1)), true);
		else // if (CHAR.isAssignableFrom(value.getType().unwrap()))
			return (ObservableValue<T>) ((ObservableValue<Character>) value).mapV(CHAR,
				ch -> Character.valueOf((char) (ch.charValue() - 1)), true);
	}

	/**
	 * Applies the posit operation
	 *
	 * @param value The operand
	 * @return The result
	 * @throws IllegalArgumentException If the operator cannot be applied to the given operand
	 */
	public static ObservableValue<? extends Number> posit(ObservableValue<?> value) throws IllegalArgumentException {
		if (!isMathable(value.getType()))
			throw new IllegalArgumentException("Posit cannot be applied to operand type " + value.getType());
		return mathableToNumber(value);
	}

	/**
	 * Applies the negate operation
	 *
	 * @param value The operand
	 * @return The result
	 * @throws IllegalArgumentException If the operator cannot be applied to the given operand
	 */
	public static ObservableValue<? extends Number> negate(ObservableValue<?> value) throws IllegalArgumentException {
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

	/**
	 * Applies the complement operation
	 *
	 * @param value The operand
	 * @return The result
	 * @throws IllegalArgumentException If the operator cannot be applied to the given operand
	 */
	public static ObservableValue<? extends Number> complement(ObservableValue<?> value) throws IllegalArgumentException {
		if (!isIntMathable(value.getType()))
			throw new IllegalArgumentException("Complement cannot be applied to operand type " + value.getType());
		ObservableValue<? extends Number> n = mathableToNumber(value);
		if (LONG.isAssignableFrom(n.getType().unwrap()))
			return ((ObservableValue<Long>) n).mapV(v -> ~v);
		else
			return ((ObservableValue<? extends Number>) n).mapV(v -> ~v.intValue());
	}

	/**
	 * Applies the addition operation
	 *
	 * @param v1 The first operand
	 * @param v2 The second operand
	 * @return The result
	 * @throws IllegalArgumentException If the operator cannot be applied to the given operands
	 */
	public static ObservableValue<? extends Number> add(ObservableValue<?> v1, ObservableValue<?> v2) throws IllegalArgumentException {
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

	/**
	 * Applies the subtraction operation
	 *
	 * @param v1 The first operand
	 * @param v2 The second operand
	 * @return The result
	 * @throws IllegalArgumentException If the operator cannot be applied to the given operands
	 */
	public static ObservableValue<? extends Number> subtract(ObservableValue<?> v1, ObservableValue<?> v2) throws IllegalArgumentException {
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

	/**
	 * Applies the multiplication operation
	 *
	 * @param v1 The first operand
	 * @param v2 The second operand
	 * @return The result
	 * @throws IllegalArgumentException If the operator cannot be applied to the given operands
	 */
	public static ObservableValue<? extends Number> multiply(ObservableValue<?> v1, ObservableValue<?> v2) throws IllegalArgumentException {
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

	/**
	 * Applies the division operation
	 *
	 * @param v1 The first operand
	 * @param v2 The second operand
	 * @return The result
	 * @throws IllegalArgumentException If the operator cannot be applied to the given operands
	 */
	public static ObservableValue<? extends Number> divide(ObservableValue<?> v1, ObservableValue<?> v2) throws IllegalArgumentException {
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

	/**
	 * Applies the modulus operation
	 *
	 * @param v1 The first operand
	 * @param v2 The second operand
	 * @return The result
	 * @throws IllegalArgumentException If the operator cannot be applied to the given operands
	 */
	public static ObservableValue<? extends Number> modulo(ObservableValue<?> v1, ObservableValue<?> v2) throws IllegalArgumentException {
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

	/**
	 * Applies the left shift operation
	 *
	 * @param v1 The first operand
	 * @param v2 The second operand
	 * @return The result
	 * @throws IllegalArgumentException If the operator cannot be applied to the given operands
	 */
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

	/**
	 * Applies the right shift operation
	 *
	 * @param v1 The first operand
	 * @param v2 The second operand
	 * @return The result
	 * @throws IllegalArgumentException If the operator cannot be applied to the given operands
	 */
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

	/**
	 * Applies the unsigned right shift operation
	 *
	 * @param v1 The first operand
	 * @param v2 The second operand
	 * @return The result
	 * @throws IllegalArgumentException If the operator cannot be applied to the given operands
	 */
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

	/**
	 * Applies the binary and operation
	 *
	 * @param v1 The first operand
	 * @param v2 The second operand
	 * @return The result
	 * @throws IllegalArgumentException If the operator cannot be applied to the given operands
	 */
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

	/**
	 * Applies the binary or operation
	 *
	 * @param v1 The first operand
	 * @param v2 The second operand
	 * @return The result
	 * @throws IllegalArgumentException If the operator cannot be applied to the given operands
	 */
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

	/**
	 * Applies the binary exclusive or operation
	 *
	 * @param v1 The first operand
	 * @param v2 The second operand
	 * @return The result
	 * @throws IllegalArgumentException If the operator cannot be applied to the given operands
	 */
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

	/**
	 * Applies the boolean and operation
	 *
	 * @param v1 The first operand
	 * @param v2 The second operand
	 * @return The result
	 * @throws IllegalArgumentException If the operator cannot be applied to the given operands
	 */
	public static ObservableValue<Boolean> and(ObservableValue<?> v1, ObservableValue<?> v2){
		if (BOOLEAN.isAssignableFrom(v1.getType().unwrap()) && BOOLEAN.isAssignableFrom(v2.getType().unwrap())) {
			return ((ObservableValue<Boolean>) v1).combineV(BOOLEAN, (b1, b2) -> b1 && b2, (ObservableValue<Boolean>) v2, true);
		} else
			throw new IllegalArgumentException("And cannot be applied to operand types " + v1.getType() + " and " + v2.getType());
	}

	/**
	 * Applies the boolean or operation
	 *
	 * @param v1 The first operand
	 * @param v2 The second operand
	 * @return The result
	 * @throws IllegalArgumentException If the operator cannot be applied to the given operands
	 */
	public static ObservableValue<Boolean> or(ObservableValue<?> v1, ObservableValue<?> v2){
		if (BOOLEAN.isAssignableFrom(v1.getType().unwrap()) && BOOLEAN.isAssignableFrom(v2.getType().unwrap())) {
			return ((ObservableValue<Boolean>) v1).combineV(BOOLEAN, (b1, b2) -> b1 || b2, (ObservableValue<Boolean>) v2, true);
		} else
			throw new IllegalArgumentException("Or cannot be applied to operand types " + v1.getType() + " and " + v2.getType());
	}
}
