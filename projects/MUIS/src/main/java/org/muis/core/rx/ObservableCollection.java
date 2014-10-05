package org.muis.core.rx;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;

/**
 * A collection whose content can be observed
 *
 * @param <E> The type of element in the collection
 */
public interface ObservableCollection<E> extends Collection<E>, Observable<Observable<E>> {
	Observable<Void> changes();

	/**
	 * @param map The mapping function
	 * @return An observable collection of a new type backed by this collection and the mapping function
	 */
	default <T> ObservableCollection<T> mapC(Function<E, T> map) {
		ObservableCollection<E> outerColl = this;
		class MappedObservableCollection extends java.util.AbstractCollection<T> implements ObservableCollection<T> {
			@Override
			public Observable<Void> changes() {
				return outerColl.changes();
			}

			@Override
			public int size() {
				return outerColl.size();
			}

			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					private final Iterator<E> backing = outerColl.iterator();

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
				Subscription<Observable<E>> sub = outerColl.subscribe(new Observer<Observable<E>>() {
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
						return MappedObservableCollection.this.subscribe(observer2);
					}

					@Override
					public void unsubscribe() {
						sub.unsubscribe();
					}
				};
			}
		}
		return new MappedObservableCollection();
	}

	/**
	 * @param filterMap The mapping function
	 * @return An observable collection of a new type backed by this collection and the mapping function
	 */
	default <T> ObservableCollection<T> filterMapC(Function<E, T> filterMap) {
		ObservableCollection<E> outerColl = this;
		class MappedObservableCollection extends java.util.AbstractCollection<T> implements ObservableCollection<T> {
			@Override
			public Observable<Void> changes() {
				return outerColl.changes();
			}

			@Override
			public int size() {
				int ret = 0;
				for(E value : outerColl)
					if(filterMap.apply(value) != null)
						ret++;
				return ret;
			}

			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					private final Iterator<E> backing = outerColl.iterator();

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
				Subscription<Observable<E>> sub = outerColl.subscribe(new Observer<Observable<E>>() {
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
						return MappedObservableCollection.this.subscribe(observer2);
					}

					@Override
					public void unsubscribe() {
						sub.unsubscribe();
					}
				};
			}
		}
		return new MappedObservableCollection();
	}

	/**
	 * @param coll The collection to flatten
	 * @return A collection containing all elements of all collections in the outer collection
	 */
	public static <T> ObservableCollection<T> flatten(ObservableCollection<? extends ObservableCollection<T>> coll) {
		CompoundObservableSet<T> ret = new CompoundObservableSet<>();
		coll.act(subList -> {
			subList.subscribe(new Observer<ObservableCollection<T>>() {
				@Override
				public <V extends ObservableCollection<T>> void onNext(V value) {
					ret.addSet(value);
				}

				@Override
				public <V extends ObservableCollection<T>> void onCompleted(V value) {
					ret.removeSet(value);
				}
			});
		});
		return ret;
	}

	public static <T> Observable<T> fold(ObservableCollection<? extends Observable<T>> coll) {
		return new Observable<T>() {
			@Override
			public Subscription<T> subscribe(Observer<? super T> observer) {

				// TODO Auto-generated method stub
				return null;
			}
		};
	}
}
