package org.quick.core.model;

import org.observe.Observable;

/** A mutable, selectable document model which provides some extra utility methods */
public interface MutableSelectableDocumentModel extends MutableDocumentModel, SelectableDocumentModel {
	@Override
	default MutableSelectableDocumentModel subSequence(int start, int end) {
		return (MutableSelectableDocumentModel) MutableDocumentModel.super.subSequence(start, end);
	}

	@Override
	default MutableSelectableDocumentModel subSequence(int start, int end, Observable<?> until) {
		return new MutableSelectableSubDoc(this, start, end, until);
	}

	/**
	 * Inserts a character sequence at this model's cursor
	 *
	 * @param csq The character sequence to insert
	 * @return This model, for chaining
	 */
	default MutableSelectableDocumentModel insert(CharSequence csq) {
		insert(getCursor(), csq);
		return this;
	}

	/**
	 * Inserts a character at this model's cursor
	 *
	 * @param c The character to insert
	 * @return This model, for chaining
	 */
	default MutableSelectableDocumentModel insert(char c) {
		return insert(String.valueOf(c));
	};

	/** Implements {@link MutableSelectableDocumentModel#subSequence(int, int, Observable)} */
	class MutableSelectableSubDoc extends MutableSubDoc implements MutableSelectableDocumentModel {
		public MutableSelectableSubDoc(MutableSelectableDocumentModel outer, int start, int length, Observable<?> until) {
			super(outer, start, length, until);
		}

		@Override
		protected MutableSelectableDocumentModel getWrapped() {
			return (MutableSelectableDocumentModel) super.getWrapped();
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

		@Override
		public MutableSelectableDocumentModel insert(CharSequence csq) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public MutableSelectableDocumentModel subSequence(int start, int end) {
			return MutableSelectableDocumentModel.super.subSequence(start, end);
		}
	}
}
