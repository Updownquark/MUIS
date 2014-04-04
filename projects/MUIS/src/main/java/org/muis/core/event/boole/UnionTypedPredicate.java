package org.muis.core.event.boole;

import java.util.List;

/**
 * Represents the OR operation of more than one child predicate
 * 
 * @param <F> The type of object that this predicate can test
 * @param <T> The type of object that this predicate can produce
 */
public interface UnionTypedPredicate<F, T> extends TypedPredicate<F, T> {
	/** @return This OR operation's component testers */
	List<TypedPredicate<? super F, ? extends T>> getComponents();
}
