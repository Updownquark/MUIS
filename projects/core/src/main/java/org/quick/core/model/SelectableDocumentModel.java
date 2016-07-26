package org.quick.core.model;

import org.observe.Observable;
import org.observe.ObservableValue;
import org.qommons.Transaction;

/**
 * <p>
 * A document with a cursor and selection anchor.
 * </p>
 * <p>
 * Important Note: In order to reduce unnecessary repaints {@link SelectionChangeEvent} may not be fired if the selection changes as a
 * result of a content change (e.g. inserting characters before the cursor is a single operation that affects both content and selection).
 * In these cases, a single event which implements both {@link QuickDocumentModel.ContentChangeEvent} and {@link SelectionChangeEvent} will
 * be fired from {@link #contentChanges()}.
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

	/** @return An observable that fires each time the selection of a document changes */
	Observable<SelectionChangeEvent> selectionChanges();

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

	public static SelectableDocumentModel flatten(ObservableValue<? extends SelectableDocumentModel> modelWrapper) {
		return new FlattenedSelectableDocumentModel(modelWrapper);
	}

	class FlattenedSelectableDocumentModel extends FlattenedDocumentModel implements SelectableDocumentModel {
		protected FlattenedSelectableDocumentModel(ObservableValue<? extends SelectableDocumentModel> wrapper) {
			super(wrapper);
		}

		@Override
		protected ObservableValue<? extends SelectableDocumentModel> getWrapper() {
			return (ObservableValue<? extends SelectableDocumentModel>) super.getWrapper();
		}

		@Override
		public Observable<SelectionChangeEvent> selectionChanges() {
			return Observable.flatten(getWrapper().value().map(doc -> doc == null ? null : doc.selectionChanges()))
				.filter(evt -> evt != null);
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
}
