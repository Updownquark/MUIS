package org.quick.core.model;

import org.observe.ObservableValue;
import org.qommons.Transaction;

/**
 * <p>
 * A document with a cursor and selection anchor.
 * </p>
 * <p>
 * Important Note: In order to reduce unnecessary repaints, when the selection changes as a result of a content change (e.g. inserting
 * characters before the cursor is a single operation that affects both content and selection), a single event which implements both
 * {@link QuickDocumentModel.ContentChangeEvent} and {@link SelectionChangeEvent} will be fired.
 * </p>
 */
public interface SelectableDocumentModel extends QuickDocumentModel {
	/** Fired when a document model's selection changes */
	public static interface SelectionChangeEvent extends QuickDocumentChangeEvent {
		/** @return The location of the model's selection anchor */
		int getSelectionAnchor();

		/** @return The location of the model's cursor */
		int getCursor();

		@Override
		default int getStartIndex() {
			return Math.min(getSelectionAnchor(), getCursor());
		}

		@Override
		default int getEndIndex() {
			return Math.max(getSelectionAnchor(), getCursor());
		}
	}

	@Override
	default QuickDocumentModel subSequence(int start, int end) {
		return new SelectableSubDoc(this, start, end - start);
	}

	/**
	 * @param cause The event or thing that is causing the changes in the transaction
	 * @return A transaction to close when the caller finishes its operation
	 */
	Transaction holdForWrite(Object cause);

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
	 * @return This model, for chaining
	 */
	SelectableDocumentModel setCursor(int cursor);

	/**
	 * Changes the selection interval (and with it, the cursor) in this document
	 *
	 * @param anchor The new anchor for the selection in this document
	 * @param cursor The new location for the cursor in this document
	 * @return This model, for chaining
	 */
	SelectableDocumentModel setSelection(int anchor, int cursor);

	/** @return The text selected in this document */
	String getSelectedText();

	/**
	 * @param modelWrapper An observableValue containing a selectable document model
	 * @return A model that reflects the data in the contained model as it changes
	 */
	public static SelectableDocumentModel flatten(ObservableValue<? extends SelectableDocumentModel> modelWrapper) {
		return new FlattenedSelectableDocumentModel(modelWrapper);
	}

	/** Implements {@link SelectableDocumentModel#subSequence(int, int)} */
	class SelectableSubDoc extends SubDocument implements SelectableDocumentModel {
		protected SelectableSubDoc(SelectableDocumentModel outer, int start, int length) {
			super(outer, start, length);
		}

		@Override
		protected SelectableDocumentModel getWrapped() {
			return (SelectableDocumentModel) super.getWrapped();
		}

		@Override
		public Transaction holdForWrite(Object cause) {
			return getWrapped().holdForWrite(cause);
		}

		@Override
		public int getCursor() {
			return transform(getWrapped().getCursor());
		}

		@Override
		public int getSelectionAnchor() {
			return transform(getWrapped().getSelectionAnchor());
		}

		@Override
		public SelectableDocumentModel setCursor(int cursor) {
			getWrapped().setCursor(cursor + getStart());
			return this;
		}

		@Override
		public SelectableDocumentModel setSelection(int anchor, int cursor) {
			getWrapped().setSelection(anchor + getStart(), cursor + getStart());
			return this;
		}

		@Override
		public String getSelectedText() {
			return filter(getWrapped().getSelectedText(), Math.min(getWrapped().getSelectionAnchor(), getWrapped().getCursor()));
		}

		@Override
		protected QuickDocumentChangeEvent filterMap(QuickDocumentChangeEvent change) {
			if (!(change instanceof SelectionChangeEvent))
				return super.filterMap(change);
			if (change instanceof ContentChangeEvent) {
				ContentChangeEvent contentChange = (ContentChangeEvent) change;
				return new ContentAndSelectionChangeEventImpl(this, toString(), filter(contentChange.getChange(), change.getStartIndex()),
					transform(change.getStartIndex()), transform(change.getEndIndex()), contentChange.isRemove(), getCursor(),
					getSelectionAnchor(), change);
			} else
				return new SelectionChangeEventImpl(this, getSelectionAnchor(), getCursor(), change);
		}
	}

	/** Implements {@link SelectableDocumentModel#flatten(ObservableValue)} */
	class FlattenedSelectableDocumentModel extends FlattenedDocumentModel implements SelectableDocumentModel {
		protected FlattenedSelectableDocumentModel(ObservableValue<? extends SelectableDocumentModel> wrapper) {
			super(wrapper);
		}

		@Override
		protected ObservableValue<? extends SelectableDocumentModel> getWrapper() {
			return (ObservableValue<? extends SelectableDocumentModel>) super.getWrapper();
		}

		@Override
		protected ContentChangeEvent createClearEvent(QuickDocumentModel oldModel, Object cause) {
			class ClearEvent implements ContentChangeEvent, SelectionChangeEvent {
				@Override
				public QuickDocumentModel getModel() {
					return FlattenedSelectableDocumentModel.this;
				}

				@Override
				public int getStartIndex() {
					return 0;
				}

				@Override
				public int getEndIndex() {
					return oldModel.length();
				}

				@Override
				public boolean isRemove() {
					return true;
				}

				@Override
				public String getValue() {
					return oldModel.toString();
				}

				@Override
				public String getChange() {
					return oldModel.toString();
				}

				@Override
				public int getSelectionAnchor() {
					return 0;
				}

				@Override
				public int getCursor() {
					return 0;
				}

				@Override
				public Object getCause() {
					return cause;
				}
			}
			return new ClearEvent();
		}

