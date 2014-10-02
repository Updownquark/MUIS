package org.muis.core.style.stateful;

import org.muis.core.rx.Subscription;
import org.muis.core.style.StyleAttribute;
import org.muis.core.style.StyleExpressionEvent;
import org.muis.core.style.StyleExpressionValue;

import prisms.util.ArrayUtils;

/** A more full partial implementation of StatefulStyle */
public abstract class AbstractStatefulStyle extends SimpleStatefulStyle {
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

	private StatefulStyle [] theDependencies;
	private Subscription<?> [] theDependSubscriptions;

	private final org.muis.core.rx.Action<StyleExpressionEvent<StatefulStyle, StateExpression, ?>> theDependencyListener;

	/**
	 * Creates an abstract stateful style
	 *
	 * @param dependencies The initial set of dependencies for this style
	 */
	public AbstractStatefulStyle(StatefulStyle... dependencies) {
		theDependencies = dependencies;
		theDependencyListener = event -> {
			for(StyleExpressionValue<StateExpression, ?> expr : getExpressions(event.getAttribute())) {
				if(expr.getExpression() == event.getExpression())
					break;
				if(expr.getExpression() == null
					|| (event.getExpression() != null && expr.getExpression().getWhenTrue(event.getExpression()) > 0))
					return;
			}
			styleChanged(event.getAttribute(), event.getExpression(), event.getRootStyle());
		};
		for(int i = 0; i < theDependencies.length; i++)
			theDependSubscriptions[i] = theDependencies[i].expressions().act(theDependencyListener);
	}

	@Override
	public final StatefulStyle [] getConditionalDependencies() {
		return theDependencies;
	}

	/**
	 * @param depend The dependency to add
	 * @param after The dependency to add the new dependency after, or null to add it as the first dependency
	 */
	protected void addDependency(StatefulStyle depend, StatefulStyle after) {
		int idx;
		if(after == null)
			idx = 0;
		else {
			idx = ArrayUtils.indexOf(theDependencies, after);
			if(idx < 0)
				throw new IllegalArgumentException(after + " is not a dependency of " + this);
			idx++;
		}
		theDependencies = ArrayUtils.add(theDependencies, depend, idx);
		theDependSubscriptions = ArrayUtils.add(theDependSubscriptions, depend.expressions().act(theDependencyListener), idx);
		for(StyleAttribute<?> attr : depend.allAttrs()) {
			for(StyleExpressionValue<StateExpression, ?> sev : depend.getExpressions(attr)) {
				boolean foundOverride = false;
				for(StyleExpressionValue<StateExpression, ?> expr : getExpressions(attr)) {
					if(expr.getExpression() == sev.getExpression())
						break;
					if(expr.getExpression() == null
						|| (sev.getExpression() != null && expr.getExpression().getWhenTrue(sev.getExpression()) > 0)) {
						foundOverride = true;
						break;
					}
				}
				if(!foundOverride)
					styleChanged(attr, sev.getExpression(), null);
			}
		}
	}

	/**
	 * Adds a dependency as the last dependency
	 *
	 * @param depend The dependency to add
	 */
	protected void addDependency(StatefulStyle depend) {
		theDependencies = ArrayUtils.add(theDependencies, depend);
		theDependSubscriptions = ArrayUtils.add(theDependSubscriptions, depend.expressions().act(theDependencyListener));
		for(StyleAttribute<?> attr : depend.allAttrs()) {
			for(StyleExpressionValue<StateExpression, ?> sev : depend.getExpressions(attr)) {
				boolean foundOverride = false;
				for(StyleExpressionValue<StateExpression, ?> expr : getExpressions(attr)) {
					if(expr.getExpression() == sev.getExpression())
						break;
					if(expr.getExpression() == null
						|| (sev.getExpression() != null && expr.getExpression().getWhenTrue(sev.getExpression()) > 0)) {
						foundOverride = true;
						break;
					}
				}
				if(!foundOverride)
					styleChanged(attr, sev.getExpression(), null);
			}
		}
	}

