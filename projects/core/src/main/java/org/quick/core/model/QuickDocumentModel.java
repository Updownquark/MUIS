package org.quick.core.model;

import java.util.Collections;
import java.util.Iterator;

import org.observe.Observable;
import org.observe.ObservableValue;
import org.observe.Observer;
import org.observe.Subscription;
import org.qommons.Causable;
import org.qommons.IterableUtils;
import org.qommons.Transaction;
import org.quick.core.model.QuickDocumentModel.StyledSequence;
import org.quick.core.style.QuickStyle;

/** Stores and displays text in Quick */
public interface QuickDocumentModel extends CharSequence, Iterable<StyledSequence> {
	/**
	 * A piece of text with style attributes. It is important that every implementation of this interface implement {@link #toString()} as
	 * specified by {@link CharSequence#toString()}.
	 */
	public static interface StyledSequence extends CharSequence {
		/** @return The style for this sequence */
		QuickStyle getStyle();
	}

	/** A change in a {@link QuickDocumentModel} */
	public static interface QuickDocumentChangeEvent {
		/** @return The document model that changed */
		QuickDocumentModel getModel();

		/** @return The position in the document at the beginning of the sequence where the change occurred */
		int getStartIndex();

		/** @return The position in the document at the end of the sequence where the change occurred */
		int getEndIndex();
	}

	/** Fired when a document model's content changes */
	public static interface ContentChangeEvent extends QuickDocumentChangeEvent {
		/** @return The document model's content after the change */
		String getValue();

		/** @return The section of content that was added or removed */
		String getChange();

		/** @return Whether this change represents a removal or an addition */
		boolean isRemove();
	}

	/** Fired when a document model's style changes for any portion of its content */
	public static interface StyleChangeEvent extends QuickDocumentChangeEvent {
		/** @return The styles of the document before the style change. This may be null if the information is not available. */
		Iterable<StyledSequence> styleBefore();

		/** @return The styles of the document after the style change. This may be null if the information is not available. */
		Iterable<StyledSequence> styleAfter();
	}

	/**
	 * Allows notification of changes to a document. The events may be instances of:
	 * <ul>
	 * <li>{@link ContentChangeEvent} if the event is the result of changes to a document's content,</li>
	 * <li>{@link StyleChangeEvent} if the style of a portion of the document changed,</li>
	 * </ul>
	 * or another event type to communicate changes to properties not supported by the base interface.
	 *
	 * {@link Observable#filterMap(Class)} may be used to select a particular type of change.
	 *
	 * @return An observable that fires each time anything in the document changes
	 */
	Observable<QuickDocumentChangeEvent> changes();

	/** @return An observable that fires a single change at the end of every group of transactions */
	Observable<?> simpleChanges();

	/**
	 * @param position The position to get the style for
	 * @return The style at the given position
	 */
	default QuickStyle getStyleAt(int position) {
		try (Transaction t = holdForRead()) {
			int pos = 0;
			QuickStyle last = null;
			for (StyledSequence seq : this) {
				last = seq.getStyle();
				pos += seq.length();
				if (pos > position)
					break;
			}
			if (position > pos)
				throw new IndexOutOfBoundsException(position + ">" + pos);
			return last;
		}
	}

	/**
	 * @param position The position to begin iteration from
	 * @return This document's content starting from the given position
	 */
	default Iterable<StyledSequence> iterateFrom(final int position) {
		return iterateFrom(position, Integer.MAX_VALUE);
	}

	/**
	 * @param start The position to begin iteration from
	 * @param end The position to end iteration at
	 * @return This document's content starting from the given position
	 */
	default Iterable<StyledSequence> iterateFrom(final int start, final int end) {
		final Iterator<StyledSequence> iterator = iterator();
		int pos = 0;
		while (iterator.hasNext()) {
			final StyledSequence seq = iterator.next();
			int seqLen = seq.length();
			if (pos + seqLen > start) {
				final int fPos = pos;
				return new Iterable<StyledSequence>() {
					@Override
					public Iterator<StyledSequence> iterator() {
						return new Iterator<StyledSequence>() {
							private boolean hasReturnedBegin;

							private int thePos = fPos;

							private Iterator<StyledSequence> theBackingIterator = iterator;

							@Override
							public boolean hasNext() {
								if (!hasReturnedBegin)
									return true;
								else if (thePos >= end)
									return false;
								else
									return theBackingIterator.hasNext();
							}

							@Override
							public StyledSequence next() {
								StyledSequence ret;
								if (!hasReturnedBegin) {
									hasReturnedBegin = true;
									ret = new StyledSubSequence(seq, start - thePos, end - thePos);
								} else
									ret = theBackingIterator.next();
								if (thePos + ret.length() > end)
									ret = new StyledSubSequence(ret, 0, end - thePos);
								thePos += ret.length();
								return ret;
							}

							@Override
							public void remove() {
								throw new UnsupportedOperationException();
							}
						};
					}
				};
			}
			pos += seqLen;
		}
		if (pos == start)
			return java.util.Collections.EMPTY_SET;
		else
			throw new IndexOutOfBoundsException(start + ">" + pos);
	}

