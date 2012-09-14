package org.muis.core.style;

import org.muis.core.MuisElement;

import prisms.util.ArrayUtils;

/** Implements dependencies on top of {@link SimpleStyleSheet} */
public abstract class AbstractStyleSheet extends SimpleStyleSheet {
	/**
	 * @param style The style to check
	 * @param attr The style attribute to check
	 * @param expr The state expression to check
	 * @return Whether the given style contains (locally) a style expression that is true when the given state expression is true for the
	 *         given attribute
	 */
	static boolean isSet(StyleSheet style, StyleAttribute<?> attr, String groupName, Class<? extends MuisElement> type, StateExpression expr) {
		for(StyleGroupTypeExpressionValue<?, ?> sev : style.getLocalExpressions(attr))
			if(ArrayUtils.equals(groupName, sev.getGroupName()) && sev.getType().isAssignableFrom(type)
				&& (sev.getExpression() == null || (expr != null && sev.getExpression().getWhenTrue(expr) > 0)))
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
	static boolean isSetDeep(StyleSheet style, StyleAttribute<?> attr, String groupName, Class<? extends MuisElement> type,
		StateExpression expr) {
		if(isSet(style, attr, groupName, type, expr))
			return true;
		for(StyleSheet depend : style.getStyleSheetDependencies())
			if(isSetDeep(depend, attr, groupName, type, expr))
				return true;
		return false;
	}

	private StyleSheet [] theDependencies;

	private final StyleGroupTypeExpressionListener theDependencyListener;

	/** Creates the style sheet */
	public AbstractStyleSheet() {
		theDependencies = new StyleSheet[0];
		theDependencyListener = new StyleGroupTypeExpressionListener() {
			@Override
			public void eventOccurred(StyleGroupTypeExpressionEvent<?, ?> event) {
				if(isSet(AbstractStyleSheet.this, event.getAttribute(), event.getGroupName(), event.getType(), event.getExpression()))
					return;
				for(StyleGroupTypeExpressionValue<?, ?> expr : getExpressions(event.getAttribute()))
					if(expr.getExpression() == null
						|| (event.getExpression() != null && expr.getExpression().getWhenTrue(event.getExpression()) > 0))
						return;
				int idx = ArrayUtils.indexOf(theDependencies, event.getRootStyle());
				if(idx < 0)
					return;
				for(int i = 0; i < idx; i++)
					if(isSetDeep(theDependencies[i], event.getAttribute(), event.getGroupName(), event.getType(), event.getExpression()))
						return;
				styleChanged(event.getAttribute(), event.getGroupName(), event.getType(), event.getExpression(), event.getRootStyle());
			}
		};
	}

	@Override
	public StyleSheet [] getStyleSheetDependencies() {
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
			for(StyleGroupTypeExpressionValue<?, ?> sev : depend.getExpressions(attr)) {
				if(isSet(this, attr, sev.getGroupName(), sev.getType(), sev.getExpression()))
					continue;
				for(int i = 0; i < idx; i++)
					if(isSetDeep(theDependencies[i], attr, sev.getGroupName(), sev.getType(), sev.getExpression()))
						continue;
				styleChanged(attr, sev.getGroupName(), sev.getType(), sev.getExpression(), null);
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
			for(StyleGroupTypeExpressionValue<?, ?> sev : depend.getExpressions(attr)) {
				if(isSet(this, attr, sev.getGroupName(), sev.getType(), sev.getExpression()))
					continue;
				for(int i = 0; i < theDependencies.length - 1; i++)
					if(isSetDeep(theDependencies[i], attr, sev.getGroupName(), sev.getType(), sev.getExpression()))
						continue;
				styleChanged(attr, sev.getGroupName(), sev.getType(), sev.getExpression(), null);
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
			for(StyleGroupTypeExpressionValue<?, ?> sev : depend.getExpressions(attr)) {
				if(isSet(this, attr, sev.getGroupName(), sev.getType(), sev.getExpression()))
					continue;
				for(int i = 0; i < theDependencies.length - 1; i++)
					if(isSetDeep(theDependencies[i], attr, sev.getGroupName(), sev.getType(), sev.getExpression()))
						continue;
				styleChanged(attr, sev.getGroupName(), sev.getType(), sev.getExpression(), null);
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
		java.util.HashSet<prisms.util.MultiKey> attrs = new java.util.HashSet<>();
		for(StyleAttribute<?> attr : toReplace.allAttrs()) {
			for(StyleGroupTypeExpressionValue<?, ?> sev : depend.getExpressions(attr)) {
				if(isSet(this, attr, sev.getGroupName(), sev.getType(), sev.getExpression()))
					continue;
				for(int i = 0; i < theDependencies.length - 1; i++)
					if(isSetDeep(theDependencies[i], attr, sev.getGroupName(), sev.getType(), sev.getExpression()))
						continue;
				attrs.add(new prisms.util.MultiKey(attr, sev.getGroupName(), sev.getType(), sev.getExpression()));
			}
		}
		depend.addListener(theDependencyListener);
		for(StyleAttribute<?> attr : depend.allAttrs()) {
			for(StyleGroupTypeExpressionValue<?, ?> sev : depend.getExpressions(attr)) {
				if(isSet(this, attr, sev.getGroupName(), sev.getType(), sev.getExpression()))
					continue;
				for(int i = 0; i < theDependencies.length - 1; i++)
					if(isSetDeep(theDependencies[i], attr, sev.getGroupName(), sev.getType(), sev.getExpression()))
						continue;
				styleChanged(attr, sev.getGroupName(), sev.getType(), sev.getExpression(), null);
			}
		}
		for(prisms.util.MultiKey attr : attrs)
			styleChanged((StyleAttribute<?>) attr.getKey(0), (String) attr.getKey(1), (Class<? extends MuisElement>) attr.getKey(2),
				(StateExpression) attr.getKey(3), null);
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
	public <T> StyleGroupTypeExpressionValue<?, T> [] getExpressions(StyleAttribute<T> attr) {
		StyleGroupTypeExpressionValue<?, T> [] ret = getLocalExpressions(attr);
		for(StyleSheet dep : theDependencies) {
			StyleGroupTypeExpressionValue<?, T> [] depRet = dep.getExpressions(attr);
			if(depRet.length > 0)
				ret = ArrayUtils.addAll(ret, depRet);
		}
		return ret;
	}
}
