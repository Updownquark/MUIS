package org.muis.rx.collect;

import java.util.Collection;

public class CollectionChangeEvent<E> {
	public final CollectionChangeType type;
	public final Collection<E> values;

	public CollectionChangeEvent(CollectionChangeType aType, Collection<E> val) {
		type = aType;
		values = val;
	}
}
