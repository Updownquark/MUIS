package org.muis.base.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.muis.core.model.MutableDocumentModel;
import org.muis.core.model.MutableSelectableDocumentModel;
import org.muis.core.style.MuisStyle;
import org.muis.core.style.MutableStyle;
import org.muis.core.style.StyleAttribute;
import org.muis.core.style.stateful.InternallyStatefulStyle;
import org.muis.util.Transaction;

import prisms.util.ArrayUtils;

/** A {@link MutableDocumentModel} that allows different styles for different sections of text */
public class RichDocumentModel extends org.muis.core.model.AbstractSelectableDocumentModel implements MutableSelectableDocumentModel {
	private class RichStyleSequence implements StyledSequence, MuisStyle {
		private final StringBuilder theContent;

		private final java.util.HashMap<StyleAttribute<?>, Object> theStyles;

		RichStyleSequence() {
			theContent = new StringBuilder();
			theStyles = new java.util.HashMap<>();
		}

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
		public MuisStyle getStyle() {
			return this;
		}

		@Override
		public Iterator<StyleAttribute<?>> iterator() {
			return ArrayUtils.iterator(localAttributes().iterator(), getParentStyle().iterator());
		}

		@Override
		public MuisStyle [] getDependencies() {
			return new MuisStyle[] {getParentStyle()};
		}

		@Override
		public boolean isSet(StyleAttribute<?> attr) {
			try (Transaction t = holdForRead()) {
				return theStyles.containsKey(attr);
			}
		}

		@Override
		public boolean isSetDeep(StyleAttribute<?> attr) {
			return isSet(attr) || getParentStyle().isSet(attr);
		}

		@Override
		public <T> T getLocal(StyleAttribute<T> attr) {
			try (Transaction t = holdForRead()) {
				return (T) theStyles.get(attr);
			}
		}

		@Override
		public Iterable<StyleAttribute<?>> localAttributes() {
			try (Transaction t = holdForRead()) {
				return java.util.Collections.unmodifiableList(java.util.Arrays.asList((StyleAttribute<?> []) theStyles.keySet().toArray(
					new StyleAttribute[theStyles.size()])));
			}
		}

		@Override
		public <T> T get(StyleAttribute<T> attr) {
			T ret = getLocal(attr);
			if(ret != null)
				return ret;
			return getParentStyle().get(attr);
		}

		@Override
		public void addListener(org.muis.core.style.StyleListener listener) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void removeListener(org.muis.core.style.StyleListener listener) {
			throw new UnsupportedOperationException();
		}
	}

	private List<RichStyleSequence> theSequences;

	/** @param parentStyle The style that all this model's sequences should inherit from */
	public RichDocumentModel(InternallyStatefulStyle parentStyle) {
		super(parentStyle);
		theSequences = new ArrayList<>();
	}

	/**
	 * It should be noted that this iterator is not entirely thread safe by itself. It will never throw concurrency exceptions, but if this
	 * document is modified by other threads the content returned by the iterator's methods may not accurately reflect the current contents
	 * of the document. Use {@link #holdForRead()} in a try around the usage of the iterator to secure against this.
	 */
	@Override
	protected Iterator<StyledSequence> internalIterator() {
		try (Transaction t = holdForRead()) {
			return java.util.Collections.unmodifiableList((List<StyledSequence>) (List<?>) new ArrayList<>(theSequences)).iterator();
		}
	}

	@Override
	protected void internalAppend(CharSequence csq, int start, int end) {
		if(csq instanceof MuisStyle) {
			RichStyleSequence newSeq = new RichStyleSequence();
			for(StyleAttribute<?> att : (MuisStyle) csq) {
				newSeq.theStyles.put(att, ((MuisStyle) csq).get(att));
			}
			theSequences.add(newSeq);
		}
		theSequences.get(theSequences.size() - 1).theContent.append(csq, start, end);
	}

	@Override
	protected void internalInsert(int offset, CharSequence csq) {
		int pos = 0;
		boolean inserted = false;
		for(RichStyleSequence seq : theSequences) {
			int nextPos = pos + seq.length();
			if(nextPos > offset) {
				inserted = true;
				seq.theContent.insert(offset - pos, csq);
				break;
			}
			pos = nextPos;
		}
		if(!inserted) {
			if(pos == length())
				append(csq);
			else
				throw new IndexOutOfBoundsException(offset + ">" + length());
		}
	}

