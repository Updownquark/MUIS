package org.muis.core.model;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.muis.core.style.MuisStyle;

/** Wraps a document, allowing swapping out documents while preserving added listeners */
public class WrappingDocumentModel {
	private volatile MuisDocumentModel theWrapped;
	private volatile MuisDocumentModel theWrapper;

	private final Collection<MuisDocumentModel.ContentListener> theContentListeners;

	private final Collection<SelectableDocumentModel.SelectionListener> theSelectionListeners;

	/** @param model The initial document model to wrap */
	public WrappingDocumentModel(MuisDocumentModel model) {
		theWrapped = model;
		theContentListeners = new java.util.concurrent.ConcurrentLinkedQueue<>();
		theSelectionListeners = new java.util.concurrent.ConcurrentLinkedQueue<>();
	}

	/** @return The wrapped document */
	public MuisDocumentModel getWrapped() {
		return theWrapped;
	}

	/**
	 * @return The wrapping document model, to which added listeners can be safely switched between wrapped documents. The returned value
	 *         will be {@link MutableDocumentModel mutable} if the wrapped model is, and {@link SelectableDocumentModel selectable} if the
	 *         wrapped model is.
	 */
	public MuisDocumentModel getDocumentModel() {
		return theWrapper;
	}

	/**
	 * Swaps out the wrapped document model with another. This method transfers all listeners added to the old model (through this wrapper)
	 * into the new one.
	 *
	 * @param model The new document model to wrap
	 */
	protected void setWrapped(MuisDocumentModel model) {
		MuisDocumentModel oldModel = model;
		ArrayList<MuisDocumentModel.ContentListener> cls = new ArrayList<>(theContentListeners);
		ArrayList<SelectableDocumentModel.SelectionListener> sls = new ArrayList<>(theSelectionListeners);
		if(model instanceof MutableDocumentModel) {
			if(model instanceof SelectableDocumentModel)
				theWrapper = new InternalMutableSelectableDocumentModel((MutableSelectableDocumentModel) model);
			else
				theWrapper = new InternalMutableDocumentModel((MutableDocumentModel) model);
		} else if(model instanceof SelectableDocumentModel)
			theWrapper = new InternalSelectableDocumentModel((SelectableDocumentModel) model);
		else
			theWrapper = new InternalSimpleDocumentModel(model);
		theWrapped = model;
		for(MuisDocumentModel.ContentListener cl : cls) {
			oldModel.removeContentListener(cl);
			model.addContentListener(cl);
		}
		for(SelectableDocumentModel.SelectionListener sl : sls) {
			if(oldModel instanceof SelectableDocumentModel)
				((SelectableDocumentModel) oldModel).removeSelectionListener(sl);
			if(model instanceof SelectableDocumentModel)
				((SelectableDocumentModel) model).addSelectionListener(sl);
		}
	}

	private class InternalSimpleDocumentModel implements MuisDocumentModel {
		private final MuisDocumentModel theInternalWrapped;

		InternalSimpleDocumentModel(MuisDocumentModel wrap) {
			theInternalWrapped = wrap;
		}

		MuisDocumentModel doc() {
			return theInternalWrapped;
		}

		@Override
		public MuisStyle getStyleAt(int position) {
			return theInternalWrapped.getStyleAt(position);
		}

		@Override
		public Iterable<StyledSequence> iterateFrom(int position) {
			return theInternalWrapped.iterateFrom(position);
		}

		@Override
		public Iterable<StyledSequenceMetric> metrics(int start, float breakWidth) {
			return theInternalWrapped.metrics(start, breakWidth);
		}

		@Override
		public float getPositionAt(float x, float y, int breakWidth) {
			return theInternalWrapped.getPositionAt(x, y, breakWidth);
		}

		@Override
		public Point2D getLocationAt(float position, int breakWidth) {
			return theInternalWrapped.getLocationAt(position, breakWidth);
		}

		@Override
		public void draw(Graphics2D graphics, Rectangle window, int breakWidth) {
			theInternalWrapped.draw(graphics, window, breakWidth);
		}

		@Override
		public void addContentListener(ContentListener listener) {
			theContentListeners.add(listener);
			theInternalWrapped.addContentListener(listener);
		}

