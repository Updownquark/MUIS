package org.muis.core.model;

/** A document with a cursor and selection anchor */
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

	/** @return The text selected in this document */
	String getSelectedText();

	/** @param listener The listener to be notified when this model's selection changes */
	void addSelectionListener(SelectableDocumentModel.SelectionListener listener);

	/** @param listener The listener to stop notification for */
	void removeSelectionListener(SelectableDocumentModel.SelectionListener listener);
}
