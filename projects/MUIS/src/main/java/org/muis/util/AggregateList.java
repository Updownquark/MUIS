package org.muis.util;

import java.util.*;

import prisms.util.ArrayUtils;

/**
 * Aggregates a number of lists into a single list
 * 
 * @param <E> The type of this list
 */
public class AggregateList<E> implements List<E> {
	private final List<? extends E> [] theComponents;

	private final Collection<?> theIgnores;

	/**
	 * @param ignores The values to ignore. These will not be part of this collection even if they exist in the component lists.
	 * @param components The component lists to combine into this list
	 */
	public AggregateList(Collection<?> ignores, List<? extends E>... components) {
		theComponents = components;
		theIgnores = ignores;
	}

	@Override
	public int size() {
		int ret = 0;
		for(List<? extends E> comp : theComponents)
			ret += comp.size();
		return ret;
	}

	@Override
	public boolean isEmpty() {
		for(List<? extends E> comp : theComponents)
			if(!comp.isEmpty())
				return false;
		return true;
	}

	@Override
	public boolean contains(Object o) {
		for(List<? extends E> comp : theComponents)
			if(comp.contains(o))
				return true;
		return false;
	}

	@Override
	public Iterator<E> iterator() {
		return ArrayUtils.conditionalIterator(ArrayUtils.iterable(theComponents).iterator(), new ArrayUtils.Accepter<E, E>() {
			@Override
			public E accept(E value) {
				return (theIgnores != null && theIgnores.contains(value)) ? null : value;
			}
		}, false);
	}

	@Override
	public Object [] toArray() {
		ArrayList<E> ret = new ArrayList<>();
		for(List<? extends E> comp : theComponents)
			ret.addAll(comp);
		return ret.toArray();
	}

	@Override
	public <T> T [] toArray(T [] a) {
		ArrayList<E> ret = new ArrayList<>();
		for(List<? extends E> comp : theComponents)
			ret.addAll(comp);
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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		throw new UnsupportedOperationException(getClass().getName() + " does not support modification");
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		throw new UnsupportedOperationException(getClass().getName() + " does not support modification");
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException(getClass().getName() + " does not support modification");
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException(getClass().getName() + " does not support modification");
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException(getClass().getName() + " does not support modification");
	}

	@Override
	public E get(int index) {
		int i = 0;
		for(List<? extends E> comp : theComponents) {
			if(index - 1 < comp.size())
				return comp.get(index - 1);
			i += comp.size();
		}
		throw new IndexOutOfBoundsException(index + " of " + i);
	}

	@Override
	public E set(int index, E element) {
		throw new UnsupportedOperationException(getClass().getName() + " does not support modification");
	}

	@Override
	public void add(int index, E element) {
		throw new UnsupportedOperationException(getClass().getName() + " does not support modification");
	}

	@Override
	public E remove(int index) {
		throw new UnsupportedOperationException(getClass().getName() + " does not support modification");
	}

	@Override
	public int indexOf(Object o) {
		int i = 0;
		for(List<? extends E> comp : theComponents) {
			int index = comp.indexOf(o);
			if(index >= 0)
				return i + index;
			i += comp.size();
		}
		return -1;
	}

	@Override
	public int lastIndexOf(Object o) {
		int i = size();
		for(List<? extends E> comp : theComponents) {
			i -= comp.size();
			int index = comp.lastIndexOf(o);
			if(index >= 0)
				return i + index;
		}
		return -1;
	}

	@Override
	public ListIterator<E> listIterator() {
		throw new UnsupportedOperationException(getClass().getName() + " does not support list iterators");
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		throw new UnsupportedOperationException(getClass().getName() + " does not support list iterators");
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException(getClass().getName() + " does not support sublists");
	}
}
