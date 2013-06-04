package org.muis.core.style.stateful;

import java.util.Iterator;
import java.util.Map;

import org.muis.core.mgr.MuisState;
import org.muis.core.style.*;

import prisms.util.ArrayUtils;

/** Implements the functionality specified by {@link InternallyStatefulStyle} that is not implemented by {@link AbstractStatefulStyle} */
public abstract class AbstractInternallyStatefulStyle extends AbstractStatefulStyle implements InternallyStatefulStyle {
	private MuisState [] theCurrentState;

	private final java.util.concurrent.ConcurrentLinkedQueue<StyleListener> theStyleListeners;

	private final StyleListener theDependencyStyleListener;

	/** Creates the style */
	public AbstractInternallyStatefulStyle() {
		theCurrentState = new MuisState[0];
		theStyleListeners = new java.util.concurrent.ConcurrentLinkedQueue<>();
		addListener(new StyleExpressionListener<StatefulStyle, StateExpression>() {
			@Override
			public void eventOccurred(StyleExpressionEvent<StatefulStyle, StateExpression, ?> evt) {
				if(evt.getExpression() != null && !evt.getExpression().matches(theCurrentState))
					return;
				for(StyleExpressionValue<StateExpression, ?> sev : getExpressions(evt.getAttribute())) {
					if(sev.getExpression() == evt.getExpression())
						break;
					if(sev.getExpression() == null || sev.getExpression().matches(theCurrentState))
						return;
				}
				MuisStyle root;
				if(evt.getRootStyle() instanceof MuisStyle)
					root = (MuisStyle) evt.getRootStyle();
				else
					root = new StatefulStyleSample(evt.getRootStyle(), theCurrentState);
				styleChanged(evt.getAttribute(), get(evt.getAttribute()), root);
			}
		});
		theDependencyStyleListener = new StyleListener() {
			@Override
			public void eventOccurred(StyleAttributeEvent<?> event) {
				styleChanged(event.getAttribute(), event.getNewValue(), event.getRootStyle());
			}
		};
	}

	@Override
	protected void addDependency(StatefulStyle depend, StatefulStyle after) {
		super.addDependency(depend, after);
		if(depend instanceof InternallyStatefulStyle)
			((InternallyStatefulStyle) depend).addListener(theDependencyStyleListener);
	}

	@Override
	protected void addDependency(StatefulStyle depend) {
		super.addDependency(depend);
		if(depend instanceof InternallyStatefulStyle)
			((InternallyStatefulStyle) depend).addListener(theDependencyStyleListener);
	}

	@Override
	protected void removeDependency(StatefulStyle depend) {
		super.removeDependency(depend);
		if(depend instanceof InternallyStatefulStyle)
			((InternallyStatefulStyle) depend).removeListener(theDependencyStyleListener);
	}

	@Override
	protected void replaceDependency(StatefulStyle toReplace, StatefulStyle depend) {
		super.replaceDependency(toReplace, depend);
		if(toReplace instanceof InternallyStatefulStyle)
			((InternallyStatefulStyle) toReplace).removeListener(theDependencyStyleListener);
		if(depend instanceof InternallyStatefulStyle)
			((InternallyStatefulStyle) depend).addListener(theDependencyStyleListener);
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
		StatefulStyle [] deps = getConditionalDependencies();
		theCurrentState = newState;
		MuisStyle forNewState = new StatefulStyleSample(this, newState);
		Map<StyleAttribute<?>, Object> newValues = new java.util.HashMap<>();
		for(StyleAttribute<?> attr : allLocal()) {
			for(StyleExpressionValue<StateExpression, ?> sev : getLocalExpressions(attr)) {
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
		for(StyleAttribute<?> attr : dep.allAttrs()) {
			for(StyleExpressionValue<StateExpression, ?> sev : getExpressions(attr)) {
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
	public MuisStyle [] getDependencies() {
		StatefulStyle [] depends = getConditionalDependencies();
		MuisState [] state = theCurrentState;
		MuisStyle [] ret = new MuisStyle[depends.length];
		for(int i = 0; i < ret.length; i++) {
			if(depends[i] instanceof InternallyStatefulStyle)
				ret[i] = (InternallyStatefulStyle) depends[i];
			else
				ret[i] = new StatefulStyleSample(depends[i], state);
		}
		return ret;
	}

	@Override
	public boolean isSet(StyleAttribute<?> attr) {
		for(StyleExpressionValue<StateExpression, ?> sev : getExpressions(attr))
			if(sev.getExpression() == null || sev.getExpression().matches(theCurrentState))
				return true;
		return false;
	}

	@Override
	public boolean isSetDeep(StyleAttribute<?> attr) {
		if(isSet(attr))
			return true;
		for(StatefulStyle dep : getConditionalDependencies()) {
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
		for(StyleExpressionValue<StateExpression, T> value : getLocalExpressions(attr))
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
							for(StyleExpressionValue<StateExpression, ?> sev : getLocalExpressions(value))
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
		for(StyleExpressionValue<StateExpression, T> sev : getExpressions(attr)) {
			if(sev.getExpression() == null || sev.getExpression().matches(theCurrentState))
				return sev.getValue();
		}
		return attr.getDefault();
	}

	@Override
	public void addListener(StyleListener listener) {
		if(listener != null)
			theStyleListeners.add(listener);
	}

	@Override
	public void removeListener(StyleListener listener) {
		theStyleListeners.remove(listener);
	}

	@Override
	public Iterator<StyleAttribute<?>> iterator() {
		final MuisState [] stateCapture = theCurrentState;
		final java.util.HashSet<StyleAttribute<?>> used = new java.util.HashSet<>();
		return ArrayUtils.conditionalIterator(allAttrs().iterator(), new ArrayUtils.Accepter<StyleAttribute<?>, StyleAttribute<?>>() {
			@Override
			public StyleAttribute<?> accept(StyleAttribute<?> value) {
				if(!used.add(value))
					return null;
				for(StyleExpressionValue<StateExpression, ?> sev : getExpressions(value))
					if(sev.getExpression() == null || sev.getExpression().matches(stateCapture))
						return value;
				return null;
			}
		}, false);
	}

	void styleChanged(StyleAttribute<?> attr, Object value, MuisStyle root) {
		StyleAttributeEvent<?> evt = new StyleAttributeEvent<>(root, this, (StyleAttribute<Object>) attr, value);
		for(StyleListener listener : theStyleListeners) {
			listener.eventOccurred(evt);
		}
	}
}
