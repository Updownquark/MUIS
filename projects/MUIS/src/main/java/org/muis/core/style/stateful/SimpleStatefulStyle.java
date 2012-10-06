package org.muis.core.style.stateful;

import java.util.Iterator;

import org.muis.core.style.*;

import prisms.util.ArrayUtils;

/** Implements StatefulStyle without dependencies */
public abstract class SimpleStatefulStyle implements StatefulStyle, Cloneable {
	private static final Object NULL = new Object();

	private static java.util.Comparator<StyleExpressionValue<?>> EXPR_COMPARE = new java.util.Comparator<StyleExpressionValue<?>>() {
		@Override
		public int compare(StyleExpressionValue<?> o1, StyleExpressionValue<?> o2) {
			StateExpression exp1 = o1.getExpression();
			StateExpression exp2 = o2.getExpression();
			if(exp1 == null)
				return exp2 == null ? 0 : 1;
			if(exp2 == null)
				return -1;
			return exp2.getPriority() - exp1.getPriority();
		}
	};

	private class StyleValueHolder<T> implements Cloneable {
		StyleExpressionValue<T> [] theValues;

		boolean isSorted;

		StyleValueHolder(StyleExpressionValue<T> value) {
			theValues = new StyleExpressionValue[] {value};
		}

		void add(StyleExpressionValue<T> value) {
			isSorted = false;
			theValues = ArrayUtils.add(theValues, value);
		}

		boolean remove(StateExpression exp) {
			StyleExpressionValue<T> [] values = theValues;
			boolean fireEvent = true;
			for(int i = 0; i < values.length; i++) {
				if(ArrayUtils.equals(exp, values[i].getExpression())) {
					theValues = ArrayUtils.remove(theValues, values[i]);
					return fireEvent;
				} else if(values[i].getExpression() == null || (exp != null && values[i].getExpression().getWhenTrue(exp) > 0))
					fireEvent = false;
			}
			return false;
		}

		StyleExpressionValue<T> [] sort() {
			if(isSorted)
				return theValues;
			StyleExpressionValue<T> [] values = theValues;
			java.util.Arrays.sort(values, EXPR_COMPARE);
			if(values == theValues) {
				theValues = values;
				isSorted = true;
			}
			return values;
		}

		void set(StyleExpressionValue<T> sev) {
			boolean set = false;
			boolean found = true;
			while(found && !set) {
				found = false;
				StyleExpressionValue<T> [] values = theValues;
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

	private java.util.concurrent.ConcurrentLinkedQueue<StyleExpressionListener> theListeners;

	/** Creates a SimpleStatefulStyle */
	protected SimpleStatefulStyle() {
		theAttributes = new java.util.concurrent.ConcurrentHashMap<>();
		theListeners = new java.util.concurrent.ConcurrentLinkedQueue<>();
	}

	@Override
	public Iterable<StyleAttribute<?>> allLocal() {
		return ArrayUtils.immutableIterable(theAttributes.keySet());
	}

	@Override
	public <T> StyleExpressionValue<T> [] getLocalExpressions(StyleAttribute<T> attr) {
		StyleValueHolder<T> holder = (StyleValueHolder<T>) theAttributes.get(attr);
		return holder == null ? new StyleExpressionValue[0] : holder.sort();
	}

	@Override
	public void addListener(StyleExpressionListener listener) {
		if(listener != null)
			theListeners.add(listener);
	}

	@Override
	public void removeListener(StyleExpressionListener listener) {
		theListeners.remove(listener);
	}

	/**
	 * @param <T> The type of the attribute
	 * @see MutableStyle#set(StyleAttribute, Object) Implemented to make extensions of this class easily support MutableStyle
	 * @param attr The attribute to set the value of
	 * @param value The value to set for the attribute
	 * @throws IllegalArgumentException If the given value is invalid for the given attribute
	 */
	protected <T> void set(StyleAttribute<T> attr, T value) throws IllegalArgumentException {
		set(attr, null, value);
	}

	/**
	 * @see MutableStatefulStyle#set(StyleAttribute, StateExpression, Object) Implemented to make extensions of this class easily support
	 *      MutableStatefulStyle
	 * @param <T> The type of the attribute
	 * @param attr The attribute to set the value of
	 * @param exp The state expression for the value to be active for
	 * @param value The value to set for the attribute
	 * @throws IllegalArgumentException If the given value is invalid for the given attribute
	 */
	protected <T> void set(StyleAttribute<T> attr, StateExpression exp, T value) throws IllegalArgumentException {
		if(value == null) {
			clear(attr, exp);
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
		setValue(attr, exp, value);
	}

	private <T> void setValue(StyleAttribute<T> attr, StateExpression exp, T value) {
		if(value == null)
			value = (T) NULL;
		StyleExpressionValue<T> sev = new StyleExpressionValue<T>(exp, value);
		StyleValueHolder<T> holder = (StyleValueHolder<T>) theAttributes.get(attr);
		if(holder == null) {
			holder = new StyleValueHolder<>(sev);
			theAttributes.put(attr, holder);
		} else
			holder.set(sev);
		styleChanged(attr, exp, null);
	}

	/**
	 * @see MutableStyle#clear(StyleAttribute) Implemented to make extensions of this class easily support MutableStyle
	 * @param attr The attribute to clear the value of
	 */
	protected void clear(StyleAttribute<?> attr) {
		clear(attr, null);
	}

	/**
	 * @see MutableStatefulStyle#clear(StyleAttribute, StateExpression) Implemented to make extensions of this class easily support
	 *      MutableStatefulStyle
	 * @param attr The attribute to clear the value of
	 * @param exp The state expression to clear the value for
	 */
	protected void clear(StyleAttribute<?> attr, StateExpression exp) {
		StyleValueHolder<?> holder = theAttributes.get(attr);
		if(holder != null && holder.remove(exp))
			styleChanged(attr, exp, null);
		if(holder.theValues.length == 0)
			theAttributes.remove(attr);
	}

	/**
	 * @param attr The attribute for which a style value has changed
	 * @param expr The state expression for which a style value has changed
	 * @param root The stateful style in which the style expression changed--may be this style or one of its dependencies or their
	 *            dependencies, etc.
	 */
	protected void styleChanged(StyleAttribute<?> attr, StateExpression expr, StatefulStyle root) {
		StyleExpressionEvent<?> newEvent = new StyleExpressionEvent<Object>(root == null ? this : root, this,
			(StyleAttribute<Object>) attr, expr);
		for(StyleExpressionListener listener : theListeners)
			listener.eventOccurred(newEvent);
	}

	@Override
	public SimpleStatefulStyle clone() {
		SimpleStatefulStyle ret;
		try {
			ret = (SimpleStatefulStyle) super.clone();
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
