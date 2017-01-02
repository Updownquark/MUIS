package org.quick.base.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.observe.ObservableValue;
import org.observe.assoc.ObservableMap;
import org.observe.assoc.impl.ObservableMapImpl;
import org.observe.collect.ObservableCollection;
import org.observe.collect.ObservableSet;
import org.observe.collect.impl.ObservableHashSet;
import org.observe.util.DefaultTransactable;
import org.qommons.Transaction;
import org.quick.core.QuickElement;
import org.quick.core.mgr.QuickMessageCenter;
import org.quick.core.mgr.QuickState;
import org.quick.core.model.AbstractSelectableDocumentModel;
import org.quick.core.model.MutableDocumentModel;
import org.quick.core.model.StyleableSelectableDocumentModel;
import org.quick.core.style.*;

import com.google.common.reflect.TypeToken;

/** A {@link MutableDocumentModel} that allows different styles for different sections of text */
public class RichDocumentModel extends AbstractSelectableDocumentModel implements StyleableSelectableDocumentModel {
	private class RichStyleSequence implements StyledSequence, QuickStyle {
		private final StringBuilder theContent;
		private final ObservableMap<StyleAttribute<?>, StyleValue<?>> theStyles;
		private final ObservableSet<String> theExtraGroups;
		private final QuickStyle theBacking;

		RichStyleSequence() {
			theContent = new StringBuilder();
			DefaultTransactable transactable = new DefaultTransactable(getLock());
			theStyles = new ObservableMapImpl<>(new TypeToken<StyleAttribute<?>>() {}, new TypeToken<StyleValue<?>>() {},
				ObservableHashSet::new, getLock(), transactable.getSession(), transactable);
			theExtraGroups = new ObservableHashSet<>(TypeToken.of(String.class), getLock(), transactable.getSession(), transactable);
			theBacking = getNormalStyle().forExtraGroups(theExtraGroups);
		}

		void addGroup(String group) {
			theExtraGroups.add(group);
		}

		void removeGroup(String group) {
			theExtraGroups.remove(group);
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
		public String toString() {
			return theContent.toString();
		}

		@Override
		public QuickStyle getStyle() {
			return this;
		}

		@Override
		public ObservableSet<StyleAttribute<?>> attributes() {
			return ObservableSet.unique(ObservableCollection.flattenCollections(theStyles.keySet(), theBacking.attributes()),
				Object::equals);
		}

		@Override
		public boolean isSet(StyleAttribute<?> attr) {
			return theStyles.containsKey(attr) || theBacking.isSet(attr);
		}

		@Override
		public <T> ObservableValue<T> get(StyleAttribute<T> attr, boolean withDefault) {
			ObservableValue<T> localValue = ObservableValue
				.flatten((ObservableValue<StyleValue<T>>) (ObservableValue<?>) theStyles.observe(attr));
			return ObservableValue.firstValue(localValue.getType(), v -> v != null, null, localValue,
				theBacking.get(attr, withDefault));
		}

		@Override
		public QuickStyle forExtraStates(ObservableCollection<QuickState> extraStates) {
			return new RichStyleWithExtraStates(getNormalStyle().forExtraStates(extraStates));
		}

		@Override
		public QuickStyle forExtraGroups(ObservableCollection<String> extraGroups) {
			return new RichStyleWithExtraStates(getNormalStyle().forExtraGroups(extraGroups));
		}

		class RichStyleWithExtraStates implements QuickStyle {
			private final QuickStyle theBacking2;

			RichStyleWithExtraStates(QuickStyle backing) {
				theBacking2 = backing;
			}

			@Override
			public boolean isSet(StyleAttribute<?> attr) {
				return theStyles.containsKey(attr) || theBacking2.isSet(attr);
			}

			@Override
			public ObservableSet<StyleAttribute<?>> attributes() {
				return ObservableSet.unique(ObservableCollection.flattenCollections(theStyles.keySet(), theBacking2.attributes()),
					Object::equals);
			}

			@Override
			public <T> ObservableValue<T> get(StyleAttribute<T> attr, boolean withDefault) {
				ObservableValue<T> localValue = ObservableValue
					.flatten((ObservableValue<StyleValue<T>>) (ObservableValue<?>) theStyles.observe(attr));
				return ObservableValue.firstValue(localValue.getType(), v -> v != null, null, localValue,
					theBacking2.get(attr, withDefault));
			}

			@Override
			public QuickStyle forExtraStates(ObservableCollection<QuickState> extraStates2) {
				return new RichStyleWithExtraStates(theBacking2.forExtraStates(extraStates2));
			}

			@Override
			public QuickStyle forExtraGroups(ObservableCollection<String> extraGroups2) {
				return new RichStyleWithExtraStates(theBacking2.forExtraGroups(extraGroups2));
			}
		}
	}

