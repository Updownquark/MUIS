package org.muis.base.model;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.muis.core.model.MutableDocumentModel;
import org.muis.core.style.*;

import prisms.util.ArrayUtils;

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

	public RichDocumentModel(MuisStyle parentStyle) {
		theParentStyle = parentStyle;
		theSequences = new java.util.ArrayList<>();
		theLock = new ReentrantReadWriteLock();
	}

	@Override
	public Iterator<StyledSequence> iterator() {
		return java.util.Collections.unmodifiableList((List<StyledSequence>)(List<?>)theSequences).iterator();
	}

	@Override
	public MutableDocumentModel append(CharSequence csq) {
		Lock lock=theLock.writeLock();
		lock.lock();
		try{
			theSequences.get(theSequences.size()-1).
		} finally{
			lock.unlock();
		}
		return this;
	}

	@Override
	public MutableDocumentModel append(CharSequence csq, int start, int end) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MutableDocumentModel append(char c) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MutableDocumentModel delete(int start, int end) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MutableDocumentModel setText(String text) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addContentListener(ContentListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeContentListener(ContentListener listener) {
		// TODO Auto-generated method stub

	}

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
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public <T> T getLocal(StyleAttribute<T> attr) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Iterable<StyleAttribute<?>> localAttributes() {
			// TODO Auto-generated method stub
			return null;
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
