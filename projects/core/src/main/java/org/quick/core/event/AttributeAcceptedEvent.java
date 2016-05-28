package org.quick.core.event;

import org.quick.core.QuickElement;
import org.quick.core.prop.QuickAttribute;

/**
 * Fired when an attribute is {@link org.quick.core.mgr.AttributeManager#accept(Object, boolean, QuickAttribute, Object) accepted} or
 * {@link org.quick.core.mgr.AttributeManager#reject(Object, QuickAttribute []) rejected} on an element.
 */
public class AttributeAcceptedEvent implements QuickEvent {
	/** Filters events of this type */
	public static final AttributeAcceptedEventCondition attAccept = AttributeAcceptedEventCondition.attAccept;

	private final QuickElement theElement;
	private final QuickAttribute<?> theAttribute;
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
	public AttributeAcceptedEvent(QuickElement element, QuickAttribute<?> attr, boolean accepted, boolean required, Object initVal) {
		theElement = element;
		theAttribute = attr;
		isAccepted = accepted;
		isRequired = required;
		theInitialValue = initVal;
	}

	@Override
	public QuickElement getElement() {
		return theElement;
	}

	@Override
	public QuickEvent getCause() {
		return null;
	}

	/** @return The attribute that was accepted or rejected on the element */
	public QuickAttribute<?> getAttribute() {
		return theAttribute;
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

	@Override
	public boolean isOverridden() {
		return false;
	}

	@Override
	public String toString() {
		String action;
		if(!isAccepted)
			action = "rejected";
		else if(isRequired)
			action = "required";
		else
			action = "accepted";
		return theAttribute + " " + action + " in " + theElement;
	}
}
