package org.quick.core.mgr;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.observe.Observable;
import org.observe.ObservableValue;
import org.observe.ObservableValueEvent;
import org.observe.Observer;
import org.observe.SettableStampedValue;
import org.observe.SettableValue;
import org.observe.Subscription;
import org.observe.collect.ObservableCollection;
import org.qommons.QommonsUtils;
import org.qommons.Transaction;
import org.qommons.collect.BetterCollections;
import org.qommons.collect.BetterList;
import org.qommons.collect.BetterSortedSet.SortedSearchFilter;
import org.qommons.collect.CollectionElement;
import org.qommons.collect.CollectionLockingStrategy;
import org.qommons.collect.ElementId;
import org.qommons.collect.ListenerList;
import org.qommons.collect.MutableCollectionElement.StdMsg;
import org.qommons.tree.SortedTreeList;
import org.quick.core.QuickElement;
import org.quick.core.event.AttributeChangedEvent;
import org.quick.core.prop.QuickAttribute;

import com.google.common.reflect.TypeToken;

/**
 * Holds all the {@link QuickAttribute} values for a {@link QuickElement}.
 *
 * TODO Raw attributes not handled yet
 *
 * TODO Multiple attributes of the same name are sorted earliest-added first. Check to see if this is right.
 */
public class AttributeManager2 {
	public class AttributeValue<T> implements SettableStampedValue<T>, Comparable<AttributeValue<?>> {
		private final QuickAttribute<T> theAttribute;
		private final ListenerList<ValueListener<T>> theObservers;
		private final Observable<ObservableValueEvent<T>> theChanges;
		private ElementId theCollectionElement;
		private T theValue;
		private long theStamp;
		private int theAcceptedCount;
		private int theRequiredCount;

		AttributeValue(QuickAttribute<T> attribute) {
			theAttribute = attribute;
			theObservers = ListenerList.build()// Make this list performant--we'll thread-secure it ourselves
				.forEachSafe(false).withFastSize(false).withSyncType(ListenerList.SynchronizationType.LIST).allowReentrant().build();
			theAcceptedCount = 1; // If this is created, then something is interested
			theChanges = new Observable<ObservableValueEvent<T>>() {
				@Override
				public Subscription subscribe(Observer<? super ObservableValueEvent<T>> observer) {
					try (Transaction t = theLocker.lock(false, null)) {
						observer.onNext(new AttributeChangedEvent<>(theElement, theAttribute, true, null, theValue, null));
						return theObservers.add(new ValueListener<>(observer, theStamp, theValue), true)::run;
					}
				}

				@Override
				public boolean isSafe() {
					return true;
				}

				@Override
				public Transaction lock() {
					return theLocker.lock(false, null);
				}

				@Override
				public Transaction tryLock() {
					return theLocker.tryLock(false, false, null);
				}
			};
		}

		void element(ElementId element) {
			theCollectionElement = element;
		}

		public QuickAttribute<T> getAttribute() {
			return theAttribute;
		}

		public AttributeManager2 getManager() {
			return AttributeManager2.this;
		}

		@Override
		public TypeToken<T> getType() {
			return theAttribute.getType().getType();
		}

		@Override
		public T get() {
			return theValue;
		}

		@Override
		public long getStamp() {
			return theStamp;
		}

		@Override
		public Observable<ObservableValueEvent<T>> changes() {
			return theChanges;
		}

		@Override
		public <V extends T> T set(V value, Object cause) throws IllegalArgumentException, UnsupportedOperationException {
			if (!theAttribute.canAccept(value))
				throw new IllegalArgumentException("Illegal value " + value + " for attribute " + theAttribute);
			try (Transaction t = theLocker.lock(true, cause)) {
				long oldStamp = theStamp++;
				long newStamp = theStamp;
				T oldValue = theValue;
				theValue = value;
				AttributeChangedEvent<T> evt = new AttributeChangedEvent<>(theElement, theAttribute, false, oldValue, value, cause);
				theObservers.forEach(obs -> {
					if (obs.lastUpdate > oldStamp)
						return; // re-entrant update
					AttributeChangedEvent<T> obsEvt;
					if (obs.lastUpdate == oldStamp)
						obsEvt = evt;
					else
						obsEvt = new AttributeChangedEvent<>(theElement, theAttribute, false, obs.knownValue, value, cause);
					obs.lastUpdate = newStamp;
					obs.observer.onNext(obsEvt);
				});
				return oldValue;
			}
		}

