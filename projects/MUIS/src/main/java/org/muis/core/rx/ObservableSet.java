package org.muis.core.rx;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import prisms.lang.Type;

/**
 * A set whose content can be observed
 *
 * @param <E> The type of element in the set
 */
public interface ObservableSet<E> extends ObservableCollection<E>, Set<E> {
	@Override
	default ObservableValue<? extends ObservableSet<E>> changes() {
		return new ObservableValue<ObservableSet<E>>() {
			ObservableSet<E> coll = ObservableSet.this;

			@Override
			public Type getType() {
				return new Type(ObservableSet.class, coll.getType());
			}

			@Override
			public ObservableSet<E> get() {
				return coll;
			}

			@Override
			public Runnable internalSubscribe(Observer<? super ObservableValueEvent<ObservableSet<E>>> observer) {
				ObservableValue<ObservableSet<E>> obsVal = this;
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
				return outerSub;
			}
		};
	}

	/**
	 * @param map The mapping function
	 * @return An observable collection of a new type backed by this collection and the mapping function
	 */
	@Override
	default <T> ObservableSet<T> mapC(Function<? super E, T> map) {
		return mapC(ComposedObservableValue.getReturnType(map), map);
	}

	@Override
	default <T> ObservableSet<T> mapC(Type type, Function<? super E, T> map) {
		ObservableSet<E> outerSet = this;
		class MappedObservableSet extends java.util.AbstractSet<T> implements ObservableSet<T> {
			@Override
			public Type getType() {
				return type;
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
			public Runnable internalSubscribe(Observer<? super ObservableElement<T>> observer) {
				Runnable sub = outerSet.internalSubscribe(new Observer<ObservableElement<E>>() {
					@Override
					public <V extends ObservableElement<E>> void onNext(V value) {
						observer.onNext(value.mapV(map));
					}

					@Override
					public <V extends ObservableElement<E>> void onCompleted(V value) {
						observer.onCompleted(value.mapV(map));
					}

					@Override
					public void onError(Throwable e) {
						observer.onError(e);

					}
				});
				return sub;
			}
		}
		return new MappedObservableSet();
	}

	default ObservableSet<E> filterC(Function<? super E, Boolean> filter) {
		return filterMapC(value -> {
			return filter.apply(value) ? value : null;
		});
	}

	@Override
	default <T> ObservableSet<T> filterMapC(Function<? super E, T> filterMap) {
		return filterMapC(ComposedObservableValue.getReturnType(filterMap), filterMap);
	}

	@Override
	default <T> ObservableSet<T> filterMapC(Type type, Function<? super E, T> filterMap) {
		ObservableSet<E> outerSet = this;
		class MappedObservableSet extends java.util.AbstractSet<T> implements ObservableSet<T> {
			@Override
			public Type getType() {
				return type;
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
			public Runnable internalSubscribe(Observer<? super ObservableElement<T>> observer) {
				return outerSet.internalSubscribe(new Observer<ObservableElement<E>>() {
					@Override
					public <V extends ObservableElement<E>> void onNext(V value) {
						observer.onNext(value.mapV(filterMap));
					}

					@Override
					public <V extends ObservableElement<E>> void onCompleted(V value) {
						observer.onCompleted(value.mapV(filterMap));
					}

					@Override
					public void onError(Throwable e) {
						observer.onError(e);

					}
				});
			}
		}
		return new MappedObservableSet();
	}

	default <T, V> ObservableSet<V> combineC(ObservableValue<T> arg, BiFunction<? super E, ? super T, V> func) {
		return combineC(arg, ComposedObservableValue.getReturnType(func), func);
	}

	default <T, V> ObservableSet<V> combineC(ObservableValue<T> arg, Type type, BiFunction<? super E, ? super T, V> func) {
		ObservableSet<E> outerSet = this;
		class CombinedObservableSet extends AbstractSet<V> implements ObservableSet<V> {
			@Override
			public Type getType() {
				return type;
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
						return func.apply(backing.next(), arg.get());
					}
				};
			}

			@Override
			public Runnable internalSubscribe(Observer<? super ObservableElement<V>> observer) {
				boolean [] complete = new boolean[1];
				Runnable setSub = outerSet.internalSubscribe(new Observer<ObservableElement<E>>() {
					@Override
					public <V2 extends ObservableElement<E>> void onNext(V2 value) {
						observer.onNext(value.combineV(func, arg));
					}

					@Override
					public <V2 extends ObservableElement<E>> void onCompleted(V2 value) {
						complete[0] = true;
						observer.onCompleted(value.combineV(func, arg));
					}

					@Override
					public void onError(Throwable e) {
						observer.onError(e);
					}
				});
				if(complete[0])
					return () -> {
					};
				Runnable argSub = arg.internalSubscribe(new Observer<ObservableValueEvent<T>>() {
					@Override
					public <V2 extends ObservableValueEvent<T>> void onNext(V2 value) {
					}

					@Override
					public <V2 extends ObservableValueEvent<T>> void onCompleted(V2 value) {
						complete[0] = true;
						observer.onCompleted(null);
						setSub.run();
					}
				});
				if(complete[0])
					return () -> {
					};
				return () -> {
					setSub.run();
					argSub.run();
				};
			}
		}
		return new CombinedObservableSet();
	}

	public static <T> ObservableSet<T> constant(Type type, java.util.Collection<T> coll) {
		Set<T> modSet = new java.util.LinkedHashSet<>(coll);
		Set<T> constSet = java.util.Collections.unmodifiableSet(modSet);
		java.util.List<ObservableElement<T>> els = new java.util.ArrayList<>();
		for(T value : constSet)
			els.add(new ObservableElement<T>() {
				@Override
				public Type getType() {
					return type;
				}

				@Override
				public T get() {
					return value;
				}

				@Override
				public Runnable internalSubscribe(Observer<? super ObservableValueEvent<T>> observer) {
					observer.onNext(new ObservableValueEvent<>(this, null, value, null));
					return () -> {
					};
				}

				@Override
				public ObservableValue<T> persistent() {
					return this;
				}
			});
		class ConstantObservableSet extends AbstractSet<T> implements ObservableSet<T> {
			@Override
			public Type getType() {
				return type;
			}

			@Override
			public Runnable internalSubscribe(Observer<? super ObservableElement<T>> observer) {
				for(ObservableElement<T> el : els)
					observer.onNext(el);
				return () -> {
				};
			}

			@Override
			public ObservableValue<? extends ObservableSet<T>> changes() {
				return ObservableValue.constant(new Type(ObservableValue.class, type), this);
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

	public static <T> ObservableSet<T> constant(Type type, T... values) {
		return constant(type, java.util.Arrays.asList(values));
	}
}
