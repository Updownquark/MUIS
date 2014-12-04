package org.muis.core.style.stateful;

import org.muis.core.rx.ObservableList;
import org.muis.core.style.StyleAttribute;
import org.muis.core.style.StyleExpressionValue;

/** A more full partial implementation of StatefulStyle */
public abstract class AbstractStatefulStyle extends SimpleStatefulStyle {
	private final ObservableList<StatefulStyle> theDependencies;

	/** @param dependencies The stateful styles that this style inherits style information from */
	protected AbstractStatefulStyle(ObservableList<StatefulStyle> dependencies) {
		theDependencies = dependencies;
	}

	@Override
	public final ObservableList<StatefulStyle> getConditionalDependencies() {
		return theDependencies;
	}

	/**
	 * @param style The style to check
	 * @param attr The style attribute to check
	 * @param expr The state expression to check
	 * @return Whether the given style contains (locally) a style expression that is true when the given state expression is true for the
	 *         given attribute
	 */
	static boolean isSet(StatefulStyle style, StyleAttribute<?> attr, StateExpression expr) {
		for(StyleExpressionValue<StateExpression, ?> sev : style.getLocalExpressions(attr))
			if(sev.getExpression() == null || (expr != null && sev.getExpression().getWhenTrue(expr) > 0))
				return true;
		return false;
	}

	/**
	 * @param style The style to check
	 * @param attr The style attribute to check
	 * @param expr The state expression to check
	 * @return Whether the given style contains (anywhere in the dependency tree) a style expression that is true when the given state
	 *         expression is true for the given attribute
	 */
	static boolean isSetDeep(StatefulStyle style, StyleAttribute<?> attr, StateExpression expr) {
		if(isSet(style, attr, expr))
			return true;
		for(StatefulStyle depend : style.getConditionalDependencies())
			if(isSetDeep(depend, attr, expr))
				return true;
		return false;
	}
}