		@Override
		public <V extends T> String isAcceptable(V value) {
			if (theAttribute.canAccept(value))
				return null;
			else
				return StdMsg.ILLEGAL_ELEMENT;
		}

		@Override
		public ObservableValue<String> isEnabled() {
			return SettableValue.ALWAYS_ENABLED;
		}

		@Override
		public int compareTo(AttributeValue<?> o) {
			return compare(theAttribute, o.theAttribute);
		}

		void accept() {
			theAcceptedCount++;
		}

		void incRequired() {
			theRequiredCount++;
		}

		void decRequired() {
			theRequiredCount--;
		}

		void reject(boolean wasRequired, Object wanter) {
			if (wasRequired)
				theRequiredCount--;
			theAcceptedCount--;
			if (theAcceptedCount == 0) {
				theObservableAttributes.mutableElement(theCollectionElement).remove();
				AttributeChangedEvent<T> evt = new AttributeChangedEvent<>(theElement, theAttribute, false, theValue, theValue, wanter);
				theObservers.forEach(obs -> {
					obs.observer.onCompleted(evt);
				});
			}
		}
	}

	private static class ValueListener<T> {
		final Observer<? super ObservableValueEvent<T>> observer;
		long lastUpdate;
		T knownValue;

		ValueListener(Observer<? super ObservableValueEvent<T>> observer, long stamp, T knownValue) {
			this.observer = observer;
			lastUpdate = stamp;
			this.knownValue = knownValue;
		}
	}

	public interface AttributeAcceptance {
		AttributeAcceptance required();

		AttributeAcceptance optional();

		void reject();

		AttributeManager2 getAttributes();
	}

	public interface IndividualAttributeAcceptance<T> extends AttributeAcceptance {
		AttributeValue<T> getValue();

		@Override
		IndividualAttributeAcceptance<T> required();

		@Override
		IndividualAttributeAcceptance<T> optional();

		IndividualAttributeAcceptance<T> init(T value);
	}

	static int compare(QuickAttribute<?> attr1, QuickAttribute<?> attr2) {
		return QommonsUtils.compareNumberTolerant(attr1.getName(), attr2.getName(), true, true);
	}

	private static final TypeToken<AttributeValue<?>> ATT_VALUE_TYPE = new TypeToken<AttributeValue<?>>() {};

	private final QuickElement theElement;
	private final CollectionLockingStrategy theLocker;
	private final SortedTreeList<AttributeValue<?>> theSortedAttributes;
	private final ObservableCollection<AttributeValue<?>> theObservableAttributes;
	private final ObservableCollection<AttributeValue<?>> theExposedAttributes;

	public AttributeManager2(QuickElement element, CollectionLockingStrategy lock) {
		theElement = element;
		theLocker = lock;
		// IMPORTANT!! NEVER, EVER modify the bare sorted attributes directly!
		theSortedAttributes = new SortedTreeList<>(lock, AttributeValue::compareTo);
		theObservableAttributes = ObservableCollection.create(ATT_VALUE_TYPE, theSortedAttributes);
		theExposedAttributes = theObservableAttributes.flow().filterMod(f -> f.unmodifiable(StdMsg.UNSUPPORTED_OPERATION, false))
			.collectPassive();
	}

