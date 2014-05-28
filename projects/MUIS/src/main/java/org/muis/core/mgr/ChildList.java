package org.muis.core.mgr;

import java.util.*;

import org.muis.core.MuisConstants.CoreStage;
import org.muis.core.MuisElement;
import org.muis.core.MuisToolkit;
import org.muis.core.event.ChildEvent;
import org.muis.util.SimpleElementList;

import prisms.util.ArrayUtils;

/** A list that manages child elements for a parent element */
public class ChildList extends AbstractElementList<MuisElement> {
	private MuisElement [] theChildren;

	/** @param parent The parent to manage the children of */
	public ChildList(MuisElement parent) {
		super(parent);
		theChildren = new MuisElement[0];
	}

	@Override
	public int size() {
		return theChildren.length;
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

	@Override
	public MuisElement getLast() {
		MuisElement [] children = theChildren;
		if(children.length == 0)
			throw new java.util.NoSuchElementException();
		return children[children.length - 1];
	}

	@Override
	public MuisElement [] toArray() {
		return theChildren.clone();
	}

	MuisLock lock() {
		return getParent().getDocument().getLocker().lock(MuisElement.CHILDREN_LOCK_TYPE, getParent(), true);
	}

	/**
	 * If {@code child} already exists in this list, then it will be moved to the new index and other elements will be moved to make room.
	 * Otherwise, the child at {@code index} will be removed and {@code child} added there.
	 *
	 * @see java.util.List#set(int, java.lang.Object)
	 */
	@Override
	public MuisElement set(int index, MuisElement child) {
		MuisElement [] oldChildren;
		MuisElement [] newChildren;
		int oldIndex;
		try (MuisLock lock = lock()) {
			oldChildren = theChildren;
			MuisElement oldChild = oldChildren[index];
			if(oldChild == child)
				return child;
			newChildren = oldChildren.clone();
			oldIndex = ArrayUtils.indexOf(newChildren, child);
			if(oldIndex >= 0) {
				int tempIdx = oldIndex;
				for(; tempIdx < index; tempIdx++)
					newChildren[tempIdx] = newChildren[tempIdx + 1];
				for(; tempIdx > index; tempIdx--)
					newChildren[tempIdx] = newChildren[tempIdx - 1];
			} else {
			}
			newChildren[index] = child;
			theChildren = newChildren;
			checkChildState(child);
		}
		ImmutableChildList<MuisElement> oldList = list(oldChildren);
		ImmutableChildList<MuisElement> newList = list(newChildren);
		if(oldIndex >= 0)
			fireEvent(new ChildEvent(getParent(), child, index, oldIndex, ChildEvent.ChildEventType.MOVE, oldList, newList));
		else {
			ImmutableChildList<MuisElement> middleList = list(ArrayUtils.remove(oldChildren, index));
			fireEvent(new ChildEvent(getParent(), oldChildren[index], index, index, ChildEvent.ChildEventType.REMOVE, oldList, middleList));
			fireEvent(new ChildEvent(getParent(), child, index, index, ChildEvent.ChildEventType.ADD, middleList, newList));
		}
		return oldChildren[index];
	}

	private ImmutableChildList<MuisElement> list(MuisElement [] children) {
		return new ImmutableChildList<>(new SimpleElementList<>(getParent(), children));
	}

	@Override
	public boolean add(MuisElement child) {
		if(child == null)
			return false;
		MuisElement [] oldChildren;
		MuisElement [] newChildren;
		int index;
		try (MuisLock lock = lock()) {
			oldChildren = theChildren;
			if(ArrayUtils.contains(oldChildren, child))
				return false;
			index = oldChildren.length;
			newChildren = ArrayUtils.add(oldChildren, child);
			theChildren = newChildren;
			checkChildState(child);
		}
		ImmutableChildList<MuisElement> oldList = list(oldChildren);
		ImmutableChildList<MuisElement> newList = list(newChildren);
		fireEvent(new ChildEvent(getParent(), child, index, index, ChildEvent.ChildEventType.ADD, oldList, newList));
		return true;
	}

	/**
	 * This method does nothing if {@code child} is already a child under this element.
	 *
	 * @see java.util.List#add(int, java.lang.Object)
	 */
	@Override
	public void add(int index, MuisElement child) {
		if(child == null)
			throw new NullPointerException();
		MuisElement [] oldChildren;
		MuisElement [] newChildren;
		try (MuisLock lock = lock()) {
			oldChildren = theChildren;
			if(ArrayUtils.contains(oldChildren, child))
				return;
			if(index < 0)
				index = oldChildren.length;
			newChildren = ArrayUtils.add(oldChildren, child, index);
			theChildren = newChildren;
			checkChildState(child);
		}
		ImmutableChildList<MuisElement> oldList = list(oldChildren);
		ImmutableChildList<MuisElement> newList = list(newChildren);
		fireEvent(new ChildEvent(getParent(), child, index, index, ChildEvent.ChildEventType.ADD, oldList, newList));
	}

	@Override
	public MuisElement remove(int index) {
		MuisElement [] oldChildren;
		MuisElement [] newChildren;
		MuisElement oldChild;
		try (MuisLock lock = lock()) {
			oldChildren = theChildren;
			oldChild = oldChildren[index];
			newChildren = ArrayUtils.remove(oldChildren, index);
			theChildren = newChildren;
		}
		ImmutableChildList<MuisElement> oldList = list(oldChildren);
		ImmutableChildList<MuisElement> newList = list(newChildren);
		fireEvent(new ChildEvent(getParent(), oldChild, index, index, ChildEvent.ChildEventType.REMOVE, oldList, newList));
		return oldChild;
	}

	@Override
	public boolean remove(Object o) {
		if(!(o instanceof MuisElement))
			return false;
		MuisElement [] oldChildren;
		MuisElement [] newChildren;
		MuisElement oldChild = (MuisElement) o;
		int index;
		try (MuisLock lock = lock()) {
			oldChildren = theChildren;
			index = ArrayUtils.indexOf(oldChildren, (MuisElement) o);
			if(index < 0)
				return false;
			newChildren = ArrayUtils.remove(theChildren, index);
			theChildren = newChildren;
		}
		ImmutableChildList<MuisElement> oldList = list(oldChildren);
		ImmutableChildList<MuisElement> newList = list(newChildren);
		fireEvent(new ChildEvent(getParent(), oldChild, index, index, ChildEvent.ChildEventType.REMOVE, oldList, newList));
		return true;
	}

	@Override
	public void clear() {
		MuisElement [] oldChildren;
		try (MuisLock lock = lock()) {
			if(theChildren.length == 0)
				return;
			oldChildren = theChildren;
			theChildren = new MuisElement[0];
		}
		SimpleElementList<MuisElement> tempOldChildren = new SimpleElementList<>(getParent(), oldChildren);
		SimpleElementList<MuisElement> tempNewChildren = new SimpleElementList<>(getParent(), ArrayUtils.remove(oldChildren,
			oldChildren.length - 1));
		ImmutableChildList<MuisElement> immOld = new ImmutableChildList<>(tempOldChildren);
		ImmutableChildList<MuisElement> immNew = new ImmutableChildList<>(tempNewChildren);
		for(int c = oldChildren.length - 1; c >= 0; c--) {
			fireEvent(new ChildEvent(getParent(), oldChildren[c], c, c, ChildEvent.ChildEventType.REMOVE, immOld, immNew));
			tempOldChildren.remove(c);
			if(c > 0)
				tempNewChildren.remove(c - 1);
		}
	}

	/**
	 * This method skips over elements of the collection which are already in this child list
	 *
	 * @see java.util.List#addAll(java.util.Collection)
	 */
	@Override
	public boolean addAll(Collection<? extends MuisElement> children) {
		MuisElement [] oldChildren;
		ArrayList<MuisElement> toAdd = new ArrayList<>();
		int index;
		try (MuisLock lock = lock()) {
			oldChildren = theChildren;
			for(MuisElement child : children) {
				if(child == null)
					continue;
				if(ArrayUtils.contains(oldChildren, child) || toAdd.contains(child))
					continue;
				toAdd.add(child);
			}
			index = oldChildren.length;
			MuisElement [] newChildren;
			if(!toAdd.isEmpty()) {
				newChildren = ArrayUtils.addAll(oldChildren, toAdd.toArray(new MuisElement[toAdd.size()]));
				theChildren = newChildren;
				for(MuisElement child : toAdd)
					checkChildState(child);
			}
		}

		SimpleElementList<MuisElement> tempOldChildren = new SimpleElementList<>(getParent(), oldChildren);
		SimpleElementList<MuisElement> tempNewChildren = new SimpleElementList<>(getParent(), oldChildren);
		ImmutableChildList<MuisElement> immOld = new ImmutableChildList<>(tempOldChildren);
		ImmutableChildList<MuisElement> immNew = new ImmutableChildList<>(tempNewChildren);
		for(int c = 0; c < toAdd.size(); c++) {
			tempNewChildren.add(toAdd.get(c));
			fireEvent(new ChildEvent(getParent(), toAdd.get(c), index + c, index + c, ChildEvent.ChildEventType.ADD, immOld, immNew));
			tempOldChildren.add(toAdd.get(c));
		}
		return !toAdd.isEmpty();
	}

	@Override
	public boolean addAll(MuisElement [] children) {
		return addAll(Arrays.asList(children));
	}

	@Override
	public boolean addAll(int index, Collection<? extends MuisElement> children) {
		MuisElement [] oldChildren;
		ArrayList<MuisElement> toAdd = new ArrayList<>();
		try (MuisLock lock = lock()) {
			oldChildren = theChildren;
			for(MuisElement child : children) {
				if(child == null)
					continue;
				if(ArrayUtils.contains(oldChildren, child) || toAdd.contains(child))
					continue;
				toAdd.add(child);
			}
			if(!toAdd.isEmpty()) {
				MuisElement [] newChildren2 = new MuisElement[oldChildren.length + toAdd.size()];
				System.arraycopy(oldChildren, 0, newChildren2, 0, index);
				System.arraycopy(oldChildren, index, toAdd.toArray(new MuisElement[toAdd.size()]), 0, toAdd.size());
				System.arraycopy(oldChildren, index, newChildren2, index + toAdd.size(), oldChildren.length - index);
				theChildren = newChildren2;
				for(MuisElement child : toAdd)
					checkChildState(child);
			}
		}

		SimpleElementList<MuisElement> tempOldChildren = new SimpleElementList<>(getParent(), oldChildren);
		SimpleElementList<MuisElement> tempNewChildren = new SimpleElementList<>(getParent(), oldChildren);
		ImmutableChildList<MuisElement> immOld = new ImmutableChildList<>(tempOldChildren);
		ImmutableChildList<MuisElement> immNew = new ImmutableChildList<>(tempNewChildren);
		for(int c = 0; c < toAdd.size(); c++) {
			tempNewChildren.add(toAdd.get(c));
			fireEvent(new ChildEvent(getParent(), toAdd.get(c), index + c, index + c, ChildEvent.ChildEventType.ADD, immOld, immNew));
			tempOldChildren.add(toAdd.get(c));
		}
		return !toAdd.isEmpty();
	}

	@Override
	public boolean addAll(int index, MuisElement [] children) {
		return addAll(index, Arrays.asList(children));
	}

	@Override
	public boolean removeAll(Collection<?> children) {
		LinkedHashSet<Integer> toRemove = new LinkedHashSet<>();
		MuisElement [] oldChildren = null;
		try (MuisLock lock = lock()) {
			oldChildren = theChildren;
			for(Object o : children) {
				int index = ArrayUtils.indexOf(oldChildren, o);
				if(index >= 0)
					toRemove.add(index);
			}
			if(!toRemove.isEmpty()) {
				MuisElement [] newChildren = new MuisElement[oldChildren.length - toRemove.size()];
				int i, j;
				for(i = 0, j = 0; i < oldChildren.length; i++) {
					if(!toRemove.contains(i))
						newChildren[j++] = oldChildren[i];
				}
				theChildren = newChildren;
			}
		}

		SimpleElementList<MuisElement> tempOldChildren = new SimpleElementList<>(getParent(), oldChildren);
		SimpleElementList<MuisElement> tempNewChildren = new SimpleElementList<>(getParent(), oldChildren);
		ImmutableChildList<MuisElement> immOld = new ImmutableChildList<>(tempOldChildren);
		ImmutableChildList<MuisElement> immNew = new ImmutableChildList<>(tempNewChildren);
		for(Integer index : toRemove) {
			tempNewChildren.remove(index);
			fireEvent(new ChildEvent(getParent(), oldChildren[index], index, index, ChildEvent.ChildEventType.REMOVE, immOld, immNew));
			tempOldChildren.remove(index);
		}
		return !toRemove.isEmpty();
	}

	@Override
	public boolean retainAll(Collection<?> children) {
		HashSet<Integer> toRetain = new HashSet<>();
		MuisElement [] oldChildren;
		boolean changed = false;
		try (MuisLock lock = lock()) {
			oldChildren = theChildren;
			for(Object o : children) {
				int index = ArrayUtils.indexOf(oldChildren, o);
				if(index >= 0)
					toRetain.add(index);
			}
			if(toRetain.size() < oldChildren.length) {
				changed = true;
				MuisElement [] newChildren = new MuisElement[toRetain.size()];
				int i, j;
				for(i = 0, j = 0; i < oldChildren.length; i++) {
					if(toRetain.contains(i))
						newChildren[j++] = oldChildren[i];
				}
				theChildren = newChildren;
			}
		}
		if(changed) {
			SimpleElementList<MuisElement> tempOldChildren = new SimpleElementList<>(getParent(), oldChildren);
			SimpleElementList<MuisElement> tempNewChildren = new SimpleElementList<>(getParent(), oldChildren);
			ImmutableChildList<MuisElement> immOld = new ImmutableChildList<>(tempOldChildren);
			ImmutableChildList<MuisElement> immNew = new ImmutableChildList<>(tempNewChildren);
			for(int i = oldChildren.length - 1; i >= 0; i--) {
				if(toRetain.contains(i))
					continue;
				tempNewChildren.remove(i);
				fireEvent(new ChildEvent(getParent(), oldChildren[i], i, i, ChildEvent.ChildEventType.REMOVE, immOld, immNew));
				tempOldChildren.remove(i);
			}
		}
		return changed;
	}

	@Override
	protected void fireEvent(ChildEvent evt) {
		super.fireEvent(evt);
		getParent().events().fire(evt);
	}

	private void checkChildState(MuisElement child) {
		// Need to catch the child up to where the parent is in the life cycle
		if(child.life().isAfter(CoreStage.INIT_SELF.name()) < 0 && getParent().life().isAfter(CoreStage.PARSE_CHILDREN.name()) > 0) {
			MuisToolkit tk;
			if(child.getClass().getClassLoader() instanceof MuisToolkit)
				tk = (MuisToolkit) child.getClass().getClassLoader();
			else
				tk = getParent().getDocument().getEnvironment().getCoreToolkit();
			org.muis.core.MuisClassView classView = new org.muis.core.MuisClassView(getParent().getDocument().getEnvironment(), getParent()
				.getClassView(), tk);
			child.init(getParent().getDocument(), tk, classView, getParent(), null, null);
		}
		if(child.life().isAfter(CoreStage.INIT_CHILDREN.name()) < 0 && getParent().life().isAfter(CoreStage.INITIALIZED.name()) > 0) {
			child.initChildren(new MuisElement[0]);
		}
		if(child.life().isAfter(CoreStage.STARTUP.name()) < 0 && getParent().life().isAfter(CoreStage.READY.name()) > 0) {
			child.postCreate();
		}
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
