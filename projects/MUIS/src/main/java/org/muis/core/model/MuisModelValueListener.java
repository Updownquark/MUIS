package org.muis.core.model;

/**
 * Listens for changes to the value of a {@link MuisModelValue}. This listener is also called when a value's
 * {@link MuisModelValue#isMutable() mutability} changes.
 *
 * @param <T> The type of model values this listener can listen to
 */
public interface MuisModelValueListener<T> {
	/** @param evt The event representing the change */
	void valueChanged(MuisModelValueEvent<? extends T> evt);
}
