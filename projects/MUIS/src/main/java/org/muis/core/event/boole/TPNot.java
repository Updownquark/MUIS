package org.muis.core.event.boole;

public class TPNot<T> implements TypedPredicate<T, T> {
	private final TypedPredicate<T, T> theWrapped;

	public TPNot(TypedPredicate<T, T> wrap) {
		theWrapped = wrap;
	}

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
