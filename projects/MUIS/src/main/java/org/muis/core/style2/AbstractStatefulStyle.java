package org.muis.core.style2;

import java.util.Iterator;

import org.muis.core.style.StateExpression;
import org.muis.core.style.StyleAttribute;
import org.muis.core.style.StyleExpressionValue;

import prisms.util.ArrayUtils;

/** A more full partial implementation of MuisStyle */
public abstract class AbstractStatefulStyle implements StatefulStyle {
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

	private class StyleValueHolder<T> {
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
	}

	private final java.util.concurrent.ConcurrentHashMap<StyleAttribute<?>, StyleValueHolder<?>> theAttributes;

	private final java.util.concurrent.ConcurrentLinkedQueue<StyleExpressionListener> theListeners;

	private StatefulStyle [] theDependencies;

	private final StyleExpressionListener theDependencyListener;

	/**
	 * Creates an abstract MUIS style
	 *
	 * @param dependencies The initial set of dependencies for this style
	 */
	public AbstractStatefulStyle(StatefulStyle... dependencies) {
		theAttributes = new java.util.concurrent.ConcurrentHashMap<>();
		theListeners = new java.util.concurrent.ConcurrentLinkedQueue<>();
		theDependencies = dependencies;
		theDependencyListener = new StyleExpressionListener() {
			@Override
			public void eventOccurred(StyleExpressionEvent<?> event) {
				if(isSet(AbstractStatefulStyle.this, event.getAttribute(), event.getExpression()))
					return;
				for(StyleExpressionValue<?> expr : getExpressions(event.getAttribute()))
					if(expr.getExpression() == null
						|| (event.getExpression() != null && expr.getExpression().getWhenTrue(event.getExpression()) > 0))
						return;
				int idx = ArrayUtils.indexOf(theDependencies, event.getRootStyle());
				if(idx < 0)
					return;
				for(int i = 0; i < idx; i++)
					if(isSetDeep(theDependencies[i], event.getAttribute(), event.getExpression()))
						return;
				styleChanged(event.getAttribute(), event.getExpression(), event.getRootStyle());
			}
		};
		for(StatefulStyle dep : theDependencies)
			dep.addListener(theDependencyListener);
	}

	private static boolean isSet(StatefulStyle style, StyleAttribute<?> attr, StateExpression expr) {
		for(StyleExpressionValue<?> sev : style.getExpressions(attr))
			if(sev.getExpression() == null || (expr != null && sev.getExpression().getWhenTrue(expr) > 0))
				return true;
		return false;
	}

	private static boolean isSetDeep(StatefulStyle style, StyleAttribute<?> attr, StateExpression expr) {
		if(isSet(style, attr, expr))
			return true;
		for(StatefulStyle depend : style.getStatefulDependencies())
			if(isSet(depend, attr, expr))
				return true;
		return false;
	}

	@Override
	public final StatefulStyle [] getStatefulDependencies() {
		return theDependencies;
	}

	/**
	 * @param depend The dependency to add
	 * @param after The dependency to add the new dependency after, or null to add it as the first dependency
	 */
	protected void addDependency(AbstractStatefulStyle depend, AbstractStatefulStyle after) {
		int idx;
		if(after == null)
			idx = 0;
		else
			idx = ArrayUtils.indexOf(theDependencies, after);
		if(idx < 0)
			throw new IllegalArgumentException(after + " is not a dependency of " + this);
		theDependencies = ArrayUtils.add(theDependencies, depend, idx);
		depend.addListener(theDependencyListener);
		for(StyleAttribute<?> attr : depend.allAttrs()) {
			for(StyleExpressionValue<?> sev : depend.getExpressions(attr)) {
				if(isSet(this, attr, sev.getExpression()))
					continue;
				for(int i = 0; i < idx; i++)
					if(isSetDeep(theDependencies[i], attr, sev.getExpression()))
						continue;
				styleChanged(attr, sev.getExpression(), null);
			}
		}
	}

	/**
	 * Adds a dependency as the last dependency
	 *
	 * @param depend The dependency to add
	 */
	protected void addDependency(AbstractStatefulStyle depend) {
		theDependencies = ArrayUtils.add(theDependencies, depend);
		depend.addListener(theDependencyListener);
		for(StyleAttribute<?> attr : depend.allAttrs()) {
			for(StyleExpressionValue<?> sev : depend.getExpressions(attr)) {
				if(isSet(this, attr, sev.getExpression()))
					continue;
				for(int i = 0; i < theDependencies.length - 1; i++)
					if(isSetDeep(theDependencies[i], attr, sev.getExpression()))
						continue;
				styleChanged(attr, sev.getExpression(), null);
			}
		}
	}

