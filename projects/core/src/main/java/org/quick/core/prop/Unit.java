package org.quick.core.prop;

import org.qommons.ex.ExFunction;
import org.quick.core.QuickException;

import com.google.common.reflect.TypeToken;

/**
 * Basically a named function, a unit knows how to modify a value when it appears in a unit value expression
 * 
 * @param <F> The type that this unit knows how to convert from
 * @param <T> The type of value that this unit knows how to convert to
 */
public class Unit<F, T> extends TypeMapping<F, T> {
	private final String theName;

	/**
	 * @param name The name of the unit
	 * @param from The type that this unit knows how to convert from
	 * @param to The type that this unit knows how to convert to
	 * @param operator The function to apply to do the conversion
	 */
	public Unit(String name, TypeToken<F> from, TypeToken<T> to, ExFunction<? super F, ? extends T, QuickException> operator) {
		super(from, to, operator);
		theName = name;
	}

	/** @return The name of the unit */
	public String getName() {
		return theName;
	}
}