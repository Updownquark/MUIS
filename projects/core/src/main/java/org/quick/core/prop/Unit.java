package org.quick.core.prop;

import org.qommons.ex.ExFunction;
import org.quick.core.QuickException;

import com.google.common.reflect.TypeToken;

public class Unit<F, T> extends TypeMapping<F, T> {
	private final String theName;

	public Unit(String name, TypeToken<F> from, TypeToken<T> to, ExFunction<? super F, ? extends T, QuickException> operator) {
		super(from, to, operator);
		theName = name;
	}

	public String getName() {
		return theName;
	}
}