	public <T> AttributeValue<T> accept(QuickAttribute<T> attribute, Object wanter, Consumer<IndividualAttributeAcceptance<T>> acceptance) {
		try (Transaction t = theObservableAttributes.lock(true, wanter)) {
			AttributeValue<T> value = getOrAccept(attribute, wanter, true, acceptance == null ? null : av -> {
				AttributeAcceptanceImpl<T> accepter = new AttributeAcceptanceImpl<>(av, wanter);
				acceptance.accept(accepter);
				accepter.initialized();
			});
			return value;
		}
	}

	private <T> AttributeValue<T> getOrAccept(QuickAttribute<T> attribute, Object wanter, boolean accept,
		Consumer<AttributeValue<T>> onValue) {
		CollectionElement<AttributeValue<?>> found = theSortedAttributes.search(av -> compare(attribute, av.getAttribute()),
			SortedSearchFilter.PreferLess);
		if (found != null) {
			if (found.get().equals(attribute)) {
				if (accept) {
					synchronized (found.get()) {
						if (onValue != null)
							onValue.accept((AttributeValue<T>) found.get());
						found.get().accept();
					}
				}
				return (AttributeValue<T>) found.get();
			} else if (!accept)
				return null;
			int comp = compare(attribute, found.get().getAttribute());
			if (comp == 0) {
				// Same name, different attribute
				// Complete the search to see if we do actually accept the attribute
				// Look left
				CollectionElement<AttributeValue<?>> adj = theSortedAttributes.getAdjacentElement(found.getElementId(), false);
				while (adj != null && compare(attribute, adj.get().getAttribute()) == 0) {
					if (adj.get().getAttribute().equals(attribute))
						return (AttributeValue<T>) adj.get();
					adj = theSortedAttributes.getAdjacentElement(found.getElementId(), false);
				}
				// Look right
				adj = theSortedAttributes.getAdjacentElement(found.getElementId(), true);
				while (adj != null && compare(attribute, adj.get().getAttribute()) == 0) {
					if (adj.get().getAttribute().equals(attribute))
						return (AttributeValue<T>) adj.get();
					adj = theSortedAttributes.getAdjacentElement(found.getElementId(), true);
				}
			}
			// We don't currently accept the attribute. Need to add it.
			AttributeValue<T> value = new AttributeValue<>(attribute);
			if (onValue != null)
				onValue.accept(value);
			found = theObservableAttributes.addElement(value, //
				comp >= 0 ? found.getElementId() : null, comp < 0 ? found.getElementId() : null, true);
			value.element(found.getElementId());
			return value;
		} else if (accept) {
			// No attributes accepted, we'll be the first
			AttributeValue<T> value = new AttributeValue<>(attribute);
			if (onValue != null)
				onValue.accept(value);
			found = theObservableAttributes.addElement(value, true);
			value.element(found.getElementId());
			return value;
		} else
			return null;
	}

	public AttributeAcceptance accept(Object wanter, QuickAttribute<?>... attributes) {
		try (Transaction t = theObservableAttributes.lock(true, wanter)) {
			Map<QuickAttribute<?>, AttributeValue<?>> attrMap = new HashMap<>(attributes.length * 3 / 2);
			for (QuickAttribute<?> attr : attributes) {
				attrMap.computeIfAbsent(attr, a -> getOrAccept(attr, wanter, true, null));
			}
			return new MultiAttributeAcceptance(attrMap, wanter);
		}
	}

	public <T> AttributeValue<T> get(QuickAttribute<T> attribute) throws IllegalArgumentException {
		try (Transaction t = theObservableAttributes.lock(false, null)) {
			return getOrAccept(attribute, null, false, null);
		}
	}

