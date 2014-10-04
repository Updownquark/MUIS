package org.muis.core.style.stateful;

import java.util.Set;

import org.muis.core.mgr.MuisState;
import org.muis.core.rx.*;
import org.muis.core.style.MuisStyle;
import org.muis.core.style.StyleAttribute;
import org.muis.core.style.StyleExpressionValue;

/** Implements the functionality specified by {@link InternallyStatefulStyle} that is not implemented by {@link AbstractStatefulStyle} */
public abstract class AbstractInternallyStatefulStyle extends AbstractStatefulStyle implements InternallyStatefulStyle {
	private DefaultObservableSet<MuisState> theState;
	private Set<MuisState> theStateController;

	/** Creates the style */
	public AbstractInternallyStatefulStyle() {
		theState = new DefaultObservableSet<>();
		theStateController = theState.control(null);
	}

	@Override
	public ObservableSet<MuisState> getState() {
		return theState;
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
	public ObservableSet<StyleAttribute<?>> localAttributes() {
		return allLocal().combineC(theState.changes(), (StyleAttribute<?> attr, Void v) -> attr).filterMapC(attr -> {
			for(StyleExpressionValue<StateExpression, ?> sev : getLocalExpressions(attr))
				if(sev.getExpression() == null || sev.getExpression().matches(theState))
					return attr;
			return null;
		});
	}
}