	private final QuickMessageCenter theMessageCenter;
	private List<RichStyleSequence> theSequences;

	/** @param element The element that this document model is for */
	public RichDocumentModel(QuickElement element) {
		super(element);
		theMessageCenter = element.msg();
		theSequences = new ArrayList<>();
	}

	/**
	 * Sets a style attribute at the end of this document. This operation will not affect the style of any existing content.
	 *
	 * @param <T> The type of the attribute
	 * @param attr The attribute to set at the end of this document
	 * @param value The value to set for the attribute at the end of this document
	 * @return This document, for chaining
	 */
	public <T> RichDocumentModel set(StyleAttribute<T> attr, ObservableValue<T> value) {
		try (Transaction t = holdForWrite(getWriteLockCause())) {
			if(last().getStyle().get(attr) == value)
				return this;

			RichStyleSequence seq = getEmptyLast();
			seq.theStyles.put(attr, new StyleValue<>(attr, value, theMessageCenter));
		}
		return this;
	}

	private RichStyleSequence getEmptyLast() {
		RichStyleSequence seq = null;
		if(theSequences.size() > 0) {
			seq = theSequences.get(theSequences.size() - 1);
			if(seq.theContent.length() > 0)
				seq = null;
		}
		if(seq == null) {
			seq = new RichStyleSequence();
			if(!theSequences.isEmpty())
				seq.theStyles.putAll(theSequences.get(theSequences.size() - 1).theStyles);
			theSequences.add(seq);
		}
		return seq;
	}

	/**
	 * Clears a style attribute at the end of this document. This operation will not affect the style of any existing content.
	 *
	 * @param attr The attribute to clear at the end of this document
	 * @return This document, for chaining
	 */
	public RichDocumentModel clear(StyleAttribute<?> attr) {
		try (Transaction t = holdForWrite(getWriteLockCause())) {
			if(last().getStyle().get(attr) == null)
				return this;

			RichStyleSequence seq = getEmptyLast();
			seq.theStyles.remove(attr);
		}
		return this;
	}

	/** @return The last sequence in this model */
	public StyledSequence last() {
		try (Transaction t = holdForRead()) {
			if(theSequences.size() > 0)
				return theSequences.get(theSequences.size() - 1);
		}
		try (Transaction t = holdForWrite(null)) {
			RichStyleSequence seq = null;
			if(theSequences.size() > 0) {
				seq = theSequences.get(theSequences.size() - 1);
			}
			if(seq == null) {
				seq = new RichStyleSequence();
				theSequences.add(seq);
			}
			return seq;
		}
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
		QuickStyle style = null;
		if(csq instanceof QuickStyle)
			style = (QuickStyle) csq;
		else if(csq instanceof StyledSequence)
			style = ((StyledSequence) csq).getStyle();

		try (Transaction t = holdForWrite(null)) {
			if (style != null) {
				RichStyleSequence newSeq = new RichStyleSequence();
				if (!theSequences.isEmpty()) {
					RichStyleSequence last = theSequences.get(theSequences.size() - 1);
					newSeq.theStyles.putAll(last.theStyles);
				}
				for (StyleAttribute<?> att : style.attributes())
					newSeq.theStyles.put(att, new StyleValue<>((StyleAttribute<Object>) att, style.get(att), theMessageCenter));
				newSeq.theContent.append(csq);
				theSequences.add(newSeq);
			} else if (theSequences.isEmpty())
				theSequences.add(new RichStyleSequence());
			theSequences.get(theSequences.size() - 1).theContent.append(csq, start, end);
		}
	}

