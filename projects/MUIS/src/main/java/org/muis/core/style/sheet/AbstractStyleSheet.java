package org.muis.core.style.sheet;

import org.muis.core.style.StyleAttribute;
import org.muis.core.style.StyleExpressionValue;
import org.observe.collect.ObservableList;

/** Implements dependencies on top of {@link SimpleStyleSheet} */
public abstract class AbstractStyleSheet extends SimpleStyleSheet {
	private ObservableList<StyleSheet> theDependencies;

	/**
	 * Creates the style sheet
	 *
	 * @param dependencies The style sheets that this style sheet inherits style information from
	 */
	public AbstractStyleSheet(ObservableList<StyleSheet> dependencies) {
		theDependencies = dependencies;
	}

	@Override
	public ObservableList<StyleSheet> getConditionalDependencies() {
		return theDependencies;
	}

	/**
	 * @param style The style to check
	 * @param attr The style attribute to check
	 * @param expr The expression to check
	 * @return Whether the given style contains (locally) a style expression that is true when the given state expression is true for the
	 *         given attribute
	 */
	static boolean isSet(StyleSheet style, StyleAttribute<?> attr, StateGroupTypeExpression<?> expr) {
		for(StyleExpressionValue<StateGroupTypeExpression<?>, ?> sev : style.getLocalExpressions(attr))
			if(sev.getExpression().getWhenTrue(expr) > 0)
				return true;
		return false;
	}
}
