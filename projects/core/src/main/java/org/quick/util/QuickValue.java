package org.quick.util;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.observe.ObservableValueEvent;
import org.observe.SimpleObservable;
import org.observe.SimpleSettableValue;

import com.google.common.reflect.TypeToken;

public class QuickValue<T> extends SimpleSettableValue<T> {
	private final ReentrantReadWriteLock theLocker;

	public QuickValue(TypeToken<T> type, boolean nullable, ReentrantReadWriteLock locker) {
		super(type, nullable);
		theLocker = locker;
	}

	@Override
	protected SimpleObservable<ObservableValueEvent<T>> createEventer() {
		return new SimpleObservable<>(observer -> fireInitial(observer), true, theLocker, lb -> lb.forEachSafe(false).withFastSize(false));
	}
}
