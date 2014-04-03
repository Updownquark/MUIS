package org.muis.core.event.boole;

public class TPAnd<F, M, T> implements IntersectTypedPredicate<F, M, T> {
	private final TypedPredicate<F, M> theFirst;
	private final TypedPredicate<M, T> theSecond;

	public TPAnd(TypedPredicate<F, M> first, TypedPredicate<M, T> second) {
		theFirst = first;
		theSecond = second;
	}

	public TypedPredicate<F, M> getFirst() {
		return theFirst;
	}

	public TypedPredicate<M, T> getSecond() {
		return theSecond;
	}

	@Override
	public T cast(F value) {
		M middle = theFirst.cast(value);
		if(middle == null)
			return null;
		return theSecond.cast(middle);
	}
}
