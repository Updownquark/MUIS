package org.muis.core.style.stateful;

import java.util.Iterator;

import org.muis.core.mgr.MuisState;
import org.muis.core.style.*;

import prisms.util.ArrayUtils;

/** A {@link MuisStyle} implementation that gets all its information from a {@link StatefulStyle} for a particular state */
public class StatefulStyleSample implements MuisStyle {
	private final StatefulStyle theStatefulStyle;

	private final MuisState [] theState;

	private java.util.HashMap<StyleListener, StyleExpressionListener<StatefulStyle, StateExpression>> theListenerMap;

	/**
	 * @param statefulStyle The stateful style to get the attribute information from
	 * @param state The state to get the attribute information for
	 */
	public StatefulStyleSample(StatefulStyle statefulStyle, MuisState [] state) {
		theStatefulStyle = statefulStyle;
		theState = state;
	}

	/** @return The stateful style that this style uses to get its attribute information from */
	public StatefulStyle getStatefulStyle() {
		return theStatefulStyle;
	}

	/** @return The state that this style gets its attribute information from its stateful style for */
	public MuisState [] getState() {
		return theState;
	}

	@Override
	public Iterator<StyleAttribute<?>> iterator() {
		Iterable<StyleAttribute<?>> [] array = new Iterable[getDependencies().length];
		System.arraycopy(getDependencies(), 0, array, 0, array.length);
		return ArrayUtils.iterable(ArrayUtils.add(array, localAttributes(), 0)).iterator();
	}

	@Override
	public MuisStyle [] getDependencies() {
		StatefulStyle [] deps = theStatefulStyle.getConditionalDependencies();
		MuisStyle [] ret = new MuisStyle[deps.length];
		for(int i = 0; i < ret.length; i++)
			ret[i] = new StatefulStyleSample(deps[i], theState);
		return ret;
	}

	@Override
	public boolean isSet(StyleAttribute<?> attr) {
		for(StyleExpressionValue<StateExpression, ?> value : theStatefulStyle.getLocalExpressions(attr))
			if(value.getExpression() == null || value.getExpression().matches(theState))
				return true;
		return false;
	}

	@Override
	public boolean isSetDeep(StyleAttribute<?> attr) {
		if(isSet(attr))
			return true;
		for(MuisStyle dep : getDependencies())
			if(dep.isSetDeep(attr))
				return true;
		return false;
	}

	@Override
	public <T> T getLocal(StyleAttribute<T> attr) {
		for(StyleExpressionValue<StateExpression, T> value : theStatefulStyle.getLocalExpressions(attr))
			if(value.getExpression() == null || value.getExpression().matches(theState))
				return value.getValue();
		return null;
	}

	@Override
	public Iterable<StyleAttribute<?>> localAttributes() {
		return new Iterable<StyleAttribute<?>>() {
			@Override
			public Iterator<StyleAttribute<?>> iterator() {
				return ArrayUtils.conditionalIterator(theStatefulStyle.allLocal().iterator(),
					new ArrayUtils.Accepter<StyleAttribute<?>, StyleAttribute<?>>() {
						@Override
						public StyleAttribute<?> accept(StyleAttribute<?> value) {
							for(StyleExpressionValue<StateExpression, ?> sev : theStatefulStyle.getExpressions(value))
								if(sev.getExpression() == null || sev.getExpression().matches(theState))
									return value;
							return null;
						}
					}, false);
			}
		};
	}

	@Override
	public <T> T get(StyleAttribute<T> attr) {
		T ret = getLocal(attr);
		if(ret != null)
			return ret;
		for(MuisStyle dep : getDependencies()) {
			ret = dep.get(attr);
			if(ret != null)
				return ret;
		}
		return attr.getDefault();
	}

	@Override
	public void addListener(final StyleListener listener) {
		StyleExpressionListener<StatefulStyle, StateExpression> expListener = new StyleExpressionListener<StatefulStyle, StateExpression>() {
			@Override
			public void eventOccurred(StyleExpressionEvent<StatefulStyle, StateExpression, ?> evt) {
				if(evt.getExpression() == null || evt.getExpression().matches(theState)) {
					StyleAttribute<Object> attr = (StyleAttribute<Object>) evt.getAttribute();
					listener.eventOccurred(new org.muis.core.style.StyleAttributeEvent<>(null, StatefulStyleSample.this,
						StatefulStyleSample.this, attr, get(attr)));
				}
			}
		};
		theStatefulStyle.addListener(expListener);
		if(theListenerMap == null)
			theListenerMap = new java.util.HashMap<>();
		theListenerMap.put(listener, expListener);
	}

	@Override
	public void removeListener(StyleListener listener) {
		if(theListenerMap == null)
			return;
		theStatefulStyle.removeListener(theListenerMap.remove(listener));
		if(theListenerMap.isEmpty())
			theListenerMap = null;
	}
}
