package org.muis.core.style;

import java.util.Iterator;

import org.muis.core.MuisElement;

import prisms.util.ArrayUtils;

/** A simple implementation of style sheet that does not have dependencies */
public abstract class SimpleStyleSheet implements StyleSheet, Cloneable {
	private static final Object NULL = new Object();

	private static java.util.Comparator<StyleGroupTypeExpressionValue<?, ?>> GTE_COMPARE = new java.util.Comparator<StyleGroupTypeExpressionValue<?, ?>>() {
		@Override
		public int compare(StyleGroupTypeExpressionValue<?, ?> o1, StyleGroupTypeExpressionValue<?, ?> o2) {
			int ret;
			StateExpression exp1 = o1.getExpression();
			StateExpression exp2 = o2.getExpression();
			if(exp1 != null && exp2 != null && !exp1.equals(exp2)) {
				ret = exp2.getPriority() - exp1.getPriority();
				if(ret != 0)
					return ret;
				ret = exp1.compareTo(exp2);
				if(ret != 0)
					return ret;
			} else if(exp1 != null)
				return -1;
			else if(exp2 != null)
				return 1;

			Class<?> type1 = o1.getType();
			Class<?> type2 = o2.getType();
			if(type1 != type2) {
				if(type1.isAssignableFrom(type2))
					return 1;
				else if(type2.isAssignableFrom(type1))
					return -1;
				int typeDepth1 = 0;
				Class<?> tempType = type1;
				while(tempType != MuisElement.class) {
					tempType = tempType.getSuperclass();
					typeDepth1++;
				}
				int typeDepth2 = 0;
				tempType = type2;
				while(tempType != MuisElement.class) {
					tempType = tempType.getSuperclass();
					typeDepth2++;
				}
				if(typeDepth1 != typeDepth2)
					return typeDepth2 - typeDepth1;
				return type1.getName().compareTo(type2.getName());
			}
			String group1 = o1.getGroupName();
			String group2 = o2.getGroupName();
			if(group1 != null && group2 != null)
				return group1.compareTo(group2);
			else if(group1 != null)
				return -1;
			else if(group2 != null)
				return 1;
			return 0;
		}
	};

	private static class StyleValueHolder<T> implements Cloneable {
		private StyleGroupTypeExpressionValue<?, T> [] theValues;

		private boolean isSorted;

		StyleValueHolder(StyleGroupTypeExpressionValue<?, T> value) {
			theValues = new StyleGroupTypeExpressionValue[] {value};
		}

		void add(StyleGroupTypeExpressionValue<?, T> value) {
			isSorted = false;
			theValues = ArrayUtils.add(theValues, value);
		}

		boolean remove(String groupName, Class<? extends MuisElement> type, StateExpression exp) {
			StyleGroupTypeExpressionValue<?, T> [] values = theValues;
			boolean fireEvent = true;
			for(int i = 0; i < values.length; i++) {
				if(!ArrayUtils.equals(groupName, values[i].getGroupName()))
					continue;
				if(ArrayUtils.equals(exp, values[i].getExpression()) && type.equals(values[i].getType())) {
					theValues = ArrayUtils.remove(theValues, values[i]);
					return fireEvent;
				} else if(values[i].getType().isAssignableFrom(type)
					&& (values[i].getExpression() == null || (exp != null && values[i].getExpression().getWhenTrue(exp) > 0)))
					fireEvent = false;
			}
			return false;
		}

		StyleGroupTypeExpressionValue<?, T> [] sort() {
			if(isSorted)
				return theValues;
			StyleGroupTypeExpressionValue<?, T> [] values = theValues;
			java.util.Arrays.sort(values, GTE_COMPARE);
			if(values == theValues) {
				theValues = values;
				isSorted = true;
			}
			return values;
		}

