package org.muis.util;

import java.util.*;

/**
 * Combines more than one sorted set
 *
 * @param <E> The type of elements in the set
 */
public class AggregateSortedSet<E> implements SortedSet<E> {
	private SortedSet<E> [] theComponents;

	/** @param components The component sets to aggregate */
	public AggregateSortedSet(SortedSet<E>... components) {
		theComponents = components;
	}

	@Override
	public int size() {
		int ret = 0;
		for(SortedSet<? extends E> comp : theComponents)
			ret += comp.size();
		return ret;
	}

	@Override
	public boolean isEmpty() {
		for(SortedSet<? extends E> comp : theComponents)
			if(!comp.isEmpty())
				return false;
		return true;
	}

	@Override
	public boolean contains(Object o) {
		for(SortedSet<? extends E> comp : theComponents)
			if(comp.contains(o))
				return true;
		return false;
	}

	@Override
	public Iterator<E> iterator() {
		if(theComponents.length == 0)
			return Collections.EMPTY_LIST.iterator();
		return new Iterator<E>() {
			private Iterator<E> [] theComponentIters;
			private List<E> theLatest;
			private Comparator<? super E> theComparator = theComponents[0].comparator();

			{
				theComponentIters = new Iterator[theComponents.length];
				theLatest = new ArrayList<>();
				for(int i = 0; i < theComponents.length; i++) {
					theComponentIters[i] = theComponents[i].iterator();
					theLatest.add(null);
				}
			}

			@Override
			public boolean hasNext() {
				for(int i = 0; i < theComponentIters.length; i++) {
					if(theLatest.get(i) != null || theComponentIters[i].hasNext())
						return true;
				}
				return false;
			}

			@Override
			public E next() {
				E best = null;
				for(int i = 0; i < theComponentIters.length; i++) {
					E test = theLatest.get(i);
					if(test == null) {
						if(!theComponentIters[i].hasNext())
							continue;
						test = theComponentIters[i].next();
					}
					if(best == null || theComparator.compare(best, test) < 1) {
						best = test;
						test = null;
					}
					theLatest.set(i, test);
				}
				return best;
			}
		};
	}

	@Override
	public Object [] toArray() {
		ArrayList<E> ret = new ArrayList<>();
		for(E el : this)
			ret.add(el);
		return ret.toArray();
	}

	@Override
	public <T> T [] toArray(T [] a) {
		ArrayList<E> ret = new ArrayList<>();
		for(E el : this)
			ret.add(el);
		return ret.toArray(a);
	}

	@Override
	public boolean add(E e) {
		throw new UnsupportedOperationException(getClass().getName() + " does not support modification");
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException(getClass().getName() + " does not support modification");
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		ArrayList<Object> c2 = new ArrayList<>(c);

		for(SortedSet<? extends E> comp : theComponents) {
			if(c2.isEmpty())
				break;
			Iterator<Object> iter = c2.iterator();
			while(iter.hasNext()) {
				if(comp.contains(iter.next()))
					iter.remove();
			}
		}
		return c2.isEmpty();
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		throw new UnsupportedOperationException(getClass().getName() + " does not support modification");
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException(getClass().getName() + " does not support modification");
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException(getClass().getName() + " does not support modification");
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException(getClass().getName() + " does not support modification");
	}

	@Override
	public Comparator<? super E> comparator() {
		return null;
	}

	@Override
	public SortedSet<E> subSet(E fromElement, E toElement) {
		throw new UnsupportedOperationException(getClass().getName() + " does not support subsets");
	}

	@Override
	public SortedSet<E> headSet(E toElement) {
		throw new UnsupportedOperationException(getClass().getName() + " does not support subsets");
	}

	@Override
	public SortedSet<E> tailSet(E fromElement) {
		throw new UnsupportedOperationException(getClass().getName() + " does not support subsets");
	}

	@Override
	public E first() {
		if(theComponents.length == 0)
			throw new java.util.NoSuchElementException();
		Comparator<? super E> comparator = theComponents[0].comparator();
		E best = null;
		for(SortedSet<? extends E> comp : theComponents) {
			if(comp.isEmpty())
				continue;
			E test = comp.first();
			if(best == null || comparator.compare(best, test) < 0)
				best = test;
		}
		if(best == null)
			throw new java.util.NoSuchElementException();
		return best;
	}

	@Override
	public E last() {
		if(theComponents.length == 0)
			throw new java.util.NoSuchElementException();
		Comparator<? super E> comparator = theComponents[0].comparator();
		E best = null;
		for(SortedSet<? extends E> comp : theComponents) {
			if(comp.isEmpty())
				continue;
			E test = comp.last();
			if(best == null || comparator.compare(best, test) > 0)
				best = test;
		}
		if(best == null)
			throw new java.util.NoSuchElementException();
		return best;
	}
}
