package org.muis.core.rx;

import java.util.AbstractList;
import java.util.List;
import java.util.function.Function;

import prisms.lang.Type;

/**
 * A list whose content can be observed. This list is immutable in that none of its methods, including {@link List} methods, can modify its
 * content (List modification methods will throw {@link UnsupportedOperationException}).
 *
 * @param <E> The type of element in the list
 */
public interface ObservableList<E> extends ObservableCollection<E>, List<E> {
	/**
	 * @param map The mapping function
	 * @return An observable list of a new type backed by this list and the mapping function
	 */
	@Override
	default <T> ObservableList<T> mapC(Function<E, T> map) {
		ObservableList<E> outerList = this;
		class MappedObservableList extends AbstractList<T> implements ObservableList<T> {
			@Override
			public int size() {
				return outerList.size();
			}

			@Override
			public T get(int index) {
				return map.apply(outerList.get(index));
			}

			@Override
			public Subscription<Observable<T>> subscribe(Observer<? super Observable<T>> observer) {
				Subscription<Observable<E>> sub = outerList.subscribe(new Observer<Observable<E>>() {
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
						return MappedObservableList.this.subscribe(observer2);
					}

					@Override
					public void unsubscribe() {
						sub.unsubscribe();
					}
				};
			}
		}
		return new MappedObservableList();
	}

	default <T, V> ObservableValue<V> first(Type type, Function<E, V> map) {
		return new DefaultObservableValue<V>() {
			private V theValue;

			private int theIndex;

			private Observer<ObservableValueEvent<V>> theController;

			{
				theController = control(null);
				theIndex = -1;
				ObservableList.this.act(element -> {
					element.subscribe(new Observer<E>() {
						@Override
						public <V2 extends E> void onNext(V2 value) {
							{
								int listIndex = indexOf(value);
								if(theIndex < 0 || listIndex <= theIndex) {
									V mapped = map.apply(value);
									if(mapped != null)
										newBest(mapped, listIndex);
								}
							}
						}

						@Override
						public <V2 extends E> void onCompleted(V2 value) {
							int listIndex = indexOf(value);
							if(listIndex == theIndex) {
								boolean found = false;
								for(int i = listIndex; i < size(); i++) {
									E val = ObservableList.this.get(i);
									V mapped = map.apply(val);
									if(mapped != null) {
										found = true;
										newBest(mapped, i);
										break;
									}
								}
								if(!found)
									newBest(null, -1);
							} else if(listIndex < theIndex)
								theIndex--;
						}
					});
				});
			}

			@Override
			public Type getType() {
				return type;
			}

			@Override
			public V get() {
				return theValue;
			}

			void newBest(V value, int index) {
				V oldValue = theValue;
				theValue = value;
				theIndex = index;
				theController.onNext(new ObservableValueEvent<>(this, oldValue, theValue, null));
			}
		};
	}

	/**
	 * @param list The list to flatten
	 * @return A list containing all elements of all lists in the outer list
	 */
	public static <T> ObservableList<T> flatten(ObservableList<? extends ObservableList<T>> list) {
		CompoundObservableList<T> ret = new CompoundObservableList<>();
		list.act(subList -> {
			subList.subscribe(new Observer<ObservableList<T>>() {
				@Override
				public <V extends ObservableList<T>> void onNext(V value) {
					ret.addList(value);
				}

				@Override
				public <V extends ObservableList<T>> void onCompleted(V value) {
					ret.removeList(value);
				}
			});
		});
		return ret;
	}

	/**
	 * @param list The list of items for the new list
	 * @return An observable list whose contents are given and never changes
	 */
	public static <T> ObservableList<T> constant(List<T> list) {
		List<T> constList = java.util.Collections.unmodifiableList(list);
		class ConstantObservableList extends AbstractList<T> implements ObservableList<T> {
			@Override
			public Subscription<Observable<T>> subscribe(Observer<? super Observable<T>> observer) {
				return Observable.nullSubscribe(this);
			}

			@Override
			public T get(int index) {
				return constList.get(index);
			}

			@Override
			public int size() {
				return constList.size();
			}
		}
		return new ConstantObservableList();
	}

	/**
	 * @param list The array of items for the new list
	 * @return An observable list whose contents are given and never changes
	 */
	public static <T> ObservableList<T> constant(T... list) {
		return constant(java.util.Arrays.asList(list));
	}

}
