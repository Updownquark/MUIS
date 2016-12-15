package org.quick.core.model;

/**
 * Installs listeners that perform actions on a widget, giving it a certain behavior
 *
 * @param <E> The type of the widget (or interface) that this behavior can be installed on
 */
public interface QuickBehavior<E> {
	/** @param element The element to install this behavior in */
	void install(E element);

	/** @param element The element to uninstall this behavior from */
	void uninstall(E element);
}