	@Override
	protected void internalInsert(int offset, CharSequence csq) {
		QuickStyle style = null;
		if(csq instanceof QuickStyle)
			style = (QuickStyle) csq;
		else if(csq instanceof StyledSequence)
			style = ((StyledSequence) csq).getStyle();

		int pos = 0;
		boolean inserted = false;
		for(int i = 0; i < theSequences.size(); i++) {
			RichStyleSequence seq = theSequences.get(i);
			int nextPos = pos + seq.length();
			if(nextPos > offset) {
				inserted = true;
				if(style != null) {
					boolean split = offset != pos;
					if(split)
						splitSequence(i, offset - pos);
					RichStyleSequence newSeq = new RichStyleSequence();
					newSeq.theStyles.putAll(seq.theStyles);
					for(StyleAttribute<?> att : style.attributes())
						newSeq.theStyles.put(att, new StyleValue<>((StyleAttribute<Object>) att, style.get(att), theMessageCenter));
					newSeq.theContent.append(csq);
					theSequences.add(split ? i + 1 : 1, newSeq);
				} else
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
		if(theSequences.isEmpty())
			theSequences.add(new RichStyleSequence());
		theSequences.get(0).theContent.delete(0, theSequences.get(0).theContent.length());
		theSequences.get(0).theContent.append(text);
	}

	private void splitSequence(int seqIndex, int seqPos) {
		RichStyleSequence seq = theSequences.get(seqIndex);
		String postSplit = seq.theContent.substring(seqPos);
		seq.theContent.delete(seqPos, seq.theContent.length());
		RichStyleSequence newSeq = new RichStyleSequence();
		newSeq.theStyles.putAll(seq.theStyles);
		newSeq.theContent.append(postSplit);
		theSequences.add(seqIndex + 1, newSeq);
	}

	@Override
	public GroupableStyle getSegmentStyle(int start, int end) {
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

	private class RichSegmentStyle implements GroupableStyle {
		private final int theStart;
		private final int theEnd;

		RichSegmentStyle(int start, int end) {
			theStart = start;
			theEnd = end;
		}

		@Override
		public GroupableStyle addGroup(String group) {
			try (Transaction t = holdForWrite(getWriteLockCause())) {
				for (RichStyleSequence seq : getSeqsForMod())
					seq.addGroup(group);
			}

			fireStyleEvent(theStart, theEnd, getWriteLockCause());
			return this;
		}

		@Override
		public GroupableStyle removeGroup(String group) {
			try (Transaction t = holdForWrite(getWriteLockCause())) {
				for (RichStyleSequence seq : getSeqsForMod())
					seq.removeGroup(group);
			}

			fireStyleEvent(theStart, theEnd, getWriteLockCause());
			return this;
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
		public <T> ObservableValue<T> get(StyleAttribute<T> attr, boolean withDefault) {
			try (Transaction t = holdForRead()) {
				for(StyledSequence seq : iterateFrom(theStart, theEnd))
					if(seq.getStyle().isSet(attr))
						return seq.getStyle().get(attr);
			}
			return ObservableValue.constant(attr.getType().getType(), withDefault ? attr.getDefault() : null);
		}

		@Override
		public ObservableSet<StyleAttribute<?>> attributes() {
			ObservableHashSet<ObservableSet<StyleAttribute<?>>> ret = new ObservableHashSet<>(
				new TypeToken<ObservableSet<StyleAttribute<?>>>() {});
			try (Transaction t = holdForRead()) {
				for(StyledSequence seq : iterateFrom(theStart, theEnd))
					ret.add(seq.getStyle().attributes());
			}
			return ObservableSet.unique(ObservableCollection.flatten(ret), Object::equals).immutable();
		}

		@Override
		public <T> MutableStyle set(StyleAttribute<T> attr, ObservableValue<? extends T> value) throws IllegalArgumentException {
			StyleValue<T> safe = new StyleValue<>(attr, value, theMessageCenter);
			try (Transaction t = holdForWrite(getWriteLockCause())) {
				for (RichStyleSequence seq : getSeqsForMod())
					seq.theStyles.put(attr, safe);
			}

			fireStyleEvent(theStart, theEnd, getWriteLockCause());
			return this;
		}

		@Override
		public MutableStyle clear(StyleAttribute<?> attr) {
			try (Transaction t = holdForWrite(getWriteLockCause())) {
				for (RichStyleSequence seq : getSeqsForMod()) {
					seq.theStyles.remove(attr);
				}
			}

			fireStyleEvent(theStart, theEnd, getWriteLockCause());
			return this;
		}

		List<RichStyleSequence> getSeqsForMod() {
			ArrayList<RichStyleSequence> ret = new ArrayList<>();
			int pos = 0;
			for (int i = 0; i < theSequences.size() && pos < theEnd; i++) {
				RichStyleSequence seq = theSequences.get(i);
				int nextPos = pos + seq.length();
				if (nextPos <= theStart) {
				} else if (pos >= theStart && nextPos < theEnd) {
					ret.add(seq);
				} else if (pos < theStart) {
					splitSequence(i, theStart - pos);
					i++;
					ret.add(theSequences.get(i));
				} else {
					splitSequence(i, theEnd - pos);
					ret.add(seq);
					i++;
				}
				pos = nextPos;
			}
			return ret;
		}

		@Override
		public QuickStyle forExtraStates(ObservableCollection<QuickState> extraStates) {
			return this; // Not state-dependent
		}

		@Override
		public QuickStyle forExtraGroups(ObservableCollection<String> extraGroups) {
			return this; // Not group-dependent
		}
	}
}