	/** @return A transaction that prevents any other threads from modifying this document model until the transaction is closed */
	Transaction holdForRead();

	/**
	 * Creates a sub-document consisting of this document's content after the given position
	 *
	 * @param start The starting position for the sub-document
	 * @return The sub-document
	 */
	default QuickDocumentModel subSequence(int start) {
		return subSequence(start, Integer.MAX_VALUE);
	}

	@Override
	default int length() {
		int ret = 0;
		try (Transaction t = holdForRead()) {
			for (StyledSequence seq : this)
				ret += seq.length();
		}
		return ret;
	}

	@Override
	default char charAt(int index) {
		int pos = 0;
		try (Transaction t = holdForRead()) {
			for (StyledSequence seq : this) {
				int seqLen = seq.length();
				if (pos + seqLen > index)
					return seq.charAt(index - pos);
				pos += seqLen;
			}
		}
		throw new IndexOutOfBoundsException(index + ">" + pos);
	}

	@Override
	default QuickDocumentModel subSequence(int start, int end) {
		if (start > end)
			throw new IndexOutOfBoundsException(start + ">" + end);
		return new SubDocument(this, start, end - start);
	}

	/**
	 * @param modelWrapper An observable value that supplies documents
	 * @return A document model reflecting the value in the observable
	 */
	public static QuickDocumentModel flatten(ObservableValue<? extends QuickDocumentModel> modelWrapper) {
		return new FlattenedDocumentModel(modelWrapper);
	}

	/**
	 * Implements {@link QuickDocumentModel#iterateFrom(int, int)}
	 */
	class StyledSubSequence implements StyledSequence {
		private final StyledSequence theBacking;
		private final int theStart;
		private final int theEnd;

		StyledSubSequence(StyledSequence backing, int start, int end) {
			theBacking = backing;
			theStart = start;
			theEnd = end;
		}

		@Override
		public int length() {
			int end = theEnd;
			if (theBacking.length() < end)
				end = theBacking.length();
			return end - theStart;
		}

		@Override
		public char charAt(int index) {
			return theBacking.charAt(index + theStart);
		}

		@Override
		public CharSequence subSequence(int start, int end) {
			return theBacking.subSequence(start + theStart, end + theStart);
		}

		@Override
		public QuickStyle getStyle() {
			return theBacking.getStyle();
		}

		@Override
		public String toString() {
			int end = theEnd;
			boolean unlimitedEnd = false;
			if (theBacking.length() < end) {
				unlimitedEnd = true;
				end = theBacking.length();
			}
			if (theStart == 0 && unlimitedEnd)
				return theBacking.toString();
			else
				return theBacking.subSequence(theStart, end).toString();
		}
	}

	/** Implements {@link QuickDocumentModel#subSequence(int, int)} */
	class SubDocument implements QuickDocumentModel {
		private final QuickDocumentModel theOuter;
		private int theStart;
		private int theLength;

		protected SubDocument(QuickDocumentModel outer, int start, int length) {
			theOuter = outer;
			theStart = start;
			if (length > outer.length() - start)
				length = outer.length() - start;
			theLength = length;
		}

		protected QuickDocumentModel getWrapped() {
			return theOuter;
		}

		protected int getStart() {
			return theStart;
		}

		@Override
		public int length() {
			return theLength;
		}

		protected void adjustPosition(int startDiff, int lenDiff) {
			theStart += startDiff;
			theLength += lenDiff;
		}

		protected int getEnd() {
			int end = theStart + theLength;
			if (end > theOuter.length())
				end = theOuter.length();
			return end;
		}

		@Override
		public Iterator<StyledSequence> iterator() {
			return theOuter.iterateFrom(theStart, getEnd()).iterator();
		}

		@Override
		public char charAt(int index) {
			return theOuter.charAt(index + theStart);
		}

		@Override
		public Observable<QuickDocumentChangeEvent> changes() {
			return theOuter.changes().filterMap(this::filterMap);
		}

		@Override
		public Observable<?> simpleChanges() {
			return theOuter.simpleChanges();
		}

