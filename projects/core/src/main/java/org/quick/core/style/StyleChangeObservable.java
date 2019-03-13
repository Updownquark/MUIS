package org.quick.core.style;

import java.util.LinkedHashSet;
import java.util.Set;

import org.observe.Observable;
import org.observe.ObservableValueEvent;
import org.observe.Observer;
import org.observe.SimpleObservable;
import org.observe.Subscription;
import org.observe.collect.ObservableCollection;
import org.qommons.ConcurrentHashSet;
import org.qommons.ListenerSet;
import org.qommons.Lockable;
import org.qommons.Transaction;

import com.google.common.reflect.TypeToken;

/** A utility for listening to style changes where the particular attributes of interest may not be known initially and may change */
public class StyleChangeObservable implements Observable<ObservableValueEvent<?>> {
	private final QuickStyle theStyle;
	private final Set<StyleDomain> theDomains;
	private final Set<StyleAttribute<?>> theAttributes;
	private final ListenerSet<Observer<? super ObservableValueEvent<?>>> theStyleListeners;
	private final ListenerSet<Runnable> theWatchListeners;
	private final SimpleObservable<Void> theRestart;
	private Lockable theLockable;
	private Subscription theSubscription;
	private boolean isStarted;

	/** @param style The style to listen to */
	public StyleChangeObservable(QuickStyle style) {
		theStyle = style;
		theDomains = new ConcurrentHashSet<>();
		theAttributes = new LinkedHashSet<>();
		theStyleListeners = new ListenerSet<>();
		theStyleListeners.setUsedListener(used -> {
			if (used) {
				if (!theDomains.isEmpty() || !theAttributes.isEmpty() && isStarted)
					start();
			} else if (theSubscription != null) {
				theSubscription.unsubscribe();
				theSubscription = null;
			}
		});
		theWatchListeners = new ListenerSet<>();
		theRestart = new SimpleObservable<>(null, false, null, null);
	}

	/**
	 * @param style The style to listen to
	 * @param other Another {@link StyleChangeObservable}. This observable will listen to the same attributes as this other observable.
	 */
	public StyleChangeObservable(QuickStyle style, StyleChangeObservable other) {
		theStyle = style;
		theDomains = other.theDomains;
		theAttributes = other.theAttributes;
		theStyleListeners = new ListenerSet<>();
		theStyleListeners.setUsedListener(used -> {
			if (used) {
				if (!theDomains.isEmpty() || !theAttributes.isEmpty() && isStarted)
					start();
			} else if (theSubscription != null) {
				theSubscription.unsubscribe();
				theSubscription = null;
			}
		});
		theWatchListeners = new ListenerSet<>();
		theRestart = new SimpleObservable<>(null, false, null, null);
		other.theWatchListeners.add(() -> {
			restart();
		});
	}

	/** Makes this observable start listening for style events */
	public void begin() {
		if (isStarted)
			return;
		isStarted = true;
		if (theStyleListeners.isUsed())
			start();
	}

	@Override
	public Subscription subscribe(Observer<? super ObservableValueEvent<?>> observer) {
		theStyleListeners.add(observer);
		return () -> {
			theStyleListeners.remove(observer);
		};
	}

	@Override
	public boolean isSafe() {
		return false;
	}

	@Override
	public Transaction lock() {
		return theLockable.lock();
	}

	@Override
	public Transaction tryLock() {
		return theLockable.tryLock();
	}

	/**
	 * Adds one or more domains, for which all style values will be listened to
	 *
	 * @param domains The style domains to watch
	 * @return This observable
	 */
	public StyleChangeObservable watch(StyleDomain... domains) {
		boolean changed = false;
		for (StyleDomain domain : domains)
			changed |= theDomains.add(domain);
		if (changed)
			restart();
		return this;
	}

	/**
	 * Removes one or more domains from the set of domains for which all styles are listened to
	 *
	 * @param domains The domains to cease listening to
	 * @return This observable
	 */
	public StyleChangeObservable unwatch(StyleDomain... domains) {
		boolean changed = false;
		for (StyleDomain domain : domains)
			changed |= theDomains.remove(domain);
		if (changed)
			restart();
		return this;
	}

	/**
	 * Adds one or more attributes which will be listened to
	 *
	 * @param attributes The attributes to watch
	 * @return This observable
	 */
	public StyleChangeObservable watch(StyleAttribute<?>... attributes) {
		boolean changed = false;
		for (StyleAttribute<?> attr : attributes) {
			if (!theDomains.contains(attr.getDomain()))
				changed |= theAttributes.add(attr);
		}
		if (changed)
			restart();
		return this;
	}

	/**
	 * Removes one or more attributes from the set of attributes which are listened to
	 *
	 * @param attributes The attributes to cease listening to
	 * @return This observable
	 */
	public StyleChangeObservable unwatch(StyleAttribute<?>... attributes) {
		boolean changed = false;
		for (StyleAttribute<?> attr : attributes) {
			if (theDomains.contains(attr.getDomain()))
				theAttributes.remove(attr);
			else
				changed |= theAttributes.remove(attr);
		}
		if (changed)
			restart();
		return this;
	}

	private void restart() {
		if (!isStarted)
			return;
		theRestart.onNext(null);
		if (theSubscription != null) {
			theSubscription.unsubscribe();
			theSubscription = null;
		}
		start();
		theWatchListeners.forEach(r -> r.run());
	}

	private static final TypeToken<Observable<? extends ObservableValueEvent<?>>> OBS_TYPE = new TypeToken<Observable<? extends ObservableValueEvent<?>>>() {};

	private void start() {
		if (theStyle == null || !isStarted)
			return;
		ObservableCollection<Observable<? extends ObservableValueEvent<?>>> filteredAttributeChanges = theStyle.attributes().flow()
			.filter(att -> {
				return (theDomains.contains(att.getDomain()) || theAttributes.contains(att)) ? null : "Not interested";
			}).map(OBS_TYPE, att -> theStyle.get(att, true).changes().noInit()).collectActive(theRestart);
		Observable<? extends ObservableValueEvent<?>> styleEventObservable = ObservableCollection.fold(filteredAttributeChanges);
		theSubscription = styleEventObservable.act(evt -> {
			theStyleListeners.forEach(listener -> listener.onNext(evt));
		});
	}
}