		void set(StyleGroupTypeExpressionValue<?, T> sev) {
			boolean set = false;
			boolean found = true;
			while(found && !set) {
				found = false;
				StyleGroupTypeExpressionValue<?, T> [] values = theValues;
				for(int i = 0; i < values.length; i++) {
					if(ArrayUtils.equals(sev.getExpression(), values[i].getExpression())) {
						found = true;
						if(theValues[i] == values[i]) {
							theValues[i] = sev;
							set = true;
						}
						break;
					}
				}
			}
			if(!found)
				add(sev);
		}

		@Override
		protected StyleValueHolder<T> clone() {
			StyleValueHolder<T> ret;
			try {
				ret = (StyleValueHolder<T>) super.clone();
			} catch(CloneNotSupportedException e) {
				throw new IllegalStateException(e);
			}
			ret.theValues = theValues.clone();
			return ret;
		}
	}

	private java.util.concurrent.ConcurrentHashMap<StyleAttribute<?>, StyleValueHolder<?>> theAttributes;

	private java.util.concurrent.ConcurrentLinkedQueue<StyleGroupTypeExpressionListener> theListeners;

	/** Creates the style sheet */
	public SimpleStyleSheet() {
		theAttributes = new java.util.concurrent.ConcurrentHashMap<>();
		theListeners = new java.util.concurrent.ConcurrentLinkedQueue<>();
	}

	@Override
	public Iterable<StyleAttribute<?>> allLocal() {
		return ArrayUtils.immutableIterable(theAttributes.keySet());
	}

	@Override
	public <T> StyleGroupTypeExpressionValue<?, T> [] getLocalExpressions(StyleAttribute<T> attr) {
		StyleValueHolder<T> holder = (StyleValueHolder<T>) theAttributes.get(attr);
		return holder == null ? new StyleGroupTypeExpressionValue[0] : holder.sort();
	}

	@Override
	public void addListener(StyleGroupTypeExpressionListener listener) {
		if(listener != null)
			theListeners.add(listener);
	}

	@Override
	public void removeListener(StyleGroupTypeExpressionListener listener) {
		theListeners.remove(listener);
	}

	/**
	 * @see MutableStyleSheet#set(StyleAttribute, String, Class, StateExpression, Object) Implemented to make extensions of this class
	 *      easily support MutableStyleSheet
	 * @param <T> The type of the attribute
	 * @param attr The attribute to set the value of
	 * @param groupName The name of the group for the value to be active for
	 * @param type The type of element for the value to be active for
	 * @param exp The state expression for the value to be active for
	 * @param value The value to set for the attribute
	 * @throws IllegalArgumentException If the given value is invalid for the given attribute
	 */
	protected <T> void set(StyleAttribute<T> attr, String groupName, Class<? extends MuisElement> type, StateExpression exp, T value)
		throws IllegalArgumentException {
		if(value == null) {
			clear(attr, groupName, type, exp);
			return;
		}
		if(attr == null)
			throw new NullPointerException("Cannot set the value of a null attribute");
		T value2 = attr.getType().cast(value);
		if(value2 == null)
			throw new ClassCastException(value.getClass().getName() + " instance " + value + " cannot be set for attribute " + attr
				+ " of type " + attr.getType());
		value = value2;
		if(attr.getValidator() != null)
			try {
				attr.getValidator().assertValid(value);
			} catch(org.muis.core.MuisException e) {
				throw new IllegalArgumentException(e.getMessage());
			}
		setValue(attr, groupName, type, exp, value);
	}

	private <T, E extends MuisElement> void setValue(StyleAttribute<T> attr, String groupName, Class<E> type, StateExpression exp, T value) {
		if(value == null)
			value = (T) NULL;
		StyleGroupTypeExpressionValue<E, T> sev = new StyleGroupTypeExpressionValue<E, T>(groupName, type, exp, value);
		StyleValueHolder<T> holder = (StyleValueHolder<T>) theAttributes.get(attr);
		if(holder == null) {
			holder = new StyleValueHolder<>(sev);
			theAttributes.put(attr, holder);
		} else
			holder.set(sev);
		styleChanged(attr, groupName, type, exp, null);
	}

