package org.quick.core.model;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.observe.ObservableValueEvent;
import org.observe.SimpleObservable;
import org.observe.SimpleSettableValue;
import org.qommons.collect.ListenerList;

import com.google.common.reflect.TypeToken;

/**
 * A settable value that has its toString() value set
 *
 * @param <T> The type of the value
 */
public class SimpleModelValue<T> extends SimpleSettableValue<T> {
	private final ReentrantReadWriteLock theLock;
	private final String theToString;

	/**
	 * @param type The type for the value
	 * @param nullable Whether this value can take null values
	 * @param toString The toString() value for this value
	 */
	public SimpleModelValue(ReentrantReadWriteLock lock, Class<T> type, boolean nullable, String toString) {
		super(type, nullable);
		theLock = lock;
		theToString = toString;
	}

	/**
	 * @param type The type for the value
	 * @param nullable Whether this value can take null values
	 * @param toString The toString() value for this value
	 */
	public SimpleModelValue(ReentrantReadWriteLock lock, TypeToken<T> type, boolean nullable, String toString) {
		super(type, nullable);
		theLock = lock;
		theToString = toString;
	}

	@Override
	protected SimpleObservable<ObservableValueEvent<T>> createEventer() {
		return new SimpleObservable<>(observer -> fireInitial(observer), true, theLock,
			lb -> lb.forEachSafe(false).withFastSize(false).withSyncType(ListenerList.SynchronizationType.LIST));
	}

	@Override
	public String toString() {
		return theToString;
	}
}
