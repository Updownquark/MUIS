package org.muis.core.event.boole;

public class TPAnd<F, MT extends MF, MF, T> implements IntersectTypedPredicate<F, MT, MF, T> {
	private final TypedPredicate<F, MT> theFirst;
	private final TypedPredicate<MF, T> theSecond;

	public TPAnd(TypedPredicate<F, MT> first, TypedPredicate<MF, T> second) {
		theFirst = first;
		theSecond = second;
	}

	@Override
	public TypedPredicate<F, MT> getFirst() {
		return theFirst;
	}

	@Override
	public TypedPredicate<MF, T> getSecond() {
		return theSecond;
	}

	@Override
	public T cast(F value) {
		MT middle = theFirst.cast(value);
		if(middle == null)
			return null;
		return theSecond.cast(middle);
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == this)
			return true;
		if(!(obj instanceof TPAnd))
			return false;
		return theFirst.equals(((TPAnd<?, ?, ?, ?>) obj).theFirst) && theSecond.equals(((TPAnd<?, ?, ?, ?>) obj).theSecond);
	}
}
