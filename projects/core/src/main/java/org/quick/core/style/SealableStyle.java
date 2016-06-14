package org.quick.core.style;

import org.observe.ObservableValue;
import org.observe.collect.ObservableList;
import org.observe.collect.ObservableSet;
import org.observe.collect.impl.ObservableHashSet;
import org.quick.core.mgr.QuickMessageCenter;

import com.google.common.reflect.TypeToken;

/**
 * A style that can be sealed to be immutable. All observables returned by this style are just to satisfy the interface--the implementations
 * assume that the style is sealed and therefore that no changes occur.
 */
public class SealableStyle implements MutableStyle, org.qommons.Sealable {
	private final QuickMessageCenter theMessageCenter;
	private java.util.HashMap<StyleAttribute<?>, ObservableValue<?>> theValues;
	private ObservableSet<StyleAttribute<?>> theObservableAttributes;
	private ObservableSet<StyleAttribute<?>> theExposedAttributes;

	/** Always empty, just here so we can return the same value from the dependencies every time */
	private final ObservableList<QuickStyle> theDepends;

	private boolean isSealed;

	/** Creates a sealable style */
	public SealableStyle(QuickMessageCenter msg) {
		theMessageCenter = msg;
		theValues = new java.util.HashMap<>();
		theDepends = ObservableList.constant(TypeToken.of(QuickStyle.class));
		theObservableAttributes = new ObservableHashSet<>(new TypeToken<StyleAttribute<?>>() {});
		theExposedAttributes = theObservableAttributes.immutable();
	}

	@Override
	public ObservableList<QuickStyle> getDependencies() {
		return theDepends;
	}

	@Override
	public boolean isSet(StyleAttribute<?> attr) {
		return theValues.containsKey(attr);
	}

	@Override
	public <T> ObservableValue<T> getLocal(StyleAttribute<T> attr) {
		ObservableValue<T> ret = (ObservableValue<T>) theValues.get(attr);
		if(ret == null)
			return ObservableValue.constant(attr.getType().getType(), null);
		return ret;
	}

	@Override
	public <T> SealableStyle set(StyleAttribute<T> attr, ObservableValue<T> value) {
		if(isSealed)
			throw new SealedException(this);
		if(value == null) {
			clear(attr);
			return this;
		}
		if(attr == null)
			throw new NullPointerException("Cannot set the value of a null attribute");
		theValues.put(attr, new SafeStyleValue<>(attr, value, theMessageCenter));
		theObservableAttributes.add(attr);
		return this;
	}

	@Override
	public SealableStyle clear(StyleAttribute<?> attr) {
		if(isSealed)
			throw new SealedException(this);
		theValues.remove(attr);
		return this;
	}

	@Override
	public ObservableSet<StyleAttribute<?>> attributes() {
		return theExposedAttributes;
	}

	@Override
	public ObservableSet<StyleAttribute<?>> localAttributes() {
		return theExposedAttributes;
	}

	@Override
	public boolean isSealed() {
		return isSealed;
	}

	@Override
	public void seal() {
		isSealed = true;
	}

	@Override
	public <T> ObservableValue<T> get(StyleAttribute<T> attr, boolean withDefault) {
		ObservableValue<T> ret = (ObservableValue<T>) theValues.get(attr);
		if(withDefault && ret.get() == null)
			return ObservableValue.constant(attr.getDefault());
		return ret;
	}

	@Override
	public SealableStyle clone() {
		SealableStyle ret;
		try {
			ret = (SealableStyle) super.clone();
		} catch(CloneNotSupportedException e) {
			throw new IllegalStateException(e);
		}
		// At the moment, no need to copy the dependencies because it's always empty, but if sealable styles ever do have dependencies,
		// we'll need to copy them here
		ret.theValues = new java.util.HashMap<>();
		ret.theValues.putAll(theValues);
		ret.theObservableAttributes = new ObservableHashSet<>(new TypeToken<StyleAttribute<?>>() {});
		ret.theObservableAttributes.addAll(theObservableAttributes);
		ret.theExposedAttributes = ret.theObservableAttributes.immutable();
		return ret;
	}
}