	public BetterList<AttributeValue<?>> getAttributesByName(String name) {
		try (Transaction t = theObservableAttributes.lock(false, null)) {
			CollectionElement<AttributeValue<?>> found = theSortedAttributes.search(
				av -> QommonsUtils.compareNumberTolerant(name, av.getAttribute().getName(), true, true), SortedSearchFilter.PreferLess);
			if (found != null) {
				int comp = QommonsUtils.compareNumberTolerant(name, found.get().getAttribute().getName(), true, true);
				if (comp == 0) {
					// Find the first and last attributes with the same name
					ElementId first = found.getElementId(), last = first;
					// Look left
					CollectionElement<AttributeValue<?>> adj = theSortedAttributes.getAdjacentElement(found.getElementId(), false);
					while (adj != null && QommonsUtils.compareNumberTolerant(name, adj.get().getAttribute().getName(), true, true) == 0) {
						first = adj.getElementId();
						adj = theSortedAttributes.getAdjacentElement(found.getElementId(), false);
					}
					// Look right
					adj = theSortedAttributes.getAdjacentElement(found.getElementId(), true);
					while (adj != null && QommonsUtils.compareNumberTolerant(name, adj.get().getAttribute().getName(), true, true) == 0) {
						last = adj.getElementId();
						adj = theSortedAttributes.getAdjacentElement(found.getElementId(), true);
					}
					return BetterCollections.unmodifiableList(theSortedAttributes.subList(//
						theSortedAttributes.getElementsBefore(first), theSortedAttributes.getElementsBefore(last) + 1));
				} else
					return BetterList.empty();
			} else
				return BetterList.empty();
		}
	}

	public ObservableCollection<AttributeValue<?>> getAllAttributes() {
		return theExposedAttributes;
	}

	public Observable<AttributeChangedEvent<?>> watch(QuickAttribute<?>... attributes) {
		if (attributes.length == 0)
			return Observable.empty();
		Map<QuickAttribute<?>, Subscription[]> attrs = new HashMap<>(attributes.length * 3 / 2);
		for (QuickAttribute<?> attr : attributes)
			attrs.put(attr, new Subscription[1]);
		return new Observable<AttributeChangedEvent<?>>() {
			@Override
			public Subscription subscribe(Observer<? super AttributeChangedEvent<?>> observer) {
				boolean[] initialized = new boolean[1];
				Subscription attrSub = theObservableAttributes.subscribe(evt -> {
					switch (evt.getType()) {
					case add:
						Subscription[] sub = attrs.get(evt.getNewValue().getAttribute());
						if (sub[0] != null) {
							sub[0] = evt.getNewValue().changes().subscribe(new Observer<ObservableValueEvent<?>>() {
								@Override
								public <V extends ObservableValueEvent<?>> void onNext(V value) {
									observer.onNext((AttributeChangedEvent<?>) value);
								}

								@Override
								public <V extends ObservableValueEvent<?>> void onCompleted(V value) {
									observer.onCompleted((AttributeChangedEvent<?>) value);
								}
							});
						}
						break;
					case remove:
						sub = attrs.get(evt.getNewValue().getAttribute());
						if (sub != null) {
							sub[0].unsubscribe();
							sub[0] = null;
						}
						break;
					case set:
						if (evt.getNewValue() != evt.getOldValue()) { // Weird, but whatever
							sub = attrs.get(evt.getNewValue().getAttribute());
							if (sub != null) {
								sub[0].unsubscribe();
								sub[0] = evt.getNewValue().changes().subscribe(new Observer<ObservableValueEvent<?>>() {
									@Override
									public <V extends ObservableValueEvent<?>> void onNext(V value) {
										observer.onNext((AttributeChangedEvent<?>) value);
									}

									@Override
									public <V extends ObservableValueEvent<?>> void onCompleted(V value) {
										observer.onCompleted((AttributeChangedEvent<?>) value);
									}
								});
							}
						}
					}
				}, true);
				initialized[0] = true;
				return () -> {
					try (Transaction t = theLocker.lock(false, null)) {
						for (Subscription[] sub : attrs.values()) {
							if (sub[0] != null) {
								sub[0].unsubscribe();
							}
						}
						attrs.clear();
						attrSub.unsubscribe();
					}
				};
			}

			@Override
			public boolean isSafe() {
				return true;
			}

			@Override
			public Transaction lock() {
				return theLocker.lock(false, null);
			}

			@Override
			public Transaction tryLock() {
				return theLocker.tryLock(false, false, null);
			}
		};
	}