	@Override
	protected void internalDelete(int start, int end) {
		int pos = 0;
		Iterator<RichStyleSequence> seqs = theSequences.iterator();
		while(seqs.hasNext()) {
			RichStyleSequence seq = seqs.next();
			int nextPos = pos + seq.length();
			if(pos >= end) {
				break;
			} else if(pos >= start) {
				if(nextPos <= end)
					seqs.remove();
				else
					seq.theContent.delete(0, end - pos);
			} else if(nextPos > start) {
				seq.theContent.delete(start - pos, seq.theContent.length());
			}
			pos = nextPos;
		}
	}

	@Override
	protected void internalSetText(String text) {
		while(theSequences.size() > 1)
			theSequences.remove(theSequences.size() - 1);
		theSequences.get(0).theContent.delete(0, theSequences.get(0).theContent.length());
		theSequences.get(0).theContent.append(text);
	}

	/**
	 * <p>
	 * Creates a setter for styles on a subsequence in this model. The returned style does attempt to return intelligent values from the
	 * style getter methods, but complete consistency is impossible since the given subsequence may overlap sequences with differing styles.
	 * This method is really intended for setting styles in this model. For accessing style information, use the {@link #iterator()
	 * iterator}.
	 * </p>
	 * <p>
	 * The returned style will reflect its sequences directly--"local" values from the sequences will become the style's loal attributes.
	 * The returned style will not support listeners.
	 * </p>
	 * <p>
	 * Note that using the setter methods of the returned style may give inconsistent or unexpected results or exceptions if this document
	 * is modified by another thread while the returned style is still in use. Use {@link #holdForWrite()} to prevent this.
	 * </p>
	 * 
	 * @param start The start of the sequence to the style-setter for
	 * @param end The end of the sequence to get the style-setter for
	 * @return A style that represents and allows setting of styles for the given subsequence in this document.
	 */
	public MutableStyle getSegmentStyle(int start, int end) {
		return new RichSegmentStyle(start, end);
	}

	@Override
	public RichDocumentModel setCursor(int cursor) {
		return (RichDocumentModel) super.setCursor(cursor);
	}

	@Override
	public RichDocumentModel setSelection(int anchor, int cursor) {
		return (RichDocumentModel) super.setSelection(anchor, cursor);
	}

	@Override
	public RichDocumentModel append(CharSequence csq) {
		return (RichDocumentModel) super.append(csq);
	}

	@Override
	public RichDocumentModel append(char c) {
		return (RichDocumentModel) super.append(c);
	}

	@Override
	public RichDocumentModel append(CharSequence csq, int start, int end) {
		return (RichDocumentModel) super.append(csq, start, end);
	}

	@Override
	public RichDocumentModel insert(int offset, char c) {
		return (RichDocumentModel) super.insert(offset, c);
	}

	@Override
	public RichDocumentModel insert(int offset, CharSequence csq) {
		return (RichDocumentModel) super.insert(offset, csq);
	}

	@Override
	public RichDocumentModel delete(int start, int end) {
		return (RichDocumentModel) super.delete(start, end);
	}

	@Override
	public RichDocumentModel setText(String text) {
		return (RichDocumentModel) super.setText(text);
	}

	@Override
	public RichDocumentModel insert(CharSequence csq) {
		return (RichDocumentModel) super.insert(csq);
	}

	@Override
	public RichDocumentModel insert(char c) {
		return (RichDocumentModel) super.insert(c);
	}

	private class RichSegmentStyle implements MutableStyle {
		private final int theStart;

		private final int theEnd;

		RichSegmentStyle(int start, int end) {
			theStart = start;
			theEnd = end;
		}

		@Override
		public MuisStyle [] getDependencies() {
			ArrayList<MuisStyle> ret = new ArrayList<>();
			try (Transaction t = holdForRead()) {
				for(StyledSequence seq : iterateFrom(theStart, theEnd))
					ret.add(seq.getStyle());
			}
			return ret.toArray(new MuisStyle[ret.size()]);
		}

		@Override
		public boolean isSet(StyleAttribute<?> attr) {
			try (Transaction t = holdForRead()) {
				for(StyledSequence seq : iterateFrom(theStart, theEnd))
					if(seq.getStyle().isSet(attr))
						return true;
			}
			return false;
		}

		@Override
		public boolean isSetDeep(StyleAttribute<?> attr) {
			return isSet(attr);
		}

		@Override
		public <T> T getLocal(StyleAttribute<T> attr) {
			try (Transaction t = holdForRead()) {
				for(StyledSequence seq : iterateFrom(theStart, theEnd))
					if(seq.getStyle().isSet(attr))
						return seq.getStyle().get(attr);
			}
			return null;
		}

