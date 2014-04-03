package org.muis.core.style.attach;

import org.muis.core.MuisElement;
import org.muis.core.event.ElementMovedEvent;
import org.muis.core.event.MuisEvent;
import org.muis.core.mgr.MuisState;
import org.muis.core.style.MuisStyle;
import org.muis.core.style.StyleAttribute;
import org.muis.core.style.stateful.AbstractInternallyStatefulStyle;
import org.muis.core.style.stateful.AbstractStatefulStyle;
import org.muis.core.style.stateful.MutableStatefulStyle;
import org.muis.core.style.stateful.StateExpression;

/** A style controlling the appearance of a specific element */
public class ElementStyle extends AbstractInternallyStatefulStyle implements MutableStatefulStyle {
	private final MuisElement theElement;

	private ElementStyle theParentStyle;

	private ElementSelfStyle theSelfStyle;

	private ElementHeirStyle theHeirStyle;

	private TypedStyleGroup<?> [] theStyleGroups;

	/**
	 * Creates an element style
	 *
	 * @param element The element that this style is for
	 */
	public ElementStyle(MuisElement element) {
		theElement = element;
		theSelfStyle = new ElementSelfStyle(this);
		theHeirStyle = new ElementHeirStyle(this);
		theStyleGroups = new TypedStyleGroup[0];
		element.life().runWhen(new Runnable() {
			@Override
			public void run() {
				addDependencies();
			}
		}, org.muis.core.MuisConstants.CoreStage.INIT_SELF.toString(), 1);
	}

	private void addDependencies() {
		if(theElement.getParent() != null) {
			theParentStyle = theElement.getParent().getStyle();
			addDependency(theParentStyle.getHeir(), null);
		}
		theElement.events().listen(ElementMovedEvent.moved, new org.muis.core.event.MuisEventListener<ElementMovedEvent>() {
			@Override
			public void eventOccurred(ElementMovedEvent event) {
				ElementStyle oldParentStyle = theParentStyle;
				if(oldParentStyle != null) {
					if(event.getNewParent() != null) {
						theParentStyle = event.getNewParent().getStyle();
						replaceDependency(oldParentStyle.getHeir(), theParentStyle.getHeir());
					} else {
						theParentStyle = null;
						removeDependency(oldParentStyle.getHeir());
					}
				} else if(event.getNewParent() != null) {
					theParentStyle = event.getNewParent().getStyle();
					addDependency(theParentStyle.getHeir(), null);
				}
			}
		});
		MuisState [] currentState = theElement.state().toArray();
		setState(currentState);
		theSelfStyle.setState(currentState);
		theHeirStyle.setState(currentState);
		theElement.state().addListener(null, new org.muis.core.mgr.StateEngine.StateListener() {
			@Override
			public void entered(MuisState state, MuisEvent cause) {
				addState(state);
				theSelfStyle.addState(state);
				theHeirStyle.addState(state);
			}

			@Override
			public void exited(MuisState state, MuisEvent cause) {
				removeState(state);
				theSelfStyle.removeState(state);
				theHeirStyle.removeState(state);
			}
		});
	}

	/** @return The element that this style is for */
	public MuisElement getElement() {
		return theElement;
	}

	/** @return The set of style attributes that apply only to this style's element, not to its descendants */
	public ElementSelfStyle getSelf() {
		return theSelfStyle;
	}

	/** @return The set of style attributes that apply to all this style's element's descendants, but not to this style's element itself. */
	public ElementHeirStyle getHeir() {
		return theHeirStyle;
	}

	@Override
	public <T> void set(StyleAttribute<T> attr, T value) throws ClassCastException, IllegalArgumentException {
		super.set(attr, value);
	}

	@Override
	public <T> void set(StyleAttribute<T> attr, StateExpression exp, T value) throws ClassCastException, IllegalArgumentException {
		super.set(attr, exp, value);
	}

	@Override
	public void clear(StyleAttribute<?> attr) {
		super.clear(attr);
	}

	@Override
	public void clear(StyleAttribute<?> attr, StateExpression exp) {
		super.clear(attr, exp);
	}

	/** @return The number of named style groups in this element style */
	public int getGroupCount() {
		return theStyleGroups.length;
	}

	/** @param group The named style group to add to this element style */
	public void addGroup(NamedStyleGroup group) {
		TypedStyleGroup<?> typedGroup = group.addMember(theElement);
		AbstractStatefulStyle after;
		if(theStyleGroups.length > 0)
			after = theStyleGroups[theStyleGroups.length - 1];
		else if(theParentStyle != null)
			after = theParentStyle.getHeir();
		else
			after = null;
		addDependency(typedGroup, after);
		if(!prisms.util.ArrayUtils.contains(theStyleGroups, typedGroup))
			theStyleGroups = prisms.util.ArrayUtils.add(theStyleGroups, typedGroup);
		theElement.events().fire(new GroupMemberEvent(theElement, group, -1));
	}

