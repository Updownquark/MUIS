package org.quick.widget.core.mgr;

import org.observe.Observable;
import org.observe.Observer;
import org.observe.SimpleObservable;
import org.observe.Subscription;
import org.qommons.Transaction;
import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.event.QuickWidgetEvent;

/** Manages events for an element */
public class QuickEventManager implements Observable<QuickWidgetEvent> {
	private final QuickWidget theWidget;
	private final SimpleObservable<QuickWidgetEvent> theController;

	/** @param widget The widget that events are being managed for */
	public QuickEventManager(QuickWidget widget) {
		theWidget = widget;
		theController = new SimpleObservable<>(null, false, theWidget.getElement().getAttributeLocker(), ll -> ll.allowReentrant());
	}

	/**
	 * @param event The event to fire for the element
	 * @return This manager, for chaining
	 */
	public QuickEventManager fire(QuickWidgetEvent event) {
		if (event.getWidget() != theWidget) {
			theWidget.getElement().msg().error("The event[" + event + "] does not apply to this element");
			return this;
		}
		theController.onNext(event);
		return this;
	}

	@Override
	public Subscription subscribe(Observer<? super QuickWidgetEvent> observer) {
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