	/** @param depend The dependency to remove */
	protected void removeDependency(AbstractStatefulStyle depend) {
		int idx = ArrayUtils.indexOf(theDependencies, depend);
		if(idx < 0)
			return;
		depend.removeListener(theDependencyListener);
		theDependencies = ArrayUtils.remove(theDependencies, idx);
		for(StyleAttribute<?> attr : depend.allAttrs()) {
			for(StyleExpressionValue<?> sev : depend.getExpressions(attr)) {
				if(isSet(this, attr, sev.getExpression()))
					continue;
				for(int i = 0; i < theDependencies.length - 1; i++)
					if(isSetDeep(theDependencies[i], attr, sev.getExpression()))
						continue;
				styleChanged(attr, sev.getExpression(), null);
			}
		}
	}

	/**
	 * @param toReplace The dependency to replace
	 * @param depend The dependency to add in place of the given dependency to replace
	 */
	protected void replaceDependency(AbstractStatefulStyle toReplace, AbstractStatefulStyle depend) {
		int idx = ArrayUtils.indexOf(theDependencies, depend);
		if(idx < 0)
			throw new IllegalArgumentException(toReplace + " is not a dependency of " + this);
		toReplace.removeListener(theDependencyListener);
		theDependencies[idx] = depend;
		java.util.HashSet<prisms.util.DualKey<StyleAttribute<Object>, StateExpression>> attrs = new java.util.HashSet<>();
		for(StyleAttribute<?> attr : toReplace.allAttrs()) {
			for(StyleExpressionValue<?> sev : depend.getExpressions(attr)) {
				if(isSet(this, attr, sev.getExpression()))
					continue;
				for(int i = 0; i < theDependencies.length - 1; i++)
					if(isSetDeep(theDependencies[i], attr, sev.getExpression()))
						continue;
				attrs.add(new prisms.util.DualKey<>((StyleAttribute<Object>) attr, sev.getExpression()));
			}
		}
		depend.addListener(theDependencyListener);
		for(StyleAttribute<?> attr : depend.allAttrs()) {
			for(StyleExpressionValue<?> sev : depend.getExpressions(attr)) {
				if(isSet(this, attr, sev.getExpression()))
					continue;
				for(int i = 0; i < theDependencies.length - 1; i++)
					if(isSetDeep(theDependencies[i], attr, sev.getExpression()))
						continue;
				styleChanged(attr, sev.getExpression(), null);
			}
		}
		for(prisms.util.DualKey<StyleAttribute<Object>, StateExpression> attr : attrs)
			styleChanged(attr.getKey1(), attr.getKey2(), null);
	}

	@Override
	public <T> StyleExpressionValue<T> [] getLocalExpressions(StyleAttribute<T> attr) {
		StyleValueHolder<T> holder = (StyleValueHolder<T>) theAttributes.get(attr);
		return holder == null ? new StyleExpressionValue[0] : holder.sort();
	}

	@Override
	public <T> StyleExpressionValue<T> [] getExpressions(StyleAttribute<T> attr) {
		StyleExpressionValue<T> [] ret;
		StyleValueHolder<T> holder = (StyleValueHolder<T>) theAttributes.get(attr);
		ret = holder == null ? new StyleExpressionValue[0] : holder.sort();
		for(StatefulStyle dep : theDependencies) {
			StyleExpressionValue<T> [] depRet = dep.getExpressions(attr);
			if(depRet.length > 0)
				ret = ArrayUtils.addAll(ret, depRet);
		}
		return ret;
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

	@Override
	public Iterable<StyleAttribute<?>> allLocal() {
		return ArrayUtils.immutableIterable(theAttributes.keySet());
	}

	@Override
	public Iterable<StyleAttribute<?>> allAttrs() {
		StatefulStyle [] deps = theDependencies;
		Iterable<StyleAttribute<?>> [] iters = new Iterable[deps.length + 1];
		iters[0] = allLocal();
		for(int i = 0; i < deps.length; i++)
			iters[i + 1] = deps[i].allAttrs();
		return ArrayUtils.iterable(iters);
	}

	void styleChanged(StyleAttribute<?> attr, StateExpression expr, StatefulStyle root) {
		StyleExpressionEvent<?> newEvent = new StyleExpressionEvent<Object>(root == null ? this : root, this,
			(StyleAttribute<Object>) attr, expr);
		for(StyleExpressionListener listener : theListeners)
			listener.eventOccurred(newEvent);
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
