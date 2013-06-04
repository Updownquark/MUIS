package org.muis.core.model;

import java.lang.reflect.*;

/**
 * Uses a getter and a field or method, or just a constructor, to get a value
 *
 * @param <T> The compile time type of the value returned
 */
public class MemberGetter<T> implements Getter<T> {
	private final Getter<?> theThisGetter;

	private final Member theFieldGetter;

	/**
	 * @param thisGetter The getter to get the "this" value for to use to invoke the field or method. May be null for a static field or
	 *            method or a constructor.
	 * @param fieldGetter The member to invoke to get the value to return. Must be a field, method, or constructor
	 * @throws IllegalArgumentException If the field getter will not be able to be called for any reason
	 */
	public MemberGetter(Getter<?> thisGetter, Member fieldGetter) throws IllegalArgumentException {
		theThisGetter = thisGetter;
		theFieldGetter = fieldGetter;
		if(fieldGetter instanceof Field) {
			if(thisGetter == null && (fieldGetter.getModifiers() & Modifier.STATIC) != 0)
				throw new IllegalArgumentException("A this getter must be supplied with a non-static field for a member getter");
		} else if(fieldGetter instanceof Method) {
			if(thisGetter == null && (fieldGetter.getModifiers() & Modifier.STATIC) != 0)
				throw new IllegalArgumentException("A this getter must be supplied with a non-static method for a member getter");
			Method method = (Method) fieldGetter;
			if(method.getParameterTypes().length > 0)
				throw new IllegalArgumentException("Only methods with no parameters may be used for member getters");
			if(method.getReturnType() == Void.TYPE)
				throw new IllegalArgumentException("Methods returning void may not be used for member getters");
		} else if(fieldGetter instanceof Constructor) {
			Constructor<T> constructor = (Constructor<T>) fieldGetter;
			if(constructor.getParameterTypes().length > 0)
				throw new IllegalArgumentException("Only constructors with no parameters may be used for member getters");
			if((constructor.getDeclaringClass().getModifiers() & Modifier.ABSTRACT) != 0)
				throw new IllegalArgumentException("Constructors to abstract classes may not be used for member getters");
		} else
			throw new IllegalArgumentException("Cannot make a member getter with member type +" + fieldGetter.getClass());
		if(!((AccessibleObject) fieldGetter).isAccessible())
			((AccessibleObject) fieldGetter).setAccessible(true);
	}

	@Override
	public Class<T> getType() {
		if(theFieldGetter instanceof Field)
			return (Class<T>) ((Field) theFieldGetter).getType();
		else if(theFieldGetter instanceof Method)
			return (Class<T>) ((Method) theFieldGetter).getReturnType();
		else
			return ((Constructor<T>) theFieldGetter).getDeclaringClass();
	}

	@Override
	public T get() {
		try {
			if(theFieldGetter instanceof Field)
				return (T) ((Field) theFieldGetter).get(theThisGetter.get());
			else if(theFieldGetter instanceof Method)
				return (T) ((Method) theFieldGetter).invoke(theThisGetter.get());
			else
				return ((Constructor<T>) theFieldGetter).newInstance();
		} catch(IllegalAccessException e) {
			throw new IllegalStateException("Could not access getter " + theFieldGetter, e);
		} catch(IllegalArgumentException e) {
			throw new IllegalStateException("Illegal arguments to field getter.  Should never happen.", e);
		} catch(InvocationTargetException e) {
			throw new IllegalStateException("Getter threw exeption", e);
		} catch(InstantiationException e) {
			throw new IllegalStateException("Getter not instantiable", e);
		}
	}
}
