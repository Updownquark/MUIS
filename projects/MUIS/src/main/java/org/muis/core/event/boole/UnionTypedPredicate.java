package org.muis.core.event.boole;

import java.util.List;

public interface UnionTypedPredicate<F, T> extends TypedPredicate<F, T> {
	List<TypedPredicate<? super F, ? extends T>> getComponents();
}
