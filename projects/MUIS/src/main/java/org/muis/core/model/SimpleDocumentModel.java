package org.muis.core.model;

import static org.muis.core.MuisConstants.States.TEXT_SELECTION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;

import org.muis.core.style.MuisStyle;
import org.muis.core.style.stateful.InternallyStatefulStyle;
import org.muis.core.style.stateful.StateChangedEvent;
import org.muis.core.style.stateful.StatefulStyle;

/** A very simple document model that uses a single style and keeps a single, mutable set of content and supports single interval selection */
public class SimpleDocumentModel extends AbstractMuisDocumentModel implements SelectableDocumentModel, Appendable {
	private final InternallyStatefulStyle theParentStyle;

	private final MuisStyle theNormalStyle;

	private final MuisStyle theSelectedStyle;

	private final StringBuilder theContent;

	private int theCursor;

	private int theSelectionAnchor;

	private java.util.concurrent.locks.ReentrantReadWriteLock theLock;

	private Collection<MuisDocumentModel.ContentListener> theContentListeners;

	private Collection<SelectableDocumentModel.SelectionListener> theSelectionListeners;

	/** @param parentStyle The parent style for this document */
	public SimpleDocumentModel(final InternallyStatefulStyle parentStyle) {
		theParentStyle = parentStyle;
		theNormalStyle = new SelectionStyle(parentStyle, false);
		theSelectedStyle = new SelectionStyle(parentStyle, true);
		theContent = new StringBuilder();
		theLock = new java.util.concurrent.locks.ReentrantReadWriteLock();
		theContentListeners = new java.util.concurrent.ConcurrentLinkedQueue<>();
		theSelectionListeners = new java.util.concurrent.ConcurrentLinkedQueue<>();
	}

	/** @return This document's parent's style */
	public StatefulStyle getParentStyle() {
		return theParentStyle;
	}

	/** @return The style for text that is not selected */
	public MuisStyle getNormalStyle() {
		return theNormalStyle;
	}

	/** @return The style for text that is selected */
	public MuisStyle getSelectedStyle() {
		return theSelectedStyle;
	}

	@Override
	public void addContentListener(MuisDocumentModel.ContentListener listener) {
		if(listener != null)
			theContentListeners.add(listener);
	}

	@Override
	public void removeContentListener(MuisDocumentModel.ContentListener listener) {
		theContentListeners.remove(listener);
	}

	@Override
	public void addSelectionListener(SelectableDocumentModel.SelectionListener listener) {
		if(listener != null)
			theSelectionListeners.add(listener);
	}

	@Override
	public void removeSelectionListener(SelectableDocumentModel.SelectionListener listener) {
		theSelectionListeners.remove(listener);
	}

	@Override
	public int getCursor() {
		return theCursor;
	}

	@Override
	public int getSelectionAnchor() {
		return theSelectionAnchor;
	}

	/**
	 * Sets the cursor in this document and cancels any existing selection interval
	 *
	 * @param cursor The new location for the cursor in this document
	 */
	public void setCursor(int cursor) {
		theCursor = cursor;
		theSelectionAnchor = cursor;
		fireSelectionEvent(cursor, cursor);
	}

	/**
	 * Changes the selection interval (and with it, the cursor) in this document
	 *
	 * @param anchor The new anchor for the selection in this document
	 * @param cursor The new location for the cursor in this document
	 */
	public void setSelection(int anchor, int cursor) {
		if(theSelectionAnchor == anchor && theCursor == cursor)
			return;
		theSelectionAnchor = anchor;
		theCursor = cursor;
		fireSelectionEvent(anchor, cursor);
	}

