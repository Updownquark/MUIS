package org.muis.core.style.stateful;

import java.util.Set;

import org.muis.core.mgr.MuisState;
import org.muis.core.rx.DefaultObservableSet;
import org.muis.core.rx.ObservableList;
import org.muis.core.rx.ObservableSet;
import org.muis.core.rx.ObservableValue;
import org.muis.core.style.MuisStyle;
import org.muis.core.style.StyleAttribute;
import org.muis.core.style.StyleExpressionValue;

import prisms.lang.Type;

/** Implements the functionality specified by {@link InternallyStatefulStyle} that is not implemented by {@link AbstractStatefulStyle} */
public abstract class AbstractInternallyStatefulStyle extends AbstractStatefulStyle implements InternallyStatefulStyle {
	private DefaultObservableSet<MuisState> theState;
	private Set<MuisState> theStateController;

	/** Creates the style */
	public AbstractInternallyStatefulStyle() {
		theState = new DefaultObservableSet<>(new prisms.lang.Type(MuisState.class));
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
		return ObservableValue.flatten(
			attr.getType().getType(),
			getLocalExpressions(attr).combineC(theState.changes(),
				(StyleExpressionValue<StateExpression, T> sev, Set<MuisState> state) -> {
					if(sev.getExpression() == null || sev.getExpression().matches(theState))
						return sev;
					return null;
				}).find(new Type(StyleExpressionValue.class, new Type(StateExpression.class), attr.getType().getType()), value -> {
					return value != null ? value : null;
				})).mapEvent(event -> mapEvent(attr, event));
	}

	@Override
	public ObservableSet<StyleAttribute<?>> localAttributes() {
		return allLocal().combineC(theState.changes(), (StyleAttribute<?> attr, Set<MuisState> state) -> attr).filterMapC(attr -> {
			if(attr == null)
				return null;
			for(StyleExpressionValue<StateExpression, ?> sev : getLocalExpressions(attr))
				if(sev.getExpression() == null || sev.getExpression().matches(theState))
					return attr;
			return null;
		});
	}
}
