package org.muis.core.mgr;

import static org.muis.core.MuisConstants.Events.CHILD_ADDED;
import static org.muis.core.MuisConstants.Events.CHILD_REMOVED;

import java.util.*;

import org.muis.core.MuisElement;
import org.muis.core.event.MuisEvent;
import org.muis.core.event.MuisEventListener;
import org.muis.core.event.MuisEventType;

import prisms.arch.event.ListenerManager;
import prisms.util.ArrayUtils;

/** A list that manages child elements for a parent element */
public class ChildList implements List<MuisElement> {
	@SuppressWarnings("rawtypes")
	private final ListenerManager<MuisEventListener> theChildListeners;

	private final MuisElement theParent;

	private MuisElement [] theChildren;

	/** @param parent The parent to manage the children of */
	public ChildList(MuisElement parent) {
		theChildListeners = new ListenerManager<>(MuisEventListener.class);
		theParent = parent;
		theChildren = new MuisElement[0];
	}

	/** @return The parent whose children this list manages */
	public MuisElement getParent() {
		return theParent;
	}

	private void childAdded(MuisElement child) {
		for(Object type : theChildListeners.getAllProperties())
			for(MuisEventListener<Object> listener : theChildListeners.getRegisteredListeners(type))
				child.addListener((MuisEventType<Object>) type, listener);
	}

	private void childRemoved(MuisElement child) {
		for(Object type : theChildListeners.getAllProperties())
			for(MuisEventListener<Object> listener : theChildListeners.getRegisteredListeners(type))
				child.removeListener(listener);
	}

	/**
	 * Adds a listener for an event type to this element's direct children
	 *
	 * @param <T> The type of the property that the event represents
	 * @param type The event type to listen for
	 * @param listener The listener to notify when an event of the given type occurs
	 */
	public final <T> void addChildListener(MuisEventType<T> type, MuisEventListener<? super T> listener) {
		theChildListeners.addListener(type, listener);
		for(MuisElement child : theChildren)
			child.addListener(type, listener);
	}

	/** @param listener The listener to remove from this element's children */
	public final void removeChildListener(MuisEventListener<?> listener) {
		theChildListeners.removeListener(listener);
		for(MuisElement child : theChildren)
			child.removeListener(listener);
	}

	@Override
	public int size() {
		return theChildren.length;
	}

	@Override
	public boolean isEmpty() {
		return theChildren.length == 0;
	}

