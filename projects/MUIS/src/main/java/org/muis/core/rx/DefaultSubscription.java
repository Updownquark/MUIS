
package org.muis.core.rx;

public abstract class DefaultSubscription<T> implements Subscription<T> {
	private final Observable<T> theObservable;
	private final java.util.concurrent.CopyOnWriteArrayList<Subscription<T>> theSubsciptions;
	private volatile boolean isAlive;

	public DefaultSubscription(Observable<T> observable) {
		theObservable = observable;
		theSubsciptions = new java.util.concurrent.CopyOnWriteArrayList<>();
		theSubscriptions.add(theObservable.completed().act(value -> {
			isAlive = false;
			theSubsciptions.clear();
		}));
	}

	@Override
	public Subscription<T> subscribe(Observer<? super T> observer) {
		if(!isAlive)
			return theObservable.subscribe(observer);
		Subscription<T> ret = theObservable.subscribe(observer);
		theSubsciptions.add(ret);
		return ret;
	}

	@Override
	public void unsubscribe() {
		isAlive = false;
		unsubscribeSelf();
		Subscription<T> [] subs = theSubsciptions.toArray(new Subscription[theSubsciptions.size()]);
		theSubsciptions.clear();
		// TODO Auto-generated method stub

	}

	public abstract void unsubscribeSelf();
}
