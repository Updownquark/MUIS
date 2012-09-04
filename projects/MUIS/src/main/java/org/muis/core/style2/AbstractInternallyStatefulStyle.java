package org.muis.core.style2;

import java.util.Iterator;
import java.util.Map;

import org.muis.core.mgr.MuisState;
import org.muis.core.style.StateExpression;
import org.muis.core.style.StyleAttribute;
import org.muis.core.style.StyleExpressionValue;
import org.muis.core.style.StyleListener;

import prisms.util.ArrayUtils;

public class AbstractInternallyStatefulStyle extends AbstractStatefulStyle implements InternallyStatefulStyle {
	private MuisState [] theCurrentState;

	private final java.util.concurrent.ConcurrentLinkedQueue<StyleListener> theStyleListeners;

	public AbstractInternallyStatefulStyle() {
		theCurrentState = new MuisState[0];
		theStyleListeners = new java.util.concurrent.ConcurrentLinkedQueue<>();
	}

	/**
	 * Adds a state to this style's internal state set, firing appropriate events for style attributes that become active or inactive
	 * consequently
	 *
	 * @param state The state to add
	 */
	protected void addState(MuisState state) {
		if(ArrayUtils.contains(theCurrentState, state))
			return;
		setState(ArrayUtils.add(theCurrentState, state));
	}

	/**
	 * Removes a state from this style's internal state set, firing appropriate events for style attributes that become active or inactive
	 * consequently
	 *
	 * @param state The state to remove
	 */
	protected void removeState(MuisState state) {
		MuisState [] newState = ArrayUtils.remove(theCurrentState, state);
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
	protected void setState(MuisState... newState) {
		MuisState [] oldState = theCurrentState;
		StatefulStyle [] deps = getStatefulDependencies();
		theCurrentState = newState;
		MuisStyle forNewState = new StatefulStyleSample(this, newState);
		Map<StyleAttribute<?>, Object> newValues = new java.util.HashMap<>();
		for(StyleAttribute<?> attr : allLocal()) {
			for(StyleExpressionValue<?> sev : getLocalExpressions(attr)) {
				StateExpression expr = sev.getExpression();
				if(expr == null)
					continue;
				boolean oldMatch = expr.matches(oldState);
				boolean newMatch = expr.matches(newState);
				if(oldMatch == newMatch)
					continue;
				if(newMatch)
					newValues.put(attr, sev.getValue());
				else
					newValues.put(attr, forNewState.get(attr));
				break;
			}
		}
		for(StatefulStyle dep : deps)
			checkValues(dep, oldState, newState, forNewState, newValues);
		for(Map.Entry<StyleAttribute<?>, Object> value : newValues.entrySet())
			styleChanged(value.getKey(), value.getValue(), null);
	}

	private void checkValues(StatefulStyle dep, MuisState [] oldState, MuisState [] newState, MuisStyle forNewState,
		Map<StyleAttribute<?>, Object> newValues) {
		if(dep instanceof InternallyStatefulStyle)
			return;
		for(StyleAttribute<?> attr : dep.allLocal()) {
			for(StyleExpressionValue<?> sev : getLocalExpressions(attr)) {
				if(newValues.containsKey(attr) || forNewState.isSet(attr))
					continue;
				StateExpression expr = sev.getExpression();
				if(expr == null)
					continue;
				boolean oldMatch = expr.matches(oldState);
				boolean newMatch = expr.matches(newState);
				if(oldMatch == newMatch)
					continue;
				if(newMatch)
					newValues.put(attr, sev.getValue());
				else
					newValues.put(attr, forNewState.get(attr));
				break;
			}
		}
	}

	@Override
	public boolean isSet(StyleAttribute<?> attr) {
		for(StyleExpressionValue<?> sev : getExpressions(attr))
			if(sev.getExpression() == null || sev.getExpression().matches(theCurrentState))
				return true;
		return false;
	}

	@Override
	public boolean isSetDeep(StyleAttribute<?> attr) {
		if(isSet(attr))
			return true;
		for(StatefulStyle dep : getStatefulDependencies()) {
			if(dep instanceof InternallyStatefulStyle) {
				if(((InternallyStatefulStyle) dep).isSetDeep(attr))
					return true;
			} else {
				if(new StatefulStyleSample(dep, theCurrentState).isSetDeep(attr))
					return true;
			}
		}
		return false;
	}

	@Override
	public <T> T getLocal(StyleAttribute<T> attr) {
		for(StyleExpressionValue<T> value : getExpressions(attr))
			if(value.getExpression() == null || value.getExpression().matches(theCurrentState))
				return value.getValue();
		return null;
	}

	@Override
	public Iterable<StyleAttribute<?>> localAttributes() {
		return new Iterable<StyleAttribute<?>>() {
			@Override
			public Iterator<StyleAttribute<?>> iterator() {
				final MuisState [] stateCapture = theCurrentState;
				return ArrayUtils.conditionalIterator(allLocal().iterator(),
					new ArrayUtils.Accepter<StyleAttribute<?>, StyleAttribute<?>>() {
						@Override
						public StyleAttribute<?> accept(StyleAttribute<?> value) {
							for(StyleExpressionValue<?> sev : getLocalExpressions(value))
								if(sev.getExpression() == null || sev.getExpression().matches(stateCapture))
									return value;
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
		for(StatefulStyle dep : getStatefulDependencies()) {
			if(dep instanceof InternallyStatefulStyle) {
				if(((InternallyStatefulStyle) dep).isSetDeep(attr))
					return ((InternallyStatefulStyle) dep).get(attr);
			} else {
				StatefulStyleSample sample = new StatefulStyleSample(dep, theCurrentState);
				if(sample.isSetDeep(attr))
					return sample.get(attr);
			}
		}
		return attr.getDefault();
	}

	@Override
	public void addListener(StyleListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeListener(StyleListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public Iterator<StyleAttribute<?>> iterator() {
		final MuisState [] stateCapture = theCurrentState;
		final java.util.HashSet<StyleAttribute<?>> used=new java.util.HashSet<>();
		return ArrayUtils.conditionalIterator(allAttrs().iterator(), new ArrayUtils.Accepter<StyleAttribute<?>, StyleAttribute<?>>() {
			@Override
			public StyleAttribute<?> accept(StyleAttribute<?> value) {
				if(!used.add(value))
					return null;
				for(StyleExpressionValue<?> sev : getExpressions(value))
					if(sev.getExpression() == null || sev.getExpression().matches(stateCapture))
						return value;
				return null;
			}
		}, false);
	}

	void styleChanged(StyleAttribute<?> attr, Object value, MuisStyle root) {
	}
}
