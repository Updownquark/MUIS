package org.muis.util;

import java.util.*;

import org.muis.core.MuisElement;
import org.muis.core.event.ChildEvent;
import org.muis.core.mgr.AbstractElementList;
import org.observe.Observer;

/**
 * A simple element list
 *
 * @param <E> The type of the elements in the list
 */
public class SimpleElementList<E extends MuisElement> extends AbstractElementList<E> {
	private ArrayList<E> theElements;

	/**
	 * @param parent The parent for this element list
	 * @param children The initial set of children for this list
	 */
	public SimpleElementList(MuisElement parent, E... children) {
		super(parent);
		theElements = new ArrayList<>(children.length == 0 ? 5 : children.length);
		addAll(children);
	}

	@Override
	public Runnable internalSubscribe(Observer<? super ChildEvent> observer) {
		return () -> {
		};
		// Listeners not supported
	}

	@Override
	public E [] toArray() {
		return (E []) theElements.toArray();
	}

	@Override
	public boolean addAll(MuisElement [] children) {
		return addAll((List<E>) java.util.Arrays.asList(children));
	}

	@Override
	public boolean addAll(int index, MuisElement [] children) {
		return addAll(index, (List<E>) java.util.Arrays.asList(children));
	}

	@Override
	public int size() {
		return theElements.size();
	}

	@Override
	public Iterator<E> iterator() {
		return theElements.iterator();
	}

	@Override
	public boolean add(E e) {
		return theElements.add(e);
	}

	@Override
	public boolean remove(Object o) {
		return theElements.remove(o);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		return theElements.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		return theElements.addAll(index, c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return theElements.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return theElements.retainAll(c);
	}

	@Override
	public void clear() {
		theElements.clear();
	}

	@Override
	public E get(int index) {
		return theElements.get(index);
	}

	@Override
	public E set(int index, E element) {
		return theElements.set(index, element);
	}

	@Override
	public void add(int index, E element) {
		theElements.add(index, element);
	}

	@Override
	public E remove(int index) {
		return theElements.remove(index);
	}

	@Override
	public ListIterator<E> listIterator() {
		return theElements.listIterator();
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return theElements.listIterator(index);
	}
}
