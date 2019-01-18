package org.quick.core.style;

import org.observe.ObservableValue;
import org.observe.collect.ObservableCollection;
import org.observe.collect.ObservableSet;
import org.observe.collect.ObservableSortedSet;
import org.observe.util.TypeTokens;
import org.qommons.Transaction;
import org.qommons.collect.CollectionElement;
import org.qommons.collect.CollectionLockingStrategy;
import org.qommons.tree.BetterTreeList;
import org.qommons.tree.BetterTreeSet;
import org.quick.core.mgr.QuickMessageCenter;

import com.google.common.reflect.TypeToken;

/** A simple, mutable stylesheet implementation */
public class SimpleStyleSheet implements MutableStyleSheet {
	private final CollectionLockingStrategy theLocker;
	private final QuickMessageCenter theMsg;
	private final ObservableSet<StyleConditionHolder<?>> theAttributeConditions;
	private final ObservableSet<StyleAttribute<?>> theAttributes;

	/** @param msg The message center to use to report style value errors */
	public SimpleStyleSheet(CollectionLockingStrategy locker, QuickMessageCenter msg) {
		theLocker = locker;
		theMsg = msg;
		theAttributeConditions = ObservableCollection.create(SCH_TYPE, new BetterTreeList<>(locker)).flow().distinct().collect();
		// The attributes here can't just be a passive map, because when a holder is first added,
		// it contains no style expressions until the set method adds them in
		// It seems to me that the attribute set should not contain attributes for which there are no styles, even for an instant
		// This requires the set method to fire an additional update after a new holder is added
		// and it requires the attribute set to be cached
		theAttributes = theAttributeConditions.flow().filter(ac -> ac.theValues == null ? "No values" : null)//
			.mapEquivalent(StyleAttribute.TYPE, ac -> ac.attribute, attr -> new StyleConditionHolder<>(attr),
				opts -> opts.fireIfUnchanged(false))//
			.collect();
	}

	@Override
	public ObservableSet<StyleAttribute<?>> attributes() {
		return theAttributes;
	}

	@Override
	public <T> ObservableSortedSet<StyleConditionValue<T>> getStyleExpressions(StyleAttribute<T> attr) {
		TypeToken<StyleConditionValue<T>> scvType = TypeTokens.get().keyFor(StyleConditionValue.class)
			.<StyleConditionValue<T>> getCompoundType(attr.getType().getType());
		TypeToken<ObservableSortedSet<StyleConditionValue<T>>> ssType = TypeTokens.get().keyFor(ObservableSortedSet.class)
			.getCompoundType(scvType);
		ObservableValue<ObservableSortedSet<StyleConditionValue<T>>> toFlatten = theAttributeConditions
			.observeElement(new StyleConditionHolder<>(attr), true).map(ssType, holder -> {
				if (holder == null) {
					return ObservableSortedSet.of(scvType, StyleConditionValue::compareTo);
				} else
					return ((StyleConditionHolder<T>) holder).getValues();
			});
		return ObservableSortedSet.flattenValue(toFlatten, (scv1, scv2) -> scv1.compareTo(scv2));
	}

	@Override
	public <T> SimpleStyleSheet set(StyleAttribute<T> attr, StyleCondition condition, ObservableValue<? extends T> value) {
		try (Transaction t = theAttributeConditions.lock(true, null)) {
			boolean[] newHolder = new boolean[1];
			CollectionElement<StyleConditionHolder<?>> holder = theAttributeConditions.getOrAdd(new StyleConditionHolder<>(attr), false,
				() -> newHolder[0] = true);
			((StyleConditionHolder<T>) holder.get()).getValues().add(new StyleConditionValueImpl<>(attr, condition, value, theMsg));
			if (newHolder[0])
				theAttributeConditions.mutableElement(holder.getElementId()).set(holder.get()); // Fire update for the attributes
		}
		return this;
	}

	@Override
	public SimpleStyleSheet clear(StyleAttribute<?> attr, StyleCondition condition) {
		try (Transaction t = theAttributeConditions.lock(true, null)) {
			CollectionElement<StyleConditionHolder<?>> holder = theAttributeConditions.getElement(new StyleConditionHolder<>(attr), false);
			if (holder != null && holder.get().getValues().remove(new StyleConditionValueImpl<>(attr, condition, null, theMsg))) {
				if (holder.get().theValues.isEmpty())
					theAttributeConditions.mutableElement(holder.getElementId()).remove();
			}
		}
		return this;
	}

	private static TypeToken<StyleConditionHolder<?>> SCH_TYPE = new TypeToken<StyleConditionHolder<?>>() {};

	private class StyleConditionHolder<T> {
		final StyleAttribute<T> attribute;
		private ObservableSortedSet<StyleConditionValue<T>> theValues;

		StyleConditionHolder(StyleAttribute<T> attr) {
			attribute = attr;
		}

		ObservableSortedSet<StyleConditionValue<T>> getValues() {
			if (theValues == null) {
				TypeToken<StyleConditionValue<T>> type = TypeTokens.get().keyFor(StyleConditionValue.class)
					.getCompoundType(attribute.getType().getType());
				theValues = ObservableSortedSet.create(type, new BetterTreeSet<>(theLocker, StyleConditionValue::compareTo));
			}
			return theValues;
		}

		@Override
		public int hashCode() {
			return attribute.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof StyleConditionHolder && attribute.equals(((StyleConditionHolder<?>) obj).attribute);
		}

		@Override
		public String toString() {
			return attribute + "=" + theValues;
		}
	}
}
