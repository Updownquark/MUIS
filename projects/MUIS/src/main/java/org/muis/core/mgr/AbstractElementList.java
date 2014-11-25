package org.muis.core.mgr;

import java.util.Collection;
import java.util.List;

import org.muis.core.MuisElement;
import org.muis.core.event.ChildEvent;
import org.muis.core.event.MuisEvent;
import org.muis.core.rx.DefaultObservable;
import org.muis.core.rx.Observable;
import org.muis.core.rx.Observer;

import prisms.util.ArrayUtils;

/**
 * A partial implementation of ElementList
 *
 * @param <E> The type of elements in the list
 */
public abstract class AbstractElementList<E extends MuisElement> extends DefaultObservable<ChildEvent> implements ElementList<E> {
	private final Observer<ChildEvent> theController;
	private final DefaultObservable<MuisEvent> theEvents;
	private final Observer<MuisEvent> theEventController;

	private final MuisElement theParent;

	/** @param parent The parent to manage the children of */
	public AbstractElementList(MuisElement parent) {
		theParent = parent;
		theController = control(null);
		theEvents = new DefaultObservable<>();
		theEventController = theEvents.control(null);
		// On child add, subscribe to child events until the child is removed
		filterMap(ChildEvent.child.add()).act(
			evt -> {
				evt.getChild().events().takeUntil(filterMap(ChildEvent.child.remove().forChild(evt.getChild())))
					.subscribe(new Observer<MuisEvent>() {
						@Override
						public <V extends MuisEvent> void onNext(V value) {
							theEventController.onNext(value);
						}

						@Override
						public void onError(Throwable e) {
							theEventController.onError(e);
						}
					});
			});
	}

	@Override
	public MuisElement getParent() {
		return theParent;
	}

	@Override
	public final Observable<MuisEvent> events() {
		return theEvents;
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public boolean contains(Object o) {
		return ArrayUtils.contains(toArray(), o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		MuisElement [] children = toArray();
		for(Object o : c) {
			if(!(o instanceof MuisElement))
				return false;
			if(!ArrayUtils.contains(children, o))
				return false;
		}
		return true;
	}

	@Override
	public MuisElement getLast() {
		MuisElement [] children = toArray();
		if(children.length == 0)
			throw new java.util.NoSuchElementException();
		return children[children.length - 1];
	}

	@Override
	public int indexOf(Object o) {
		return ArrayUtils.indexOf(toArray(), o);
	}

	@Override
	public int lastIndexOf(Object o) {
		MuisElement [] children = toArray();
		for(int i = children.length; i >= 0; i--)
			if(children[i].equals(o))
				return i;
		return -1;
	}

	@Override
	public <T> T [] toArray(T [] a) {
		MuisElement [] children = toArray();
		if(children.length > a.length)
			a = (T []) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), children.length);
		System.arraycopy(children, 0, a, 0, children.length);
		return a;
	}

	@Override
	public final MuisElement [] at(int x, int y) {
		MuisElement [] children = sortByZ();
		MuisElement [] ret = new MuisElement[0];
		for(MuisElement child : children) {
			int relX = x - child.bounds().getX();
			if(relX < 0 || relX >= child.bounds().getWidth())
				continue;
			int relY = y - child.bounds().getY();
			if(relY < 0 || relY >= child.bounds().getHeight())
				continue;
			ret = ArrayUtils.add(ret, child);
		}
		return ret;
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		return new SubList<>(this, fromIndex, toIndex);
	}

	@Override
	public MuisElement [] sortByZ() {
		MuisElement [] children = toArray();
		if(children.length < 2)
			return children;
		boolean sameZ = true;
		int z = children[0].getZ();
		for(int c = 1; c < children.length; c++)
			if(children[c].getZ() != z) {
				sameZ = false;
				break;
			}
		if(!sameZ) {
			java.util.Arrays.sort(children, (MuisElement el1, MuisElement el2) -> {
				return el1.getZ() - el2.getZ();
			});
		}
		return children;
	}

	/** @param evt The ChildEvent to fire */
	protected void fireEvent(ChildEvent evt) {
		theController.onNext(evt);
	}
}
