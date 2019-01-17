package org.quick.core.model;

import java.util.concurrent.atomic.AtomicInteger;

import org.observe.util.WeakListening;
import org.qommons.Transaction;

/** A modifiable document model */
public interface MutableDocumentModel extends QuickDocumentModel, Appendable {
	@Override
	default MutableDocumentModel subSequence(int start) {
		return (MutableDocumentModel) QuickDocumentModel.super.subSequence(start);
	}

	@Override
	default MutableDocumentModel subSequence(int start, int end) {
		return new MutableSubDoc(this, start, end - start);
	}

	@Override
	public MutableDocumentModel append(CharSequence csq);

	@Override
	public MutableDocumentModel append(CharSequence csq, int start, int end);

	@Override
	public MutableDocumentModel append(char c);

	/**
	 * Clears this document's content
	 *
	 * @return This model, for chaining
	 */
	default MutableDocumentModel clear() {
		return delete(0, length());
	}

	/**
	 * Deletes characters from this document
	 *
	 * @param start The index of the start of the sequence to remove, inclusive
	 * @param end The index of the end of the sequence to remove, exclusive
	 * @return This model, for chaining
	 */
	MutableDocumentModel delete(int start, int end);

	/**
	 * Sets the content for this model
	 *
	 * @param text The text to set
	 * @return This model, for chaining
	 */
	MutableDocumentModel setText(String text);

	/**
	 * Inserts a character sequence
	 *
	 * @param offset The index at which to insert the character sequence
	 * @param csq The character sequence to insert
	 * @return This model, for chaining
	 */
	MutableDocumentModel insert(int offset, CharSequence csq);

	/**
	 * Inserts a character
	 *
	 * @param offset The index at which to insert the character
	 * @param c The character to insert
	 * @return This model, for chaining
	 */
	MutableDocumentModel insert(int offset, char c);

	/**
	 * @param cause The event or thing that is causing the changes to be made in the transaction
	 * @return A transaction that prevents any other threads from modifying or accessing this document model until the transaction is closed
	 */
	Transaction holdForWrite(Object cause);

	/** Implements {@link MutableDocumentModel#subSequence(int, int)} */
	class MutableSubDoc extends SubDocument implements MutableDocumentModel {
		private final AtomicInteger isLocalMod;

		public MutableSubDoc(MutableDocumentModel outer, int start, int length) {
			super(outer, start, length);
			isLocalMod = new AtomicInteger();
			// Allows this sub-doc to be GC'd when nobody cares about it anymore
			WeakListening.consumeWeakly(this::changed, getWrapped().changes()::act);
		}

		protected void changed(QuickDocumentChangeEvent change) {
			if (!(change instanceof ContentChangeEvent))
				return;
			ContentChangeEvent contentChange = (ContentChangeEvent) change;
			if (contentChange.getStartIndex() > getEnd())
				return;
			if (isLocalMod.get() == 0 && contentChange.getStartIndex() == getEnd())
				return;
			int startDiff = 0, lenDiff = 0;
			if (contentChange.isRemove()) {
				if (contentChange.getEndIndex() < getStart())
					startDiff = -(contentChange.getEndIndex() - contentChange.getStartIndex());
				else if (isLocalMod.get() == 0 && contentChange.getEndIndex() == getStart())
					startDiff = -(contentChange.getEndIndex() - contentChange.getStartIndex());
				else if (contentChange.getStartIndex() >= getStart())
					lenDiff = -(contentChange.getEndIndex() - contentChange.getStartIndex());
				else {
					startDiff = -(getStart() - contentChange.getStartIndex());
					lenDiff = -(contentChange.getEndIndex() - contentChange.getStartIndex() + startDiff);
					if ((-lenDiff) > length())
						lenDiff = -length();
				}
			} else {
				if (contentChange.getStartIndex() < getStart())
					startDiff = contentChange.getEndIndex() - contentChange.getStartIndex();
				else if (isLocalMod.get() == 0 && contentChange.getStartIndex() == getStart())
					startDiff = contentChange.getEndIndex() - contentChange.getStartIndex();
				else
					lenDiff = contentChange.getEndIndex() - contentChange.getStartIndex();
			}
			adjustPosition(startDiff, lenDiff);
		}

		@Override
		protected MutableDocumentModel getWrapped() {
			return (MutableDocumentModel) super.getWrapped();
		}

		@Override
		public MutableDocumentModel append(CharSequence csq) {
			try (Transaction trans = holdForWrite(null)) {
				getWrapped().insert(getEnd(), csq);
			}
			return this;
		}

		@Override
		public MutableDocumentModel append(CharSequence csq, int start, int end) {
			try (Transaction trans = holdForWrite(null)) {
				getWrapped().insert(getEnd(), csq.subSequence(start, end));
			}
			return this;
		}

		@Override
		public MutableDocumentModel append(char c) {
			try (Transaction trans = holdForWrite(null)) {
				getWrapped().insert(getEnd(), c);
			}
			return this;
		}

		@Override
		public MutableDocumentModel delete(int start, int end) {
			try (Transaction trans = holdForWrite(null)) {
				getWrapped().delete(getStart() + start, getStart() + end);
			}
			return this;
		}

		@Override
		public MutableDocumentModel setText(String text) {
			return clear().append(text);
		}

		@Override
		public MutableDocumentModel insert(int offset, CharSequence csq) {
			try (Transaction trans = holdForWrite(null)) {
				getWrapped().insert(getStart() + offset, csq);
			}
			return this;
		}

		@Override
		public MutableDocumentModel insert(int offset, char c) {
			try (Transaction trans = holdForWrite(null)) {
				getWrapped().insert(getStart() + offset, c);
			}
			return this;
		}

		@Override
		public Transaction holdForWrite(Object cause) {
			Transaction wrapped = getWrapped().holdForWrite(cause);
			isLocalMod.getAndIncrement();
			return new Transaction() {
				private volatile boolean hasRun;

				@Override
				public void close() {
					if (hasRun)
						return;
					hasRun = true;
					isLocalMod.getAndDecrement();
					wrapped.close();
				}

				@Override
				protected void finalize() {
					if (!hasRun)
						close();
				}
			};
		}
	}
}
