package org.muis.core.style;

import java.util.AbstractSet;
import java.util.Iterator;

import org.muis.rx.*;
import org.muis.rx.collect.DefaultObservableList;
import org.muis.rx.collect.ObservableElement;
import org.muis.rx.collect.ObservableList;
import org.muis.rx.collect.ObservableSet;

import prisms.lang.Type;

/**
 * A style that can be sealed to be immutable. All observables returned by this style are just to satisfy the interface--the implementations
 * assume that the style is sealed and therefore that no changes occur.
 */
public class SealableStyle implements MutableStyle, prisms.util.Sealable {
	private java.util.HashMap<StyleAttribute<?>, ObservableValue<?>> theValues;
	private ObservableSet<StyleAttribute<?>> theObservableAttributes;

	/** Always empty, just here so we can return the same value from the dependencies every time */
	private final ObservableList<MuisStyle> theDepends;

	private boolean isSealed;

	/** Creates a sealable style */
	public SealableStyle() {
		theValues = new java.util.HashMap<>();
		theDepends = new DefaultObservableList<>(new Type(MuisStyle.class));
		theObservableAttributes = new ConstantObservableSet();
	}

	@Override
	public ObservableList<MuisStyle> getDependencies() {
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
	public <T> SealableStyle set(StyleAttribute<T> attr, T value) {
		if(isSealed)
			throw new SealedException(this);
		if(value == null) {
			clear(attr);
			return this;
		}
		if(attr == null)
			throw new NullPointerException("Cannot set the value of a null attribute");
		T value2 = attr.getType().cast(attr.getType().getType(), value);
		if(value2 == null)
			throw new ClassCastException(value.getClass().getName() + " instance " + value + " cannot be set for attribute " + attr
				+ " of type " + attr.getType());
		value = value2;
		if(attr.getValidator() != null)
			try {
				attr.getValidator().assertValid(value);
			} catch(org.muis.core.MuisException e) {
				throw new IllegalArgumentException(e.getMessage());
			}
		theValues.put(attr, ObservableValue.constant(value));
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
		return theObservableAttributes;
	}

	@Override
	public ObservableSet<StyleAttribute<?>> localAttributes() {
		return theObservableAttributes;
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
		ret.theObservableAttributes = ret.new ConstantObservableSet();
		return ret;
	}

	class ConstantObservableSet extends AbstractSet<StyleAttribute<?>> implements ObservableSet<StyleAttribute<?>> {
		private Type theType = new Type(StyleAttribute.class, new Type(Object.class, true));

		@Override
		public Type getType() {
			return theType;
		}

		@Override
		public Iterator<StyleAttribute<?>> iterator() {
			if(isSealed)
				return theValues.keySet().iterator();
			else
				return prisms.util.ArrayUtils.immutableIterator(theValues.keySet().iterator());
		}

		@Override
		public int size() {
			return theValues.size();
		}

		@Override
		public Runnable internalSubscribe(Observer<? super ObservableElement<StyleAttribute<?>>> observer) {
			for(StyleAttribute<?> att : theValues.keySet())
				observer.onNext(new ObservableElement<StyleAttribute<?>>() {
					@Override
					public Type getType() {
						return theType;
					}

					@Override
					public StyleAttribute<?> get() {
						return att;
					}

					@Override
					public Runnable internalSubscribe(Observer<? super ObservableValueEvent<StyleAttribute<?>>> observer2) {
						observer2.onNext(new ObservableValueEvent<>(this, null, att, null));
						return ()->{
						};
					}

					@Override
					public ObservableValue<StyleAttribute<?>> persistent() {
						return this;
					}
				});
			return () -> {
			};
		}
	}
}
