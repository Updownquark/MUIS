package org.muis.core.event.boole;

public interface TypedPredicate<F, T> {
	T cast(F value);
}
