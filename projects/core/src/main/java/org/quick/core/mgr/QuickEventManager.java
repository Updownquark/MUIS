package org.quick.core.mgr;

import org.quick.core.QuickElement;
import org.quick.core.event.QuickEvent;

/** Manages events for an element */
public class QuickEventManager extends org.observe.DefaultObservable<QuickEvent> {
	private final QuickElement theElement;
	private org.observe.Observer<QuickEvent> theController;

	/** @param element The element that events are being managed for */
	public QuickEventManager(QuickElement element) {
		theElement = element;
		theController = control(null);
	}

	/**
	 * @param event The event to fire for the element
	 * @return This manager, for chaining
	 */
	public QuickEventManager fire(QuickEvent event) {
		if(event.getElement() != theElement) {
			theElement.msg().error("The event[" + event + "] does not apply to this element");
			return this;
		}
		theController.onNext(event);
		return this;
	}
}
