package org.quick.core.model;

import java.util.Iterator;

import org.qommons.IterableUtils;
import org.qommons.Transaction;
import org.quick.core.QuickElement;

/** A very simple document model that uses a single style and keeps a single, mutable set of content and supports single interval selection */
public class SimpleDocumentModel extends AbstractSelectableDocumentModel implements MutableSelectableDocumentModel {
	private final StringBuilder theContent;

	/**
	 * @param text The initial text for this field
	 */
	public SimpleDocumentModel(QuickElement element, String text) {
		this(element);
		theContent.append(text);
	}

	public SimpleDocumentModel(QuickElement element) {
		super(element);
		theContent = new StringBuilder();
	}

	@Override
	protected Iterator<StyledSequence> internalIterator() {
		return IterableUtils.iterator(new StyledSequence[] { new SimpleStyledSequence(theContent.toString(), getNormalStyle()) }, true);
	}

	/*@Override
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
	}*/

	@Override
	public int length() {
		try (Transaction t = holdForRead()) {
			return theContent.length();
		}
	}

	@Override
	public char charAt(int index) {
		try (Transaction t = holdForRead()) {
			return theContent.charAt(index);
		}
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		try (Transaction t = holdForRead()) {
			return theContent.subSequence(start, end);
		}
	}

	@Override
	public String toString() {
		try (Transaction t = holdForRead()) {
			return theContent.toString();
		}
	}

	@Override
	protected void internalAppend(CharSequence csq, int start, int end) {
		theContent.append(csq, start, end);
	}

	@Override
	protected void internalInsert(int offset, CharSequence csq) {
		theContent.insert(offset, csq);
	}

	@Override
	protected void internalDelete(int start, int end) {
		theContent.delete(start, end);
	}

	@Override
	protected void internalSetText(String text) {
		theContent.setLength(0);
		theContent.append(text);
	}

	// Publicize and override the return types for the modification methods

	@Override
	public Transaction holdForWrite(Object cause) {
		return super.holdForWrite(cause);
	}

	@Override
	public SimpleDocumentModel setCursor(int cursor) {
		return (SimpleDocumentModel) super.setCursor(cursor);
	}

	@Override
	public SimpleDocumentModel setSelection(int anchor, int cursor) {
		return (SimpleDocumentModel) super.setSelection(anchor, cursor);
	}

	@Override
	public SimpleDocumentModel append(CharSequence csq) {
		return (SimpleDocumentModel) super.append(csq);
	}

	@Override
	public SimpleDocumentModel append(char c) {
		return (SimpleDocumentModel) super.append(c);
	}

	@Override
	public SimpleDocumentModel append(CharSequence csq, int start, int end) {
		return (SimpleDocumentModel) super.append(csq, start, end);
	}

	@Override
	public SimpleDocumentModel insert(int offset, char c) {
		return (SimpleDocumentModel) super.insert(offset, c);
	}

	@Override
	public SimpleDocumentModel insert(int offset, CharSequence csq) {
		return (SimpleDocumentModel) super.insert(offset, csq);
	}

	@Override
	public SimpleDocumentModel delete(int start, int end) {
		return (SimpleDocumentModel) super.delete(start, end);
	}

	@Override
	public SimpleDocumentModel setText(String text) {
		return (SimpleDocumentModel) super.setText(text);
	}

	@Override
	public SimpleDocumentModel insert(CharSequence csq) {
		return (SimpleDocumentModel) super.insert(csq);
	}

	@Override
	public SimpleDocumentModel insert(char c) {
		return (SimpleDocumentModel) super.insert(c);
	}
}
