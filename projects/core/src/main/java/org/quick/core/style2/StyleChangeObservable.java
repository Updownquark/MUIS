package org.quick.core.style2;

import java.util.LinkedHashSet;
import java.util.Set;

import org.observe.Observable;
import org.observe.Observer;
import org.observe.Subscription;
import org.observe.collect.ObservableCollection;
import org.observe.collect.ObservableSet;
import org.qommons.ConcurrentHashSet;
import org.qommons.ListenerSet;
import org.quick.core.style.StyleAttribute;
import org.quick.core.style.StyleDomain;

public class StyleChangeObservable implements Observable<StyleAttributeEvent<?>> {
	private final QuickStyle theStyle;
	private final Set<StyleDomain> theDomains;
	private final Set<StyleAttribute<?>> theAttributes;
	private final ListenerSet<Observer<? super StyleAttributeEvent<?>>> theStyleListeners;
	private final ListenerSet<Runnable> theWatchListeners;
	private Subscription theSubscription;

	public StyleChangeObservable(QuickStyle style) {
		theStyle = style;
		theDomains = new ConcurrentHashSet<>();
		theAttributes = new LinkedHashSet<>();
		theStyleListeners = new ListenerSet<>();
		theStyleListeners.setUsedListener(used -> {
			if (used) {
				if (!theDomains.isEmpty() || !theAttributes.isEmpty())
					start();
			} else if (theSubscription != null) {
				theSubscription.unsubscribe();
				theSubscription = null;
			}
		});
		theWatchListeners = new ListenerSet<>();
	}

	public StyleChangeObservable(QuickStyle style, StyleChangeObservable other) {
		theStyle = style;
		theDomains = other.theDomains;
		theAttributes = other.theAttributes;
		theStyleListeners = new ListenerSet<>();
		theStyleListeners.setUsedListener(used -> {
			if (used) {
				if (!theDomains.isEmpty() || !theAttributes.isEmpty())
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

	@Override
	public Subscription subscribe(Observer<? super StyleAttributeEvent<?>> observer) {
		theStyleListeners.add(observer);
		return () -> {
			theStyleListeners.remove(observer);
		};
	}

	@Override
	public boolean isSafe() {
		return false;
	}

	public StyleChangeObservable watch(StyleDomain... domains) {
		boolean changed = false;
		for (StyleDomain domain : domains)
			changed |= theDomains.add(domain);
		if (changed)
			restart();
		return this;
	}

	public StyleChangeObservable unwatch(StyleDomain... domains) {
		boolean changed = false;
		for (StyleDomain domain : domains)
			changed |= theDomains.remove(domain);
		if (changed)
			restart();
		return this;
	}

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
		if (theSubscription == null)
			return;
		theSubscription.unsubscribe();
		theSubscription = null;
		start();
		theWatchListeners.forEach(r -> r.run());
	}

	private void start() {
		ObservableSet<StyleAttribute<?>> filteredAttributes = theStyle.attributes().filterStatic(att -> {
			return theDomains.contains(att.getDomain()) || theAttributes.contains(att);
		});
		Observable<StyleAttributeEvent<?>> styleEventObservable = ObservableCollection
			.fold(filteredAttributes.map(att -> theStyle.get(att, true))).noInit()
			.filterMap(evt -> evt instanceof StyleAttributeEvent ? (StyleAttributeEvent<?>) evt : null);
		theSubscription = styleEventObservable.act(evt -> theStyleListeners.forEach(listener -> listener.onNext(evt)));
	}
}
