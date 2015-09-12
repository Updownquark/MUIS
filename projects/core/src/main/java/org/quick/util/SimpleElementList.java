package org.quick.util;

import java.util.*;

import org.observe.Observer;
import org.observe.Subscription;
import org.quick.core.QuickElement;
import org.quick.core.event.ChildEvent;
import org.quick.core.mgr.AbstractElementList;

/**
 * A simple element list
 *
 * @param <E> The type of the elements in the list
 */
public class SimpleElementList<E extends QuickElement> extends AbstractElementList<E> {
	private ArrayList<E> theElements;

	/**
	 * @param parent The parent for this element list
	 * @param children The initial set of children for this list
	 */
	public SimpleElementList(QuickElement parent, E... children) {
		super(parent);
		theElements = new ArrayList<>(children.length == 0 ? 5 : children.length);
		addAll(children);
	}

	@Override
	public Subscription subscribe(Observer<? super ChildEvent> observer) {
		return () -> {
		};
		// Listeners not supported
	}

	@Override
	public E [] toArray() {
		return (E []) theElements.toArray();
	}

	@Override
	public boolean addAll(QuickElement [] children) {
		return addAll((List<E>) java.util.Arrays.asList(children));
	}

	@Override
	public boolean addAll(int index, QuickElement [] children) {
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
