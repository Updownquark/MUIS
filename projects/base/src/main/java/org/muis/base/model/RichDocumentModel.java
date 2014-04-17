package org.muis.base.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.muis.core.model.MutableDocumentModel;
import org.muis.core.style.*;
import org.muis.util.Transaction;

import prisms.util.ArrayUtils;

/** A {@link MutableDocumentModel} that allows different styles for different sections of text */
public class RichDocumentModel extends org.muis.core.model.AbstractMuisDocumentModel implements MutableDocumentModel {
	private class RichStyleSequence implements StyledSequence, MuisStyle {
		private final StringBuilder theContent;

		private final java.util.HashMap<StyleAttribute<?>, Object> theStyles;

		RichStyleSequence() {
			theContent = new StringBuilder();
			theStyles = new java.util.HashMap<>();
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
		public MuisStyle getStyle() {
			return this;
		}

		@Override
		public Iterator<StyleAttribute<?>> iterator() {
			return ArrayUtils.iterator(localAttributes().iterator(), theParentStyle.iterator());
		}

		@Override
		public MuisStyle [] getDependencies() {
			return new MuisStyle[] {theParentStyle};
		}

		@Override
		public boolean isSet(StyleAttribute<?> attr) {
			Lock lock = theLock.readLock();
			lock.lock();
			try {
				return theStyles.containsKey(attr);
			} finally {
				lock.unlock();
			}
		}

		@Override
		public boolean isSetDeep(StyleAttribute<?> attr) {
			return isSet(attr) || theParentStyle.isSet(attr);
		}

		@Override
		public <T> T getLocal(StyleAttribute<T> attr) {
			Lock lock = theLock.readLock();
			lock.lock();
			try {
				return (T) theStyles.get(attr);
			} finally {
				lock.unlock();
			}
		}

		@Override
		public Iterable<StyleAttribute<?>> localAttributes() {
			Lock lock = theLock.readLock();
			lock.lock();
			try {
				return java.util.Collections.unmodifiableList(java.util.Arrays.asList((StyleAttribute<?> []) theStyles.keySet().toArray(
					new StyleAttribute[theStyles.size()])));
			} finally {
				lock.unlock();
			}
		}

		@Override
		public <T> T get(StyleAttribute<T> attr) {
			T ret = getLocal(attr);
			if(ret != null)
				return ret;
			return theParentStyle.get(attr);
		}

		@Override
		public void addListener(StyleListener listener) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void removeListener(StyleListener listener) {
			throw new UnsupportedOperationException();
		}
	}

	private MuisStyle theParentStyle;

	private List<RichStyleSequence> theSequences;

	private ReentrantReadWriteLock theLock;

	/** @param parentStyle The style that all this model's sequences should inherit from */
	public RichDocumentModel(MuisStyle parentStyle) {
		theParentStyle = parentStyle;
		theSequences = new ArrayList<>();
		theLock = new ReentrantReadWriteLock();
	}

	/** @return A transaction that prevents any other threads from modifying this document model until the transaction is closed */
	public Transaction holdForRead() {
		final Lock lock = theLock.readLock();
		return new Transaction() {
			@Override
			public void close() {
				lock.unlock();
			}

			@Override
			protected void finalize() throws Throwable {
				super.finalize();
				lock.unlock();
			}
		};
	}

	/** @return A transaction that prevents any other threads from modifying or accessing this document model until the transaction is closed */
	public Transaction holdForWrite() {
		if(theLock.getReadHoldCount() > 0)
			throw new IllegalStateException("A write lock cannot be acquired for this document model while a read lock is held."
				+ "  The read lock must be released before attempting to acquire a write lock.");
		final Lock lock = theLock.writeLock();
		return new Transaction() {
			@Override
			public void close() {
				lock.unlock();
			}

			@Override
			protected void finalize() throws Throwable {
				super.finalize();
				lock.unlock();
			}
		};
	}