		@Override
		public void removeContentListener(ContentListener listener) {
			theContentListeners.remove(listener);
			theInternalWrapped.removeContentListener(listener);
		}

		@Override
		public int length() {
			return theInternalWrapped.length();
		}

		@Override
		public char charAt(int index) {
			return theInternalWrapped.charAt(index);
		}

		@Override
		public CharSequence subSequence(int start, int end) {
			return theInternalWrapped.subSequence(start, end);
		}

		@Override
		public Iterator<StyledSequence> iterator() {
			return theInternalWrapped.iterator();
		}
	}

	private class InternalMutableDocumentModel extends InternalSimpleDocumentModel implements MutableDocumentModel {
		InternalMutableDocumentModel(MutableDocumentModel wrap) {
			super(wrap);
		}

		@Override
		MutableDocumentModel doc() {
			return (MutableDocumentModel) super.doc();
		}

		@Override
		public MutableDocumentModel append(CharSequence csq) {
			doc().append(csq);
			return this;
		}

		@Override
		public MutableDocumentModel append(CharSequence csq, int start, int end) {
			doc().append(csq, start, end);
			return this;
		}

		@Override
		public MutableDocumentModel append(char c) {
			doc().append(c);
			return this;
		}

		@Override
		public MutableDocumentModel setText(String text) {
			doc().setText(text);
			return this;
		}
	}

	private class InternalSelectableDocumentModel extends InternalSimpleDocumentModel implements SelectableDocumentModel {
		InternalSelectableDocumentModel(SelectableDocumentModel wrap) {
			super(wrap);
		}

		@Override
		SelectableDocumentModel doc() {
			return (SelectableDocumentModel) super.doc();
		}

		@Override
		public int getCursor() {
			return doc().getCursor();
		}

		@Override
		public int getSelectionAnchor() {
			return doc().getSelectionAnchor();
		}

		@Override
		public void setCursor(int cursor) {
			doc().setCursor(cursor);
		}

		@Override
		public void setSelection(int anchor, int cursor) {
			doc().setSelection(anchor, cursor);
		}

		@Override
		public String getSelectedText() {
			return doc().getSelectedText();
		}

		@Override
		public void addSelectionListener(SelectionListener listener) {
			theSelectionListeners.add(listener);
			doc().addSelectionListener(listener);
		}

		@Override
		public void removeSelectionListener(SelectionListener listener) {
			theSelectionListeners.remove(listener);
			doc().removeSelectionListener(listener);
		}
	}

	private class InternalMutableSelectableDocumentModel extends InternalMutableDocumentModel implements MutableSelectableDocumentModel {
		InternalMutableSelectableDocumentModel(MutableSelectableDocumentModel wrap) {
			super(wrap);
		}

		@Override
		MutableSelectableDocumentModel doc() {
			return (MutableSelectableDocumentModel) super.doc();
		}

		@Override
		public int getCursor() {
			return doc().getCursor();
		}

		@Override
		public int getSelectionAnchor() {
			return doc().getSelectionAnchor();
		}

		@Override
		public void setCursor(int cursor) {
			doc().setCursor(cursor);
		}

		@Override
		public void setSelection(int anchor, int cursor) {
			doc().setSelection(anchor, cursor);
		}

		@Override
		public String getSelectedText() {
			return doc().getSelectedText();
		}

		@Override
		public void addSelectionListener(SelectionListener listener) {
			theSelectionListeners.add(listener);
			doc().addSelectionListener(listener);
		}

		@Override
		public void removeSelectionListener(SelectionListener listener) {
			theSelectionListeners.remove(listener);
			doc().removeSelectionListener(listener);
		}

		@Override
		public MutableSelectableDocumentModel insert(CharSequence csq) {
			doc().insert(csq);
			return this;
		}

		@Override
		public MutableSelectableDocumentModel insert(char c) {
			doc().insert(c);
			return this;
		}

		@Override
		public MutableSelectableDocumentModel insert(int offset, CharSequence csq) {
			doc().insert(offset, csq);
			return this;
		}

		@Override
		public MutableSelectableDocumentModel insert(int offset, char c) {
			doc().insert(offset, c);
			return this;
		}

		@Override
		public MutableSelectableDocumentModel delete(int start, int end) {
			doc().delete(start, end);
			return this;
		}
	}
}
