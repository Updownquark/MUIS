package org.muis.core.style.stateful;

import org.muis.core.mgr.MuisState;
import org.muis.core.rx.ObservableList;
import org.muis.core.rx.ObservableSet;
import org.muis.core.rx.ObservableValue;
import org.muis.core.style.MuisStyle;
import org.muis.core.style.StyleAttribute;
import org.muis.core.style.StyleExpressionValue;

/** Implements the functionality specified by {@link InternallyStatefulStyle} that is not implemented by {@link AbstractStatefulStyle} */
public abstract class AbstractInternallyStatefulStyle extends AbstractStatefulStyle implements InternallyStatefulStyle {
	private ObservableSet<MuisState> theState;

	/**
	 * Creates the style
	 *
	 * @param state The state for the style
	 */
	public AbstractInternallyStatefulStyle(ObservableSet<MuisState> state) {
		theState = state;
	}

	@Override
	public ObservableSet<MuisState> getState() {
		return theState;
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
		return new org.muis.util.ObservableValueWrapper<T>(ObservableValue.flatten(attr.getType().getType(),
			getLocalExpressions(attr).refireWhen(theState.changes()).filterC(sev -> {
				if(sev.getExpression() == null || sev.getExpression().matches(theState))
					return true;
				return false;
			}).first()).mapEvent(event -> mapEvent(attr, event))) {
			@Override
			public String toString() {
				return AbstractInternallyStatefulStyle.this + ".getLocal(" + attr + ")";
			}
		};
	}

	@Override
	public ObservableSet<StyleAttribute<?>> localAttributes() {
		return new org.muis.util.ObservableSetWrapper<StyleAttribute<?>>(allLocal().refireWhen(theState.changes()).filterMapC(attr -> {
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
