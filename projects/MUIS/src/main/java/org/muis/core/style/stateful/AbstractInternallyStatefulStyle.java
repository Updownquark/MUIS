package org.muis.core.style.stateful;

import org.muis.core.mgr.MuisState;
import org.muis.core.rx.*;
import org.muis.core.style.MuisStyle;
import org.muis.core.style.StyleAttribute;
import org.muis.core.style.StyleAttributeEvent;
import org.muis.core.style.StyleExpressionValue;

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
		return new org.muis.util.ObservableListWrapper<MuisStyle>(getConditionalDependencies().mapC(depend -> {
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
		for(StyleExpressionValue<StateExpression, ?> sev : getExpressions(attr))
			if(sev.getExpression() == null || sev.getExpression().matches(theState))
				return true;
		return false;
	}

	@Override
	public <T> ObservableValue<T> getLocal(StyleAttribute<T> attr) {
		return new org.muis.util.ObservableValueWrapper<T>(ObservableValue.flatten(
			attr.getType().getType(),
			getLocalExpressions(attr).refireWhen(theState.changes()).filterC(sev -> {
				return sev.getExpression() == null || sev.getExpression().matches(theState);
			}).first()).mapEvent(event -> mapEvent(attr, event))) {
			@Override
			public String toString() {
				return AbstractInternallyStatefulStyle.this + ".getLocal(" + attr + ")";
			}
		};
	}

	@Override
	public ObservableSet<StyleAttribute<?>> localAttributes() {
		return new org.muis.util.ObservableSetWrapper<StyleAttribute<?>>(allLocal().refireWhen(theState.changes()).filterMapC(attr -> {
			if(attr == null)
				return null;
			for(StyleExpressionValue<StateExpression, ?> sev : getLocalExpressions(attr))
				if(sev.getExpression() == null || sev.getExpression().matches(theState))
					return attr;
			return null;
		})) {
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
			public Runnable internalSubscribe(Observer<? super StyleAttributeEvent<?>> observer) {
				return theState.internalSubscribe(new Observer<ObservableElement<MuisState>>() {
					@Override
					public <V extends ObservableElement<MuisState>> void onNext(V elValue) {
						elValue.internalSubscribe(new Observer<ObservableValueEvent<MuisState>>() {
							@Override
							public <V2 extends ObservableValueEvent<MuisState>> void onNext(V2 value) {
								/* Find attributes that have expressions matching the state set *without* the new state but none *with*
								 * the new state */
								// TODO
							}

							@Override
							public <V2 extends ObservableValueEvent<MuisState>> void onCompleted(V2 value) {
								/* Find attributes that have expressions matching the state set *with* the removed state but none *without*
								 * the removed state */
								// TODO
							}
						});
					}
				});
			}
		});
	}
}
