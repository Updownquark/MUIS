package org.muis.base.model;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.muis.core.model.MutableDocumentModel;
import org.muis.core.style.MuisStyle;
import org.muis.core.style.StyleAttribute;
import org.muis.core.style.StyleListener;

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

	private java.util.List<RichStyleSequence> theSequences;

	private ReentrantReadWriteLock theLock;

	public RichDocumentModel(MuisStyle parentStyle) {
		theParentStyle = parentStyle;
		theSequences = new java.util.ArrayList<>();
		theLock = new ReentrantReadWriteLock();
	}

	@Override
	public MuisStyle getStyleAt(int position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<StyledSequence> iterateFrom(int position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<StyledSequenceMetric> metrics(int start, float breakWidth) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public float getPositionAt(float x, float y, int breakWidth) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Point2D getLocationAt(float position, int breakWidth) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void draw(Graphics2D graphics, Rectangle window, int breakWidth) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addContentListener(ContentListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeContentListener(ContentListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public int length() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public char charAt(int index) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<StyledSequence> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MutableDocumentModel append(CharSequence csq) {
		// TODO Auto-generated method stub
		return null;
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

}