		protected QuickDocumentChangeEvent filterMap(QuickDocumentChangeEvent change) {
			if (change.getEndIndex() <= theStart || change.getStartIndex() >= getEnd())
				return null; // Outside the bounds of this sub-document
			int changeStart = transform(change.getStartIndex());
			int changeEnd = transform(change.getEndIndex());
			if (change instanceof ContentChangeEvent) {
				ContentChangeEvent contentChange = (ContentChangeEvent) change;
				return new ContentChangeEventImpl(this, toString(), filter(contentChange.getChange(), change.getStartIndex()), changeStart,
					changeEnd, contentChange.isRemove(), change);
			} else if (change instanceof StyleChangeEvent) {
				StyleChangeEvent styleChange = (StyleChangeEvent) change;
				return new StyleChangeEventImpl(this, changeStart, changeEnd, styleChange.styleBefore(), styleChange.styleAfter(), change);
			} else {
				System.err.println("Unrecognized change event type: " + change.getClass().getName() + " in sub-document of "
					+ theOuter.getClass().getName());
				return null;
			}
		}

		/**
		 * @param index The index in the super-sequence
		 * @return The corresponding index in this sub-document
		 */
		protected int transform(int index) {
			index -= theStart;
			if (index < 0)
				index = 0;
			else if (index > length())
				index = length();
			return index;
		}

		/**
		 * @param subSeq The sub-sequence in this sub-document's super-document to filter
		 * @param start The location of the sequence in the super-document
		 * @return The filtered sub-sequence relative to this sub-document
		 */
		protected String filter(String subSeq, int start) {
			int end = getEnd();
			int subEnd = start + subSeq.length();
			if (start >= theStart && subEnd <= end)
				return subSeq;
			else if (subEnd <= end)
				return subSeq.substring(theStart - start);
			else if (start >= theStart)
				return subSeq.substring(0, end - start);
			else
				return subSeq.substring(theStart - start, end - start);
		}

		@Override
		public QuickStyle getStyleAt(int position) {
			return theOuter.getStyleAt(position + theStart);
		}

		@Override
		public Iterable<StyledSequence> iterateFrom(int position) {
			return theOuter.iterateFrom(position + theStart, getEnd());
		}

		@Override
		public Iterable<StyledSequence> iterateFrom(int start, int end) {
			if (theStart + end > getEnd())
				end = getEnd();
			return theOuter.iterateFrom(theStart + start, end);
		}

		@Override
		public Transaction holdForRead() {
			return theOuter.holdForRead();
		}
	}

	/** Implements {@link QuickDocumentModel#flatten(ObservableValue)} */
	class FlattenedDocumentModel implements QuickDocumentModel {
		private final ObservableValue<? extends QuickDocumentModel> theWrapper;

		public FlattenedDocumentModel(ObservableValue<? extends QuickDocumentModel> wrapper) {
			theWrapper = wrapper;
		}

		protected ObservableValue<? extends QuickDocumentModel> getWrapper() {
			return theWrapper;
		}

		@Override
		public Observable<QuickDocumentChangeEvent> changes() {
			return new Observable<QuickDocumentChangeEvent>() {
				@Override
				public Transaction lock() {
					return theWrapper.lock();
				}

				@Override
				public Transaction tryLock() {
					return theWrapper.tryLock();
				}

				@Override
				public Subscription subscribe(Observer<? super QuickDocumentChangeEvent> observer) {
					return theWrapper.changes().act(event -> {
						if (event.getOldValue() == event.getNewValue())
							return;
						QuickDocumentModel old = event.getOldValue();
						if (old != null && old.length() > 0 && !event.isInitial()) {
							ContentChangeEvent clear = createClearEvent(old, event);
							try (Transaction clearT = Causable.use(clear)) {
								observer.onNext(clear);
							}
						}

						QuickDocumentModel current = event.getNewValue();
						if (current != null) {
							if (!event.isInitial()) {
								ContentChangeEvent pop = createPopulateEvent(current, event);
								try (Transaction popT = Causable.use(pop)) {
									observer.onNext(pop);
								}
							}
							current.changes()
								.takeUntil(theWrapper.changes().noInit().filter(evt -> evt.getOldValue() != event.getNewValue()))
								.subscribe(new Observer<QuickDocumentChangeEvent>() {
									@Override
									public <V extends QuickDocumentChangeEvent> void onNext(V value) {
										observer.onNext(value);
									}

									@Override
									public <V extends QuickDocumentChangeEvent> void onCompleted(V value) {
										observer.onCompleted(value);
									}
								});
						}
					});
				}

				@Override
				public boolean isSafe() {
					return true; // Assume the contained document model itself will be safe
				}
			};
		}

		@Override
		public Observable<?> simpleChanges() {
			return Observable.flatten(theWrapper.value().map(doc -> doc == null ? null : doc.simpleChanges()));
		}