		@Override
		protected ContentChangeEvent createPopulateEvent(QuickDocumentModel newModel, Object cause) {
			class PopulateEvent implements ContentChangeEvent, SelectionChangeEvent {
				@Override
				public QuickDocumentModel getModel() {
					return FlattenedSelectableDocumentModel.this;
				}

				@Override
				public int getStartIndex() {
					return 0;
				}

				@Override
				public int getEndIndex() {
					return newModel.length();
				}

				@Override
				public boolean isRemove() {
					return true;
				}

				@Override
				public String getValue() {
					return newModel.toString();
				}

				@Override
				public String getChange() {
					return newModel.toString();
				}

				@Override
				public int getSelectionAnchor() {
					return ((SelectableDocumentModel) newModel).getSelectionAnchor();
				}

				@Override
				public int getCursor() {
					return ((SelectableDocumentModel) newModel).getCursor();
				}

				@Override
				public Object getCause() {
					return cause;
				}
			}
			return new PopulateEvent();
		}

		@Override
		public Transaction holdForWrite(Object cause) {
			SelectableDocumentModel wrapped = getWrapper().get();
			if (wrapped == null)
				return () -> {
				};
			return wrapped.holdForWrite(cause);
		}

		@Override
		public int getCursor() {
			SelectableDocumentModel wrapped = getWrapper().get();
			if (wrapped == null)
				return 0;
			return wrapped.getCursor();
		}

		@Override
		public int getSelectionAnchor() {
			SelectableDocumentModel wrapped = getWrapper().get();
			if (wrapped == null)
				return 0;
			return wrapped.getSelectionAnchor();
		}

		@Override
		public SelectableDocumentModel setCursor(int cursor) {
			SelectableDocumentModel wrapped = getWrapper().get();
			if (wrapped == null){
				if(cursor!=0)
					throw new IndexOutOfBoundsException(cursor+" of 0");
				else
					return this;
			}
			return wrapped.setCursor(cursor);
		}

		@Override
		public SelectableDocumentModel setSelection(int anchor, int cursor) {
			SelectableDocumentModel wrapped = getWrapper().get();
			if (wrapped == null) {
				if (cursor != 0 && cursor != 0)
					throw new IndexOutOfBoundsException(cursor + " of 0");
				else
					return this;
			}
			return wrapped.setCursor(cursor);
		}

		@Override
		public String getSelectedText() {
			SelectableDocumentModel wrapped = getWrapper().get();
			if (wrapped == null)
				return "";
			return wrapped.getSelectedText();
		}
	}

	/** A default implementation of SelectionChangeEvent */
	class SelectionChangeEventImpl implements SelectionChangeEvent {
		private final SelectableDocumentModel theModel;

		private final int theSelectionAnchor;

		private final int theCursor;

		private final Object theCause;

		/**
		 * @param model The document model whose selection changed
		 * @param anchor The location of the model's selection anchor
		 * @param cursor The location of the model's cursor
		 * @param cause The cause of the change
		 */
		public SelectionChangeEventImpl(SelectableDocumentModel model, int anchor, int cursor, Object cause) {
			theModel = model;
			theSelectionAnchor = anchor;
			theCursor = cursor;
			theCause = cause;
		}

		/** @return The document model whose selection changed */
		@Override
		public SelectableDocumentModel getModel() {
			return theModel;
		}

		/** @return The location of the model's selection anchor */
		@Override
		public int getSelectionAnchor() {
			return theSelectionAnchor;
		}

		/** @return The location of the model's cursor */
		@Override
		public int getCursor() {
			return theCursor;
		}

		@Override
		public Object getCause() {
			return theCause;
		}

		@Override
		public String toString() {
			return "Selection=" + (theCursor == theSelectionAnchor ? "" + theCursor : theSelectionAnchor + "->" + theCursor);
		}
	}

	/** A default implementation of both ContentChangeEvent and SelectionChangeEvent */
	class ContentAndSelectionChangeEventImpl extends ContentChangeEventImpl implements SelectionChangeEvent {
		private final int theAnchor;

		private final int theCursor;

		/**
		 * @param model The document model whose content changed
		 * @param value The document model's content after the change
		 * @param change The section of content that was added or removed
		 * @param startIndex The index of the addition or removal
		 * @param endIndex The end index of the addition or removal
		 * @param remove Whether this change represents a removal or an addition
		 * @param cursor The cursor location after the change
		 * @param anchor The anchor location after the change
		 * @param cause The cause of this event
		 */
		public ContentAndSelectionChangeEventImpl(SelectableDocumentModel model, String value, String change, int startIndex, int endIndex,
			boolean remove, int cursor, int anchor, Object cause) {
			super(model, value, change, startIndex, endIndex, remove, cause);
			theCursor = cursor;
			theAnchor = anchor;
		}

		@Override
		public SelectableDocumentModel getModel() {
			return (SelectableDocumentModel) super.getModel();
		}

		@Override
		public int getSelectionAnchor() {
			return theAnchor;
		}

		@Override
		public int getCursor() {
			return theCursor;
		}

		@Override
		public String toString() {
			return super.toString() + " and Selection=" + (theCursor == theAnchor ? "" + theCursor : theAnchor + "->" + theCursor);
		}
	}
}
