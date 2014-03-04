package org.muis.core.model;

/**
 * <p>
 * A document with a cursor and selection anchor.
 * </p>
 * <p>
 * Important Note: In order to reduce unnecessary repaints {@link SelectionChangeEvent} may not be fired if the selection changes as a
 * result of a content change (e.g. inserting characters before the cursor is a single operation that affects both content and selection).
 * In these cases, a single event which implements both {@link MuisDocumentModel.ContentChangeEvent} and {@link SelectionChangeEvent} will
 * be fired through registered {@link MuisDocumentModel.ContentListener}s.
 * </p>
 */
public interface SelectableDocumentModel extends MuisDocumentModel {
	/** Fired when a document model's selection changes */
	public static interface SelectionChangeEvent {
		/** @return The document model whose selection changed */
		SelectableDocumentModel getModel();

		/** @return The location of the model's selection anchor */
		int getSelectionAnchor();

		/** @return The location of the model's cursor */
		int getCursor();
	}

	/** Listens for changes to a document's selection */
	public static interface SelectionListener {
		/** @param evt The event containing information about the selection change */
		void selectionChanged(SelectionChangeEvent evt);
	}

	/**
	 * @return The location of the cursor in this document--the location at which text will be added in response to non-positioned events
	 *         (e.g. character input). If text is selected in this document then this position is also one end of the selection interval.
	 */
	int getCursor();

	/**
	 * @return The other end of the selection interval (opposite cursor). If no text is selected in this document then this value will be
	 *         the same as the {@link #getCursor() cursor}
	 */
	int getSelectionAnchor();

	/**
	 * Sets the cursor in this document and cancels any existing selection interval
	 * 
	 * @param cursor The new location for the cursor in this document
	 */
	void setCursor(int cursor);

	/**
	 * Changes the selection interval (and with it, the cursor) in this document
	 * 
	 * @param anchor The new anchor for the selection in this document
	 * @param cursor The new location for the cursor in this document
	 */
	void setSelection(int anchor, int cursor);

	/** @return The text selected in this document */
	String getSelectedText();

	/** @param listener The listener to be notified when this model's selection changes */
	void addSelectionListener(SelectableDocumentModel.SelectionListener listener);

	/** @param listener The listener to stop notification for */
	void removeSelectionListener(SelectableDocumentModel.SelectionListener listener);
}
