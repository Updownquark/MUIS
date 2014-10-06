package org.muis.core.style.sheet;

import java.util.List;

import org.muis.core.rx.DefaultObservableList;
import org.muis.core.rx.ObservableList;
import org.muis.core.style.StyleAttribute;
import org.muis.core.style.StyleExpressionValue;

/** Implements dependencies on top of {@link SimpleStyleSheet} */
public abstract class AbstractStyleSheet extends SimpleStyleSheet {
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

	private DefaultObservableList<StyleSheet> theDependencies;
	private List<StyleSheet> theDependController;

	/** Creates the style sheet */
	public AbstractStyleSheet() {
		theDependencies = new DefaultObservableList<>(new prisms.lang.Type(StyleSheet.class));
		theDependController = theDependencies.control(null);
	}

	@Override
	public ObservableList<StyleSheet> getConditionalDependencies() {
		return theDependencies;
	}

	/**
	 * @param depend The dependency to add
	 * @param after The dependency to add the new dependency after, or null to add it as the first dependency
	 */
	protected void addDependency(StyleSheet depend, StyleSheet after) {
		int idx;
		if(after == null)
			idx = 0;
		else {
			idx = theDependencies.indexOf(after);
			if(idx < 0)
				throw new IllegalArgumentException(after + " is not a dependency of " + this);
			idx++;
		}
		theDependController.add(idx, depend);
	}

	/**
	 * Adds a dependency as the last dependency
	 *
	 * @param depend The dependency to add
	 */
	protected void addDependency(StyleSheet depend) {
		theDependController.add(depend);
	}

	/** @param depend The dependency to remove */
	protected void removeDependency(StyleSheet depend) {
		theDependController.remove(depend);
	}

	/**
	 * @param toReplace The dependency to replace
	 * @param depend The dependency to add in place of the given dependency to replace
	 */
	protected void replaceDependency(StyleSheet toReplace, StyleSheet depend) {
		int idx = theDependencies.indexOf(depend);
		if(idx < 0)
			throw new IllegalArgumentException(toReplace + " is not a dependency of " + this);
		theDependController.set(idx, depend);
	}
}
