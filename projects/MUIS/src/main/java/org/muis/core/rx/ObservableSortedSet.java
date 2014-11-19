package org.muis.core.rx;

import java.util.NavigableSet;
import java.util.function.Function;

import prisms.lang.Type;

/**
 * A sorted set whose content can be observed. This set is immutable in that none of its methods, including {@link java.util.Set} methods,
 * can modify its content (Set modification methods will throw {@link UnsupportedOperationException}). All {@link ObservableElement}s
 * returned by this observable will be instances of {@link OrderedObservableElement}.
 *
 * @param <E> The type of element in the set
 */
public interface ObservableSortedSet<E> extends ObservableSet<E>, NavigableSet<E> {
	@Override
	default ObservableValue<? extends ObservableSortedSet<E>> changes() {
		return new ObservableValue<ObservableSortedSet<E>>() {
			ObservableSortedSet<E> coll = ObservableSortedSet.this;

			@Override
			public Type getType() {
				return new Type(ObservableList.class, coll.getType());
			}

			@Override
			public ObservableSortedSet<E> get() {
				return coll;
			}

			@Override
			public Runnable internalSubscribe(Observer<? super ObservableValueEvent<ObservableSortedSet<E>>> observer) {
				ObservableValue<ObservableSortedSet<E>> obsVal = this;
				Runnable outerSub = coll.internalSubscribe(new Observer<ObservableElement<E>>() {
					boolean [] finishedInitial = new boolean[1];

					@Override
					public <V extends ObservableElement<E>> void onNext(V value) {
						value.subscribe(new Observer<ObservableValueEvent<E>>() {
							@Override
							public <V2 extends ObservableValueEvent<E>> void onNext(V2 value2) {
								if(finishedInitial[0])
									observer.onNext(new ObservableValueEvent<>(obsVal, null, coll, null));
							}

							@Override
							public <V2 extends ObservableValueEvent<E>> void onCompleted(V2 value2) {
								observer.onNext(new ObservableValueEvent<>(obsVal, null, coll, null));
							}

							@Override
							public void onError(Throwable e) {
								observer.onError(e);
							}
						});
						finishedInitial[0] = true;
					}

					@Override
					public <V extends ObservableElement<E>> void onCompleted(V value) {
						observer.onCompleted(new ObservableValueEvent<>(obsVal, null, coll, null));
					}

					@Override
					public void onError(Throwable e) {
						observer.onError(e);
					}
				});
				observer.onNext(new ObservableValueEvent<>(obsVal, null, coll, null));
				return () -> {
					outerSub.run();
				};
			}
		};
	}

	/**
	 * @param filter The filter function
	 * @return A sorted set containing all elements of this collection that pass the given test
	 */
	@Override
	default ObservableSortedSet<E> filterC(Function<? super E, Boolean> filter) {
		// TODO
		return null;
	}
}
