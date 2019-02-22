package org.quick.core.mgr;

import org.observe.Observable;
import org.qommons.Transactable;

public interface ObservableResourcePool extends Transactable {
	<T> Observable<T> pool(Observable<T> resource);
}
