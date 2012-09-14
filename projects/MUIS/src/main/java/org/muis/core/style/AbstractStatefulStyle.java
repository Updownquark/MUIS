package org.muis.core.style;

import prisms.util.ArrayUtils;

/** A more full partial implementation of StatefulStyle */
public abstract class AbstractStatefulStyle extends SimpleStatefulStyle {
	private StatefulStyle [] theDependencies;

	private final StyleExpressionListener theDependencyListener;

	/**
	 * Creates an abstract stateful style
	 * 
	 * @param dependencies The initial set of dependencies for this style
	 */
	public AbstractStatefulStyle(StatefulStyle... dependencies) {
		theDependencies = dependencies;
		theDependencyListener = new StyleExpressionListener() {
			@Override
			public void eventOccurred(StyleExpressionEvent<?> event) {
				if(isSet(AbstractStatefulStyle.this, event.getAttribute(), event.getExpression()))
					return;
				for(StyleExpressionValue<?> expr : getExpressions(event.getAttribute()))
					if(expr.getExpression() == null
						|| (event.getExpression() != null && expr.getExpression().getWhenTrue(event.getExpression()) > 0))
						return;
				int idx = ArrayUtils.indexOf(theDependencies, event.getRootStyle());
				if(idx < 0)
					return;
				for(int i = 0; i < idx; i++)
					if(isSetDeep(theDependencies[i], event.getAttribute(), event.getExpression()))
						return;
				styleChanged(event.getAttribute(), event.getExpression(), event.getRootStyle());
			}
		};
		for(StatefulStyle dep : theDependencies)
			dep.addListener(theDependencyListener);
	}

	@Override
	public final StatefulStyle [] getStatefulDependencies() {
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
		else
			idx = ArrayUtils.indexOf(theDependencies, after);
		if(idx < 0)
			throw new IllegalArgumentException(after + " is not a dependency of " + this);
		theDependencies = ArrayUtils.add(theDependencies, depend, idx);
		depend.addListener(theDependencyListener);
		for(StyleAttribute<?> attr : depend.allAttrs()) {
			for(StyleExpressionValue<?> sev : depend.getExpressions(attr)) {
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
	protected void addDependency(StatefulStyle depend) {
		theDependencies = ArrayUtils.add(theDependencies, depend);
		depend.addListener(theDependencyListener);
		for(StyleAttribute<?> attr : depend.allAttrs()) {
			for(StyleExpressionValue<?> sev : depend.getExpressions(attr)) {
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
	protected void removeDependency(StatefulStyle depend) {
		int idx = ArrayUtils.indexOf(theDependencies, depend);
		if(idx < 0)
			return;
		depend.removeListener(theDependencyListener);
		theDependencies = ArrayUtils.remove(theDependencies, idx);
		for(StyleAttribute<?> attr : depend.allAttrs()) {
			for(StyleExpressionValue<?> sev : depend.getExpressions(attr)) {
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
	protected void replaceDependency(StatefulStyle toReplace, StatefulStyle depend) {
		int idx = ArrayUtils.indexOf(theDependencies, depend);
		if(idx < 0)
			throw new IllegalArgumentException(toReplace + " is not a dependency of " + this);
		toReplace.removeListener(theDependencyListener);
		theDependencies[idx] = depend;
		java.util.HashSet<prisms.util.DualKey<StyleAttribute<Object>, StateExpression>> attrs = new java.util.HashSet<>();
		for(StyleAttribute<?> attr : toReplace.allAttrs()) {
			for(StyleExpressionValue<?> sev : depend.getExpressions(attr)) {
				if(isSet(this, attr, sev.getExpression()))
					continue;
				for(int i = 0; i < theDependencies.length - 1; i++)
					if(isSetDeep(theDependencies[i], attr, sev.getExpression()))
						continue;
				attrs.add(new prisms.util.DualKey<>((StyleAttribute<Object>) attr, sev.getExpression()));
			}
		}
		depend.addListener(theDependencyListener);
		for(StyleAttribute<?> attr : depend.allAttrs()) {
			for(StyleExpressionValue<?> sev : depend.getExpressions(attr)) {
				if(isSet(this, attr, sev.getExpression()))
					continue;
				for(int i = 0; i < theDependencies.length - 1; i++)
					if(isSetDeep(theDependencies[i], attr, sev.getExpression()))
						continue;
				styleChanged(attr, sev.getExpression(), null);
			}
		}
		for(prisms.util.DualKey<StyleAttribute<Object>, StateExpression> attr : attrs)
			styleChanged(attr.getKey1(), attr.getKey2(), null);
	}

	@Override
	public <T> StyleExpressionValue<T> [] getExpressions(StyleAttribute<T> attr) {
		StyleExpressionValue<T> [] ret = getLocalExpressions(attr);
		for(StatefulStyle dep : theDependencies) {
			StyleExpressionValue<T> [] depRet = dep.getExpressions(attr);
			if(depRet.length > 0)
				ret = ArrayUtils.addAll(ret, depRet);
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
