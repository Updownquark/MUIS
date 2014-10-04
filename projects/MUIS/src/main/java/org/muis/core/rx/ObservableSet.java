package org.muis.core.rx;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A set whose content can be observed
 *
 * @param <E> The type of element in the set
 */
public interface ObservableSet<E> extends ObservableCollection<E>, Set<E> {
	/**
	 * @param map The mapping function
	 * @return An observable collection of a new type backed by this collection and the mapping function
	 */
	@Override
	default <T> ObservableSet<T> mapC(Function<E, T> map) {
		ObservableSet<E> outerSet = this;
		class MappedObservableSet extends java.util.AbstractSet<T> implements ObservableSet<T> {
			@Override
			public Observable<Void> changes() {
				return outerSet.changes();
			}

			@Override
			public int size() {
				return outerSet.size();
			}

			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					private final Iterator<E> backing = outerSet.iterator();

					@Override
					public boolean hasNext() {
						return backing.hasNext();
					}

					@Override
					public T next() {
						return map.apply(backing.next());
					}

					@Override
					public void remove() {
						backing.remove();
					}
				};
			}

			@Override
			public Subscription<Observable<T>> subscribe(Observer<? super Observable<T>> observer) {
				Subscription<Observable<E>> sub = outerSet.subscribe(new Observer<Observable<E>>() {
					@Override
					public <V extends Observable<E>> void onNext(V value) {
						observer.onNext(value.map(map));
					}

					@Override
					public <V extends Observable<E>> void onCompleted(V value) {
						observer.onCompleted(value.map(map));
					}

					@Override
					public void onError(Throwable e) {
						observer.onError(e);

					}
				});
				return new Subscription<Observable<T>>() {
					@Override
					public Subscription<Observable<T>> subscribe(Observer<? super Observable<T>> observer2) {
						return MappedObservableSet.this.subscribe(observer2);
					}

					@Override
					public void unsubscribe() {
						sub.unsubscribe();
					}
				};
			}
		}
		return new MappedObservableSet();
	}

	default ObservableSet<E> filterC(Function<E, Boolean> filter) {
		return filterMapC(value -> {
			return filter.apply(value) ? value : null;
		});
	}

	/**
	 * @param filterMap The mapping function
	 * @return An observable collection of a new type backed by this collection and the mapping function
	 */
	@Override
	default <T> ObservableSet<T> filterMapC(Function<E, T> filterMap) {
		ObservableSet<E> outerSet = this;
		class MappedObservableSet extends java.util.AbstractSet<T> implements ObservableSet<T> {
			@Override
			public Observable<Void> changes() {
				return outerSet.changes();
			}

			@Override
			public int size() {
				int ret = 0;
				for(E value : outerSet)
					if(filterMap.apply(value) != null)
						ret++;
				return ret;
			}

			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					private final Iterator<E> backing = outerSet.iterator();

					private T theNext;

					@Override
					public boolean hasNext() {
						while(theNext == null && backing.hasNext()) {
							theNext = filterMap.apply(backing.next());
						}
						return theNext != null;
					}

					@Override
					public T next() {
						if(theNext == null && !hasNext())
							throw new java.util.NoSuchElementException();
						T ret = theNext;
						theNext = null;
						return ret;
					}

					@Override
					public void remove() {
						backing.remove();
					}
				};
			}

			@Override
			public Subscription<Observable<T>> subscribe(Observer<? super Observable<T>> observer) {
				Subscription<Observable<E>> sub = outerSet.subscribe(new Observer<Observable<E>>() {
					@Override
					public <V extends Observable<E>> void onNext(V value) {
						observer.onNext(value.map(filterMap));
					}

					@Override
					public <V extends Observable<E>> void onCompleted(V value) {
						observer.onCompleted(value.map(filterMap));
					}

					@Override
					public void onError(Throwable e) {
						observer.onError(e);

					}
				});
				return new Subscription<Observable<T>>() {
					@Override
					public Subscription<Observable<T>> subscribe(Observer<? super Observable<T>> observer2) {
						return MappedObservableSet.this.subscribe(observer2);
					}

					@Override
					public void unsubscribe() {
						sub.unsubscribe();
					}
				};
			}
		}
		return new MappedObservableSet();
	}

	default <T, V> ObservableSet<V> combineC(Observable<T> arg, BiFunction<E, T, V> func) {
		ObservableSet<E> outerSet = this;
		class CombinedObservableSet extends AbstractSet<V> implements ObservableSet<V> {
			private T theArgValue;
			private boolean isArgSet;

			@Override
			public Observable<Void> changes() {
				return outerSet.changes();
			}

			@Override
			public int size() {
				return outerSet.size();
			}

			@Override
			public Iterator<V> iterator() {
				return new Iterator<V>() {
					private final Iterator<E> backing = outerSet.iterator();

					@Override
					public boolean hasNext() {
						return backing.hasNext();
					}

					@Override
					public V next() {
						if(isArgSet)
							return func.apply(backing.next(), theArgValue);
						else {
							backing.next();
							return null;
						}
					}
				};
			}

			@Override
			public Subscription<Observable<V>> subscribe(Observer<? super Observable<V>> observer) {
				boolean [] complete = new boolean[1];
				Subscription<Observable<E>> setSub = outerSet.subscribe(new Observer<Observable<E>>() {
					@Override
					public <V2 extends Observable<E>> void onNext(V2 value) {
						observer.onNext(value.combine(arg, func));
					}

					@Override
					public <V2 extends Observable<E>> void onCompleted(V2 value) {
						complete[0] = true;
						observer.onCompleted(value.combine(arg, func));
					}

					@Override
					public void onError(Throwable e) {
						observer.onError(e);
					}
				});
				if(complete[0])
					return Observable.nullSubscribe(this);
				Subscription<T> argSub = arg.subscribe(new Observer<T>() {
					@Override
					public <V2 extends T> void onNext(V2 value) {
						theArgValue = value;
						isArgSet = true;
					}

					@Override
					public <V2 extends T> void onCompleted(V2 value) {
						complete[0] = true;
						observer.onCompleted(null);
						setSub.unsubscribe();
					}
				});
				if(complete[0])
					return Observable.nullSubscribe(this);
				return new Subscription<Observable<V>>() {
					@Override
					public Subscription<Observable<V>> subscribe(Observer<? super Observable<V>> observer2) {
						if(complete[0])
							return Observable.nullSubscribe(CombinedObservableSet.this);
						return CombinedObservableSet.this.subscribe(observer2);
					}

					@Override
					public void unsubscribe() {
						if(complete[0])
							return;
						setSub.unsubscribe();
						argSub.unsubscribe();
					}
				};
			}
		}
		return new CombinedObservableSet();
	}

	public static <T> ObservableSet<T> constant(java.util.Collection<T> coll) {
		Set<T> modSet = new java.util.LinkedHashSet<>(coll);
		Set<T> constSet = java.util.Collections.unmodifiableSet(modSet);
		class ConstantObservableSet extends AbstractSet<T> implements ObservableSet<T> {
			@Override
			public Subscription<Observable<T>> subscribe(Observer<? super Observable<T>> observer) {
				for(T value : constSet)
					observer.onNext(Observable.constant(value));
				return Observable.nullSubscribe(this);
			}

			@Override
			public Observable<Void> changes() {
				return (Observable<Void>) Observable.empty;
			}

			@Override
			public int size() {
				return constSet.size();
			}

			@Override
			public Iterator<T> iterator() {
				return constSet.iterator();
			}
		}
		return new ConstantObservableSet();
	}
}
