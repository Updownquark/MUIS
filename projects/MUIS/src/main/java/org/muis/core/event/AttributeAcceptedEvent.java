package org.muis.core.event;

import org.muis.core.MuisAttribute;
import org.muis.core.MuisElement;

/**
 * Fired when an attribute is {@link org.muis.core.mgr.AttributeManager#accept(Object, boolean, MuisAttribute, Object) accepted} or
 * {@link org.muis.core.mgr.AttributeManager#reject(Object, MuisAttribute) rejected} on an element.
 */
public class AttributeAcceptedEvent implements MuisEvent {
	private final MuisElement theElement;
	private final boolean isAccepted;
	private final boolean isRequired;
	private final Object theInitialValue;

	/**
	 * @param element The element in which the attribute was just accepted or rejected
	 * @param attr The attribute that was accepted or rejected
	 * @param accepted Whether the attribute was accepted or rejected
	 * @param required Whether the attribute was requested as required or optional
	 * @param initVal The value to set for the attribute if a value is not set already
	 */
	public AttributeAcceptedEvent(MuisElement element, MuisAttribute<?> attr, boolean accepted, boolean required, Object initVal) {
		theElement = element;
		isAccepted = accepted;
		isRequired = required;
		theInitialValue = initVal;
	}

	@Override
	public MuisElement getElement() {
		return theElement;
	}

	/** @return Whether the attribute was accepted or rejected */
	public boolean isAccepted() {
		return isAccepted;
	}

	/** @return Whether the attribute was requested as required or optional */
	public boolean isRequired() {
		return isRequired;
	}

	/** @return The value to set for the attribute if a value is not set already */
	public Object getInitialValue() {
		return theInitialValue;
	}
}
