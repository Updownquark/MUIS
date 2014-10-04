package org.muis.core.style.stateful;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.muis.core.mgr.MuisState;
import org.muis.core.model.MuisDocumentModel.StyleListener;
import org.muis.core.rx.*;
import org.muis.core.style.MuisStyle;
import org.muis.core.style.StyleAttribute;
import org.muis.core.style.StyleAttributeEvent;
import org.muis.core.style.StyleExpressionValue;

import prisms.util.ArrayUtils;

/** Implements the functionality specified by {@link InternallyStatefulStyle} that is not implemented by {@link AbstractStatefulStyle} */
public abstract class AbstractInternallyStatefulStyle extends AbstractStatefulStyle implements InternallyStatefulStyle {
	private MuisState [] theCurrentState;

	private DefaultObservableSet<MuisState> theState;
	private Set<MuisState> theStateController;

	private final StyleListener theDependencyStyleListener;

	/** Creates the style */
	public AbstractInternallyStatefulStyle() {
		theCurrentState = new MuisState[0];
		theState = new DefaultObservableSet<>();
		theStateController = theState.control();
		expressions().act(evt -> {
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
		});
		theDependencyStyleListener = event -> {
			styleChanged(event.getAttribute(), event.getValue(), event.getRootStyle());
		};
	}

	@Override
	public ObservableSet<MuisState> getState() {
		return theState;
	}

	private static Set<MuisState> toSet(MuisState... states) {
		java.util.HashSet<MuisState> ret = new java.util.HashSet<>();
		for(MuisState state : states)
			ret.add(state);
		return java.util.Collections.unmodifiableSet(ret);
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
		theStateController.add(state);
	}

	/**
	 * Removes a state from this style's internal state set, firing appropriate events for style attributes that become active or inactive
	 * consequently
	 *
	 * @param state The state to remove
	 */
	protected void removeState(MuisState state) {
		theStateController.remove(state);
	}

	/**
	 * Sets this style's internal state set and marks it has having an internal state. This method fires appropriate events for style
	 * attributes that become active or inactive as a result of the state set changing
	 *
	 * @param newState The new state set for this style
	 */
	protected void setState(MuisState... newState) {
		theStateController.clear();
		theStateController.addAll(java.util.Arrays.asList(newState));
		// TODO
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
		StateChangedEvent evt = new StateChangedEvent(this, toSet(oldState), toSet(newState));
		theStateController.onNext(evt);
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
	public ObservableList<MuisStyle> getDependencies() {
		return getConditionalDependencies().mapC(depend -> {
			if(depend instanceof InternallyStatefulStyle)
				return (InternallyStatefulStyle) depend;
			else
				return new StatefulStyleSample(depend, theState);
		});
	}

	@Override
	public boolean isSet(StyleAttribute<?> attr) {
		for(StyleExpressionValue<StateExpression, ?> sev : getExpressions(attr))
			if(sev.getExpression() == null || sev.getExpression().matches(theState))
				return true;
		return false;
	}

	@Override
	public <T> ObservableValue<T> getLocal(StyleAttribute<T> attr) {
		return getLocalExpressions(attr).combineC(theState,
			(StyleExpressionValue<StateExpression, T> sev, Observable<MuisState> state) -> {
				if(sev.getExpression() == null || sev.getExpression().matches(theState))
					return sev.getValue();
				return null;
			}).first(attr.getType().getType(), value -> {
			return value != null ? value : null;
		});
	}

	@Override
	public ObservableCollection<StyleAttribute<?>> localAttributes() {
		return allLocal().filterC;
		return () -> {
			final MuisState [] stateCapture = theCurrentState;
			return ArrayUtils.conditionalIterator(allLocal().iterator(), value -> {
				for(StyleExpressionValue<StateExpression, ?> sev : getLocalExpressions(value))
					if(sev.getExpression() == null || sev.getExpression().matches(stateCapture))
						return value;
				return null;
			}, false);
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
		return ArrayUtils.conditionalIterator(allAttrs().iterator(), value -> {
			if(!used.add(value))
				return null;
			for(StyleExpressionValue<StateExpression, ?> sev : getExpressions(value))
				if(sev.getExpression() == null || sev.getExpression().matches(stateCapture))
					return value;
			return null;
		}, false);
	}

	void styleChanged(StyleAttribute<?> attr, Object value, MuisStyle root) {
		StyleAttributeEvent<?> evt = new StyleAttributeEvent<>(null, root, this, (StyleAttribute<Object>) attr, value);
		for(StyleListener listener : theStyleListeners) {
			listener.eventOccurred(evt);
		}
	}
}
