package org.quick.core.model;

import org.quick.core.style.GroupableStyle;

/** A document that is mutable, selectable, and styleable */
public interface StyleableSelectableDocumentModel extends MutableSelectableDocumentModel, StyleableDocumentModel {
	@Override
	default StyleableSelectableDocumentModel subSequence(int start) {
		return (StyleableSelectableDocumentModel) MutableSelectableDocumentModel.super.subSequence(start);
	}

	@Override
	default StyleableSelectableDocumentModel subSequence(int start, int end) {
		return new StyleableSelectableSubDoc(this, start, end - start);
	}

	/** Implements {@link StyleableSelectableDocumentModel#subSequence(int, int)} */
	class StyleableSelectableSubDoc extends MutableSelectableSubDoc implements StyleableSelectableDocumentModel {
		public StyleableSelectableSubDoc(StyleableSelectableDocumentModel outer, int start, int length) {
			super(outer, start, length);
		}

		@Override
		protected StyleableSelectableDocumentModel getWrapped() {
			return (StyleableSelectableDocumentModel) super.getWrapped();
		}

		@Override
		public GroupableStyle getSegmentStyle(int start, int end) {
			return getWrapped().getSegmentStyle(transform(start), transform(end));
		}

		@Override
		public StyleableSelectableDocumentModel subSequence(int start, int end) {
			return StyleableSelectableDocumentModel.super.subSequence(start, end);
		}
	}
}
