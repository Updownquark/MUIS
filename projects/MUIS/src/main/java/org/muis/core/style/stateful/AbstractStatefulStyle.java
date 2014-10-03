package org.muis.core.style.stateful;

import java.util.List;

import org.muis.core.rx.DefaultObservableList;
import org.muis.core.rx.Observable;
import org.muis.core.rx.ObservableList;
import org.muis.core.rx.Observer;
import org.muis.core.style.StyleAttribute;
import org.muis.core.style.StyleExpressionValue;

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

	private DefaultObservableList<StatefulStyle> theDependencies;
	private List<StatefulStyle> theDependController;

	/**
	 * Creates an abstract stateful style
	 *
	 * @param dependencies The initial set of dependencies for this style
	 */
	public AbstractStatefulStyle(StatefulStyle... dependencies) {
		theDependencies = new DefaultObservableList<>();
		theDependController=theDependencies.control();
		for(StatefulStyle depend : dependencies)
			theDependController.add(depend);

		theDependencies.act((Observable<StatefulStyle> element) -> {
			element.subscribe(new Observer<StatefulStyle>() {
				StatefulStyle depend;

				@Override
				public <V extends StatefulStyle> void onNext(V value) {
					java.util.HashSet<prisms.util.DualKey<StyleAttribute<Object>, StateExpression>> attrs = new java.util.HashSet<>();
					if(depend != null) {
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
								if(!foundOverride) {
									for(StyleExpressionValue<StateExpression, ?> expr : value.getExpressions(attr)) {
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
					}
					depend = value;
					depend
						.expressions()
						.takeUntil(element.completed())
						.act(
							event -> {
								for(StyleExpressionValue<StateExpression, ?> expr : getExpressions(event.getAttribute())) {
									if(expr.getExpression() == event.getExpression())
										break;
									if(expr.getExpression() == null
										|| (event.getExpression() != null && expr.getExpression().getWhenTrue(event.getExpression()) > 0))
										return;
								}
								styleChanged(event.getAttribute(), event.getExpression(), event.getRootStyle());
							});
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
				public void onCompleted(StatefulStyle value) {
					for(StyleAttribute<?> attr : value.allAttrs()) {
						for(StyleExpressionValue<StateExpression, ?> sev : value.getExpressions(attr)) {
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
			});
		});
	}

	@Override
	public final ObservableList<StatefulStyle> getConditionalDependencies() {
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
	protected void addDependency(StatefulStyle depend) {
		theDependController.add(depend);
	}

	/** @param depend The dependency to remove */
	protected void removeDependency(StatefulStyle depend) {
		theDependController.remove(depend);
	}

	/**
	 * @param toReplace The dependency to replace
	 * @param depend The dependency to add in place of the given dependency to replace
	 */
	protected void replaceDependency(StatefulStyle toReplace, StatefulStyle depend) {
		int idx = theDependencies.indexOf(toReplace);
		if(idx < 0)
			throw new IllegalArgumentException(toReplace + " is not a dependency of " + this);
		theDependController.set(idx, depend);
	}
}
