package org.muis.core.style;

import java.util.Iterator;

import org.muis.core.rx.ObservableValue;
import org.muis.core.rx.Observer;
import org.muis.core.rx.Subscription;

/** A style that can be sealed to be immutable */
public class SealableStyle implements MutableStyle, prisms.util.Sealable {
	private java.util.HashMap<StyleAttribute<?>, ObservableValue<?>> theValues;

	private boolean isSealed;

	/** Creates a sealable style */
	public SealableStyle() {
		theValues = new java.util.HashMap<>();
	}

	@Override
	public MuisStyle [] getDependencies() {
		return new MuisStyle[0];
	}

	@Override
	public boolean isSet(StyleAttribute<?> attr) {
		return theValues.containsKey(attr);
	}

	@Override
	public boolean isSetDeep(StyleAttribute<?> attr) {
		return isSet(attr);
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
		T value2 = attr.getType().cast(value);
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
	public Iterable<StyleAttribute<?>> localAttributes() {
		return prisms.util.ArrayUtils.immutableIterable(theValues.keySet());
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
	public <T> ObservableValue<T> get(StyleAttribute<T> attr) {
		ObservableValue<T> ret = (ObservableValue<T>) theValues.get(attr);
		if(ret == null)
			return ObservableValue.constant(attr.getDefault());
		return ret;
	}

	@Override
	public Iterator<StyleAttribute<?>> iterator() {
		return prisms.util.ArrayUtils.immutableIterator(theValues.keySet().iterator());
	}

	@Override
	public SealableStyle clone() {
		SealableStyle ret;
		try {
			ret = (SealableStyle) super.clone();
		} catch(CloneNotSupportedException e) {
			throw new IllegalStateException(e);
		}
		ret.theValues = new java.util.HashMap<>();
		ret.theValues.putAll(theValues);
		return ret;
	}

	@Override
	public Subscription<StyleAttributeEvent<?>> subscribe(Observer<? super StyleAttributeEvent<?>> observer) {
		// Assume the style is sealed and immutable
		return new Subscription<StyleAttributeEvent<?>>() {
			@Override
			public Subscription<StyleAttributeEvent<?>> subscribe(Observer<? super StyleAttributeEvent<?>> observer2) {
				return this;
			}

			@Override
			public void unsubscribe() {
			}
		};
	}
}
