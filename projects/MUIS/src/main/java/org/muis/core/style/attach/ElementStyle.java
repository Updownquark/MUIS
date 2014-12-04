package org.muis.core.style.attach;

import java.util.List;

import org.muis.core.MuisElement;
import org.muis.core.event.ElementMovedEvent;
import org.muis.core.rx.DefaultObservableList;
import org.muis.core.style.StyleAttribute;
import org.muis.core.style.StyleAttributeEvent;
import org.muis.core.style.stateful.*;

import prisms.lang.Type;

/** A style controlling the appearance of a specific element */
public class ElementStyle extends AbstractInternallyStatefulStyle implements MutableStatefulStyle, org.muis.core.style.MuisStyle {
	private final MuisElement theElement;

	private ElementStyle theParentStyle;

	private ElementSelfStyle theSelfStyle;

	private ElementHeirStyle theHeirStyle;

	private TypedStyleGroup<?> [] theStyleGroups;

	private List<StatefulStyle> theDependencyController;

	/**
	 * Creates an element style
	 *
	 * @param element The element that this style is for
	 */
	public ElementStyle(MuisElement element) {
		super(new DefaultObservableList<>(new Type(StatefulStyle.class)), element.state().activeStates());
		theDependencyController = ((DefaultObservableList<StatefulStyle>) getConditionalDependencies()).control(null);
		theElement = element;
		theSelfStyle = new ElementSelfStyle(this);
		theHeirStyle = new ElementHeirStyle(this);
		theStyleGroups = new TypedStyleGroup[0];
		element.life().runWhen(() -> {
			addDependencies();
		}, org.muis.core.MuisConstants.CoreStage.INIT_SELF.toString(), 1);
	}

	private void addDependencies() {
		if(theElement.getParent() != null) {
			theParentStyle = theElement.getParent().getStyle();
			theDependencyController.add(0, theParentStyle.getHeir());
		}
		theElement.events().filterMap(ElementMovedEvent.moved).act(event -> {
			ElementStyle oldParentStyle = theParentStyle;
			if(oldParentStyle != null) {
				if(event.getNewParent() != null) {
					theParentStyle = event.getNewParent().getStyle();
					int index = theDependencyController.indexOf(oldParentStyle.getHeir());
					theDependencyController.set(index, theParentStyle.getHeir());
				} else {
					theParentStyle = null;
					theDependencyController.remove(oldParentStyle.getHeir());
				}
			} else if(event.getNewParent() != null) {
				theParentStyle = event.getNewParent().getStyle();
				theDependencyController.add(0, theParentStyle.getHeir());
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
		int index = theDependencyController.indexOf(after);
		theDependencyController.add(index < 0 ? theDependencyController.size() : index + 1, typedGroup);
		if(!prisms.util.ArrayUtils.contains(theStyleGroups, typedGroup))
			theStyleGroups = prisms.util.ArrayUtils.add(theStyleGroups, typedGroup);
		theElement.events().fire(new GroupMemberEvent(theElement, group, -1));
	}

	/** @param group The named style group to remove from this element style */
	public void removeGroup(NamedStyleGroup group) {
		TypedStyleGroup<?> typedGroup = group.getGroupForType(theElement.getClass());
		theDependencyController.remove(typedGroup);
		group.removeMember(theElement);
		int index = prisms.util.ArrayUtils.indexOf(theStyleGroups, typedGroup);
		if(index < 0)
			return;
		theStyleGroups = prisms.util.ArrayUtils.remove(theStyleGroups, typedGroup);
		theElement.events().fire(new GroupMemberEvent(theElement, group, index));
	}

	/**
	 * @param forward Whether to iterate forward through the groups or backward
	 * @return An iterable to get the groups associated with this style
	 */
	public Iterable<TypedStyleGroup<?>> groups(final boolean forward) {
		return () -> {
			return new NamedStyleIterator(theStyleGroups, forward);
		};
	}

	@Override
	public <T> StyleAttributeEvent<T> mapEvent(StyleAttribute<T> attr, org.muis.core.rx.ObservableValueEvent<T> event) {
		StyleAttributeEvent<T> superMap = org.muis.core.style.MuisStyle.super.mapEvent(attr, event);
		return new StyleAttributeEvent<>(theElement, superMap.getRootStyle(), this, attr, superMap.getOldValue(), superMap.getValue(),
			superMap.getCause());
	}

	@Override
	public String toString() {
		return "style of " + theElement;
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
