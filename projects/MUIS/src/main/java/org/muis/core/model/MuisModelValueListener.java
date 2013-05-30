package org.muis.core.model;

/**
 * Listens for changes to the value of a {@link MuisModelValue}
 * 
 * @param <T> The type of model values this listener can listen to
 */
public interface MuisModelValueListener<T> {
	/** @param evt The event representing the change */
	void valueChanged(MuisModelValueEvent<? extends T> evt);
}
