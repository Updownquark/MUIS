package org.muis.core.style.sheet;

import org.muis.core.style.StyleAttribute;
import org.muis.core.style.StyleExpressionEvent;
import org.muis.core.style.StyleExpressionListener;
import org.muis.core.style.StyleExpressionValue;

import prisms.util.ArrayUtils;

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

	/**
	 * @param style The style to check
	 * @param attr The style attribute to check
	 * @param expr The expression to check
	 * @return Whether the given style contains (anywhere in the dependency tree) a style expression that is true when the given state
	 *         expression is true for the given attribute
	 */
	static boolean isSetDeep(StyleSheet style, StyleAttribute<?> attr, StateGroupTypeExpression<?> expr) {
		if(isSet(style, attr, expr))
			return true;
		for(StyleSheet depend : style.getConditionalDependencies())
			if(isSetDeep(depend, attr, expr))
				return true;
		return false;
	}

	private StyleSheet [] theDependencies;

	private final StyleExpressionListener<StyleSheet, StateGroupTypeExpression<?>> theDependencyListener;

	/** Creates the style sheet */
	public AbstractStyleSheet() {
		theDependencies = new StyleSheet[0];
		theDependencyListener = new StyleExpressionListener<StyleSheet, StateGroupTypeExpression<?>>() {
			@Override
			public void eventOccurred(StyleExpressionEvent<StyleSheet, StateGroupTypeExpression<?>, ?> event) {
				if(isSet(AbstractStyleSheet.this, event.getAttribute(), event.getExpression()))
					return;
				for(StyleExpressionValue<StateGroupTypeExpression<?>, ?> expr : getExpressions(event.getAttribute())) {
					if(expr.getExpression() == event.getExpression())
						break;
					if(expr.getExpression() == null
						|| (event.getExpression() != null && expr.getExpression().getWhenTrue(event.getExpression()) > 0))
						return;
				}
				int idx = ArrayUtils.indexOf(theDependencies, event.getRootStyle());
				if(idx < 0)
					return;
				for(int i = 0; i < idx; i++)
					if(isSetDeep(theDependencies[i], event.getAttribute(), event.getExpression()))
						return;
				styleChanged(event.getAttribute(), event.getExpression(), event.getRootStyle());
			}
		};
	}

	@Override
	public StyleSheet [] getConditionalDependencies() {
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
		else
			idx = ArrayUtils.indexOf(theDependencies, after);
		if(idx < 0)
			throw new IllegalArgumentException(after + " is not a dependency of " + this);
		theDependencies = ArrayUtils.add(theDependencies, depend, idx);
		depend.addListener(theDependencyListener);
		for(StyleAttribute<?> attr : depend.allAttrs()) {
			for(StyleExpressionValue<StateGroupTypeExpression<?>, ?> sev : depend.getExpressions(attr)) {
				if(isSet(this, attr, sev.getExpression()))
					continue;
				for(int i = 0; i < idx; i++)
					if(isSetDeep(theDependencies[i], attr, sev.getExpression()))
						continue;
				styleChanged(attr, sev.getExpression(), null);
			}
		}
	}

	/**
	 * Adds a dependency as the last dependency
	 *
	 * @param depend The dependency to add
	 */
	protected void addDependency(StyleSheet depend) {
		theDependencies = ArrayUtils.add(theDependencies, depend);
		depend.addListener(theDependencyListener);
		for(StyleAttribute<?> attr : depend.allAttrs()) {
			for(StyleExpressionValue<StateGroupTypeExpression<?>, ?> sev : depend.getExpressions(attr)) {
				if(isSet(this, attr, sev.getExpression()))
					continue;
				for(int i = 0; i < theDependencies.length - 1; i++)
					if(isSetDeep(theDependencies[i], attr, sev.getExpression()))
						continue;
				styleChanged(attr, sev.getExpression(), null);
			}
		}
	}

	/** @param depend The dependency to remove */
	protected void removeDependency(StyleSheet depend) {
		int idx = ArrayUtils.indexOf(theDependencies, depend);
		if(idx < 0)
			return;
		depend.removeListener(theDependencyListener);
		theDependencies = ArrayUtils.remove(theDependencies, idx);
		for(StyleAttribute<?> attr : depend.allAttrs()) {
			for(StyleExpressionValue<StateGroupTypeExpression<?>, ?> sev : depend.getExpressions(attr)) {
				if(isSet(this, attr, sev.getExpression()))
					continue;
				for(int i = 0; i < theDependencies.length - 1; i++)
					if(isSetDeep(theDependencies[i], attr, sev.getExpression()))
						continue;
				styleChanged(attr, sev.getExpression(), null);
			}
		}
	}

	/**
	 * @param toReplace The dependency to replace
	 * @param depend The dependency to add in place of the given dependency to replace
	 */
	protected void replaceDependency(StyleSheet toReplace, StyleSheet depend) {
		int idx = ArrayUtils.indexOf(theDependencies, depend);
		if(idx < 0)
			throw new IllegalArgumentException(toReplace + " is not a dependency of " + this);
		toReplace.removeListener(theDependencyListener);
		theDependencies[idx] = depend;
		java.util.HashSet<prisms.util.DualKey<StyleAttribute<?>, StateGroupTypeExpression<?>>> attrs = new java.util.HashSet<>();
		for(StyleAttribute<?> attr : toReplace.allAttrs()) {
			for(StyleExpressionValue<StateGroupTypeExpression<?>, ?> sev : depend.getExpressions(attr)) {
				if(isSet(this, attr, sev.getExpression()))
					continue;
				for(int i = 0; i < theDependencies.length - 1; i++)
					if(isSetDeep(theDependencies[i], attr, sev.getExpression()))
						continue;
				attrs.add(new prisms.util.DualKey<StyleAttribute<?>, StateGroupTypeExpression<?>>(attr, sev.getExpression()));
			}
		}
		depend.addListener(theDependencyListener);
		for(StyleAttribute<?> attr : depend.allAttrs()) {
			for(StyleExpressionValue<StateGroupTypeExpression<?>, ?> sev : depend.getExpressions(attr)) {
				if(isSet(this, attr, sev.getExpression()))
					continue;
				for(int i = 0; i < theDependencies.length - 1; i++)
					if(isSetDeep(theDependencies[i], attr, sev.getExpression()))
						continue;
				styleChanged(attr, sev.getExpression(), null);
			}
		}
		for(prisms.util.DualKey<StyleAttribute<?>, StateGroupTypeExpression<?>> attr : attrs)
			styleChanged(attr.getKey1(), attr.getKey2(), null);
	}

	@Override
	public Iterable<StyleAttribute<?>> allAttrs() {
		StyleSheet [] deps = theDependencies;
		Iterable<StyleAttribute<?>> [] iters = new Iterable[deps.length + 1];
		iters[0] = allLocal();
		for(int i = 0; i < deps.length; i++)
			iters[i + 1] = deps[i].allAttrs();
		return ArrayUtils.iterable(iters);
	}

	@Override
	public <T> StyleExpressionValue<StateGroupTypeExpression<?>, T> [] getExpressions(StyleAttribute<T> attr) {
		StyleExpressionValue<StateGroupTypeExpression<?>, T> [] ret = getLocalExpressions(attr);
		for(StyleSheet dep : theDependencies) {
			StyleExpressionValue<StateGroupTypeExpression<?>, T> [] depRet = dep.getExpressions(attr);
			if(depRet.length > 0)
				ret = ArrayUtils.addAll(ret, depRet);
		}
		java.util.Arrays.sort(ret, org.muis.core.style.StyleValueHolder.STYLE_EXPRESSION_COMPARE);
		return ret;
	}
}