	/** @param group The named style group to remove from this element style */
	public void removeGroup(NamedStyleGroup group) {
		TypedStyleGroup<?> typedGroup = group.getGroupForType(theElement.getClass());
		removeDependency(typedGroup);
		group.removeMember(theElement);
		int index = prisms.util.ArrayUtils.indexOf(theStyleGroups, typedGroup);
		if(index < 0)
			return;
		theStyleGroups = prisms.util.ArrayUtils.remove(theStyleGroups, typedGroup);
		theElement.events().fire(new GroupMemberEvent(theElement, group, index));
	}

	/**
	 * @return All style attributes applying to this style's element directly--those local to this element style or any named style groups
	 *         attached to it. It does not include attributes inherited from the element's ancestors
	 */
	public Iterable<StyleAttribute<?>> elementAttributes() {
		return new Iterable<StyleAttribute<?>>() {
			@Override
			public java.util.Iterator<StyleAttribute<?>> iterator() {
				MuisStyle [] depends = new MuisStyle[theStyleGroups.length + 1];
				depends[0] = theSelfStyle;
				System.arraycopy(depends, 1, theStyleGroups, 0, theStyleGroups.length);
				return new AttributeIterator(ElementStyle.this, depends);
			}
		};
	}

	/**
	 * @param forward Whether to iterate forward through the groups or backward
	 * @return An iterable to get the groups associated with this style
	 */
	public Iterable<TypedStyleGroup<?>> groups(final boolean forward) {
		return new Iterable<TypedStyleGroup<?>>() {
			@Override
			public NamedStyleIterator iterator() {
				return new NamedStyleIterator(theStyleGroups, forward);
			}
		};
	}

	private class NamedStyleIterator implements java.util.ListIterator<TypedStyleGroup<?>> {
		private TypedStyleGroup<?> [] theGroups;

		private int index;

		private boolean isForward;

		Boolean lastCalledNext;

		NamedStyleIterator(TypedStyleGroup<?> [] groups, boolean forward) {
			isForward = forward;
			if(!forward)
				groups = prisms.util.ArrayUtils.reverse(groups);
			theGroups = groups;
		}

		@Override
		public boolean hasNext() {
			return index < theGroups.length;
		}

		@Override
		public TypedStyleGroup<?> next() {
			TypedStyleGroup<?> ret = theGroups[index];
			index++;
			lastCalledNext = Boolean.TRUE;
			return ret;
		}

		@Override
		public int nextIndex() {
			if(isForward)
				return index;
			else
				return theGroups.length - index - 1;
		}

		@Override
		public boolean hasPrevious() {
			return index > 0;
		}

		@Override
		public TypedStyleGroup<?> previous() {
			index--;
			lastCalledNext = Boolean.FALSE;
			return theGroups[index];
		}

		@Override
		public int previousIndex() {
			if(isForward)
				return index - 1;
			else
				return theGroups.length - index;
		}

		@Override
		public void add(TypedStyleGroup<?> e) {
			if(e == null)
				throw new NullPointerException("Cannot add a null style group");
			lastCalledNext = null;
			addGroup(e.getRoot());
			theGroups = prisms.util.ArrayUtils.add(theGroups, e, index);
			index++;
		}

		@Override
		public void remove() {
			if(lastCalledNext == null)
				throw new IllegalStateException("remove() can only be called if neither add() nor"
					+ " remove() has been called since the previous call of next() or previous()");
			if(lastCalledNext.booleanValue())
				index--;
			lastCalledNext = null;
			if(index < 0) {
				index = 0;
				throw new IndexOutOfBoundsException("No element to remove--at beginning of list");
			} else if(index >= theGroups.length)
				throw new IndexOutOfBoundsException("No element to remove--at end of list");
			removeGroup(theGroups[index].getRoot());
			theGroups = prisms.util.ArrayUtils.remove(theGroups, index);
			index--;
		}

		@Override
		public void set(TypedStyleGroup<?> e) {
			if(e == null)
				throw new NullPointerException("Cannot set a null style group");
			if(lastCalledNext == null)
				throw new IllegalStateException("remove() can only be called if neither add() nor"
					+ " remove() has been called since the previous call of next() or previous()");
			int setIndex = index;
			if(lastCalledNext.booleanValue())
				setIndex--;
			if(setIndex < 0)
				throw new IndexOutOfBoundsException("No element to remove--at beginning of list");
			else if(setIndex >= theGroups.length)
				throw new IndexOutOfBoundsException("No element to remove--at end of list");
			removeGroup(theGroups[setIndex].getRoot());
			addGroup(e.getRoot());
			theGroups[setIndex] = e;
		}
	}
}
