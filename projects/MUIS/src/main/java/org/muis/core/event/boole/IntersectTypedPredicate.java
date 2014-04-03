package org.muis.core.event.boole;

public interface IntersectTypedPredicate<F, MT extends MF, MF, T> extends TypedPredicate<F, T> {
	TypedPredicate<F, MT> getFirst();

	TypedPredicate<MF, T> getSecond();
}