	/** @param depend The dependency to remove */
	protected void removeDependency(StatefulStyle depend) {
		int idx = ArrayUtils.indexOf(theDependencies, depend);
		if(idx < 0)
			return;
		theDependSubscriptions[idx].unsubscribe();
		theDependSubscriptions = ArrayUtils.remove(theDependSubscriptions, idx);
		theDependencies = ArrayUtils.remove(theDependencies, idx);
		for(StyleAttribute<?> attr : depend.allAttrs()) {
			for(StyleExpressionValue<StateExpression, ?> sev : depend.getExpressions(attr)) {
				boolean foundOverride = false;
				for(StyleExpressionValue<StateExpression, ?> expr : getExpressions(attr)) {
					if(expr.getExpression() == sev.getExpression())
						break;
					if(expr.getExpression() == null
						|| (sev.getExpression() != null && expr.getExpression().getWhenTrue(sev.getExpression()) > 0)) {
						foundOverride = true;
						break;
					}
				}
				if(!foundOverride)
					styleChanged(attr, sev.getExpression(), null);
			}
		}
	}

	/**
	 * @param toReplace The dependency to replace
	 * @param depend The dependency to add in place of the given dependency to replace
	 */
	protected void replaceDependency(StatefulStyle toReplace, StatefulStyle depend) {
		int idx = ArrayUtils.indexOf(theDependencies, toReplace);
		if(idx < 0)
			throw new IllegalArgumentException(toReplace + " is not a dependency of " + this);
		theDependSubscriptions[idx].unsubscribe();
		java.util.HashSet<prisms.util.DualKey<StyleAttribute<Object>, StateExpression>> attrs = new java.util.HashSet<>();
		for(StyleAttribute<?> attr : toReplace.allAttrs()) {
			for(StyleExpressionValue<StateExpression, ?> sev : toReplace.getExpressions(attr)) {
				boolean foundOverride = false;
				for(StyleExpressionValue<StateExpression, ?> expr : getExpressions(attr)) {
					if(expr.getExpression() == sev.getExpression())
						break;
					if(expr.getExpression() == null
						|| (sev.getExpression() != null && expr.getExpression().getWhenTrue(sev.getExpression()) > 0)) {
						foundOverride = true;
						break;
					}
				}
				if(!foundOverride) {
					for(StyleExpressionValue<StateExpression, ?> expr : depend.getExpressions(attr)) {
						if(expr.getExpression() == null
							|| (sev.getExpression() != null && expr.getExpression().getWhenTrue(sev.getExpression()) > 0)) {
							foundOverride = true;
							break;
						}
					}
				}
				if(!foundOverride)
					attrs.add(new prisms.util.DualKey<>((StyleAttribute<Object>) attr, sev.getExpression()));
			}
		}
		theDependencies[idx] = depend;
		theDependSubscriptions[idx] = depend.expressions().act(theDependencyListener);
		for(StyleAttribute<?> attr : depend.allAttrs()) {
			for(StyleExpressionValue<StateExpression, ?> sev : depend.getExpressions(attr)) {
				boolean foundOverride = false;
				for(StyleExpressionValue<StateExpression, ?> expr : getExpressions(attr)) {
					if(expr.getExpression() == sev.getExpression())
						break;
					if(expr.getExpression() == null
						|| (sev.getExpression() != null && expr.getExpression().getWhenTrue(sev.getExpression()) > 0)) {
						foundOverride = true;
						break;
					}
				}
				if(!foundOverride)
					attrs.add(new prisms.util.DualKey<>((StyleAttribute<Object>) attr, sev.getExpression()));
			}
		}
		for(prisms.util.DualKey<StyleAttribute<Object>, StateExpression> attr : attrs)
			styleChanged(attr.getKey1(), attr.getKey2(), null);
	}

	@Override
	public <T> StyleExpressionValue<StateExpression, T> [] getExpressions(StyleAttribute<T> attr) {
		StyleExpressionValue<StateExpression, T> [] ret = getLocalExpressions(attr);
		for(StatefulStyle dep : theDependencies) {
			StyleExpressionValue<StateExpression, T> [] depRet = dep.getExpressions(attr);
			if(depRet.length > 0)
				ret = ArrayUtils.addAll(ret, depRet, org.muis.core.style.StyleValueHolder.STYLE_EXPRESSION_COMPARE);
		}
		return ret;
	}

	@Override
	public Iterable<StyleAttribute<?>> allAttrs() {
		StatefulStyle [] deps = theDependencies;
		Iterable<StyleAttribute<?>> [] iters = new Iterable[deps.length + 1];
		iters[0] = allLocal();
		for(int i = 0; i < deps.length; i++)
			iters[i + 1] = deps[i].allAttrs();
		return ArrayUtils.iterable(iters);
	}
}