	@Override
	public Iterator<StyledSequence> iterator() {
		final String content = toString();
		int anchor = theSelectionAnchor;
		int cursor = theCursor;
		ArrayList<StyledSequence> ret = new ArrayList<>();
		int div1 = anchor;
		int div2 = cursor;
		if(div1 > div2) {
			int temp = div1;
			div1 = div2;
			div2 = temp;
		}
		if(div1 == div2)
			ret.add(new SimpleStyledSequence(content, theNormalStyle));
		else {
			if(div1 > 0)
				ret.add(new SimpleStyledSequence(content.substring(0, div1), theNormalStyle));
			ret.add(new SimpleStyledSequence(content.substring(div1, div2), theSelectedStyle));
			if(div2 < content.length())
				ret.add(new SimpleStyledSequence(content.substring(div2), theNormalStyle));
		}
		return java.util.Collections.unmodifiableList(ret).iterator();
	}

	@Override
	public int length() {
		Lock lock = theLock.readLock();
		lock.lock();
		try {
			return theContent.length();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public char charAt(int index) {
		Lock lock = theLock.readLock();
		lock.lock();
		try {
			return theContent.charAt(index);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		Lock lock = theLock.readLock();
		lock.lock();
		try {
			return theContent.subSequence(start, end);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public String toString() {
		Lock lock = theLock.readLock();
		lock.lock();
		try {
			return theContent.toString();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public String getSelectedText() {
		int start = theSelectionAnchor;
		int end = theCursor;
		if(start == end)
			return "";
		if(start > end) {
			int temp = start;
			start = end;
			end = temp;
		}
		Lock lock = theLock.readLock();
		lock.lock();
		try {
			if(end < 0 || start >= theContent.length())
				return "";
			if(start < 0)
				start = 0;
			if(end > theContent.length())
				end = theContent.length();
			return theContent.substring(start, end);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public SimpleDocumentModel append(CharSequence csq) {
		String value;
		int index;
		String change = csq.toString();
		Lock lock = theLock.writeLock();
		lock.lock();
		try {
			if(theSelectionAnchor == theContent.length())
				theSelectionAnchor += change.length();
			if(theCursor == theContent.length())
				theCursor += change.length();
			index = theContent.length();
			theContent.append(change);
			value = theContent.toString();
		} finally {
			lock.unlock();
		}
		fireContentEvent(value, change, index, false);
		return this;
	}

	@Override
	public SimpleDocumentModel append(CharSequence csq, int start, int end) {
		append(csq.subSequence(start, end));
		return this;
	}

	@Override
	public SimpleDocumentModel append(char c) {
		String value;
		int index;
		String change = new String(new char[] {c});
		Lock lock = theLock.writeLock();
		lock.lock();
		try {
			if(theSelectionAnchor == theContent.length())
				theSelectionAnchor++;
			if(theCursor == theContent.length())
				theCursor++;
			index = theContent.length();
			theContent.append(c);
			value = theContent.toString();
		} finally {
			lock.unlock();
		}
		fireContentEvent(value, change, index, false);
		return this;
	}

	/**
	 * Inserts a character sequence at this model's cursor
	 *
	 * @param csq The character sequence to insert
	 * @return This model, for chaining
	 */
	public SimpleDocumentModel insert(CharSequence csq) {
		String value;
		int index = theCursor;
		String change = csq.toString();
		Lock lock = theLock.writeLock();
		lock.lock();
		try {
			if(theSelectionAnchor >= theCursor)
				theSelectionAnchor += change.length();
			theContent.insert(index, change);
			value = theContent.toString();
			theCursor += change.length();
		} finally {
			lock.unlock();
		}
		fireContentEvent(value, change, index, false);
		return this;
	}

	/**
	 * Inserts a character at this model's cursor
	 *
	 * @param c The character to insert
	 * @return This model, for chaining
	 */
	public SimpleDocumentModel insert(char c) {
		String value;
		int index = theCursor;
		String change = new String(new char[] {c});
		Lock lock = theLock.writeLock();
		lock.lock();
		try {
			if(theSelectionAnchor >= theCursor)
				theSelectionAnchor++;
			theContent.insert(index, c);
			value = theContent.toString();
			theCursor++;
		} finally {
			lock.unlock();
		}
		fireContentEvent(value, change, index, false);
		return this;
	}

	/**
	 * Inserts a character sequence
	 *
	 * @param offset The index at which to insert the character sequence
	 * @param csq The character sequence to insert
	 * @return This model, for chaining
	 */
	public SimpleDocumentModel insert(int offset, CharSequence csq) {
		String value;
		String change = csq.toString();
		Lock lock = theLock.writeLock();
		lock.lock();
		try {
			if(theSelectionAnchor >= offset)
				theSelectionAnchor += csq.length();
			if(theCursor >= offset)
				theCursor += csq.length();
			theContent.insert(offset, csq);
			value = theContent.toString();
		} finally {
			lock.unlock();
		}
		fireContentEvent(value, change, offset, false);
		return this;
	}

	/**
	 * Inserts a character
	 *
	 * @param offset The index at which to insert the character
	 * @param c The character to insert
	 * @return This model, for chaining
	 */
	public SimpleDocumentModel insert(int offset, char c) {
		String value;
		String change = new String(new char[] {c});
		boolean selChange = false;
		Lock lock = theLock.writeLock();
		lock.lock();
		try {
			if(theSelectionAnchor >= offset) {
				selChange = true;
				theSelectionAnchor++;
			}
			if(theCursor >= offset) {
				selChange = true;
				theCursor++;
			}
			theContent.insert(offset, c);
			value = theContent.toString();
		} finally {
			lock.unlock();
		}
		if(selChange)
			fireContentEvent(value, change, c, false, theSelectionAnchor, theCursor);
		else
			fireContentEvent(value, change, c, false, -1, -1);
		return this;
	}

	/**
	 * Deletes characters from this document
	 *
	 * @param start The index of the start of the sequence to remove, inclusive
	 * @param end The index of the end of the sequence to remove, exclusive
	 * @return This model, for chaining
	 */
	public SimpleDocumentModel delete(int start, int end) {
		if(start > end) {
			int temp = start;
			start = end;
			end = temp;
		}
		String value;
		String change;
		boolean selChange = false;
		Lock lock = theLock.writeLock();
		lock.lock();
		try {
			if(theSelectionAnchor >= start) {
				if(theSelectionAnchor >= end)
					theSelectionAnchor -= end - start;
				else
					theSelectionAnchor = start;
				selChange = true;
			}
			if(theCursor >= start) {
				if(theCursor >= end)
					theCursor -= end - start;
				else
					theCursor = start;
				selChange = true;
			}
			change = theContent.substring(start, end);
			theContent.delete(start, end);
			value = theContent.toString();
		} finally {
			lock.unlock();
		}
		if(selChange)
			fireContentEvent(value, change, start, true, theSelectionAnchor, theCursor);
		else
			fireContentEvent(value, change, start, true, -1, -1);
		return this;
	}

	/**
	 * Sets the content for this model
	 *
	 * @param text The text to set
	 * @return This model, for chaining
	 */
	public SimpleDocumentModel setText(String text) {
		String oldValue;
		Lock lock = theLock.writeLock();
		lock.lock();
		try {
			theSelectionAnchor = text.length();
			theCursor = text.length();
			int oldLen = theContent.length();
			oldValue = theContent.toString();
			theContent.setLength(0);
			theContent.append(text);
			if(oldLen - text.length() > 100 && oldLen - text.length() > text.length() / 2)
				theContent.trimToSize();
		} finally {
			lock.unlock();
		}
		fireContentEvent("", oldValue, 0, true, -1, -1);
		fireContentEvent(text, text, 0, false, theSelectionAnchor, theCursor);
		return this;
	}

	private void fireSelectionEvent(int anchor, int cursor) {
		clearCache();
		SelectableDocumentModel.SelectionChangeEvent evt = new SelectionChangeEventImpl(this, anchor, cursor);
		for(SelectableDocumentModel.SelectionListener listener : theSelectionListeners)
			listener.selectionChanged(evt);
	}

	private void fireContentEvent(String value, String change, int index, boolean remove, int anchor, int cursor) {
		clearCache();
		MuisDocumentModel.ContentChangeEvent evt;
		if(anchor < 0 && cursor < 0)
			evt= new ContentChangeEventImpl(this, value, change, index, remove);
		else
			evt = new ContentAndSelectionChangeEventImpl(this, value, change, index, remove, anchor, cursor);
		for(MuisDocumentModel.ContentListener listener : theContentListeners)
			listener.contentChanged(evt);
	}

	private static class ContentChangeEventImpl implements ContentChangeEvent {
		private final SimpleDocumentModel theModel;

		private final String theValue;

		private final String theChange;

		private final int theIndex;

		private final boolean isRemove;

		/**
		 * @param model The document model whose content changed
		 * @param value The document model's content after the change
		 * @param change The section of content that was added or removed
		 * @param index The index of the addition or removal
		 * @param remove Whether this change represents a removal or an addition
		 */
		ContentChangeEventImpl(SimpleDocumentModel model, String value, String change, int index, boolean remove) {
			theModel = model;
			theValue = value;
			theChange = change;
			theIndex = index;
			isRemove = remove;
		}

		@Override
		public SimpleDocumentModel getModel() {
			return theModel;
		}

		@Override
		public String getValue() {
			return theValue;
		}

		@Override
		public String getChange() {
			return theChange;
		}

		@Override
		public int getIndex() {
			return theIndex;
		}

		@Override
		public boolean isRemove() {
			return isRemove;
		}
	}

	private static class SelectionChangeEventImpl implements SelectionChangeEvent {
		private final SimpleDocumentModel theModel;

		private final int theSelectionAnchor;

		private final int theCursor;

		/**
		 * @param model The document model whose selection changed
		 * @param anchor The location of the model's selection anchor
		 * @param cursor The location of the model's cursor
		 */
		SelectionChangeEventImpl(SimpleDocumentModel model, int anchor, int cursor) {
			theModel = model;
			theSelectionAnchor = anchor;
			theCursor = cursor;
		}

		/** @return The document model whose selection changed */
		@Override
		public SimpleDocumentModel getModel() {
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
	}

	private static class ContentAndSelectionChangeEventImpl extends ContentChangeEventImpl implements SelectionChangeEvent {
		private final int theAnchor;

		private final int theCursor;

		/**
		 * @param model The document model whose content changed
		 * @param value The document model's content after the change
		 * @param change The section of content that was added or removed
		 * @param index The index of the addition or removal
		 * @param remove Whether this change represents a removal or an addition
		 */
		ContentAndSelectionChangeEventImpl(SimpleDocumentModel model, String value, String change, int index, boolean remove, int cursor, int anchor) {
			super(model, value, change, index, remove);
			theCursor=cursor;
			theAnchor=anchor;
		}

		@Override
		public int getSelectionAnchor() {
			return theAnchor;
		}

		@Override
		public int getCursor() {
			return theCursor;
		}
	}

	private class SelectionStyle extends org.muis.core.style.stateful.AbstractInternallyStatefulStyle {
		SelectionStyle(InternallyStatefulStyle parent, final boolean selected) {
			addDependency(parent);
			// TODO Not 100% sure I need this listener--maybe the dependency handles it automatically but I don't think so
			parent.addStateChangeListener(new org.muis.core.style.stateful.StateChangeListener() {
				@Override
				public void stateChanged(StateChangedEvent evt) {
					setState(selected ? prisms.util.ArrayUtils.add(evt.getNewState(), TEXT_SELECTION) : evt.getNewState());
				}
			});
			setState(selected ? prisms.util.ArrayUtils.add(parent.getState(), TEXT_SELECTION) : parent.getState());
		}
	}
}
