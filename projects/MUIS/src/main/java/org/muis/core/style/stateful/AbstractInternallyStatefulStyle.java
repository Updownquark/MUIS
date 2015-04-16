package org.muis.core.style.stateful;

import java.util.HashSet;
import java.util.Set;

import org.muis.core.mgr.MuisState;
import org.muis.core.style.MuisStyle;
import org.muis.core.style.StyleAttribute;
import org.muis.core.style.StyleAttributeEvent;
import org.muis.core.style.StyleExpressionValue;
import org.observe.Observable;
import org.observe.ObservableValue;
import org.observe.ObservableValueEvent;
import org.observe.Observer;
import org.observe.collect.ObservableElement;
import org.observe.collect.ObservableList;
import org.observe.collect.ObservableSet;

/** Implements the functionality specified by {@link InternallyStatefulStyle} that is not implemented by {@link AbstractStatefulStyle} */
public abstract class AbstractInternallyStatefulStyle extends AbstractStatefulStyle implements InternallyStatefulStyle {
	private ObservableSet<MuisState> theState;

	/**
	 * Creates the style
	 *
	 * @param dependencies The stateful styles that this style inherits style information from
	 * @param state The set of states to be the internal state for this style
	 */
	public AbstractInternallyStatefulStyle(ObservableList<StatefulStyle> dependencies, ObservableSet<MuisState> state) {
		super(dependencies);
		theState = state;
	}

	@Override
	public ObservableSet<MuisState> getState() {
		return theState;
	}

	@Override
	public ObservableList<MuisStyle> getDependencies() {
		return new org.observe.util.ObservableListWrapper<MuisStyle>(getConditionalDependencies().map(depend -> {
			if(depend instanceof InternallyStatefulStyle)
				return (InternallyStatefulStyle) depend;
			else
				return new StatefulStyleSample(depend, theState);
		})) {
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
			getLocalExpressions(attr).refireWhen(theState.changes()).filter(sev -> {
				return stateMatches(sev.getExpression());
			}).first()).mapEvent(event -> mapEvent(attr, event))) {
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
		return new org.observe.util.ObservableSetWrapper<StyleAttribute<?>>(allLocal().refireWhenEach(this::getLocal).filter(this::isSet)) {
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
			public Runnable observe(Observer<? super StyleAttributeEvent<?>> observer) {
				return theState.onElement(new java.util.function.Consumer<ObservableElement<MuisState>>() {
					@Override
					public void accept(ObservableElement<MuisState> elValue) {
						elValue.observe(new Observer<ObservableValueEvent<MuisState>>() {
							@Override
							public <V2 extends ObservableValueEvent<MuisState>> void onNext(V2 value) {
								/* Find attributes that have expressions matching the state set *without* the new state but none *with*
								 * the new state */
								Set<MuisState> with = new HashSet<>(theState);
								Set<MuisState> without = new HashSet<>(with);
								without.remove(value.getValue());
								seekAtts(without, with);
							}

							@Override
							public <V2 extends ObservableValueEvent<MuisState>> void onCompleted(V2 value) {
								/* Find attributes that have expressions matching the state set *with* the removed state but none *without*
								 * the removed state */
								Set<MuisState> with = new HashSet<>(theState);
								Set<MuisState> without = new HashSet<>(with);
								with.add(value.getValue());
								seekAtts(with, without);
							}

							private void seekAtts(Set<MuisState> matching, Set<MuisState> notMatching) {
								for(StyleAttribute<?> att : localAttributes()) {
									boolean hasMatch = false;
									boolean hasNoMatch = false;
									StyleExpressionValue<StateExpression, ?> matchExp = null;
									for(StyleExpressionValue<StateExpression, ?> exp : getLocalExpressions(att)) {
										boolean expMatch = exp.getExpression().matches(matching);
										boolean expNotMatch = exp.getExpression().matches(notMatching);
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
											AbstractInternallyStatefulStyle.this, (StyleAttribute<Object>) att, matchExp.get(), null, null));
								}
							}
						});
					}
				});
			}
		});
	}
}
