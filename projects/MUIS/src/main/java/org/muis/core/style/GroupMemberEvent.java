package org.muis.core.style;

import org.muis.core.MuisElement;
import org.muis.core.event.MuisEventType;

/**
 * An event representing the addition or removal of a style group in an element
 */
public class GroupMemberEvent extends org.muis.core.event.MuisEvent<NamedStyleGroup>
{
	/** The event type for {@link GroupMemberEvent}s */
	public static final MuisEventType<NamedStyleGroup> TYPE = new MuisEventType<NamedStyleGroup>(
		"Style Group Member Event", NamedStyleGroup.class);

	private final MuisElement theElement;

	private final int theRemoveIndex;

	/**
	 * Creates a GroupMemberEvent
	 * 
	 * @param element The element that the group was added to or remove from
	 * @param value The group that was added or removed
	 * @param removeIdx The index that the group was removed from, or < 0 if the group was added
	 */
	public GroupMemberEvent(MuisElement element, NamedStyleGroup value, int removeIdx)
	{
		super(TYPE, value);
		theElement = element;
		theRemoveIndex = removeIdx;
	}

	/**
	 * @return The element that the group was added to or removed from
	 */
	public MuisElement getElement()
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
