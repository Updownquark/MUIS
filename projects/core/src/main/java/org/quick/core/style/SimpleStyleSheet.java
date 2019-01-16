package org.quick.core.style;

import org.observe.ObservableValue;
import org.observe.collect.ObservableSet;
import org.observe.collect.ObservableSortedSet;
import org.observe.util.TypeTokens;
import org.qommons.collect.CollectionLockingStrategy;
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
		theAttributeConditions = ObservableSet.create(SCH_TYPE);
		TypeToken<StyleAttribute<?>> attrType = TypeTokens.get().keyFor(StyleAttribute.class)
			.parameterized(() -> new TypeToken<StyleAttribute<?>>() {});
		theAttributes = theAttributeConditions.flow().filter(ac -> ac.theValues.isEmpty() ? "No values" : null)//
			.mapEquivalent(attrType, ac -> ac.attribute, attr -> new StyleConditionHolder<>(attr),
				opts -> opts.cache(false).fireIfUnchanged(false))//
			.collect();
	}

	@Override
	public ObservableSet<StyleAttribute<?>> attributes() {
		return theAttributes;
	}

	@Override
	public <T> ObservableSortedSet<StyleConditionValue<T>> getStyleExpressions(StyleAttribute<T> attr) {
		return ((StyleConditionHolder<T>) theAttributeConditions.getOrAdd(new StyleConditionHolder<>(attr), true, null)).getValues();
	}

	@Override
	public <T> SimpleStyleSheet set(StyleAttribute<T> attr, StyleCondition condition, ObservableValue<? extends T> value) {
		getStyleExpressions(attr).add(new StyleConditionValueImpl<>(attr, condition, value, theMsg));
		return this;
	}

	@Override
	public SimpleStyleSheet clear(StyleAttribute<?> attr, StyleCondition condition) {
		getStyleExpressions(attr).remove(new StyleConditionValueImpl<>(attr, condition, null, theMsg));
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
					.getCompoundType(attribute.getType().getType(), t -> new TypeToken<StyleConditionValue<T>>() {});
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
