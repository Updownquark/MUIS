package org.muis.core.style;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

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
			return exp2.getComplexity() - exp1.getComplexity();
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

		boolean remove(StateExpression exp, String [] state) {
			StyleExpressionValue<T> [] values = theValues;
			boolean fireEvent = true;
			for(int i = 0; i < values.length; i++) {
				if(ArrayUtils.equals(exp, values[i].getExpression())) {
					theValues = ArrayUtils.remove(theValues, values[i]);
					return fireEvent;
				} else if(values[i].getExpression() == null || values[i].getExpression().matches(state))
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

	private final java.util.concurrent.ConcurrentLinkedQueue<StyleListener> theListeners;

	private AbstractStatefulStyle [] theDependencies;

	private String [] theCurrentState;

	private boolean hasInternalState;

	private final StyleListener theDependencyListener;

	/**
	 * Creates an abstract MUIS style
	 *
	 * @param dependencies The initial set of dependencies for this style
	 */
	public AbstractStatefulStyle(AbstractStatefulStyle... dependencies) {
		theAttributes = new java.util.concurrent.ConcurrentHashMap<>();
		theListeners = new java.util.concurrent.ConcurrentLinkedQueue<>();
		theDependencies = dependencies;
		theDependencyListener = new StyleListener() {
			@Override
			public void eventOccurred(StyleAttributeEvent<?> event) {
				if(isSet(event.getAttribute()))
					return;
				int idx = ArrayUtils.indexOf(theDependencies, event.getRootStyle());
				if(idx < 0)
					return;
				for(int i = 0; i < idx; i++)
					if(theDependencies[i].isSetDeep(event.getAttribute()))
						return;
				Object value = event.getRootStyle().isSetDeep(event.getAttribute()) ? event.getValue() : get(event.getAttribute());
				styleChanged(event.getAttribute(), value, event.getRootStyle());
			}
		};
		for(AbstractStatefulStyle dep : theDependencies)
			dep.addListener(theDependencyListener);
		theCurrentState = new String[0];
	}

	@Override
	public final MuisStyle [] getDependencies() {
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
		for(StyleAttribute<?> attr : depend) {
			if(isSet(attr))
				continue;
			for(int i = 0; i < idx; i++)
				if(theDependencies[i].isSetDeep(attr))
					continue;
			styleChanged(attr, depend.get(attr), null);
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
		for(StyleAttribute<?> attr : depend) {
			if(isSet(attr))
				continue;
			for(int i = 0; i < theDependencies.length - 1; i++)
				if(theDependencies[i].isSetDeep(attr))
					continue;
			styleChanged(attr, depend.get(attr), null);
		}
	}

	/** @param depend The dependency to remove */
	protected void removeDependency(AbstractStatefulStyle depend) {
		int idx = ArrayUtils.indexOf(theDependencies, depend);
		if(idx < 0)
			return;
		depend.removeListener(theDependencyListener);
		theDependencies = ArrayUtils.remove(theDependencies, idx);
		for(StyleAttribute<?> attr : depend) {
			if(isSet(attr))
				continue;
			for(int i = 0; i < idx; i++)
				if(theDependencies[i].isSetDeep(attr))
					continue;
			styleChanged(attr, get(attr), null);
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
		java.util.HashSet<StyleAttribute<?>> attrs = new java.util.HashSet<>();
		for(StyleAttribute<?> attr : toReplace) {
			if(isSet(attr))
				continue;
			for(int i = 0; i < idx; i++)
				if(theDependencies[i].isSetDeep(attr))
					continue;
			attrs.add(attr);
		}
		depend.addListener(theDependencyListener);
		for(StyleAttribute<?> attr : depend) {
			if(isSet(attr))
				continue;
			for(int i = 0; i < idx; i++)
				if(theDependencies[i].isSetDeep(attr))
					continue;
			attrs.remove(attr);
			styleChanged(attr, depend.get(attr), null);
		}
		for(StyleAttribute<?> attr : attrs)
			styleChanged(attr, get(attr), null);
	}

	/**
	 * Adds a state to this style's internal state set, firing appropriate events for style attributes that become active or inactive
	 * consequently
	 *
	 * @param state The state to add
	 */
	protected void addState(String state) {
		String [] newState = ArrayUtils.add(theCurrentState, state);
		if(newState == theCurrentState)
			return;
		setState(newState);
	}

	/**
	 * Removes a state from this style's internal state set, firing appropriate events for style attributes that become active or inactive
	 * consequently
	 *
	 * @param state The state to remove
	 */
	protected void removeState(String state) {
		String [] newState = ArrayUtils.remove(theCurrentState, state);
		if(newState == theCurrentState)
			return;
		setState(newState);
	}

	/**
	 * Sets this style's internal state set and marks it has having an internal state. This method fires appropriate events for style
	 * attributes that become active or inactive as a result of the state set changing
	 *
	 * @param newState The new state set for this style
	 */
	protected void setState(String... newState) {
		hasInternalState = true;
		String [] oldState = theCurrentState;
		MuisStyle [] deps = theDependencies;
		theCurrentState = newState;
		MuisStyle forNewState = getStyleFor(newState);
		Map<StyleAttribute<?>, Object> newValues = new java.util.HashMap<>();
		for(Map.Entry<StyleAttribute<?>, StyleValueHolder<?>> entry : theAttributes.entrySet()) {
			for(StyleExpressionValue<?> sev : entry.getValue().sort()) {
				StateExpression expr = sev.getExpression();
				if(expr == null)
					continue;
				boolean oldMatch = expr.matches(oldState);
				boolean newMatch = expr.matches(newState);
				if(oldMatch == newMatch)
					continue;
				if(newMatch)
					newValues.put(entry.getKey(), sev.getValue());
				else
					newValues.put(entry.getKey(), forNewState.get(entry.getKey()));
				break;
			}
		}
		Map<StyleAttribute<?>, MuisStyle> roots = new java.util.HashMap<>();
		for(MuisStyle dep : deps) {
			if(!(dep instanceof AbstractStatefulStyle) || ((AbstractStatefulStyle) dep).hasInternalState)
				continue;
			for(Map.Entry<StyleAttribute<?>, StyleValueHolder<?>> entry : ((AbstractStatefulStyle) dep).theAttributes.entrySet()) {
				for(StyleExpressionValue<?> sev : entry.getValue().sort()) {
					if(newValues.containsKey(entry.getKey()) || forNewState.isSet(entry.getKey()))
						continue;
					StateExpression expr = sev.getExpression();
					if(expr == null)
						continue;
					boolean oldMatch = expr.matches(oldState);
					boolean newMatch = expr.matches(newState);
					if(oldMatch == newMatch)
						continue;
					roots.put(entry.getKey(), dep);
					if(newMatch)
						newValues.put(entry.getKey(), sev.getValue());
					else
						newValues.put(entry.getKey(), forNewState.get(entry.getKey()));
					break;
				}
			}
		}
		for(Map.Entry<StyleAttribute<?>, Object> value : newValues.entrySet()) {
			MuisStyle root = roots.get(value.getKey());
			if(root == null)
				root = this;
			styleChanged(value.getKey(), value.getValue(), root);
		}
	}

	@Override
	public boolean isSet(StyleAttribute<?> attr) {
		StyleValueHolder<?> holder = theAttributes.get(attr);
		if(holder == null)
			return false;
		String [] state = theCurrentState;
		for(StyleExpressionValue<?> value : holder.theValues)
			if(value.getExpression() == null || value.getExpression().matches(state))
				return true;
		return false;
	}

	@Override
	public boolean isSetDeep(StyleAttribute<?> attr) {
		if(isSet(attr))
			return true;
		for(MuisStyle dep : theDependencies)
			if(dep.isSetDeep(attr))
				return true;
		return false;
	}

	@Override
	public <T> T getLocal(StyleAttribute<T> attr) {
		StyleValueHolder<T> holder = (StyleValueHolder<T>) theAttributes.get(attr);
		if(holder == null)
			return null;
		String [] state = theCurrentState;
		for(StyleExpressionValue<T> value : holder.sort())
			if(value.getExpression() == null || value.getExpression().matches(state))
				return value.getValue();
		return null;
	}

	@Override
	public <T> T get(StyleAttribute<T> attr) {
		T ret = getLocal(attr);
		if(ret != null)
			return ret;
		for(MuisStyle dep : getDependencies()) {
			ret = dep.get(attr);
			if(ret != null)
				return ret;
		}
		return attr.getDefault();
	}

	@Override
	public MuisStyle getStyleFor(final String... state) {
		return new MuisStyle() {
			@Override
			public Iterator<StyleAttribute<?>> iterator() {
				MuisStyle [] deps = theDependencies;
				Iterator<StyleAttribute<?>> [] iters = new Iterator[deps.length + 1];
				iters[0] = localAttributes().iterator();
				for(int i = 0; i < deps.length; i++)
					iters[i + 1] = (deps[i] instanceof StatefulStyle ? ((StatefulStyle) deps[i]).getStyleFor(state) : deps[i]).iterator();
				return ArrayUtils.iterator(iters);
			}

			@Override
			public MuisStyle [] getDependencies() {
				return getDependencies();
			}

			@Override
			public boolean isSet(StyleAttribute<?> attr) {
				StyleValueHolder<?> holder = theAttributes.get(attr);
				if(holder == null)
					return false;
				for(StyleExpressionValue<?> value : holder.theValues)
					if(value.getExpression() == null || value.getExpression().matches(state))
						return true;
				return false;
			}

			@Override
			public boolean isSetDeep(StyleAttribute<?> attr) {
				if(isSet(attr))
					return true;
				for(MuisStyle dep : theDependencies)
					if(dep instanceof StatefulStyle) {
						if(((StatefulStyle) dep).getStyleFor(state).isSet(attr))
							return true;
					} else if(dep.isSetDeep(attr))
						return true;
				return false;
			}

			@Override
			public <T> T getLocal(StyleAttribute<T> attr) {
				StyleValueHolder<T> holder = (StyleValueHolder<T>) theAttributes.get(attr);
				if(holder == null)
					return null;
				for(StyleExpressionValue<T> value : holder.sort())
					if(value.getExpression() == null || value.getExpression().matches(state))
						return value.getValue();
				return null;
			}

			@Override
			public Iterable<StyleAttribute<?>> localAttributes() {
				return new Iterable<StyleAttribute<?>>() {
					@Override
					public Iterator<StyleAttribute<?>> iterator() {
						return ArrayUtils.conditionalIterator(theAttributes.entrySet().iterator(),
							new ArrayUtils.Accepter<java.util.Map.Entry<StyleAttribute<?>, StyleValueHolder<?>>, StyleAttribute<?>>() {
								@Override
								public StyleAttribute<?> accept(Entry<StyleAttribute<?>, StyleValueHolder<?>> value) {
									for(StyleExpressionValue<?> sev : value.getValue().theValues)
										if(sev.getExpression() == null || sev.getExpression().matches(state))
											return value.getKey();
									return null;
								}
							}, false);
					}
				};
			}

			@Override
			public <T> T get(StyleAttribute<T> attr) {
				T ret = getLocal(attr);
				if(ret != null)
					return ret;
				for(MuisStyle dep : getDependencies()) {
					ret = dep.get(attr);
					if(ret != null)
						return ret;
				}
				return attr.getDefault();
			}
		};
	}

	@Override
	public <T> StyleExpressionValue<T> [] getExpressions(StyleAttribute<T> attr) {
		StyleValueHolder<T> holder = (StyleValueHolder<T>) theAttributes.get(attr);
		return holder == null ? new StyleExpressionValue[0] : holder.sort();
	}

	/**
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
		String [] state = theCurrentState;
		boolean fireEvent = false;
		if(sev.getExpression() == null || sev.getExpression().matches(state)) {
			for(StyleExpressionValue<T> v : holder.sort()) {
				if(v == sev) {
					fireEvent = true;
					break;
				} else if(v.getExpression() == null || v.getExpression().matches(state))
					break;
			}
		}
		if(fireEvent)
			styleChanged(attr, value, null);
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
		if(holder != null && holder.remove(exp, theCurrentState))
			styleChanged(attr, get(attr), null);
	}

	@Override
	public Iterable<StyleAttribute<?>> allLocal() {
		return ArrayUtils.immutableIterable(theAttributes.keySet());
	}

	@Override
	public Iterable<StyleAttribute<?>> allAttrs() {
		MuisStyle [] deps = theDependencies;
		Iterable<StyleAttribute<?>> [] iters = new Iterable[deps.length + 1];
		iters[0] = allLocal();
		for(int i = 0; i < deps.length; i++)
			iters[i + 1] = deps[i] instanceof StatefulStyle ? ((StatefulStyle) deps[i]).allAttrs() : deps[i];
		return ArrayUtils.iterable(iters);
	}

	@Override
	public Iterable<StyleAttribute<?>> localAttributes() {
		return new Iterable<StyleAttribute<?>>() {
			@Override
			public Iterator<StyleAttribute<?>> iterator() {
				final String [] state = theCurrentState;
				return ArrayUtils.conditionalIterator(theAttributes.entrySet().iterator(),
					new ArrayUtils.Accepter<java.util.Map.Entry<StyleAttribute<?>, StyleValueHolder<?>>, StyleAttribute<?>>() {
						@Override
						public StyleAttribute<?> accept(Entry<StyleAttribute<?>, StyleValueHolder<?>> value) {
							for(StyleExpressionValue<?> sev : value.getValue().theValues)
								if(sev.getExpression() == null || sev.getExpression().matches(state))
									return value.getKey();
							return null;
						}
					}, false);
			}
		};
	}

	@Override
	public Iterator<StyleAttribute<?>> iterator() {
		return new AttributeIterator(this, getDependencies());
	}

	void styleChanged(StyleAttribute<?> attr, Object value, MuisStyle root) {
		StyleAttributeEvent<Object> event = new StyleAttributeEvent<Object>(root == null ? this : root, this,
			(StyleAttribute<Object>) attr, value);
		for(StyleListener listener : theListeners)
			listener.eventOccurred(event);
	}

	/** @param listener The listener to be notified when the effective value of any style attribute in this style changes */
	public void addListener(StyleListener listener) {
		if(listener != null)
			theListeners.add(listener);
	}

	/** @param listener The listener to remove */
	public void removeListener(StyleListener listener) {
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
