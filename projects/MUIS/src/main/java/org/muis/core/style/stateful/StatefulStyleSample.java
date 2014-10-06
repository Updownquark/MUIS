package org.muis.core.style.stateful;

import org.muis.core.mgr.MuisState;
import org.muis.core.rx.ObservableList;
import org.muis.core.rx.ObservableSet;
import org.muis.core.rx.ObservableValue;
import org.muis.core.style.MuisStyle;
import org.muis.core.style.StyleAttribute;
import org.muis.core.style.StyleExpressionValue;

/** A {@link MuisStyle} implementation that gets all its information from a {@link StatefulStyle} for a particular state */
public class StatefulStyleSample implements MuisStyle {
	private final StatefulStyle theStatefulStyle;

	private final ObservableSet<MuisState> theState;

	/**
	 * @param statefulStyle The stateful style to get the attribute information from
	 * @param state The state to get the attribute information for
	 */
	public StatefulStyleSample(StatefulStyle statefulStyle, ObservableSet<MuisState> state) {
		theStatefulStyle = statefulStyle;
		theState = state;
	}

	/** @return The stateful style that this style uses to get its attribute information from */
	public StatefulStyle getStatefulStyle() {
		return theStatefulStyle;
	}

	/** @return The state that this style gets its attribute information from its stateful style for */
	public ObservableSet<MuisState> getState() {
		return theState;
	}

	@Override
	public ObservableList<MuisStyle> getDependencies() {
		return theStatefulStyle.getConditionalDependencies().mapC(depend -> new StatefulStyleSample(depend, theState));
	}

	@Override
	public boolean isSet(StyleAttribute<?> attr) {
		for(StyleExpressionValue<StateExpression, ?> value : theStatefulStyle.getLocalExpressions(attr))
			if(value.getExpression() == null || value.getExpression().matches(theState))
				return true;
		return false;
	}

	@Override
	public <T> ObservableValue<T> getLocal(StyleAttribute<T> attr) {
		return ObservableValue.flatten(
			attr.getType().getType(),
			theStatefulStyle.getLocalExpressions(attr)
			.combineC(theState.changes(), (StyleExpressionValue<StateExpression, T> exprs, ObservableSet<MuisState> state) -> exprs)
			.find(attr.getType().getType(), (StyleExpressionValue<StateExpression, T> sev) -> {
				if(sev.getExpression() == null || sev.getExpression().matches(theState))
						return sev;
				else
					return null;
				}));
	}

	@Override
	public ObservableSet<StyleAttribute<?>> localAttributes() {
		return theStatefulStyle.allLocal().filterMapC(attr -> {
			for(StyleExpressionValue<StateExpression, ?> sev : theStatefulStyle.getExpressions(attr))
				if(sev.getExpression() == null || sev.getExpression().matches(theState))
					return attr;
			return null;
		});
	}
}
