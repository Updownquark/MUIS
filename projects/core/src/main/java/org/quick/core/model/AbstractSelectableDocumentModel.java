package org.quick.core.model;

import static org.quick.core.QuickConstants.States.TEXT_SELECTION;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.observe.Observable;
import org.observe.ObservableValue;
import org.observe.SimpleObservable;
import org.observe.collect.ObservableCollection;
import org.observe.collect.ObservableList;
import org.observe.collect.ObservableSet;
import org.qommons.IterableUtils;
import org.qommons.Transaction;
import org.quick.core.mgr.QuickMessageCenter;
import org.quick.core.mgr.QuickState;
import org.quick.core.style.QuickStyle;
import org.quick.core.style.StyleAttribute;
import org.quick.core.style.stateful.InternallyStatefulStyle;
import org.quick.core.style.stateful.StatefulStyle;

import com.google.common.reflect.TypeToken;

/** A base implementation of a selectable document model */
public abstract class AbstractSelectableDocumentModel extends AbstractQuickDocumentModel implements SelectableDocumentModel {
	private final InternallyStatefulStyle theParentStyle;

	private final QuickStyle theNormalStyle;
	private final QuickStyle theSelectedStyle;

	private int theSelectionAnchor;
	private int theCursor;

	private final SimpleObservable<ContentChangeEvent> theContentChanges;
	private final SimpleObservable<StyleChangeEvent> theStyleChanges;
	private final SimpleObservable<SelectionChangeEvent> theSelectionChanges;

	private final ReentrantReadWriteLock theLock;
	private Object theCause;

	/**
	 * @param msg The message center to log errors against styles in this model
	 * @param parentStyle The parent style to use for backup styles if this document's content is missing attributes directly
	 */
	public AbstractSelectableDocumentModel(QuickMessageCenter msg, InternallyStatefulStyle parentStyle) {
		theParentStyle = parentStyle;
		theNormalStyle = new org.quick.core.model.SimpleDocumentModel.SelectionStyle(msg, parentStyle, false);
		theSelectedStyle = new org.quick.core.model.SimpleDocumentModel.SelectionStyle(msg, parentStyle, true);
		theContentChanges = new SimpleObservable<>();
		theStyleChanges = new SimpleObservable<>();
		theSelectionChanges = new SimpleObservable<>();
		theLock = new ReentrantReadWriteLock();

		/* Clear the metrics/rendering cache when the style changes.  Otherwise, style changes won't cause the document to re-render
		 * correctly because the cache may have the old color/size/etc */
		theNormalStyle.allChanges().act(event -> {
			int minSel = theCursor;
			int maxSel = theCursor;
			if (theSelectionAnchor < minSel)
				minSel = theSelectionAnchor;
			if (theSelectionAnchor > maxSel)
				maxSel = theSelectionAnchor;
			if (minSel == 0 && maxSel == length()) {
				return; // No unselected text
			}
			clearCache();
			int evtStart = minSel == 0 ? maxSel : 0;
			int evtEnd = maxSel == length() ? minSel : length();
			fireStyleEvent(evtStart, evtEnd, event);
		});
		theSelectedStyle.allChanges().act(event -> {
			if (theCursor == theSelectionAnchor) {
				return; // No selected text
			}
			clearCache();
			int evtStart = theSelectionAnchor;
			int evtEnd = theCursor;
			if (evtStart > evtEnd) {
				int temp = evtStart;
				evtStart = evtEnd;
				evtEnd = temp;
			}
			fireStyleEvent(evtStart, evtEnd, event);
		});
	}

	@Override
	public Observable<QuickDocumentChangeEvent> changes() {
		return Observable.or(theContentChanges, theStyleChanges, theSelectionChanges);
	}

	/** @return This document's parent's style */
	public InternallyStatefulStyle getParentStyle() {
		return theParentStyle;
	}

	/** @return The style for text that is not selected */
	public QuickStyle getNormalStyle() {
		return theNormalStyle;
	}

	/** @return The style for text that is selected */
	public QuickStyle getSelectedStyle() {
		return theSelectedStyle;
	}

