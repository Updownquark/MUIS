package org.quick.core.mgr;

import org.observe.Observer;
import org.observe.SimpleObservable;
import org.observe.Subscription;
import org.qommons.Transaction;
import org.quick.core.QuickElement;
import org.quick.core.event.QuickEvent;

/** Manages events for an element */
public class QuickEventManager implements org.observe.Observable<QuickEvent> {
	private final QuickElement theElement;
	private final SimpleObservable<QuickEvent> theController;

	/** @param element The element that events are being managed for */
	public QuickEventManager(QuickElement element) {
		theElement = element;
		theController = new SimpleObservable<>(null, false, theElement.getAttributeLocker(), null);
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

	@Override
	public Subscription subscribe(Observer<? super QuickEvent> observer) {
		return theController.subscribe(observer);
	}

	@Override
	public boolean isSafe() {
		return theController.isSafe();
	}

	@Override
	public Transaction lock() {
		return theController.lock();
	}

	@Override
	public Transaction tryLock() {
		return theController.tryLock();
	}
}