	/**
	 * @see MutableStatefulStyle#clear(StyleAttribute, StateExpression) Implemented to make extensions of this class easily support
	 *      MutableStatefulStyle
	 * @param attr The attribute to clear the value of
	 * @param groupName The name of the group to clear the value for
	 * @param type The type of element to clear the value for
	 * @param exp The state expression to clear the value for
	 */
	protected void clear(StyleAttribute<?> attr, String groupName, Class<? extends MuisElement> type, StateExpression exp) {
		StyleValueHolder<?> holder = theAttributes.get(attr);
		if(holder != null && holder.remove(groupName, type, exp))
			styleChanged(attr, groupName, type, exp, null);
		if(holder.theValues.length == 0)
			theAttributes.remove(attr);
	}

	/**
	 * @param attr The attribute for which a style value has changed
	 * @param groupName The name of the group for which the attribute value changed
	 * @param type The type of element for which the attribute value changed
	 * @param expr The state expression for which a style value has changed
	 * @param root The stateful style in which the style expression changed--may be this style or one of its dependencies or their
	 *            dependencies, etc.
	 */
	protected void styleChanged(StyleAttribute<?> attr, String groupName, Class<? extends MuisElement> type, StateExpression expr,
		StyleSheet root) {
		StyleGroupTypeExpressionEvent<?, ?> newEvent = new StyleGroupTypeExpressionEvent<MuisElement, Object>(root == null ? this : root,
			this, (StyleAttribute<Object>) attr, groupName, (Class<MuisElement>) type, expr);
		for(StyleGroupTypeExpressionListener listener : theListeners)
			listener.eventOccurred(newEvent);
	}

	@Override
	public SimpleStyleSheet clone() {
		SimpleStyleSheet ret;
		try {
			ret = (SimpleStyleSheet) super.clone();
		} catch(CloneNotSupportedException e) {
			throw new IllegalStateException(e);
		}
		ret.theAttributes = new java.util.concurrent.ConcurrentHashMap<>();
		for(java.util.Map.Entry<StyleAttribute<?>, StyleValueHolder<?>> entry : theAttributes.entrySet()) {
			ret.theAttributes.put(entry.getKey(), entry.getValue().clone());
		}
		ret.theListeners = new java.util.concurrent.ConcurrentLinkedQueue<>();
		// Don't add the listeners--ret is a new style
		return ret;
	}

	/** An iterator for style attributes in a style and its dependencies */
	protected static class AttributeIterator implements java.util.Iterator<StyleAttribute<?>> {
		private Iterator<StyleAttribute<?>> theLocalAttribs;

		private final Iterator<StyleAttribute<?>> [] theDependencies;

		private int childIndex;

		private boolean calledNext;

		/**
		 * @param style The style for local attributes
		 * @param dependencies The set of the style's dependencies
		 */
		public AttributeIterator(MuisStyle style, MuisStyle... dependencies) {
			theLocalAttribs = style.localAttributes().iterator();
			theDependencies = new java.util.Iterator[dependencies.length];
			for(int d = 0; d < dependencies.length; d++)
				theDependencies[d] = dependencies[d].iterator();
			childIndex = -1;
		}

		@Override
		public boolean hasNext() {
			calledNext = false;
			if(theLocalAttribs != null) {
				if(theLocalAttribs.hasNext())
					return true;
				else
					theLocalAttribs = null;
			}
			if(childIndex < 0)
				childIndex = 0;
			while(childIndex < theDependencies.length && !theDependencies[childIndex].hasNext())
				childIndex++;
			return childIndex < theDependencies.length;
		}

		@Override
		public StyleAttribute<?> next() {
			if((calledNext && !hasNext()) || childIndex >= theDependencies.length)
				throw new java.util.NoSuchElementException();
			calledNext = true;
			if(theLocalAttribs != null)
				return theLocalAttribs.next();
			return theDependencies[childIndex].next();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Style iterators are immutable");
		}
	}
}
