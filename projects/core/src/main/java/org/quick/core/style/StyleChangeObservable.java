package org.quick.core.style;

import java.util.LinkedHashSet;
import java.util.Set;

import org.observe.Observable;
import org.observe.ObservableValueEvent;
import org.observe.Observer;
import org.observe.Subscription;
import org.observe.collect.ObservableCollection;
import org.observe.collect.ObservableSet;
import org.qommons.ConcurrentHashSet;
import org.qommons.ListenerSet;

/** A utility for listening to style changes where the particular attributes of interest may not be known initially and may change */
public class StyleChangeObservable implements Observable<ObservableValueEvent<?>> {
	private final QuickStyle theStyle;
	private final Set<StyleDomain> theDomains;
	private final Set<StyleAttribute<?>> theAttributes;
	private final ListenerSet<Observer<? super ObservableValueEvent<?>>> theStyleListeners;
	private final ListenerSet<Runnable> theWatchListeners;
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
	 * REmoves one or more attributes from the set of attributes which are listened to
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
		if (theSubscription != null) {
			theSubscription.unsubscribe();
			theSubscription = null;
		}
		start();
		theWatchListeners.forEach(r -> r.run());
	}

	private void start() {
		if (theStyle == null || !isStarted)
			return;
		ObservableSet<StyleAttribute<?>> filteredAttributes = theStyle.attributes().filterStatic(att -> {
			return theDomains.contains(att.getDomain()) || theAttributes.contains(att);
		});
		Observable<? extends ObservableValueEvent<?>> styleEventObservable = ObservableCollection
			.fold(filteredAttributes.map(att -> theStyle.get(att, true))).noInit();
		theSubscription = styleEventObservable.act(evt -> {
			theStyleListeners.forEach(listener -> listener.onNext(evt));
		});
	}
}
