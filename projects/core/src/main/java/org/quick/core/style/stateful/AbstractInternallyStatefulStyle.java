package org.quick.core.style.stateful;

import java.util.HashSet;
import java.util.Set;

import org.observe.*;
import org.observe.collect.ObservableElement;
import org.observe.collect.ObservableList;
import org.observe.collect.ObservableSet;
import org.quick.core.mgr.QuickState;
import org.quick.core.style.QuickStyle;
import org.quick.core.style.StyleAttribute;
import org.quick.core.style.StyleAttributeEvent;
import org.quick.core.style.StyleExpressionValue;

/** Implements the functionality specified by {@link InternallyStatefulStyle} that is not implemented by {@link AbstractStatefulStyle} */
public abstract class AbstractInternallyStatefulStyle extends AbstractStatefulStyle implements InternallyStatefulStyle {
	private ObservableSet<QuickState> theState;

	/**
	 * Creates the style
	 *
	 * @param dependencies The stateful styles that this style inherits style information from
	 * @param state The set of states to be the internal state for this style
	 */
	public AbstractInternallyStatefulStyle(ObservableList<StatefulStyle> dependencies, ObservableSet<QuickState> state) {
		super(dependencies);
		theState = state;
	}

	@Override
	public ObservableSet<QuickState> getState() {
		return theState;
	}

	@Override
	public ObservableList<QuickStyle> getDependencies() {
		return new org.observe.util.ObservableListWrapper<QuickStyle>(getConditionalDependencies().map(depend -> {
			if(depend instanceof InternallyStatefulStyle)
				return (InternallyStatefulStyle) depend;
			else
				return new StatefulStyleSample(depend, theState);
		}), false) {
			@Override
			public String toString() {
				return "Dependencies of " + AbstractInternallyStatefulStyle.this;
			}
		};
	}

	@Override
	public boolean isSet(StyleAttribute<?> attr) {
		if(attr == null)
			return false;
		for(StyleExpressionValue<StateExpression, ?> sev : getExpressions(attr))
			if(sev.getExpression() == null || sev.getExpression().matches(theState))
				return true;
		return false;
	}

	@Override
	public <T> ObservableValue<T> getLocal(StyleAttribute<T> attr) {
		return new org.observe.util.ObservableValueWrapper<T>(ObservableValue.flatten(
			attr.getType().getType(),
			getLocalExpressions(attr).refresh(theState.changes()).filter(sev -> {
				return stateMatches(sev.getExpression());
			}).getFirst()).mapEvent(event -> mapEvent(attr, event))) {
			@Override
			public String toString() {
				return AbstractInternallyStatefulStyle.this + ".getLocal(" + attr + ")";
			}
		};
	}

	/**
	 * @param expr The state expression to test
	 * @return Whether the expression matches this style's internal state
	 */
	public boolean stateMatches(StateExpression expr) {
		return expr == null || expr.matches(theState);
	}

	@Override
	public ObservableSet<StyleAttribute<?>> localAttributes() {
		return new org.observe.util.ObservableSetWrapper<StyleAttribute<?>>(allLocal().refreshEach(this::getLocal).filter(this::isSet),
			false) {
			@Override
			public String toString() {
				return "Local attributes of " + AbstractInternallyStatefulStyle.this;
			}
		};
	}

	/** Need a little custom code here */
	@Override
	public Observable<StyleAttributeEvent<?>> localRemoves() {
		Observable<StyleAttributeEvent<?>> superLocal = InternallyStatefulStyle.super.localRemoves();
		return Observable.or(superLocal, new Observable<StyleAttributeEvent<?>>(){
			@Override
			public Subscription subscribe(Observer<? super StyleAttributeEvent<?>> observer) {
				return theState.onElement(new java.util.function.Consumer<ObservableElement<QuickState>>() {
					@Override
					public void accept(ObservableElement<QuickState> elValue) {
						elValue.subscribe(new Observer<ObservableValueEvent<QuickState>>() {
							@Override
							public <V2 extends ObservableValueEvent<QuickState>> void onNext(V2 value) {
								/* Find attributes that have expressions matching the state set *without* the new state but none *with*
								 * the new state */
								Set<QuickState> with = new HashSet<>(theState);
								Set<QuickState> without = new HashSet<>(with);
								without.remove(value.getValue());
								seekAtts(without, with);
							}

							@Override
							public <V2 extends ObservableValueEvent<QuickState>> void onCompleted(V2 value) {
								/* Find attributes that have expressions matching the state set *with* the removed state but none *without*
								 * the removed state */
								Set<QuickState> with = new HashSet<>(theState);
								Set<QuickState> without = new HashSet<>(with);
								with.add(value.getValue());
								seekAtts(with, without);
							}

							private void seekAtts(Set<QuickState> matching, Set<QuickState> notMatching) {
								for(StyleAttribute<?> att : localAttributes()) {
									boolean hasMatch = false;
									boolean hasNoMatch = false;
									StyleExpressionValue<StateExpression, ?> matchExp = null;
									for(StyleExpressionValue<StateExpression, ?> exp : getLocalExpressions(att)) {
										boolean expMatch = exp.getExpression() == null || exp.getExpression().matches(matching);
										boolean expNotMatch = exp.getExpression() == null || exp.getExpression().matches(notMatching);
										if(expNotMatch) {
											hasNoMatch = true;
											break;
										} else if(expMatch) {
											hasMatch = true;
											if(matchExp == null)
												matchExp = exp;
											// No break
										}
									}
									if(hasMatch && !hasNoMatch)
										observer.onNext(new StyleAttributeEvent<>(getElement(), AbstractInternallyStatefulStyle.this,
											AbstractInternallyStatefulStyle.this, (StyleAttribute<Object>) att, false, matchExp.get(),
											null, null));
								}
							}
						});
					}
				});
			}
		});
	}
}
