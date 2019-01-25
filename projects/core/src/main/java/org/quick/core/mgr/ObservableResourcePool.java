package org.quick.core.mgr;

import org.observe.Observable;
import org.qommons.Lockable;

public interface ObservableResourcePool extends Lockable {
	<T> Observable<T> pool(Observable<T> resource);
}
