package org.muis.core.mgr;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import org.muis.core.MuisElement;
import org.muis.core.event.ChildEvent;
import org.muis.core.event.MuisEvent;
import org.muis.core.event.MuisEventCondition;
import org.muis.core.event.MuisEventListener;
import org.muis.core.event.boole.ConditionTree;
import org.muis.core.event.boole.TypedPredicate;

import prisms.util.ArrayUtils;

/**
 * A partial implementation of ElementList
 *
 * @param <E> The type of elements in the list
 */
public abstract class AbstractElementList<E extends MuisElement> implements ElementList<E> {
	private java.util.concurrent.CopyOnWriteArrayList<MuisEventListener<ChildEvent>> theListeners;
	private final ConditionTree<MuisEvent, MuisEventListener<?>> theChildListeners;
	private final EventListenerManager theEvents;

	private final MuisElement theParent;

	/** @param parent The parent to manage the children of */
	public AbstractElementList(MuisElement parent) {
		theListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
		theChildListeners = new ConditionTree<>();
		theParent = parent;
		theEvents = new EventListenerManager() {
			@Override
			public <T extends MuisEvent> EventListenerManager listen(MuisEventCondition<T> condition, MuisEventListener<T>... listeners) {
				return listen(condition.getTester(), listeners);
			}

			@Override
			public <T extends MuisEvent> EventListenerManager remove(MuisEventCondition<T> condition, MuisEventListener<T>... listeners) {
				return remove(condition.getTester(), listeners);
			}

			@Override
			public <T extends MuisEvent> EventListenerManager listen(TypedPredicate<MuisEvent, T> condition,
				MuisEventListener<T>... listeners) {
				theChildListeners.add(condition, listeners);
				for(E child : AbstractElementList.this)
					child.events().listen(condition, listeners);
				return this;
			}

			@Override
			public <T extends MuisEvent> EventListenerManager remove(TypedPredicate<MuisEvent, T> condition,
				MuisEventListener<T>... listeners) {
				theChildListeners.remove(condition, listeners);
				for(E child : AbstractElementList.this)
					child.events().remove(condition, listeners);
				return this;
			}
		};
	}

	@Override
	public void addListener(MuisEventListener<ChildEvent> listener) {
		theListeners.add(listener);
	}

	@Override
	public void removeListener(MuisEventListener<ChildEvent> listener) {
		theListeners.remove(listener);
	}

	@Override
	public MuisElement getParent() {
		return theParent;
	}

	@Override
	public final EventListenerManager events() {
		return theEvents;
	}

	/**
	 * Called by implementation after a child is added to the list
	 *
	 * @param child The child that was added to the list
	 */
	protected void childAdded(MuisElement child) {
		for(Entry<? extends TypedPredicate<? extends MuisEvent, ?>, List<MuisEventListener<?>>> entry : theChildListeners.entries())
			for(MuisEventListener<?> listener : entry.getValue())
				child.events().listen((TypedPredicate<MuisEvent, MuisEvent>) entry.getKey(), (MuisEventListener<MuisEvent>) listener);
	}

	/**
	 * Called by implementation after a child is removed from the list
	 *
	 * @param child The child that was added to the list
	 */
	protected void childRemoved(MuisElement child) {
		for(Entry<? extends TypedPredicate<? extends MuisEvent, ?>, List<MuisEventListener<?>>> entry : theChildListeners.entries())
			for(MuisEventListener<?> listener : entry.getValue())
				child.events().remove((TypedPredicate<MuisEvent, MuisEvent>) entry.getKey(), (MuisEventListener<MuisEvent>) listener);
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
		for(MuisEventListener<ChildEvent> listener : theListeners)
			listener.eventOccurred(evt);
	}
}
