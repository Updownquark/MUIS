package org.quick.core.event.boole;

/**
 * A negating operation
 * 
 * @param <T> The type of value that this operation operates against
 */
public class TPNot<T> implements TypedPredicate<T, T> {
	private final TypedPredicate<T, T> theWrapped;

	/** @param wrap The operation to negate */
	public TPNot(TypedPredicate<T, T> wrap) {
		theWrapped = wrap;
	}

	/** @return The negated operation */
	public TypedPredicate<T, T> getWrapped() {
		return theWrapped;
	}

	@Override
	public T cast(T value) {
		if(theWrapped.cast(value) != null)
			return null;
		else
			return value;
	}
}