	/**
	 * It should be noted that this iterator is not entirely thread safe by itself. It will never throw concurrency exceptions, but if this
	 * document is modified by other threads the content returned by the iterator's methods may not accurately reflect the current contents
	 * of the document. Use {@link #holdForRead()} in a try around the usage of the iterator to secure against this.
	 */
	@Override
	public Iterator<StyledSequence> iterator() {
		try (Transaction t = holdForRead()) {
			return java.util.Collections.unmodifiableList((List<StyledSequence>) (List<?>) new ArrayList<>(theSequences)).iterator();
		}
	}

	@Override
	public MutableDocumentModel append(CharSequence csq) {
		return append(csq, 0, csq.length());
	}

	@Override
	public MutableDocumentModel append(CharSequence csq, int start, int end) {
		try (Transaction t = holdForWrite()) {
			if(csq instanceof MuisStyle) {
				RichStyleSequence newSeq = new RichStyleSequence();
				for(StyleAttribute<?> att : (MuisStyle) csq) {
					newSeq.theStyles.put(att, ((MuisStyle) csq).get(att));
				}
				theSequences.add(newSeq);
			}
			theSequences.get(theSequences.size() - 1).theContent.append(csq, start, end);
		}
		return this;
	}

	@Override
	public MutableDocumentModel append(char c) {
		try (Transaction t = holdForWrite()) {
			theSequences.get(theSequences.size() - 1).theContent.append(c);
		}
		return this;
	}

	@Override
	public MutableDocumentModel delete(int start, int end) {
		try (Transaction t = holdForWrite()) {
			int pos = 0;
			Iterator<RichStyleSequence> seqs = theSequences.iterator();
			while(seqs.hasNext()) {
				RichStyleSequence seq = seqs.next();
				int nextPos = pos + seq.length();
				if(pos > end) {
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
		return this;
	}

	@Override
	public MutableDocumentModel setText(String text) {
		try (Transaction t = holdForWrite()) {
			while(theSequences.size() > 1)
				theSequences.remove(theSequences.size() - 1);
			theSequences.get(0).theContent.delete(0, theSequences.get(0).theContent.length());
			theSequences.get(0).theContent.append(text);
		}
		return this;
	}

	@Override
	public void addContentListener(ContentListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeContentListener(ContentListener listener) {
		// TODO Auto-generated method stub

	}

	/**
	 * <p>
	 * Creates a setter for styles on a subsequence in this model. The returned style does attempt to return intelligent values from the
	 * style getter methods, but complete consistency is impossible since the given subsequence may overlap sequences with differing styles.
	 * This method is really intended for setting styles in this model. For accessing style information, use the {@link #iterator()
	 * iterator}.
	 * </p>
	 * <p>
	 * The returned style will never contain "local" attributes. Attributes set in the style will be set in the underlying model and
	 * reflected through the style as deeply set attributes.
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

	private class RichSegmentStyle implements MutableStyle {
		private final int theStart;

		private final int theEnd;

		RichSegmentStyle(int start, int end) {
			theStart = start;
			theEnd = end;
		}

		@Override
		public MuisStyle [] getDependencies() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean isSet(StyleAttribute<?> attr) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isSetDeep(StyleAttribute<?> attr) {
			return isSet(attr);
		}

		@Override
		public <T> T getLocal(StyleAttribute<T> attr) {
			return null;
		}

		@Override
		public Iterable<StyleAttribute<?>> localAttributes() {
			return java.util.Collections.EMPTY_LIST;
		}

		@Override
		public <T> T get(StyleAttribute<T> attr) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void addListener(StyleListener listener) {
			// TODO Auto-generated method stub

		}

		@Override
		public void removeListener(StyleListener listener) {
			// TODO Auto-generated method stub

		}

		@Override
		public Iterator<StyleAttribute<?>> iterator() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T> MutableStyle set(StyleAttribute<T> attr, T value) throws IllegalArgumentException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public MutableStyle clear(StyleAttribute<?> attr) {
			// TODO Auto-generated method stub
			return null;
		}
	}
}
