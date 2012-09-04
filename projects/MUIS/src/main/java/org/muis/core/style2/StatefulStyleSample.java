package org.muis.core.style2;

import java.util.Iterator;

import org.muis.core.mgr.MuisState;
import org.muis.core.style.StyleAttribute;
import org.muis.core.style.StyleExpressionValue;
import org.muis.core.style.StyleListener;

import prisms.util.ArrayUtils;

public class StatefulStyleSample implements MuisStyle {
	private final StatefulStyle theStatefulStyle;

	private final MuisState [] theState;

	private java.util.HashMap<StyleListener, StyleExpressionListener> theListenerMap;

	public StatefulStyleSample(StatefulStyle statefulStyle, MuisState [] state) {
		theStatefulStyle = statefulStyle;
		theState = state;
	}

	@Override
	public Iterator<StyleAttribute<?>> iterator() {
		return ArrayUtils.iterable(ArrayUtils.add(getDependencies(), localAttributes(), 0)).iterator();
	}

	@Override
	public MuisStyle [] getDependencies() {
		StatefulStyle [] deps = theStatefulStyle.getStatefulDependencies();
		MuisStyle [] ret = new MuisStyle[deps.length];
		for(int i = 0; i < ret.length; i++)
			ret[i] = new StatefulStyleSample(deps[i], theState);
		return ret;
	}

	@Override
	public boolean isSet(StyleAttribute<?> attr) {
		for(StyleExpressionValue<?> value : theStatefulStyle.getExpressions(attr))
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
		for(StyleExpressionValue<T> value : theStatefulStyle.getExpressions(attr))
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
							for(StyleExpressionValue<?> sev : theStatefulStyle.getExpressions(value))
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
		StyleExpressionListener expListener = new StyleExpressionListener() {
			@Override
			public void eventOccurred(StyleExpressionEvent<?> evt) {
				if(evt.getExpression() == null || evt.getExpression().matches(theState)) {
					StyleAttribute<Object> attr = (StyleAttribute<Object>) evt.getAttribute();
					listener.eventOccurred(new org.muis.core.style.StyleAttributeEvent<Object>(StatefulStyleSample.this,
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
