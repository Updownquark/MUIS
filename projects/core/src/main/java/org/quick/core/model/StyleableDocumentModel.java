package org.quick.core.model;

import org.observe.Observable;
import org.quick.core.style.MutableStyle;

/** A document model whose content can be styled */
public interface StyleableDocumentModel extends MutableDocumentModel {
	/**
	 * <p>
	 * Creates a setter for styles on a subsequence in this model. The returned style may not attempt to return intelligent values from the
	 * style getter methods, nor does it support listening, since complete consistency is impossible because the given subsequence may
	 * overlap sequences with differing styles. This method is intended for setting styles in this model. For accessing style information,
	 * use the {@link #iterator() iterator}.
	 * </p>
	 * <p>
	 * The returned style will reflect its sequences directly--"local" values from the sequences will become the style's local attributes.
	 * </p>
	 * <p>
	 * Note that using the setter methods of the returned style may give inconsistent or unexpected results or exceptions if this document
	 * is modified by another thread while the returned style is still in use. Use {@link #holdForWrite(Object)} to prevent this.
	 * </p>
	 *
	 * @param start The start of the sequence to the style-setter for
	 * @param end The end of the sequence to get the style-setter for
	 * @return A style that represents and allows setting of styles for the given subsequence in this document.
	 */
	MutableStyle getSegmentStyle(int start, int end);

	@Override
	default StyleableDocumentModel subSequence(int start) {
		return (StyleableDocumentModel) MutableDocumentModel.super.subSequence(start);
	}

	@Override
	default StyleableDocumentModel subSequence(int start, int end) {
		return (StyleableDocumentModel) MutableDocumentModel.super.subSequence(start, end);
	}

	@Override
	default StyleableDocumentModel subSequence(int start, int end, Observable<?> until) {
		return new StyleableSubDoc(this, start, end - start, until);
	}

	/** Implements {@link StyleableDocumentModel#subSequence(int, int, Observable)} */
	class StyleableSubDoc extends MutableSubDoc implements StyleableDocumentModel {
		public StyleableSubDoc(StyleableDocumentModel outer, int start, int length, Observable<?> until) {
			super(outer, start, length, until);
		}

		@Override
		protected StyleableDocumentModel getWrapped() {
			return (StyleableDocumentModel) super.getWrapped();
		}

		@Override
		public MutableStyle getSegmentStyle(int start, int end) {
			return getWrapped().getSegmentStyle(transform(start), transform(end));
		}
	}
}