	private class AttributeAcceptanceImpl<T> implements IndividualAttributeAcceptance<T> {
		private final AttributeValue<T> theAttribute;
		private final Object theWanter;
		private boolean isAccepted;
		private boolean isRequired;
		private boolean isInitialized;

		AttributeAcceptanceImpl(AttributeValue<T> attribute, Object wanter) {
			theAttribute = attribute;
			theWanter = wanter;
			isAccepted = true;
		}

		@Override
		public AttributeManager2 getAttributes() {
			return AttributeManager2.this;
		}

		@Override
		public AttributeValue<T> getValue() {
			return theAttribute;
		}

		@Override
		public IndividualAttributeAcceptance<T> required() {
			if (!isAccepted)
				return this;
			synchronized (theAttribute) {
				try (Transaction t = theLocker.lock(false, theWanter)) {
					if (!isAccepted)
						return this;
					else if (!isRequired) {
						isRequired = true;
						theAttribute.incRequired();
					}
				}
			}
			return this;
		}

		@Override
		public IndividualAttributeAcceptance<T> optional() {
			if (!isAccepted)
				return this;
			try (Transaction t = theLocker.lock(false, theWanter)) {
				synchronized (theAttribute) {
					if (!isAccepted)
						return this;
					else if (isRequired) {
						isRequired = false;
						theAttribute.decRequired();
					}
				}
			}
			return this;
		}

		@Override
		public IndividualAttributeAcceptance<T> init(T value) {
			if (isInitialized)
				throw new IllegalStateException("Initialization can only be done during acceptance");
			if (isAccepted && theAttribute.get() == null)
				theAttribute.set(value, theWanter);
			return this;
		}

		@Override
		public void reject() {
			if (!isInitialized)
				throw new IllegalStateException("Cannot reject an attribute during acceptance");
			else if (!isAccepted)
				return;
			try (Transaction t = theLocker.lock(true, theWanter)) {
				synchronized (theAttribute) {
					if (!isAccepted)
						return;
					isAccepted = false;
					theAttribute.reject(isRequired, theWanter);
				}
			}
		}

		void initialized() {
			isInitialized = true;
		}
	}

	private class MultiAttributeAcceptance implements AttributeAcceptance {
		private final Map<QuickAttribute<?>, AttributeValue<?>> theAttributes;
		private final Object theWanter;
		private boolean isAccepted;
		private boolean isRequired;

		MultiAttributeAcceptance(Map<QuickAttribute<?>, AttributeValue<?>> attributes, Object wanter) {
			theAttributes = attributes;
			theWanter = wanter;
			isAccepted = true;
		}

		@Override
		public AttributeAcceptance required() {
			if (!isAccepted)
				return this;
			synchronized (this) {
				if (!isAccepted)
					return this;
				else if (isRequired)
					return this;
				isRequired = true;
			}
			try (Transaction t = theLocker.lock(false, theWanter)) {
				for (AttributeValue<?> attr : theAttributes.values())
					synchronized (attr) {
						attr.incRequired();
					}
			}
			return this;
		}

		@Override
		public AttributeAcceptance optional() {
			if (!isAccepted)
				return this;
			synchronized (this) {
				if (!isAccepted)
					return this;
				else if (!isRequired)
					return this;
				isRequired = false;
			}
			try (Transaction t = theLocker.lock(false, theWanter)) {
				for (AttributeValue<?> attr : theAttributes.values())
					synchronized (attr) {
						attr.decRequired();
					}
			}
			return this;
		}

		@Override
		public void reject() {
			if (!isAccepted)
				return;
			synchronized (this) {
				if (!isAccepted)
					return;
				isAccepted = false;
			}
			try (Transaction t = theLocker.lock(true, theWanter)) {
				for (AttributeValue<?> attr : theAttributes.values())
					synchronized (attr) {
						attr.reject(isRequired, theWanter);
					}
			}
		}

		@Override
		public AttributeManager2 getAttributes() {
			return AttributeManager2.this;
		}
	}
}
