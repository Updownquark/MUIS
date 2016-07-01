package org.quick.core.prop;

import org.qommons.ex.ExFunction;
import org.quick.core.QuickException;

import com.google.common.reflect.TypeToken;

public class TypeMapping<F, T> {
	private final TypeToken<F> theFromType;

	private final TypeToken<T> theToType;

	private final ExFunction<? super F, ? extends T, QuickException> theMap;

	TypeMapping(TypeToken<F> from, TypeToken<T> to, ExFunction<? super F, ? extends T, QuickException> map) {
		this.theFromType = from;
		this.theToType = to;
		this.theMap = map;
	}

	public TypeToken<F> getFromType() {
		return theFromType;
	}

	public TypeToken<T> getToType() {
		return theToType;
	}

	public ExFunction<? super F, ? extends T, QuickException> getMap() {
		return theMap;
	}
}