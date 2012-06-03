package org.wam.style;

import org.wam.core.WamElement;
import org.wam.core.event.WamEvent;

/**
 * A style controlling the appearance of a specific element
 */
public class ElementStyle extends WamStyle
{
	private final WamElement theElement;

	private NamedStyleGroup [] theStyleGroups;

	/**
	 * Creates an element style
	 * 
	 * @param element The element that this style is for
	 */
	public ElementStyle(WamElement element)
	{
		theElement = element;
		theStyleGroups = new NamedStyleGroup [0];
		addListener(new org.wam.core.event.WamEventListener<Void>()
		{
			@Override
			public boolean isLocal()
			{
				return false;
			}

			@Override
			public void eventOccurred(WamEvent<? extends Void> event, WamElement evtElement)
			{
				theElement.fireEvent(event, false, false);
			}
		});
	}

	/**
	 * @return The element that this style is for
	 */
	public WamElement getElement()
	{
		return theElement;
	}

	@Override
	public WamStyle getParent()
	{
		return null;
	}

	@Override
	public WamStyle [] getDependencies()
	{
		return prisms.util.ArrayUtils.concat(WamStyle.class, super.getDependencies(),
			theStyleGroups);
	}

	/**
	 * @return The number of named style groups in this element style
	 */
	public int getGroupCount()
	{
		return theStyleGroups.length;
	}

	/**
	 * @param group The named style group to add to this element style
	 */
	public void addGroup(NamedStyleGroup group)
	{
		group.addMember(theElement);
		group.addDependent(this);
		if(!prisms.util.ArrayUtils.contains(theStyleGroups, group))
			theStyleGroups = prisms.util.ArrayUtils.add(theStyleGroups, group);
		theElement.fireEvent(new GroupMemberEvent(theElement, group, -1), false, false);
	}

	/**
	 * @param group The named style group to remove from this element style
	 */
	public void removeGroup(NamedStyleGroup group)
	{
		group.removeDependent(this);
		group.removeMember(theElement);
		int index = prisms.util.ArrayUtils.indexOf(theStyleGroups, group);
		if(index < 0)
			return;
		theStyleGroups = prisms.util.ArrayUtils.remove(theStyleGroups, group);
		theElement.fireEvent(new GroupMemberEvent(theElement, group, index), false, false);
	}

	/**
	 * @return All style attributes applying to this style's element directly--those local to this
	 *         element style or any named style groups attached to it. It does not include
	 *         attributes inherited from the element's ancestors
	 */
	public Iterable<StyleAttribute<?>> elementAttributes()
	{
		return new Iterable<StyleAttribute<?>>()
		{
			public java.util.Iterator<StyleAttribute<?>> iterator()
			{
				return new AttributeIterator(ElementStyle.this, theStyleGroups);
			}
		};
	}

	/**
	 * @param forward Whether to iterate forward through the groups or backward
	 * @return An iterable to get the groups associated with this style
	 */
	public Iterable<NamedStyleGroup> groups(final boolean forward)
	{
		return new Iterable<NamedStyleGroup>()
		{
			public NamedStyleIterator iterator()
			{
				return new NamedStyleIterator(theStyleGroups, forward);
			}
		};
	}

	@Override
	public <T> T get(StyleAttribute<T> attr)
	{
		T ret = getLocal(attr);
		if(ret != null)
			return ret;
		final NamedStyleGroup [] groups = theStyleGroups;
		for(int g = groups.length - 1; g >= 0; g--)
		{
			ret = groups[g].getGroupForType(theElement.getClass()).get(attr);
			if(ret != null)
				return ret;
		}
		return super.get(attr);
	}

	private class NamedStyleIterator implements java.util.ListIterator<NamedStyleGroup>
	{
		private NamedStyleGroup [] theGroups;

		private int index;

		private boolean isForward;

		Boolean lastCalledNext;

		NamedStyleIterator(NamedStyleGroup [] groups, boolean forward)
		{
			isForward = forward;
			if(!forward)
				groups = prisms.util.ArrayUtils.reverse(groups);
			theGroups = groups;
		}

		@Override
		public boolean hasNext()
		{
			return index < theGroups.length;
		}

		@Override
		public NamedStyleGroup next()
		{
			NamedStyleGroup ret = theGroups[index];
			index++;
			lastCalledNext = Boolean.TRUE;
			return ret;
		}

		@Override
		public int nextIndex()
		{
			if(isForward)
				return index;
			else
				return theGroups.length - index - 1;
		}

		@Override
		public boolean hasPrevious()
		{
			return index > 0;
		}

		@Override
		public NamedStyleGroup previous()
		{
			index--;
			lastCalledNext = Boolean.FALSE;
			return theGroups[index];
		}

		@Override
		public int previousIndex()
		{
			if(isForward)
				return index - 1;
			else
				return theGroups.length - index;
		}

		@Override
		public void add(NamedStyleGroup e)
		{
			if(e == null)
				throw new NullPointerException("Cannot add a null style group");
			lastCalledNext = null;
			addGroup(e);
			theGroups = prisms.util.ArrayUtils.add(theGroups, e, index);
			index++;
		}

		@Override
		public void remove()
		{
			if(lastCalledNext == null)
				throw new IllegalStateException("remove() can only be called if neither add() nor"
					+ " remove() has been called since the previous call of next() or previous()");
			if(lastCalledNext.booleanValue())
				index--;
			lastCalledNext = null;
			if(index < 0)
			{
				index = 0;
				throw new IndexOutOfBoundsException("No element to remove--at beginning of list");
			}
			else if(index >= theGroups.length)
				throw new IndexOutOfBoundsException("No element to remove--at end of list");
			removeGroup(theGroups[index]);
			theGroups = prisms.util.ArrayUtils.remove(theGroups, index);
			index--;
		}

		@Override
		public void set(NamedStyleGroup e)
		{
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
			removeGroup(theGroups[setIndex]);
			addGroup(e);
			theGroups[setIndex] = e;
		}
	}
}
