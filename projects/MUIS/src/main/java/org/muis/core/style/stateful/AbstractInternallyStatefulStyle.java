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
	private ObservableSet<MuisState> theState;
	private Set<MuisState> theStateController;

	/**
	 * Creates the style
	 * 
	 * @param dependencies The stateful styles that this style inherits style information from
	 */
	public AbstractInternallyStatefulStyle(ObservableList<StatefulStyle> dependencies) {
		super(dependencies);
		DefaultObservableSet<MuisState> state = new DefaultObservableSet<>(new prisms.lang.Type(MuisState.class));
		theStateController = state.control(null);
		theState = new org.muis.util.ObservableSetWrapper<MuisState>(state) {
			@Override
			public String toString() {
				return "state(" + AbstractInternallyStatefulStyle.this + ")=" + super.toString();
			}
		};
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
		return new org.muis.util.ObservableListWrapper<MuisStyle>(getConditionalDependencies().mapC(depend -> {
			if(depend instanceof InternallyStatefulStyle)
				return (InternallyStatefulStyle) depend;
			else
				return new StatefulStyleSample(depend, theState);
		})) {
			@Override
			public String toString() {
				return "Dependencies of " + AbstractInternallyStatefulStyle.this;
			}
		};
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
		return new org.muis.util.ObservableValueWrapper<T>(ObservableValue.flatten(
			attr.getType().getType(),
			getLocalExpressions(attr).combineC(theState.changes(),
				(StyleExpressionValue<StateExpression, T> sev, Set<MuisState> state) -> {
					if(sev.getExpression() == null || sev.getExpression().matches(theState))
						return sev;
					return null;
				}).find(new Type(StyleExpressionValue.class, new Type(StateExpression.class), attr.getType().getType()), value -> {
					return value != null ? value : null;
			})).mapEvent(event -> mapEvent(attr, event))) {
			@Override
			public String toString() {
				return AbstractInternallyStatefulStyle.this + ".getLocal(" + attr + ")";
			}
		};
	}

	@Override
	public ObservableSet<StyleAttribute<?>> localAttributes() {
		return new org.muis.util.ObservableSetWrapper<StyleAttribute<?>>(allLocal().combineC(theState.changes(),
			(StyleAttribute<?> attr, Set<MuisState> state) -> attr).filterMapC(attr -> {
			if(attr == null)
				return null;
			for(StyleExpressionValue<StateExpression, ?> sev : getLocalExpressions(attr))
				if(sev.getExpression() == null || sev.getExpression().matches(theState))
					return attr;
			return null;
		})) {
			@Override
			public String toString() {
				return "Local attributes of " + AbstractInternallyStatefulStyle.this;
			}
		};
	}
}
