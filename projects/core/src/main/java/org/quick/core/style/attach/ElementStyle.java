package org.quick.core.style.attach;

import java.util.List;

import org.observe.ObservableValue;
import org.observe.collect.impl.ObservableArrayList;
import org.observe.util.ObservableUtils;
import org.quick.core.QuickElement;
import org.quick.core.event.ElementMovedEvent;
import org.quick.core.style.StyleAttribute;
import org.quick.core.style.stateful.*;

import com.google.common.reflect.TypeToken;

/** A style controlling the appearance of a specific element */
public class ElementStyle extends AbstractInternallyStatefulStyle implements MutableStatefulStyle, org.quick.core.style.QuickStyle {
	private final QuickElement theElement;

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
	public ElementStyle(QuickElement element) {
		super(element.msg(), ObservableUtils.control(new ObservableArrayList<>(TypeToken.of(StatefulStyle.class))),
			element.state().activeStates());
		theDependencyController = ObservableUtils.getController(getConditionalDependencies());
		theElement = element;
		theSelfStyle = new ElementSelfStyle(this);
		theHeirStyle = new ElementHeirStyle(this);
		theStyleGroups = new TypedStyleGroup[0];
		element.life().runWhen(() -> {
			addDependencies();
		}, org.quick.core.QuickConstants.CoreStage.INIT_SELF.toString(), 1);
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
	@Override
	public QuickElement getElement() {
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
	public <T> ElementStyle set(StyleAttribute<T> attr, ObservableValue<T> value) throws ClassCastException, IllegalArgumentException {
		super.set(attr, value);
		return this;
	}

	@Override
	public <T> ElementStyle set(StyleAttribute<T> attr, StateExpression exp, ObservableValue<T> value)
		throws ClassCastException, IllegalArgumentException {
		super.set(attr, exp, value);
		return this;
	}

	@Override
	public ElementStyle clear(StyleAttribute<?> attr) {
		super.clear(attr);
		return this;
	}

	@Override
	public ElementStyle clear(StyleAttribute<?> attr, StateExpression exp) {
		super.clear(attr, exp);
		return this;
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
		if(!org.qommons.ArrayUtils.contains(theStyleGroups, typedGroup))
			theStyleGroups = org.qommons.ArrayUtils.add(theStyleGroups, typedGroup);
		theElement.events().fire(new GroupMemberEvent(theElement, group, -1));
	}

	/** @param group The named style group to remove from this element style */
	public void removeGroup(NamedStyleGroup group) {
		TypedStyleGroup<?> typedGroup = group.getGroupForType(theElement.getClass());
		theDependencyController.remove(typedGroup);
		group.removeMember(theElement);
		int index = org.qommons.ArrayUtils.indexOf(theStyleGroups, typedGroup);
		if(index < 0)
			return;
		theStyleGroups = org.qommons.ArrayUtils.remove(theStyleGroups, typedGroup);
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
				groups = org.qommons.ArrayUtils.reverse(groups);
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
			theGroups = org.qommons.ArrayUtils.add(theGroups, e, index);
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
			theGroups = org.qommons.ArrayUtils.remove(theGroups, index);
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
