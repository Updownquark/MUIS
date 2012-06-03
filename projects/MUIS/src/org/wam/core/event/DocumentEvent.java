package org.wam.core.event;

/**
 * A scheduled event for a document. The document performs the action represented by each added
 * event after the next UI event (mouse, keyboard, etc.) completes.
 */
public interface DocumentEvent
{
	/**
	 * Determines whether another event's functionality is completely duplicated by this event. This
	 * allows the document to avoid redundant work.
	 * 
	 * @param evt The event to test
	 * @return Whether this event duplicates evt's
	 */
	boolean contains(DocumentEvent evt);

	/**
	 * Performs the work represented by this event
	 */
	void doAction();
}
