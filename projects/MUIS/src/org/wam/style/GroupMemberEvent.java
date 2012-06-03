package org.wam.style;

import org.wam.core.WamElement;
import org.wam.core.event.WamEventType;

/**
 * An event representing the addition or removal of a style group in an element
 */
public class GroupMemberEvent extends org.wam.core.event.WamEvent<NamedStyleGroup>
{
	/** The event type for {@link GroupMemberEvent}s */
	public static final WamEventType<NamedStyleGroup> TYPE = new WamEventType<NamedStyleGroup>(
		"Style Group Member Event", NamedStyleGroup.class);

	private final WamElement theElement;

	private final int theRemoveIndex;

	/**
	 * Creates a GroupMemberEvent
	 * 
	 * @param element The element that the group was added to or remove from
	 * @param value The group that was added or removed
	 * @param removeIdx The index that the group was removed from, or < 0 if the group was added
	 */
	public GroupMemberEvent(WamElement element, NamedStyleGroup value, int removeIdx)
	{
		super(TYPE, value);
		theElement = element;
		theRemoveIndex = removeIdx;
	}

	/**
	 * @return The element that the group was added to or removed from
	 */
	public WamElement getElement()
	{
		return theElement;
	}

	/**
	 * @return The index that the group was removed from, or < 0 if the group was added
	 */
	public int getRemoveIndex()
	{
		return theRemoveIndex;
	}
}
