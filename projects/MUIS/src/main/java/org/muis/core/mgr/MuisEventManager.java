package org.muis.core.mgr;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.muis.core.MuisElement;
import org.muis.core.event.MuisEvent;
import org.muis.core.event.MuisEventCondition;
import org.muis.core.event.MuisEventListener;
import org.muis.core.event.boole.ConditionTree;
import org.muis.core.event.boole.TypedPredicate;

/** Manages events for an element */
public class MuisEventManager implements EventListenerManager {
	private final MuisElement theElement;
	private final ConditionTree<MuisEvent, MuisEventListener<?>> theTree;
	private final ReentrantReadWriteLock theTreeLock;

	/** @param element The element that events are being managed for */
	public MuisEventManager(MuisElement element) {
		theElement = element;
		theTree = new ConditionTree<>();
		theTreeLock = new ReentrantReadWriteLock();
	}

	@Override
	public <T extends MuisEvent> void listen(MuisEventCondition<T> condition, MuisEventListener<T>... listeners) {
		listen(condition.getTester(), listeners);
	}

	@Override
	public <T extends MuisEvent> void remove(MuisEventCondition<T> condition, MuisEventListener<T>... listeners) {
		remove(condition.getTester(), listeners);
	}

	@Override
	public <T extends MuisEvent> void listen(TypedPredicate<MuisEvent, T> condition, MuisEventListener<T>... listeners) {
		Lock lock = theTreeLock.writeLock();
		lock.lock();
		try {
			theTree.add(condition, listeners);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public <T extends MuisEvent> void remove(TypedPredicate<MuisEvent, T> condition, MuisEventListener<T>... listeners) {
		Lock lock = theTreeLock.writeLock();
		lock.lock();
		try {
			theTree.remove(condition, listeners);
		} finally {
			lock.unlock();
		}
	}

	/** @param event The event to fire for the element */
	public <E extends MuisEvent> void fire(E event) {
		if(event.getElement() != theElement) {
			theElement.msg().error("The event[" + event + "] does not apply to this element");
			return;
		}
		List<MuisEventListener<? super E>> listeners;
		Lock lock = theTreeLock.readLock();
		lock.lock();
		try {
			listeners = (List<MuisEventListener<? super E>>) (List<?>) theTree.get(event);
		} finally {
			lock.unlock();
		}

		for(MuisEventListener<? super E> listener : listeners)
			listener.eventOccurred(event);
	}
}