		protected ContentChangeEvent createClearEvent(QuickDocumentModel oldModel, Object cause) {
			return new ContentChangeEventImpl(FlattenedDocumentModel.this, oldModel.toString(), oldModel.toString(), 0, oldModel.length(),
				true, cause);
		}

		protected ContentChangeEvent createPopulateEvent(QuickDocumentModel newModel, Object cause) {
			return new ContentChangeEventImpl(FlattenedDocumentModel.this, newModel.toString(), newModel.toString(), 0, newModel.length(),
				false, cause);
		}

		@Override
		public Iterator<StyledSequence> iterator() {
			QuickDocumentModel wrapped = theWrapper.get();
			if (wrapped == null)
				return Collections.<StyledSequence> emptyList().iterator();
			return wrapped.iterator();
		}

		@Override
		public int length() {
			QuickDocumentModel wrapped = theWrapper.get();
			if (wrapped == null)
				return 0;
			return wrapped.length();
		}

		@Override
		public char charAt(int index) {
			QuickDocumentModel wrapped = theWrapper.get();
			if (wrapped == null)
				throw new IndexOutOfBoundsException(index + " of 0");
			return wrapped.charAt(index);
		}

		@Override
		public QuickDocumentModel subSequence(int start, int end) {
			return QuickDocumentModel.flatten(theWrapper.map(doc -> doc.subSequence(start, end)));
		}

		@Override
		public QuickStyle getStyleAt(int position) {
			QuickDocumentModel wrapped = theWrapper.get();
			if (wrapped == null)
				throw new IndexOutOfBoundsException(position + " of 0");
			return wrapped.getStyleAt(position);
		}

		@Override
		public Iterable<StyledSequence> iterateFrom(int position) {
			QuickDocumentModel wrapped = theWrapper.get();
			if (wrapped == null) {
				if (position == 0)
					return Collections.<StyledSequence> emptyList();
				else
					throw new IndexOutOfBoundsException(position + " of 0");
			}
			return wrapped.iterateFrom(position);
		}

		@Override
		public Iterable<StyledSequence> iterateFrom(int start, int end) {
			QuickDocumentModel wrapped = theWrapper.get();
			if (wrapped == null) {
				if (start == 0 && end == 0)
					return Collections.<StyledSequence> emptyList();
				else
					throw new IndexOutOfBoundsException(start + " to " + end + " of 0");
			}
			return wrapped.iterateFrom(start, end);
		}

		@Override
		public Transaction holdForRead() {
			QuickDocumentModel wrapped = theWrapper.get();
			if (wrapped == null) {
				return () -> {
				};
			}
			return wrapped.holdForRead();
		}
	}

	/** A default implementation of ContentChangeEvent */
	static class ContentChangeEventImpl extends Causable implements ContentChangeEvent {
		private final QuickDocumentModel theModel;

		private final String theValue;

		private final String theChange;

		private final int theStartIndex;
		private final int theEndIndex;

		private final boolean isRemove;

		/**
		 * @param model The document model whose content changed
		 * @param value The document model's content after the change
		 * @param change The section of content that was added or removed
		 * @param startIndex The start index of the addition or removal
		 * @param endIndex The end index of the text removed, or the end index of the added text after being added
		 * @param remove Whether this change represents a removal or an addition
		 * @param cause This event's cause
		 */
		public ContentChangeEventImpl(QuickDocumentModel model, String value, String change, int startIndex, int endIndex, boolean remove,
			Object cause) {
			super(cause);
			theModel = model;
			theValue = value;
			theChange = change;
			theStartIndex = startIndex;
			theEndIndex = endIndex;
			isRemove = remove;
		}

		@Override
		public QuickDocumentModel getModel() {
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
		public String toString() {
			return theChange + " chars " + (isRemove ? "removed" : "added") + " at index " + theStartIndex;
		}
	}

	/** A default implementation of StyleChangeEvent */
	static class StyleChangeEventImpl extends Causable implements StyleChangeEvent {
		private final QuickDocumentModel theDocument;

		private final int theStart;

		private final int theEnd;

		private final Iterable<StyledSequence> theBeforeStyles;

		private final Iterable<StyledSequence> theAfterStyles;

		public StyleChangeEventImpl(QuickDocumentModel document, int start, int end, Iterable<StyledSequence> beforeStyles,
			Iterable<StyledSequence> afterStyles, Object cause) {
			super(cause);
			theDocument = document;
			theStart = start;
			theEnd = end;
			theBeforeStyles = beforeStyles == null ? null : IterableUtils.immutableIterable(beforeStyles);
			theAfterStyles = afterStyles == null ? null : IterableUtils.immutableIterable(afterStyles);
		}

		@Override
		public QuickDocumentModel getModel() {
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
		public String toString() {
			return "Style change from " + theStart + " to " + theEnd;
		}
	}
}
