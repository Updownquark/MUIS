package org.quick.core.model;

import org.observe.*;

/**
 * An {@link ObservableAction} that can also return a value. Instances of this type may not be subscribed to.
 *
 * @param <T> The type of value that this action produces
 */
public interface ObservableActionValue<T> extends ObservableValue<T>, ObservableAction {
	@Override
	default Subscription subscribe(Observer<? super ObservableValueEvent<T>> observer) {
		throw new IllegalStateException(ObservableActionValue.class.getSimpleName() + "s may not be subscribed to."
			+ "  They represent discrete actions and values resulting from those actions.");
	}

	@Override
	default void act(Object cause) throws IllegalStateException {
		get();
	}
}