	@Override
	public Transaction holdForRead() {
		final java.util.concurrent.locks.Lock lock = theLock.readLock();
		lock.lock();
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
	 * @return A transaction that prevents any other threads from modifying or accessing this document model until the transaction is closed
	 * @see MutableDocumentModel#holdForWrite(Object)
	 */
	@Override
	public Transaction holdForWrite(Object cause) {
		if(theLock.getWriteHoldCount() == 0 && theLock.getReadHoldCount() > 0)
			throw new IllegalStateException("A write lock cannot be acquired for this document model while a read lock is held."
				+ "  The read lock must be released before attempting to acquire a write lock.");
		final java.util.concurrent.locks.Lock lock = theLock.writeLock();
		lock.lock();
		Object preCause = theCause;
		theCause = cause;
		return new Transaction() {
			private boolean isLocked = true;
			@Override
			public void close() {
				if (isLocked) {
					theCause = preCause;
					lock.unlock();
				}
			}

			@Override
			protected void finalize() throws Throwable {
				super.finalize();
				close();
			}
		};
	}

	/** @return The cause for the current write transaction, if any */
	protected Object getWriteLockCause() {
		return theCause;
	}

	@Override
	public Iterator<StyledSequence> iterator() {
		try (Transaction t = holdForRead()) {
			final int min = Math.min(theCursor, theSelectionAnchor);
			final int max = Math.max(theCursor, theSelectionAnchor);
			final Iterator<StyledSequence> internal = internalIterator();
			return new Iterator<StyledSequence>() {
				private int thePosition;

				private StyledSequence theCurrent;
				private org.qommons.IntList theDivisions = new org.qommons.IntList(true, true);
				private int theCurrentSubReturned;

				@Override
				public boolean hasNext() {
					return theCurrent != null || internal.hasNext();
				}

				@Override
				public StyledSequence next() {
					if(!hasNext())
						throw new java.util.NoSuchElementException();
					if(theCurrent == null) {
						theCurrent = internal.next();
						divideCurrent();
					}

					int start = theCurrentSubReturned == 0 ? 0 : theDivisions.get(theCurrentSubReturned - 1);
					int end = theCurrentSubReturned == theDivisions.size() ? theCurrent.length() : theDivisions.get(theCurrentSubReturned);
					boolean selected = start >= min && start < max;
					StyledSequence ret = wrap(theCurrent, selected, start, end);
					if(theCurrentSubReturned == theDivisions.size()) {
						theCurrent = null;
						theCurrentSubReturned = 0;
						theDivisions.clear();
					} else
						theCurrentSubReturned++;
					return ret;
				}

				private void divideCurrent() {
					theDivisions.clear();
					if(min != max) {
						if(min > thePosition && min < thePosition + theCurrent.length())
							theDivisions.add(min - thePosition);
						if(max > thePosition && max < thePosition + theCurrent.length())
							theDivisions.add(max - thePosition);
					}
					for(int i = 0; i < theCurrent.length() - 1; i++)
						if(theCurrent.charAt(i) == '\n')
							theDivisions.add(i + 1);// Include the line break in the sequence before it
				}

				private StyledSequence wrap(StyledSequence toWrap, boolean selected, int start, int end) {
					return new StyledSequenceWrapper(toWrap, selected ? theSelectedStyle : theNormalStyle, start, end);
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}
	}

	/** @return This document's selection-independent styled content */
	protected abstract Iterator<StyledSequence> internalIterator();

	@Override
	public int getCursor() {
		return theCursor;
	}

	@Override
	public int getSelectionAnchor() {
		return theSelectionAnchor;
	}

	@Override
	public AbstractSelectableDocumentModel setCursor(int cursor) {
		ArrayList<StyledSequence> before = new ArrayList<>();
		for(StyledSequence seq : this)
			before.add(seq);
		int oldAnchor = theSelectionAnchor;
		int oldCursor = theCursor;
		theCursor = cursor;
		theSelectionAnchor = cursor;
		ArrayList<StyledSequence> after = new ArrayList<>();
		for(StyledSequence seq : this)
			after.add(seq);
		fireSelectionEvent(oldAnchor, oldCursor, cursor, cursor, before, after, theCause);
		return this;
	}

	@Override
	public AbstractSelectableDocumentModel setSelection(int anchor, int cursor) {
		if(theSelectionAnchor == anchor && theCursor == cursor)
			return this;
		ArrayList<StyledSequence> before = new ArrayList<>();
		for(StyledSequence seq : this)
			before.add(seq);
		int oldAnchor = theSelectionAnchor;
		int oldCursor = theCursor;
		theSelectionAnchor = anchor;
		theCursor = cursor;
		ArrayList<StyledSequence> after = new ArrayList<>();
		for(StyledSequence seq : this)
			after.add(seq);
		fireSelectionEvent(oldAnchor, oldCursor, anchor, cursor, before, after, theCause);
		return this;
	}

	@Override
	public String getSelectedText() {
		StringBuilder ret = new StringBuilder();
		try (Transaction t = holdForRead()) {
			int min = theSelectionAnchor;
			int max = theCursor;
			if(min > max) {
				int temp = min;
				min = max;
				max = temp;
			}
			for(StyledSequence seq : iterateFrom(min, max)) {
				ret.append(seq);
			}
		}
		return ret.toString();
	}

	/**
	 * @param csq The sequence to append to this model
	 * @return This document, for chaining
	 * @see MutableDocumentModel#append(CharSequence)
	 */
	protected AbstractSelectableDocumentModel append(CharSequence csq) {
		return append(csq, 0, csq.length());
	}

	/**
	 * @param c The character to append to this model
	 * @return This document, for chaining
	 * @see MutableDocumentModel#append(char)
	 */
	protected AbstractSelectableDocumentModel append(char c) {
		return append(new String(new char[] { c }), 0, 1);
	}

	/**
	 * @param csq The sequence to append to this model
	 * @param start The start index of the subsequence to append
	 * @param end The end index of the subsequence to append
	 * @return This document, for chaining
	 * @see MutableDocumentModel#append(CharSequence, int, int)
	 */
	protected AbstractSelectableDocumentModel append(CharSequence csq, int start, int end) {
		String value;
		int index;
		String change = csq.toString();
		boolean selChange = false;
		try (Transaction t = holdForWrite(theCause)) {
			value = toString();
			index = value.length();
			if(theSelectionAnchor == index) {
				selChange = true;
				theSelectionAnchor += csq.length();
			}
			if(theCursor == index) {
				selChange = true;
				theCursor += csq.length();
			}

			internalAppend(csq, start, end);
		}
		if(selChange)
			fireContentEvent(value, change, index, index + end - start, false, theSelectionAnchor, theCursor, theCause);
		else
			fireContentEvent(value, change, index, index + end - start, false, -1, -1, theCause);

		return this;
	}

	/**
	 * Does the internal work to append the sequence to this document's content
	 *
	 * @param csq The sequence to append
	 * @param start The start index of the subsequence to append
	 * @param end The end index of the subsequence to append
	 */
	protected abstract void internalAppend(CharSequence csq, int start, int end);

	/**
	 * @param offset The index at which to insert the character
	 * @param c The character to insert into this model
	 * @return This document, for chaining
	 * @see MutableDocumentModel#insert(int, char)
	 */
	protected AbstractSelectableDocumentModel insert(int offset, char c) {
		return insert(offset, new String(new char[] { c }));
	}

	/**
	 * @param offset The index at which to insert the sequence
	 * @param csq The sequence to insert into this model
	 * @return This document, for chaining
	 * @see MutableDocumentModel#insert(int, CharSequence)
	 */
	protected AbstractSelectableDocumentModel insert(int offset, CharSequence csq) {
		String value;
		String change = csq.toString();
		boolean selChange = false;
		try (Transaction t = holdForWrite(theCause)) {
			value = toString();
			if(theSelectionAnchor >= offset) {
				selChange = true;
				theSelectionAnchor += csq.length();
			}
			if(theCursor >= offset) {
				selChange = true;
				theCursor += csq.length();
			}

			internalInsert(offset, csq);
		}
		if(selChange)
			fireContentEvent(value, change, offset, offset + change.length(), false, theSelectionAnchor, theCursor, theCause);
		else
			fireContentEvent(value, change, offset, offset + change.length(), false, -1, -1, theCause);

		return this;
	}

	/**
	 * Does the internal work to insert the sequence into this document's content
	 *
	 * @param csq The sequence to insert
	 * @param offset The index at which to insert the sequence
	 */
	protected abstract void internalInsert(int offset, CharSequence csq);

	/**
	 * @param start The start index of the subsequence to delete
	 * @param end The end index of the subsequence to delete
	 * @return This document, for chaining
	 * @see MutableDocumentModel#delete(int, int)
	 */
	protected AbstractSelectableDocumentModel delete(int start, int end) {
		if(start > end) {
			int temp = start;
			start = end;
			end = temp;
		}
		String value;
		String change;
		boolean selChange = false;
		try (Transaction t = holdForWrite(theCause)) {
			value = toString();
			change = value.substring(start, end);
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

			internalDelete(start, end);
		}
		if(selChange)
			fireContentEvent(value, change, start, end, true, theSelectionAnchor, theCursor, theCause);
		else
			fireContentEvent(value, change, start, end, true, -1, -1, theCause);

		return this;
	}

	/**
	 * Does the internal work to delete the sequence from this document's content
	 *
	 * @param start The start index of the subsequence to delete
	 * @param end The end index of the subsequence to delete
	 */
	protected abstract void internalDelete(int start, int end);

	/**
	 * @param text The new text for this document
	 * @return This document, for chaining
	 * @see MutableDocumentModel#setText(String)
	 */
	protected AbstractSelectableDocumentModel setText(String text) {
		String oldValue;
		try (Transaction t = holdForWrite(theCause)) {
			theSelectionAnchor = text.length();
			theCursor = text.length();
			oldValue = toString();

			internalSetText(text);
		}
		fireContentEvent("", oldValue, 0, oldValue.length(), true, -1, -1, theCause);
		fireContentEvent(text, text, 0, text.length(), false, theSelectionAnchor, theCursor, theCause);

		return this;
	}

	/**
	 * Does the internal work to change is document's content
	 *
	 * @param text The text to set for this document's content
	 */
	protected abstract void internalSetText(String text);

	/**
	 * @param csq The sequence to insert
	 * @return This document, for chaining
	 * @see MutableSelectableDocumentModel#insert(CharSequence)
	 */
	protected AbstractSelectableDocumentModel insert(CharSequence csq) {
		return insert(theCursor, csq);
	}

	/**
	 * @param c The character to insert
	 * @return This document, for chaining
	 * @see MutableSelectableDocumentModel#insert(char)
	 */
	protected AbstractSelectableDocumentModel insert(char c) {
		return insert(theCursor, c);
	}

	/**
	 * Fires a selection event representing an operation that affected this document's selection interval
	 *
	 * @param oldAnchor The selection anchor prior to the operation
	 * @param oldCursor The cursor prior to the operation
	 * @param newAnchor The selection anchor after the operation
	 * @param newCursor The cursor after the operation
	 * @param before This document's style sequences before the operation. May be null if this information is not available.
	 * @param after This document's style sequences after the operation. May be null if this information is not available.
	 * @param cause The event or thing that caused this event
	 */
	protected void fireSelectionEvent(int oldAnchor, int oldCursor, int newAnchor, int newCursor, java.util.List<StyledSequence> before,
		java.util.List<StyledSequence> after, Object cause) {
		clearCache();
		int oldMin = oldAnchor;
		int oldMax = oldCursor;
		if(oldMin > oldMax) {
			int temp = oldMin;
			oldMin = oldMax;
			oldMax = temp;
		}
		int newMin = newAnchor;
		int newMax = newCursor;
		if(newMin > newMax) {
			int temp = newMin;
			newMin = newMax;
			newMax = temp;
		}
		int start;
		int end;
		if(oldMin == oldMax) {
			start = newMin;
			end = newMax;
		} else if(newMin == newMax) {
			start = oldMin;
			end = oldMax;
		} else if(oldMax < newMin) {
			start = oldMin;
			end = newMax;
		} else if(newMax < oldMin) {
			start = newMin;
			end = oldMax;
		} else if(oldMin == newMin) {
			start = Math.min(oldMax, newMax);
			end = Math.max(oldMax, newMax);
		} else if(oldMax == newMax) {
			start = Math.min(oldMin, newMin);
			end = Math.max(oldMin, newMin);
		} else {
			start = Math.min(oldMin, newMin);
			end = Math.max(oldMax, newMax);
		}

		if(start < end) {
			StyleChangeEvent styleEvt = new StyleChangeEventImpl(this, start, end, before, after, cause);
			// System.out.println(styleEvt + ": " + oldAnchor + "->" + oldCursor + " to " + newAnchor + "->" + newCursor);
			theStyleChanges.onNext(styleEvt);
		}

		theSelectionChanges.onNext(new SelectionChangeEventImpl(this, newAnchor, newCursor, cause));
	}

	/**
	 * Fires an event representing an operation that affected the style on all or a portion of this document
	 *
	 * @param start The starting index of the subsequence affected
	 * @param end The ending index of the subsequence affected
	 * @param cause The event or thing that caused this event
	 */
	protected void fireStyleEvent(int start, int end, Object cause) {
		theStyleChanges.onNext(new StyleChangeEventImpl(this, start, end, null, null, cause));
	}

	/**
	 * Fires an event representing an operation that affected this document's content
	 *
	 * @param value The new value for this document's content
	 * @param change The content change
	 * @param startIndex The starting index at which the change occurred
	 * @param endIndex The end index of the change
	 * @param remove Whether the operation was a deletion
	 * @param anchor The selection anchor after the operation
	 * @param cursor The cursor after the operation
	 * @param cause The event or thing that caused this event
	 */
	protected void fireContentEvent(String value, String change, int startIndex, int endIndex, boolean remove, int anchor, int cursor,
		Object cause) {
		clearCache();
		ContentChangeEvent evt;
		if(anchor < 0 && cursor < 0)
			evt = new ContentChangeEventImpl(this, value, change, startIndex, endIndex, remove, cause);
		else
			evt = new ContentAndSelectionChangeEventImpl(this, value, change, startIndex, endIndex, remove, anchor, cursor, cause);
		theContentChanges.onNext(evt);
	}

	private static class ContentChangeEventImpl implements ContentChangeEvent {
		private final AbstractSelectableDocumentModel theModel;

		private final String theValue;

		private final String theChange;

		private final int theStartIndex;
		private final int theEndIndex;

		private final boolean isRemove;

		private final Object theCause;

		/**
		 * @param model The document model whose content changed
		 * @param value The document model's content after the change
		 * @param change The section of content that was added or removed
		 * @param startIndex The start index of the addition or removal
		 * @param endIndex The end index of the text removed, or the end index of the added text after being added
		 * @param remove Whether this change represents a removal or an addition
		 */
		ContentChangeEventImpl(AbstractSelectableDocumentModel model, String value, String change, int startIndex, int endIndex,
			boolean remove, Object cause) {
			theModel = model;
			theValue = value;
			theChange = change;
			theStartIndex = startIndex;
			theEndIndex = endIndex;
			isRemove = remove;
			theCause = cause;
		}

		@Override
		public AbstractSelectableDocumentModel getModel() {
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
		public int getStartIndex() {
			return theStartIndex;
		}

		@Override
		public int getEndIndex() {
			return theEndIndex;
		}

		@Override
		public boolean isRemove() {
			return isRemove;
		}

		@Override
		public Object getCause() {
			return theCause;
		}

		@Override
		public String toString() {
			return theChange + " chars " + (isRemove ? "removed" : "added") + " at index " + theStartIndex;
		}
	}

	private static class StyleChangeEventImpl implements StyleChangeEvent {
		private final AbstractSelectableDocumentModel theDocument;

		private final int theStart;

		private final int theEnd;

		private final Iterable<StyledSequence> theBeforeStyles;

		private final Iterable<StyledSequence> theAfterStyles;

		private final Object theCause;

		StyleChangeEventImpl(AbstractSelectableDocumentModel document, int start, int end, Iterable<StyledSequence> beforeStyles,
			Iterable<StyledSequence> afterStyles, Object cause) {
			theDocument = document;
			theStart = start;
			theEnd = end;
			theBeforeStyles = beforeStyles == null ? null : IterableUtils.immutableIterable(beforeStyles);
			theAfterStyles = afterStyles == null ? null : IterableUtils.immutableIterable(afterStyles);
			theCause = cause;
		}

		@Override
		public AbstractSelectableDocumentModel getModel() {
			return theDocument;
		}

		@Override
		public int getStartIndex() {
			return theStart;
		}

		@Override
		public int getEndIndex() {
			return theEnd;
		}

		@Override
		public Iterable<StyledSequence> styleBefore() {
			return theBeforeStyles;
		}

		@Override
		public Iterable<StyledSequence> styleAfter() {
			return theAfterStyles;
		}

		@Override
		public Object getCause() {
			return theCause;
		}

		@Override
		public String toString() {
			return "Style change from " + theStart + " to " + theEnd;
		}
	}

	private static class SelectionChangeEventImpl implements SelectionChangeEvent {
		private final AbstractSelectableDocumentModel theModel;

		private final int theSelectionAnchor;

		private final int theCursor;

		private final Object theCause;

		/**
		 * @param model The document model whose selection changed
		 * @param anchor The location of the model's selection anchor
		 * @param cursor The location of the model's cursor
		 */
		SelectionChangeEventImpl(AbstractSelectableDocumentModel model, int anchor, int cursor, Object cause) {
			theModel = model;
			theSelectionAnchor = anchor;
			theCursor = cursor;
			theCause = cause;
		}

		/** @return The document model whose selection changed */
		@Override
		public AbstractSelectableDocumentModel getModel() {
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

		@Override
		public Object getCause() {
			return theCause;
		}

		@Override
		public String toString() {
			return "Selection=" + (theCursor == theSelectionAnchor ? "" + theCursor : theSelectionAnchor + "->" + theCursor);
		}
	}

	private static class ContentAndSelectionChangeEventImpl extends ContentChangeEventImpl implements SelectionChangeEvent {
		private final int theAnchor;

		private final int theCursor;

		/**
		 * @param model The document model whose content changed
		 * @param value The document model's content after the change
		 * @param change The section of content that was added or removed
		 * @param startIndex The index of the addition or removal
		 * @param remove Whether this change represents a removal or an addition
		 * @param cause The cause of this event
		 */
		ContentAndSelectionChangeEventImpl(AbstractSelectableDocumentModel model, String value, String change, int startIndex, int endIndex,
			boolean remove, int cursor, int anchor, Object cause) {
			super(model, value, change, startIndex, endIndex, remove, cause);
			theCursor = cursor;
			theAnchor = anchor;
		}

		@Override
		public int getSelectionAnchor() {
			return theAnchor;
		}

		@Override
		public int getCursor() {
			return theCursor;
		}

		@Override
		public String toString() {
			return super.toString() + " and Selection=" + (theCursor == theAnchor ? "" + theCursor : theAnchor + "->" + theCursor);
		}
	}

	/** A selected or deselected style for a document */
	public static class SelectionStyle extends org.quick.core.style.stateful.AbstractInternallyStatefulStyle {
		/**
		 * @param msg The message center to log errors against style values
		 * @param parent The parent style to mutate
		 * @param selected Whether this is to be the selected or deselected style
		 */
		public SelectionStyle(QuickMessageCenter msg, InternallyStatefulStyle parent, final boolean selected) {
			super(msg,
				ObservableList
					.constant(TypeToken.of(StatefulStyle.class),
						(StatefulStyle) parent),
				selected ? ObservableSet.unique(ObservableCollection.flattenCollections(parent.getState(),
					ObservableSet.constant(TypeToken.of(QuickState.class), TEXT_SELECTION)), Object::equals) : parent.getState());
		}
	}

	private static class StyledSequenceWrapper implements StyledSequence {
		private final StyledSequence theWrapped;
		private final QuickStyle theBackup;
		private final int theStart;
		private final int theEnd;
		private final QuickStyle theStyle;
		private final ObservableList<QuickStyle> theDependencies;

		StyledSequenceWrapper(StyledSequence toWrap, QuickStyle backup, int start, int end) {
			theWrapped = toWrap;
			theBackup = backup;
			theStart = start;
			theEnd = end;
			if(start < 0 || start > toWrap.length())
				throw new IllegalArgumentException(theWrapped + " (" + theWrapped.length() + "): " + start + " to " + end);
			if(end < start || end > toWrap.length())
				throw new IllegalArgumentException(theWrapped + " (" + theWrapped.length() + "): " + start + " to " + end);
			theDependencies = ObservableList.flatten(ObservableList.constant(new TypeToken<ObservableList<QuickStyle>>() {},
				theWrapped.getStyle().getDependencies(), ObservableList.constant(TypeToken.of(QuickStyle.class), theBackup)));

			theStyle = new QuickStyle() {
				@Override
				public ObservableList<QuickStyle> getDependencies() {
					return theDependencies;
				}

				@Override
				public boolean isSet(StyleAttribute<?> attr) {
					return theWrapped.getStyle() != null && theWrapped.getStyle().isSet(attr);
				}

				@Override
				public boolean isSetDeep(StyleAttribute<?> attr) {
					return (theWrapped.getStyle() != null && theWrapped.getStyle().isSetDeep(attr)) || theBackup.isSetDeep(attr);
				}

				@Override
				public <T> ObservableValue<T> getLocal(StyleAttribute<T> attr) {
					return theWrapped.getStyle().getLocal(attr);
				}

				@Override
				public ObservableSet<StyleAttribute<?>> localAttributes() {
					return theWrapped.getStyle().localAttributes();
				}
			};
		}

		@Override
		public int length() {
			return theEnd - theStart;
		}

		@Override
		public char charAt(int index) {
			return theWrapped.charAt(theStart + index);
		}

		@Override
		public CharSequence subSequence(int start, int end) {
			return new StyledSequenceWrapper(theWrapped, theBackup, theStart + start, theStart + end);
		}

		@Override
		public String toString() {
			return theWrapped.toString().substring(theStart, theEnd);
		}

		@Override
		public QuickStyle getStyle() {
			return theStyle;
		}
	}
}
