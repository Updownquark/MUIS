package org.quick.core.model;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.Iterator;

import org.observe.*;
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

	/** A piece of text with style attributes and metrics information. The content of a StyledSequenceMetrics never contains a line break. */
	public static interface StyledSequenceMetric extends StyledSequence {
		/** @return The width of this piece of text */
		float getWidth();

		/** @return The height of this piece of text */
		float getHeight();

		/** @return The baseline of this piece of text */
		float getBaseline();

		/** @return Whether this sequence is the first of a new line. This may return false for the first line. */
		boolean isNewLine();

		@Override
		StyledSequenceMetric subSequence(int start, int end);

		/** @return The top position of this sequence in the document */
		float getTop();

		/** @return The left position of this sequence in the document */
		float getLeft();

		/**
		 * @param advance The x-coordinate, in pixels, relative to the left edge of this sequence
		 * @return The position, in characters, along this sequence that the given advance hits
		 */
		float getHitPosition(float advance);

		/**
		 * @param position The position, in characters, along this sequence
		 * @return The x-coordinate, in pixels, relative to the beginning of this sequence of the given position
		 */
		float getLocation(float position);

		/**
		 * Draws this sequence
		 *
		 * @param graphics The graphics to draw the text sequence to
		 * @param x The x-coordinate of the top left corner of the graphics to draw the text to
		 * @param y The y-coordinate of the top left corner of the graphics to draw the text to
		 */
		void draw(Graphics2D graphics, float x, float y);
	}

	/** A change in a {@link QuickDocumentModel} */
	public static interface QuickDocumentChangeEvent extends Causable {
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

	/**
	 * @param position The position to get the style for
	 * @return The style at the given position
	 */
	QuickStyle getStyleAt(int position);

	/**
	 * @param position The position to begin iteration from
	 * @return This document's content starting from the given position
	 */
	Iterable<StyledSequence> iterateFrom(int position);

	/**
	 * @param start The position to begin iteration from
	 * @param end The position to end iteration at
	 * @return This document's content starting from the given position
	 */
	Iterable<StyledSequence> iterateFrom(int start, int end);

	/**
	 * @param start The starting position within the document
	 * @param breakWidth The width, in pixels, to break lines off at in the document
	 * @return The metrics for this document. The first metric's {@link StyledSequenceMetric#isNewLine() newLine} attribute may be false
	 *         even if a new line occurred at position start-1.
	 */
	Iterable<StyledSequenceMetric> metrics(int start, float breakWidth);

	/**
	 * @param x The x-coordinate, in pixels, relative to this document's top left corner
	 * @param y The y-coordinate, in pixels, relative to this document's top left corner
	 * @param breakWidth The width, in pixels, to break lines off at in the document
	 * @return The character position at the given point in the document
	 */
	float getPositionAt(float x, float y, int breakWidth);

	/**
	 * @param position The character position in the document
	 * @param breakWidth The width, in pixels, to break lines off at in the document
	 * @return The position in the document at the given character at the top of the line
	 */
	Point2D getLocationAt(float position, int breakWidth);

	/**
	 * Draws a portion of this document
	 *
	 * @param graphics The graphics to draw on
	 * @param window The window within this document to draw
	 * @param breakWidth The width, in pixels, to break lines off at in the document
	 */
	void draw(Graphics2D graphics, Rectangle window, int breakWidth);

	/** @return A transaction that prevents any other threads from modifying this document model until the transaction is closed */
	Transaction holdForRead();

	/**
	 * @param modelWrapper An observable value that supplies documents
	 * @return A document model reflecting the value in the observable
	 */
	public static QuickDocumentModel flatten(ObservableValue<? extends QuickDocumentModel> modelWrapper) {
		return new FlattenedDocumentModel(modelWrapper);
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
				public Subscription subscribe(Observer<? super QuickDocumentChangeEvent> observer) {
					return theWrapper.noInit().act(event -> {
						QuickDocumentModel old = event.getOldValue();
						if (old != null && old.length() > 0)
							observer.onNext(createClearEvent(old, event));

						QuickDocumentModel current = event.getValue();
						if (current != null) {
							observer.onNext(createPopulateEvent(current, event));
							// Need to skip the initial event, and also the one that will be fired as a result of the same event that
							// this listener is getting.
							current.changes().takeUntil(theWrapper.skip(2))
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
							// observer);
						}
					});
				}

				@Override
				public boolean isSafe() {
					return theWrapper.isSafe(); // Assume the document model itself will be safe
				}
			};
		}

		protected ContentChangeEvent createClearEvent(QuickDocumentModel oldModel, Object cause) {
			return new ContentChangeEvent() {
				@Override
				public QuickDocumentModel getModel() {
					return FlattenedDocumentModel.this;
				}

				@Override
				public int getStartIndex() {
					return 0;
				}

				@Override
				public int getEndIndex() {
					return oldModel.length();
				}

				@Override
				public boolean isRemove() {
					return true;
				}

				@Override
				public String getValue() {
					return oldModel.toString();
				}

				@Override
				public String getChange() {
					return oldModel.toString();
				}

				@Override
				public Object getCause() {
					return cause;
				}
			};
		}

		protected ContentChangeEvent createPopulateEvent(QuickDocumentModel newModel, Object cause) {
			return new ContentChangeEvent() {
				@Override
				public QuickDocumentModel getModel() {
					return FlattenedDocumentModel.this;
				}

				@Override
				public int getStartIndex() {
					return 0;
				}

				@Override
				public int getEndIndex() {
					return newModel.length();
				}

				@Override
				public boolean isRemove() {
					return false;
				}

				@Override
				public String getValue() {
					return newModel.toString();
				}

				@Override
				public String getChange() {
					return newModel.toString();
				}

				@Override
				public Object getCause() {
					return cause;
				}
			};
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
		public CharSequence subSequence(int start, int end) {
			QuickDocumentModel wrapped = theWrapper.get();
			if (wrapped == null)
				throw new IndexOutOfBoundsException(start + " of 0");
			return wrapped.subSequence(start, end);
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
		public Iterable<StyledSequenceMetric> metrics(int start, float breakWidth) {
			QuickDocumentModel wrapped = theWrapper.get();
			if (wrapped == null) {
				if (start == 0)
					return Collections.<StyledSequenceMetric> emptyList();
				else
					throw new IndexOutOfBoundsException(start + " of 0");
			}
			return wrapped.metrics(start, breakWidth);
		}

		@Override
		public float getPositionAt(float x, float y, int breakWidth) {
			QuickDocumentModel wrapped = theWrapper.get();
			if (wrapped == null) {
				return 0;
			}
			return wrapped.getPositionAt(x, y, breakWidth);
		}

		@Override
		public Point2D getLocationAt(float position, int breakWidth) {
			QuickDocumentModel wrapped = theWrapper.get();
			if (wrapped == null) {
				if (position == 0)
					return new Point2D.Float(0, 0);
				else
					throw new IndexOutOfBoundsException(position + " of 0");
			}
			return wrapped.getLocationAt(position, breakWidth);
		}

		@Override
		public void draw(Graphics2D graphics, Rectangle window, int breakWidth) {
			QuickDocumentModel wrapped = theWrapper.get();
			if (wrapped != null)
				wrapped.draw(graphics, window, breakWidth);
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
}
