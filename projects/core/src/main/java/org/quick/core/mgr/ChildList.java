package org.quick.core.mgr;

import java.util.*;

import org.qommons.ArrayUtils;
import org.quick.core.QuickElement;
import org.quick.core.QuickToolkit;
import org.quick.core.QuickConstants.CoreStage;
import org.quick.core.event.ChildEvent;
import org.quick.util.SimpleElementList;

/** A list that manages child elements for a parent element */
public class ChildList extends AbstractElementList<QuickElement> {
	private QuickElement [] theChildren;

	/** @param parent The parent to manage the children of */
	public ChildList(QuickElement parent) {
		super(parent);
		theChildren = new QuickElement[0];
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
		QuickElement [] children = theChildren;
		for(Object o : c) {
			if(!(o instanceof QuickElement))
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
		QuickElement [] children = theChildren;
		for(int i = children.length; i >= 0; i--)
			if(children[i].equals(o))
				return i;
		return -1;
	}

	@Override
	public QuickElement get(int index) {
		return theChildren[index];
	}

	@Override
	public QuickElement getLast() {
		QuickElement [] children = theChildren;
		if(children.length == 0)
			throw new java.util.NoSuchElementException();
		return children[children.length - 1];
	}

	@Override
	public QuickElement [] toArray() {
		return theChildren.clone();
	}

	QuickLock lock() {
		return getParent().getDocument().getLocker().lock(QuickElement.CHILDREN_LOCK_TYPE, getParent(), true);
	}

	/**
	 * If {@code child} already exists in this list, then it will be moved to the new index and other elements will be moved to make room.
	 * Otherwise, the child at {@code index} will be removed and {@code child} added there.
	 *
	 * @see java.util.List#set(int, java.lang.Object)
	 */
	@Override
	public QuickElement set(int index, QuickElement child) {
		QuickElement [] oldChildren;
		QuickElement [] newChildren;
		int oldIndex;
		try (QuickLock lock = lock()) {
			oldChildren = theChildren;
			QuickElement oldChild = oldChildren[index];
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
		ImmutableChildList<QuickElement> oldList = list(oldChildren);
		ImmutableChildList<QuickElement> newList = list(newChildren);
		if(oldIndex >= 0)
			fireEvent(new ChildEvent(getParent(), child, index, oldIndex, ChildEvent.ChildEventType.MOVE, oldList, newList));
		else {
			ImmutableChildList<QuickElement> middleList = list(ArrayUtils.remove(oldChildren, index));
			fireEvent(new ChildEvent(getParent(), oldChildren[index], index, index, ChildEvent.ChildEventType.REMOVE, oldList, middleList));
			fireEvent(new ChildEvent(getParent(), child, index, index, ChildEvent.ChildEventType.ADD, middleList, newList));
		}
		return oldChildren[index];
	}

	private ImmutableChildList<QuickElement> list(QuickElement [] children) {
		return new ImmutableChildList<>(new SimpleElementList<>(getParent(), children));
	}

	@Override
	public boolean add(QuickElement child) {
		if(child == null)
			return false;
		QuickElement [] oldChildren;
		QuickElement [] newChildren;
		int index;
		try (QuickLock lock = lock()) {
			oldChildren = theChildren;
			if(ArrayUtils.contains(oldChildren, child))
				return false;
			index = oldChildren.length;
			newChildren = ArrayUtils.add(oldChildren, child);
			theChildren = newChildren;
			checkChildState(child);
		}
		ImmutableChildList<QuickElement> oldList = list(oldChildren);
		ImmutableChildList<QuickElement> newList = list(newChildren);
		fireEvent(new ChildEvent(getParent(), child, index, index, ChildEvent.ChildEventType.ADD, oldList, newList));
		return true;
	}

	/**
	 * This method does nothing if {@code child} is already a child under this element.
	 *
	 * @see java.util.List#add(int, java.lang.Object)
	 */
	@Override
	public void add(int index, QuickElement child) {
		if(child == null)
			throw new NullPointerException();
		QuickElement [] oldChildren;
		QuickElement [] newChildren;
		try (QuickLock lock = lock()) {
			oldChildren = theChildren;
			if(ArrayUtils.contains(oldChildren, child))
				return;
			if(index < 0)
				index = oldChildren.length;
			newChildren = ArrayUtils.add(oldChildren, child, index);
			theChildren = newChildren;
			checkChildState(child);
		}
		ImmutableChildList<QuickElement> oldList = list(oldChildren);
		ImmutableChildList<QuickElement> newList = list(newChildren);
		fireEvent(new ChildEvent(getParent(), child, index, index, ChildEvent.ChildEventType.ADD, oldList, newList));
	}

	@Override
	public QuickElement remove(int index) {
		QuickElement [] oldChildren;
		QuickElement [] newChildren;
		QuickElement oldChild;
		try (QuickLock lock = lock()) {
			oldChildren = theChildren;
			oldChild = oldChildren[index];
			newChildren = ArrayUtils.remove(oldChildren, index);
			theChildren = newChildren;
		}
		ImmutableChildList<QuickElement> oldList = list(oldChildren);
		ImmutableChildList<QuickElement> newList = list(newChildren);
		fireEvent(new ChildEvent(getParent(), oldChild, index, index, ChildEvent.ChildEventType.REMOVE, oldList, newList));
		return oldChild;
	}

	@Override
	public boolean remove(Object o) {
		if(!(o instanceof QuickElement))
			return false;
		QuickElement [] oldChildren;
		QuickElement [] newChildren;
		QuickElement oldChild = (QuickElement) o;
		int index;
		try (QuickLock lock = lock()) {
			oldChildren = theChildren;
			index = ArrayUtils.indexOf(oldChildren, (QuickElement) o);
			if(index < 0)
				return false;
			newChildren = ArrayUtils.remove(theChildren, index);
			theChildren = newChildren;
		}
		ImmutableChildList<QuickElement> oldList = list(oldChildren);
		ImmutableChildList<QuickElement> newList = list(newChildren);
		fireEvent(new ChildEvent(getParent(), oldChild, index, index, ChildEvent.ChildEventType.REMOVE, oldList, newList));
		return true;
	}

	@Override
	public void clear() {
		QuickElement [] oldChildren;
		try (QuickLock lock = lock()) {
			if(theChildren.length == 0)
				return;
			oldChildren = theChildren;
			theChildren = new QuickElement[0];
		}
		SimpleElementList<QuickElement> tempOldChildren = new SimpleElementList<>(getParent(), oldChildren);
		SimpleElementList<QuickElement> tempNewChildren = new SimpleElementList<>(getParent(), ArrayUtils.remove(oldChildren,
			oldChildren.length - 1));
		ImmutableChildList<QuickElement> immOld = new ImmutableChildList<>(tempOldChildren);
		ImmutableChildList<QuickElement> immNew = new ImmutableChildList<>(tempNewChildren);
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
	public boolean addAll(Collection<? extends QuickElement> children) {
		QuickElement [] oldChildren;
		ArrayList<QuickElement> toAdd = new ArrayList<>();
		int index;
		try (QuickLock lock = lock()) {
			oldChildren = theChildren;
			for(QuickElement child : children) {
				if(child == null)
					continue;
				if(ArrayUtils.contains(oldChildren, child) || toAdd.contains(child))
					continue;
				toAdd.add(child);
			}
			index = oldChildren.length;
			QuickElement [] newChildren;
			if(!toAdd.isEmpty()) {
				newChildren = ArrayUtils.addAll(oldChildren, toAdd.toArray(new QuickElement[toAdd.size()]));
				theChildren = newChildren;
				for(QuickElement child : toAdd)
					checkChildState(child);
			}
		}

		SimpleElementList<QuickElement> tempOldChildren = new SimpleElementList<>(getParent(), oldChildren);
		SimpleElementList<QuickElement> tempNewChildren = new SimpleElementList<>(getParent(), oldChildren);
		ImmutableChildList<QuickElement> immOld = new ImmutableChildList<>(tempOldChildren);
		ImmutableChildList<QuickElement> immNew = new ImmutableChildList<>(tempNewChildren);
		for(int c = 0; c < toAdd.size(); c++) {
			tempNewChildren.add(toAdd.get(c));
			fireEvent(new ChildEvent(getParent(), toAdd.get(c), index + c, index + c, ChildEvent.ChildEventType.ADD, immOld, immNew));
			tempOldChildren.add(toAdd.get(c));
		}
		return !toAdd.isEmpty();
	}

	@Override
	public boolean addAll(QuickElement [] children) {
		return addAll(Arrays.asList(children));
	}

	@Override
	public boolean addAll(int index, Collection<? extends QuickElement> children) {
		QuickElement [] oldChildren;
		ArrayList<QuickElement> toAdd = new ArrayList<>();
		try (QuickLock lock = lock()) {
			oldChildren = theChildren;
			for(QuickElement child : children) {
				if(child == null)
					continue;
				if(ArrayUtils.contains(oldChildren, child) || toAdd.contains(child))
					continue;
				toAdd.add(child);
			}
			if(!toAdd.isEmpty()) {
				QuickElement [] newChildren2 = new QuickElement[oldChildren.length + toAdd.size()];
				System.arraycopy(oldChildren, 0, newChildren2, 0, index);
				System.arraycopy(oldChildren, index, toAdd.toArray(new QuickElement[toAdd.size()]), 0, toAdd.size());
				System.arraycopy(oldChildren, index, newChildren2, index + toAdd.size(), oldChildren.length - index);
				theChildren = newChildren2;
				for(QuickElement child : toAdd)
					checkChildState(child);
			}
		}

		SimpleElementList<QuickElement> tempOldChildren = new SimpleElementList<>(getParent(), oldChildren);
		SimpleElementList<QuickElement> tempNewChildren = new SimpleElementList<>(getParent(), oldChildren);
		ImmutableChildList<QuickElement> immOld = new ImmutableChildList<>(tempOldChildren);
		ImmutableChildList<QuickElement> immNew = new ImmutableChildList<>(tempNewChildren);
		for(int c = 0; c < toAdd.size(); c++) {
			tempNewChildren.add(toAdd.get(c));
			fireEvent(new ChildEvent(getParent(), toAdd.get(c), index + c, index + c, ChildEvent.ChildEventType.ADD, immOld, immNew));
			tempOldChildren.add(toAdd.get(c));
		}
		return !toAdd.isEmpty();
	}

	@Override
	public boolean addAll(int index, QuickElement [] children) {
		return addAll(index, Arrays.asList(children));
	}

	@Override
	public boolean removeAll(Collection<?> children) {
		LinkedHashSet<Integer> toRemove = new LinkedHashSet<>();
		QuickElement [] oldChildren = null;
		try (QuickLock lock = lock()) {
			oldChildren = theChildren;
			for(Object o : children) {
				int index = ArrayUtils.indexOf(oldChildren, o);
				if(index >= 0)
					toRemove.add(index);
			}
			if(!toRemove.isEmpty()) {
				QuickElement [] newChildren = new QuickElement[oldChildren.length - toRemove.size()];
				int i, j;
				for(i = 0, j = 0; i < oldChildren.length; i++) {
					if(!toRemove.contains(i))
						newChildren[j++] = oldChildren[i];
				}
				theChildren = newChildren;
			}
		}

		SimpleElementList<QuickElement> tempOldChildren = new SimpleElementList<>(getParent(), oldChildren);
		SimpleElementList<QuickElement> tempNewChildren = new SimpleElementList<>(getParent(), oldChildren);
		ImmutableChildList<QuickElement> immOld = new ImmutableChildList<>(tempOldChildren);
		ImmutableChildList<QuickElement> immNew = new ImmutableChildList<>(tempNewChildren);
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
		QuickElement [] oldChildren;
		boolean changed = false;
		try (QuickLock lock = lock()) {
			oldChildren = theChildren;
			for(Object o : children) {
				int index = ArrayUtils.indexOf(oldChildren, o);
				if(index >= 0)
					toRetain.add(index);
			}
			if(toRetain.size() < oldChildren.length) {
				changed = true;
				QuickElement [] newChildren = new QuickElement[toRetain.size()];
				int i, j;
				for(i = 0, j = 0; i < oldChildren.length; i++) {
					if(toRetain.contains(i))
						newChildren[j++] = oldChildren[i];
				}
				theChildren = newChildren;
			}
		}
		if(changed) {
			SimpleElementList<QuickElement> tempOldChildren = new SimpleElementList<>(getParent(), oldChildren);
			SimpleElementList<QuickElement> tempNewChildren = new SimpleElementList<>(getParent(), oldChildren);
			ImmutableChildList<QuickElement> immOld = new ImmutableChildList<>(tempOldChildren);
			ImmutableChildList<QuickElement> immNew = new ImmutableChildList<>(tempNewChildren);
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

	private void checkChildState(QuickElement child) {
		// Need to catch the child up to where the parent is in the life cycle
		if(child.life().isAfter(CoreStage.INIT_SELF.name()) < 0 && getParent().life().isAfter(CoreStage.PARSE_CHILDREN.name()) > 0) {
			QuickToolkit tk;
			if(child.getClass().getClassLoader() instanceof QuickToolkit)
				tk = (QuickToolkit) child.getClass().getClassLoader();
			else
				tk = getParent().getDocument().getEnvironment().getCoreToolkit();
			org.quick.core.QuickClassView classView = new org.quick.core.QuickClassView(getParent().getDocument().getEnvironment(), getParent()
				.getClassView(), tk);
			child.init(getParent().getDocument(), tk, classView, getParent(), null, null);
		}
		if(child.life().isAfter(CoreStage.INIT_CHILDREN.name()) < 0 && getParent().life().isAfter(CoreStage.INITIALIZED.name()) > 0) {
			child.initChildren(new QuickElement[0]);
		}
		if(child.life().isAfter(CoreStage.STARTUP.name()) < 0 && getParent().life().isAfter(CoreStage.READY.name()) > 0) {
			child.postCreate();
		}
	}

	@Override
	public Iterator<QuickElement> iterator() {
		return new Iterator<QuickElement>() {
			private QuickElement [] theCache = theChildren;

			private int theIndex;

			private boolean calledNext;

			private boolean calledRemove;

			@Override
			public boolean hasNext() {
				return theIndex < theCache.length;
			}

			@Override
			public QuickElement next() {
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
	public ListIterator<QuickElement> listIterator() {
		return listIterator(0);
	}

	@Override
	public ListIterator<QuickElement> listIterator(final int index) {
		return new ListIterator<QuickElement>() {
			private QuickElement [] theCache = theChildren;

			private int theIndex = index;

			private int theLastElCall;

			private boolean calledAdd;

			private boolean calledRemove;

			@Override
			public boolean hasNext() {
				return theIndex < theCache.length;
			}

			@Override
			public QuickElement next() {
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
			public QuickElement previous() {
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
			public void add(QuickElement e) {
				if(calledRemove)
					throw new IllegalStateException("add() cannot be called after remove() until next() is called");
				calledAdd = true;
				addToList(e);
				theCache = ArrayUtils.add(theCache, e, theIndex);
				theIndex++;
			}

			void addToList(QuickElement e) {
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
					try (QuickLock lock = lock()) {
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
			public void set(QuickElement e) {
				if(calledAdd || calledRemove)
					throw new IllegalStateException("set() cannot be called after add() or remove() until next() is called");
				int cacheIndex = theIndex;
				if(theLastElCall > 0)
					cacheIndex--;
				try (QuickLock lock = lock()) {
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
