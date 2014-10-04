package org.muis.core.rx;

import java.util.Iterator;
import java.util.Set;
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

	/**
	 * @param filterMap The mapping function
	 * @return An observable collection of a new type backed by this collection and the mapping function
	 */
	@Override
	default <T> ObservableSet<T> filterMapC(Function<E, T> filterMap) {
		ObservableSet<E> outerColl = this;
		class MappedObservableSet extends java.util.AbstractSet<T> implements ObservableSet<T> {
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
}
