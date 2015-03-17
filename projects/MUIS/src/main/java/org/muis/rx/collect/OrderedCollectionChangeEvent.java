package org.muis.rx.collect;

import java.util.Collection;

import prisms.util.IntList;

public class OrderedCollectionChangeEvent<E> extends CollectionChangeEvent<E> {
	public IntList indexes;

	public OrderedCollectionChangeEvent(CollectionChangeType type, Collection<E> values, IntList idxs) {
		super(type, values);
		indexes = idxs;
		indexes.seal();
	}
}
