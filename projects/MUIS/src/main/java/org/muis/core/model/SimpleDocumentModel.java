package org.muis.core.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;

import org.muis.core.style.MuisStyle;
import org.muis.core.style.stateful.StatefulStyle;

/** A very simple document model that uses a single style and keeps a single, mutable set of content and supports single interval selection */
public class SimpleDocumentModel extends AbstractMuisDocumentModel implements Appendable {
	private final StatefulStyle theParentStyle;
	private final MuisStyle theNormalStyle;
	private final MuisStyle theSelectedStyle;

	private final StringBuilder theContent;
	private int theCursor;
	private int theSelectionAnchor;

	private java.util.concurrent.locks.ReentrantReadWriteLock theLock;

	/** @param parentStyle The parent style for this document */
	public SimpleDocumentModel(final StatefulStyle parentStyle) {
		theParentStyle = parentStyle;
		theNormalStyle = new SelectionStyle(parentStyle, false);
		theSelectedStyle = new SelectionStyle(parentStyle, true);
		theContent = new StringBuilder();
		theLock = new java.util.concurrent.locks.ReentrantReadWriteLock();
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

	/**
	 * @return The location of the cursor in this document--the location at which text will be added in response to non-positioned events
	 *         (e.g. character input). If text is selected in this document then this position is also one end of the selection interval.
	 */
	public int getCursor() {
		return theCursor;
	}

	/**
	 * @return The other end of the selection interval (opposite cursor). If no text is selected in this document then this value will be the
	 *         same as the {@link #getCursor() cursor}
	 */
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
	}

	/**
	 * Changes the selection interval (and with it, the cursor) in this document
	 *
	 * @param anchor The new anchor for the selection in this document
	 * @param cursor The new location for the cursor in this document
	 */
	public void setSelection(int anchor, int cursor) {
		theSelectionAnchor = anchor;
		theCursor = cursor;
	}

	@Override
	public Iterator<StyledSequence> iterator() {
		final String content = toString();
		int anchor=theSelectionAnchor;
		int cursor=theCursor;
		ArrayList<StyledSequence> ret=new ArrayList<>();
		int div1=anchor;
		int div2=cursor;
		if(div1>div2){
			int temp=div1;
			div1=div2;
			div2=temp;
		}
		if(div1==div2)
			ret.add(new SimpleStyledSequence(content, theNormalStyle));
		else{
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
	public String toString(){
		Lock lock = theLock.readLock();
		lock.lock();
		try {
			return theContent.toString();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public SimpleDocumentModel append(CharSequence csq) {
		Lock lock = theLock.writeLock();
		lock.lock();
		try {
			if(theSelectionAnchor == theContent.length())
				theSelectionAnchor += csq.length();
			if(theCursor == theContent.length())
				theCursor += csq.length();
			theContent.append(csq);
		} finally {
			lock.unlock();
		}
		return this;
	}

	@Override
	public SimpleDocumentModel append(CharSequence csq, int start, int end) {
		Lock lock = theLock.writeLock();
		lock.lock();
		try {
			if(theSelectionAnchor == theContent.length())
				theSelectionAnchor += end - start;
			if(theCursor == theContent.length())
				theCursor += end - start;
			theContent.append(csq, start, end);
		} finally {
			lock.unlock();
		}
		return this;
	}

	@Override
	public SimpleDocumentModel append(char c) {
		Lock lock = theLock.writeLock();
		lock.lock();
		try {
			if(theSelectionAnchor == theContent.length())
				theSelectionAnchor++;
			if(theCursor == theContent.length())
				theCursor++;
			theContent.append(c);
		} finally {
			lock.unlock();
		}
		return this;
	}

	/**
	 * Inserts a character sequence at this model's cursor
	 *
	 * @param csq The character sequence to insert
	 * @return This model, for chaining
	 */
	public SimpleDocumentModel insert(CharSequence csq) {
		Lock lock = theLock.writeLock();
		lock.lock();
		try {
			if(theSelectionAnchor >= theCursor)
				theSelectionAnchor += csq.length();
			theContent.insert(theCursor, csq);
			theCursor += csq.length();
		} finally {
			lock.unlock();
		}
		return this;
	}

	/**
	 * Inserts a character at this model's cursor
	 *
	 * @param c The character to insert
	 * @return This model, for chaining
	 */
	public SimpleDocumentModel insert(char c) {
		Lock lock = theLock.writeLock();
		lock.lock();
		try {
			if(theSelectionAnchor >= theCursor)
				theSelectionAnchor++;
			theContent.insert(theCursor, c);
			theCursor++;
		} finally {
			lock.unlock();
		}
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
		Lock lock = theLock.writeLock();
		lock.lock();
		try {
			if(theSelectionAnchor >= offset)
				theSelectionAnchor += csq.length();
			if(theCursor >= offset)
				theCursor += csq.length();
			theContent.insert(offset, csq);
		} finally {
			lock.unlock();
		}
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
		Lock lock = theLock.writeLock();
		lock.lock();
		try {
			if(theSelectionAnchor >= offset)
				theSelectionAnchor++;
			if(theCursor >= offset)
				theCursor++;
			theContent.insert(offset, c);
		} finally {
			lock.unlock();
		}
		return this;
	}

	/**
	 * Deletes characters from this document
	 *
	 * @param start The index of the start of the sequence to remove
	 * @param end The index of the end of the sequence to remove
	 * @return This model, for chaining
	 */
	public SimpleDocumentModel delete(int start, int end) {
		Lock lock = theLock.writeLock();
		lock.lock();
		try {
			if(theSelectionAnchor >= start) {
				if(theSelectionAnchor >= end)
					theSelectionAnchor -= end - start;
				else
					theSelectionAnchor = start;
			}
			if(theCursor >= start) {
				if(theCursor >= end)
					theCursor -= end - start;
				else
					theCursor = start;
			}
			theContent.delete(start, end);
		} finally {
			lock.unlock();
		}
		return this;
	}

	/**
	 * Sets the content for this model
	 *
	 * @param text The text to set
	 * @return This model, for chaining
	 */
	public SimpleDocumentModel setText(String text) {
		Lock lock = theLock.writeLock();
		lock.lock();
		try {
			theSelectionAnchor = text.length();
			theCursor = text.length();
			int oldLen = theContent.length();
			theContent.setLength(0);
			theContent.append(text);
			if(oldLen - text.length() > 100 && oldLen - text.length() > text.length() / 2)
				theContent.trimToSize();
		} finally {
			lock.unlock();
		}
		return this;
	}

	private class SelectionStyle extends org.muis.core.style.stateful.AbstractInternallyStatefulStyle {
		SelectionStyle(StatefulStyle parent, boolean selected) {
			addDependency(parent);
			if(selected)
				addState(org.muis.core.MuisConstants.States.TEXT_SELECTION);
		}
	}
}
