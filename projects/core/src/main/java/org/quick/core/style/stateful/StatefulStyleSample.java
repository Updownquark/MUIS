package org.quick.core.style.stateful;

import org.observe.ObservableValue;
import org.observe.collect.ObservableList;
import org.observe.collect.ObservableSet;
import org.quick.core.mgr.QuickState;
import org.quick.core.style.QuickStyle;
import org.quick.core.style.StyleAttribute;
import org.quick.core.style.StyleExpressionValue;

import com.google.common.reflect.TypeToken;

/** A {@link QuickStyle} implementation that gets all its information from a {@link StatefulStyle} for a particular state */
public class StatefulStyleSample implements QuickStyle {
	private final StatefulStyle theStatefulStyle;

	private final ObservableSet<QuickState> theState;

	/**
	 * @param statefulStyle The stateful style to get the attribute information from
	 * @param state The state to get the attribute information for
	 */
	public StatefulStyleSample(StatefulStyle statefulStyle, ObservableSet<QuickState> state) {
		theStatefulStyle = statefulStyle;
		theState = state;
	}

	/** @return The stateful style that this style uses to get its attribute information from */
	public StatefulStyle getStatefulStyle() {
		return theStatefulStyle;
	}

	/** @return The state that this style gets its attribute information from its stateful style for */
	public ObservableSet<QuickState> getState() {
		return theState;
	}

	@Override
	public ObservableList<QuickStyle> getDependencies() {
		return ObservableList.constant(TypeToken.of(QuickStyle.class)); // Empty list
	}

	@Override
	public boolean isSet(StyleAttribute<?> attr) {
		for(StyleExpressionValue<StateExpression, ?> value : theStatefulStyle.getExpressions(attr))
			if(value.getExpression() == null || value.getExpression().matches(theState))
				return true;
		return false;
	}

	@Override
	public <T> ObservableValue<T> getLocal(StyleAttribute<T> attr) {
		return new org.observe.util.ObservableValueWrapper<T>(ObservableValue.flatten(
			theStatefulStyle.getExpressions(attr).refresh(theState.changes()).find((StyleExpressionValue<StateExpression, T> sev) -> {
				System.out.println("Checking " + sev.getExpression() + " for " + attr);
				if(sev.getExpression() == null || sev.getExpression().matches(theState))
					return true;
				else
					return false;
			})).mapEvent(event -> mapEvent(attr, event))) {
			@Override
			public String toString() {
				return StatefulStyleSample.this + ".getLocal(" + attr + ")";
			}
		};
	}

	@Override
	public ObservableSet<StyleAttribute<?>> localAttributes() {
		return new org.observe.util.ObservableSetWrapper<StyleAttribute<?>>(ObservableSet.unique(theStatefulStyle.allAttrs().filterMap(
			attr -> {
				if(attr == null)
					return null;
				for(StyleExpressionValue<StateExpression, ?> sev : theStatefulStyle.getExpressions(attr))
					if(sev.getExpression() == null || sev.getExpression().matches(theState))
						return attr;
				return null;
			}), Object::equals), false) {

			@Override
			public String toString() {
				return "Local attributes for " + StatefulStyleSample.this;
			}
		};
	}

	@Override
	public String toString() {
		return theStatefulStyle + ".sample(" + theState + ")";
	}
}
