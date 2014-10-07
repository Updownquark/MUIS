
package org.muis.core.rx;

public abstract class DefaultSubscription<T> implements Subscription<T> {
	private final Observable<T> theObservable;
	private final java.util.concurrent.CopyOnWriteArrayList<Subscription<T>> theSubscriptions;
	private volatile boolean isAlive;

	public DefaultSubscription(Observable<T> observable) {
		theObservable = observable;
		theSubscriptions = new java.util.concurrent.CopyOnWriteArrayList<>();
		theSubscriptions.add(theObservable.completed().act(value -> {
			unsubscribe();
		}));
	}

	@Override
	public Subscription<T> subscribe(Observer<? super T> observer) {
		if(!isAlive)
			return theObservable.subscribe(observer);
		Subscription<T> ret = theObservable.subscribe(observer);
		theSubscriptions.add(ret);
		return ret;
	}

	@Override
	public void unsubscribe() {
		isAlive = false;
		unsubscribeSelf();
		Subscription<T> [] subs = theSubscriptions.toArray(new Subscription[theSubscriptions.size()]);
		theSubscriptions.clear();
	}

	public abstract void unsubscribeSelf();
}
