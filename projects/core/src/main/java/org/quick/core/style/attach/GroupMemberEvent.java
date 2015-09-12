package org.quick.core.style.attach;

import org.quick.core.QuickElement;
import org.quick.core.event.QuickEvent;

/** An event representing the addition or removal of a style group in an element */
public class GroupMemberEvent implements QuickEvent {
	/** Filters for events of this type */
	public static final java.util.function.Function<QuickEvent, GroupMemberEvent> groups = value -> {
		return value instanceof GroupMemberEvent ? (GroupMemberEvent) value : null;
	};

	private final QuickElement theElement;
	private final NamedStyleGroup theGroup;

	private final int theRemoveIndex;

	/**
	 * Creates a GroupMemberEvent
	 *
	 * @param element The element that the group was added to or remove from
	 * @param group The group that was added or removed
	 * @param removeIdx The index that the group was removed from, or < 0 if the group was added
	 */
	public GroupMemberEvent(QuickElement element, NamedStyleGroup group, int removeIdx) {
		theElement = element;
		theGroup = group;
		theRemoveIndex = removeIdx;
	}

	/** @return The element that the group was added to or removed from */
	@Override
	public QuickElement getElement() {
		return theElement;
	}

	@Override
	public QuickEvent getCause() {
		return null;
	}

	/** @return The group that was added or removed */
	public NamedStyleGroup getGroup() {
		return theGroup;
	}

	/** @return The index that the group was removed from, or < 0 if the group was added */
	public int getRemoveIndex() {
		return theRemoveIndex;
	}

	@Override
	public boolean isOverridden() {
		return false;
	}
}