		@Override
		public Iterable<StyleAttribute<?>> localAttributes() {
			ArrayList<Iterable<StyleAttribute<?>>> ret = new ArrayList<>();
			try (Transaction t = holdForRead()) {
				for(StyledSequence seq : iterateFrom(theStart, theEnd))
					ret.add(seq.getStyle().localAttributes());
			}
			return ArrayUtils.iterable(ret.toArray(new Iterable[ret.size()]));
		}

		@Override
		public <T> T get(StyleAttribute<T> attr) {
			StyledSequence first = null;
			try (Transaction t = holdForRead()) {
				for(StyledSequence seq : iterateFrom(theStart, theEnd)) {
					if(first == null)
						first = seq;
					if(seq.getStyle().isSet(attr))
						return seq.getStyle().get(attr);
				}
			}
			return first == null ? attr.getDefault() : first.getStyle().get(attr);
		}

		@Override
		public void addListener(org.muis.core.style.StyleListener listener) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void removeListener(org.muis.core.style.StyleListener listener) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Iterator<StyleAttribute<?>> iterator() {
			ArrayList<Iterator<StyleAttribute<?>>> ret = new ArrayList<>();
			try (Transaction t = holdForRead()) {
				for(StyledSequence seq : iterateFrom(theStart, theEnd))
					ret.add(seq.getStyle().iterator());
			}
			return ArrayUtils.iterator(ret.toArray(new Iterator[ret.size()]));
		}

		@Override
		public <T> MutableStyle set(StyleAttribute<T> attr, T value) throws IllegalArgumentException {
			int pos = 0;
			try (Transaction t = holdForWrite()) {
				for(int i = 0; i < theSequences.size(); i++) {
					if(pos >= theEnd)
						break;
					RichStyleSequence seq = theSequences.get(i);
					int nextPos = pos + seq.length();
					if(nextPos < theStart) {
					} else if(value.equals(seq.theStyles.get(attr))) {
					} else if(pos >= theStart && nextPos <= theEnd) {
						seq.theStyles.put(attr, value);
					} else if(pos >= theStart) {
						RichStyleSequence newSeq = new RichStyleSequence();
						newSeq.theContent.append(seq.theContent.substring(pos - theStart));
						seq.theContent.delete(pos - theStart, seq.theContent.length() - theStart);
						newSeq.theStyles.putAll(seq.theStyles);
						newSeq.theStyles.put(attr, value);
						theSequences.add(i + 1, newSeq);
					} else if(nextPos <= theEnd) {
						RichStyleSequence newSeq = new RichStyleSequence();
						newSeq.theContent.append(seq.theContent.substring(0, theEnd - pos));
						seq.theContent.delete(0, theEnd - pos);
						newSeq.theStyles.putAll(seq.theStyles);
						newSeq.theStyles.put(attr, value);
						theSequences.add(i, newSeq);
					}
					pos = nextPos;
				}
			}
			return this;
		}

		@Override
		public MutableStyle clear(StyleAttribute<?> attr) {
			int pos = 0;
			try (Transaction t = holdForWrite()) {
				for(int i = 0; i < theSequences.size(); i++) {
					if(pos >= theEnd)
						break;
					RichStyleSequence seq = theSequences.get(i);
					int nextPos = pos + seq.length();
					if(nextPos < theStart) {
					} else if(seq.theStyles.get(attr) == null) {
					} else if(pos >= theStart && nextPos <= theEnd) {
						seq.theStyles.remove(attr);
					} else if(pos >= theStart) {
						RichStyleSequence newSeq = new RichStyleSequence();
						newSeq.theContent.append(seq.theContent.substring(pos - theStart));
						seq.theContent.delete(pos - theStart, seq.theContent.length() - theStart);
						newSeq.theStyles.putAll(seq.theStyles);
						newSeq.theStyles.remove(attr);
						theSequences.add(i + 1, newSeq);
					} else if(nextPos <= theEnd) {
						RichStyleSequence newSeq = new RichStyleSequence();
						newSeq.theContent.append(seq.theContent.substring(0, theEnd - pos));
						seq.theContent.delete(0, theEnd - pos);
						newSeq.theStyles.putAll(seq.theStyles);
						newSeq.theStyles.remove(attr);
						theSequences.add(i, newSeq);
					}
					pos = nextPos;
				}
			}
			return this;
		}
	}
}