	@Override
	public boolean contains(Object o) {
		return ArrayUtils.contains(theChildren, o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		MuisElement [] children = theChildren;
		for(Object o : c) {
			if(!(o instanceof MuisElement))
				return false;
			if(!ArrayUtils.contains(children, o))
				return false;
		}
		return true;
	}

	@Override
	public int indexOf(Object o) {
		return ArrayUtils.indexOf(theChildren, o);
	}

	@Override
	public int lastIndexOf(Object o) {
		MuisElement [] children = theChildren;
		for(int i = children.length; i >= 0; i--)
			if(children[i].equals(o))
				return i;
		return -1;
	}

	@Override
	public MuisElement get(int index) {
		return theChildren[index];
	}

	/** @return The last child in this list */
	public MuisElement getLast() {
		MuisElement [] children = theChildren;
		return children[children.length - 1];
	}

	@Override
	public MuisElement [] toArray() {
		return theChildren.clone();
	}

	@Override
	public <T> T [] toArray(T [] a) {
		MuisElement [] children = theChildren;
		if(children.length > a.length)
			a = (T []) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), children.length);
		System.arraycopy(children, 0, a, 0, children.length);
		return a;
	}

	/**
	 * @return A list containing the children between the given indices. The returned list will have no link back to this list, so
	 *         operations on the returned list will succeed but have no effect on the parent's children.
	 * @see java.util.List#subList(int, int)
	 */
	@Override
	public List<MuisElement> subList(int fromIndex, int toIndex) {
		MuisElement [] children = theChildren;
		if(fromIndex < 0 || fromIndex >= toIndex || toIndex >= children.length)
			throw new IndexOutOfBoundsException("Length: " + children.length + ", from: " + fromIndex + ", to: " + toIndex);
		ArrayList<MuisElement> ret = new ArrayList<>();
		for(int i = fromIndex; i < toIndex; i++)
			ret.add(children[i]);
		return ret;
	}

	/**
	 * @param x The x-coordinate of a point relative to this element's upper left corner
	 * @param y The y-coordinate of a point relative to this element's upper left corner
	 * @return All children of this element whose bounds contain the given point
	 */
	public final MuisElement [] at(int x, int y) {
		MuisElement [] children = sortByZ();
		MuisElement [] ret = new MuisElement[0];
		for(MuisElement child : children) {
			int relX = x - child.getX();
			if(relX < 0 || relX >= child.getWidth())
				continue;
			int relY = y - child.getY();
			if(relY < 0 || relY >= child.getHeight())
				continue;
			ret = ArrayUtils.add(ret, child);
		}
		return ret;
	}

	/**
	 * This operation is useful for rendering children in correct sequence and in determining which elements should receive events first.
	 *
	 * @return The children in this list, sorted by z-index
	 */
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
			java.util.Arrays.sort(children, new java.util.Comparator<MuisElement>() {
				@Override
				public int compare(MuisElement el1, MuisElement el2) {
					return el1.getZ() - el2.getZ();
				}
			});
		}
		return children;
	}

	MuisLock lock() {
		return theParent.getDocument().getLocker().lock(MuisElement.CHILDREN_LOCK_TYPE, theParent, true);
	}

	@Override
	public MuisElement set(int index, MuisElement child) {
		MuisElement oldChild;
		try (MuisLock lock = lock()) {
			MuisElement [] children = theChildren;
			oldChild = children[index];
			if(oldChild == child)
				return child;
			children = children.clone();
			children[index] = child;
			theChildren = children;
		}
		childRemoved(oldChild);
		theParent.fireEvent(new MuisEvent<MuisElement>(CHILD_REMOVED, oldChild), false, false);
		theParent.fireEvent(new MuisEvent<MuisElement>(CHILD_ADDED, child), false, false);
		childAdded(child);
		return oldChild;
	}

	@Override
	public boolean add(MuisElement child) {
		if(child == null)
			return false;
		try (MuisLock lock = lock()) {
			MuisElement [] children = theChildren;
			if(ArrayUtils.contains(children, child))
				return false;
			children = ArrayUtils.add(children, child);
		}
		theParent.fireEvent(new MuisEvent<MuisElement>(CHILD_ADDED, child), false, false);
		childAdded(child);
		return true;
	}

	@Override
	public void add(int index, MuisElement child) {
		if(child == null)
			throw new NullPointerException();
		try (MuisLock lock = theParent.getDocument().getLocker().lock(MuisElement.CHILDREN_LOCK_TYPE, theParent, true)) {
			if(index < 0)
				index = theChildren.length;
			theChildren = ArrayUtils.add(theChildren, child, index);
		}
		theParent.fireEvent(new MuisEvent<MuisElement>(CHILD_ADDED, child), false, false);
		childAdded(child);
	}

	@Override
	public MuisElement remove(int index) {
		MuisElement oldChild;
		try (MuisLock lock = lock()) {
			MuisElement [] children = theChildren;
			oldChild = children[index];
			children = ArrayUtils.remove(theChildren, index);
			theChildren = children;
		}
		childRemoved(oldChild);
		theParent.fireEvent(new MuisEvent<MuisElement>(CHILD_REMOVED, oldChild), false, false);
		return oldChild;
	}

	@Override
	public boolean remove(Object o) {
		if(!(o instanceof MuisElement))
			return false;
		MuisElement oldChild;
		try (MuisLock lock = lock()) {
			MuisElement [] children = theChildren;
			int index = ArrayUtils.indexOf(children, (MuisElement) o);
			if(index < 0)
				return false;
			oldChild = children[index];
			children = ArrayUtils.remove(theChildren, index);
			theChildren = children;
		}
		childRemoved(oldChild);
		theParent.fireEvent(new MuisEvent<MuisElement>(CHILD_REMOVED, oldChild), false, false);
		return true;
	}

	@Override
	public void clear() {
		MuisElement [] children;
		try (MuisLock lock = lock()) {
			if(theChildren.length == 0)
				return;
			children = theChildren;
			theChildren = new MuisElement[0];
		}
		for(MuisElement child : children) {
			childRemoved(child);
			theParent.fireEvent(new MuisEvent<MuisElement>(CHILD_REMOVED, child), false, false);
		}
	}

	@Override
	public boolean addAll(Collection<? extends MuisElement> children) {
		ArrayList<MuisElement> toAdd = new ArrayList<>();
		try (MuisLock lock = lock()) {
			MuisElement [] newChildren = theChildren;
			for(MuisElement child : children) {
				if(child == null)
					continue;
				if(ArrayUtils.contains(newChildren, child) || toAdd.contains(child))
					continue;
				toAdd.add(child);
			}
			if(!toAdd.isEmpty()) {
				newChildren = ArrayUtils.addAll(newChildren, toAdd.toArray(new MuisElement[toAdd.size()]));
				theChildren = newChildren;
			}
		}

		for(MuisElement child : toAdd) {
			theParent.fireEvent(new MuisEvent<MuisElement>(CHILD_ADDED, child), false, false);
			childAdded(child);
		}
		return !toAdd.isEmpty();
	}

	/**
	 * @param children The children to add to this list
	 * @return Whether the list was modified
	 */
	public boolean addAll(MuisElement [] children) {
		return addAll(Arrays.asList(children));
	}

	@Override
	public boolean addAll(int index, Collection<? extends MuisElement> children) {
		ArrayList<MuisElement> toAdd = new ArrayList<>();
		try (MuisLock lock = lock()) {
			MuisElement [] cacheChildren = theChildren;
			for(MuisElement child : children) {
				if(child == null)
					continue;
				if(ArrayUtils.contains(cacheChildren, child) || toAdd.contains(child))
					continue;
				toAdd.add(child);
			}
			if(!toAdd.isEmpty()) {
				MuisElement [] newChildren2 = new MuisElement[cacheChildren.length + toAdd.size()];
				System.arraycopy(cacheChildren, 0, newChildren2, 0, index);
				System.arraycopy(cacheChildren, index, toAdd.toArray(new MuisElement[toAdd.size()]), 0, toAdd.size());
				System.arraycopy(cacheChildren, index, newChildren2, index + toAdd.size(), cacheChildren.length - index);
				theChildren = newChildren2;
			}
		}

		for(MuisElement child : toAdd) {
			theParent.fireEvent(new MuisEvent<MuisElement>(CHILD_ADDED, child), false, false);
			childAdded(child);
		}
		return !toAdd.isEmpty();
	}

	/**
	 * @param index The index to insert the new children at
	 * @param children The children to add to this list
	 * @return Whether the list was modified
	 */
	public boolean addAll(int index, MuisElement [] children) {
		return addAll(index, Arrays.asList(children));
	}

	@Override
	public boolean removeAll(Collection<?> children) {
		HashSet<Integer> toRemove = new HashSet<>();
		try (MuisLock lock = lock()) {
			MuisElement [] cacheChildren = theChildren;
			for(Object o : children) {
				int index = ArrayUtils.indexOf(cacheChildren, o);
				if(index >= 0)
					toRemove.add(index);
			}
			if(!toRemove.isEmpty()) {
				MuisElement [] newChildren = new MuisElement[cacheChildren.length - toRemove.size()];
				int i, j;
				for(i = 0, j = 0; i < cacheChildren.length; i++) {
					if(!toRemove.contains(i))
						newChildren[j++] = cacheChildren[i];
				}
				theChildren = newChildren;
			}
		}
		return !toRemove.isEmpty();
	}

	@Override
	public boolean retainAll(Collection<?> children) {
		HashSet<Integer> toRetain = new HashSet<>();
		boolean changed = false;
		try (MuisLock lock = lock()) {
			MuisElement [] cacheChildren = theChildren;
			for(Object o : children) {
				int index = ArrayUtils.indexOf(cacheChildren, o);
				if(index >= 0)
					toRetain.add(index);
			}
			if(toRetain.size() < cacheChildren.length) {
				changed = true;
				MuisElement [] newChildren = new MuisElement[toRetain.size()];
				int i, j;
				for(i = 0, j = 0; i < cacheChildren.length; i++) {
					if(toRetain.contains(i))
						newChildren[j++] = cacheChildren[i];
				}
				theChildren = newChildren;
			}
		}
		return changed;
	}

	@Override
	public Iterator<MuisElement> iterator() {
		return new Iterator<MuisElement>() {
			private MuisElement [] theCache = theChildren;

			private int theIndex;

			private boolean calledNext;

			private boolean calledRemove;

			@Override
			public boolean hasNext() {
				return theIndex < theCache.length;
			}

			@Override
			public MuisElement next() {
				calledRemove = false;
				calledNext = true;
				return theCache[theIndex++];
			}

			@Override
			public void remove() {
				if(calledRemove)
					throw new IllegalStateException("remove() can only be called once after each call to next()");
				if(!calledNext)
					throw new IllegalStateException("next() must be called before remove() can be called");
				calledRemove = true;
				theIndex--;
				ChildList.this.remove(theCache[theIndex]);
				theCache = ArrayUtils.remove(theCache, theIndex);
			}
		};
	}

	@Override
	public ListIterator<MuisElement> listIterator() {
		return listIterator(0);
	}

	@Override
	public ListIterator<MuisElement> listIterator(final int index) {
		return new ListIterator<MuisElement>() {
			private MuisElement [] theCache = theChildren;

			private int theIndex = index;

			private int theLastElCall;

			private boolean calledAdd;

			private boolean calledRemove;

			@Override
			public boolean hasNext() {
				return theIndex < theCache.length;
			}

			@Override
			public MuisElement next() {
				calledAdd = false;
				calledRemove = false;
				theLastElCall = 1;
				return theCache[theIndex++];
			}

			@Override
			public boolean hasPrevious() {
				return theIndex > 0;
			}

			@Override
			public MuisElement previous() {
				calledAdd = false;
				calledRemove = false;
				theLastElCall = -1;
				return theCache[--theIndex];
			}

			@Override
			public int nextIndex() {
				return theIndex;
			}

			@Override
			public int previousIndex() {
				return theIndex - 1;
			}

			@Override
			public void add(MuisElement e) {
				if(calledRemove)
					throw new IllegalStateException("add() cannot be called after remove() until next() is called");
				calledAdd = true;
				addToList(e);
				theCache = ArrayUtils.add(theCache, e, theIndex);
				theIndex++;
			}

			void addToList(MuisElement e) {
				if(theIndex == 0)
					ChildList.this.add(theIndex, e);
				else if(theIndex == theCache.length)
					ChildList.this.add(e);
				else {
					/* If the supporting list's order has changed, we want to add the new element in a position that makes sense in the
					 * context of this iterator's order. First we try to find the element behind (index-1) the cursor and insert the element
					 * after it in the list.  If we can't find it, we try the previous one and so on.  If no previous element exists in the
					 * list, we try to insert the new element before the first iterator element after the cursor that can be found in the
					 * list. If all else fails, we'll add the new element to the end of the list. */
					try (MuisLock lock = lock()) {
						int listIndex = -1;
						int i;
						for(i = theIndex - 1; listIndex < 0 && i >= 0; i--)
							listIndex = indexOf(theCache[i]);
						if(listIndex >= 0)
							ChildList.this.add(listIndex + 1, e);
						else {
							for(i = theIndex; listIndex < 0 && i < theCache.length; i++)
								listIndex = indexOf(theCache[i]);
							if(listIndex >= 0)
								ChildList.this.add(listIndex, e);
							else
								ChildList.this.add(e);
						}
					}
				}
			}

			@Override
			public void remove() {
				if(calledRemove)
					throw new IllegalStateException("remove() can only be called once after each call to next() or previous()");
				if(calledAdd)
					throw new IllegalStateException("remove() cannot be called after add() until next() is called");
				if(theLastElCall == 0)
					throw new IllegalStateException("next() must be called before remove() can be called");
				calledRemove = true;
				if(theLastElCall > 0)
					theIndex--;
				ChildList.this.remove(theCache[theIndex]);
				theCache = ArrayUtils.remove(theCache, theIndex);
			}

			@Override
			public void set(MuisElement e) {
				if(calledAdd || calledRemove)
					throw new IllegalStateException("set() cannot be called after add() or remove() until next() is called");
				int cacheIndex = theIndex;
				if(theLastElCall > 0)
					cacheIndex--;
				try (MuisLock lock = lock()) {
					/* If the element to be replaced still exists in the list, replace it at the correct index.
					 * If not, treat it as an add operation. */
					int listIndex = indexOf(theCache[cacheIndex]);
					if(index >= 0)
						ChildList.this.set(listIndex, e);
					else
						addToList(e);
				}
				theCache[cacheIndex] = e;
			}
		};
	}
}
