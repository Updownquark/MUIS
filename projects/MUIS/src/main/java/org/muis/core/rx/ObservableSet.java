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

	/**
	 * @param filter The filter function
	 * @return A set containing all elements passing the given test
	 */
	default ObservableSet<E> filterC(Function<? super E, Boolean> filter) {
		return filterMapC(value -> {
			return (value != null && filter.apply(value)) ? value : null;
		});
	}

	@Override
	default <T> ObservableSet<T> filterMapC(Function<? super E, T> filterMap) {
		return filterMapC(ComposedObservableValue.getReturnType(filterMap), filterMap);
	}

	@Override
	default <T> ObservableSet<T> filterMapC(Type type, Function<? super E, T> map) {
		ObservableSet<E> outer = this;
		class FilteredSet extends AbstractSet<T> implements ObservableSet<T> {
			@Override
			public Type getType() {
				return type;
			}

			@Override
			public int size() {
				int ret = 0;
				for(E el : outer)
					if(map.apply(el) != null)
						ret++;
				return ret;
			}

			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					private final Iterator<E> backing = outer.iterator();

					private T theNext;

					@Override
					public boolean hasNext() {
						while(theNext == null && backing.hasNext()) {
							theNext = map.apply(backing.next());
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
				Runnable outerSub = outer.internalSubscribe(new Observer<ObservableElement<E>>() {
					@Override
					public <V extends ObservableElement<E>> void onNext(V outerElement) {
						boolean [] exists = new boolean[1];
						class InnerElement implements ObservableElement<T> {
							@Override
							public ObservableValue<T> persistent() {
								return outerElement.mapV(map);
							}

							@Override
							public Type getType() {
								return type;
							}

							@Override
							public T get() {
								return map.apply(outerElement.get());
							}

							@Override
							public Runnable internalSubscribe(Observer<? super ObservableValueEvent<T>> observer2) {
								Runnable [] innerSub = new Runnable[1];
								innerSub[0] = outerElement.internalSubscribe(new Observer<ObservableValueEvent<E>>() {
									@Override
									public <V2 extends ObservableValueEvent<E>> void onNext(V2 elValue) {
										T mapped = map.apply(elValue.getValue());
										if(mapped == null) {
											exists[0] = false;
											T oldValue = map.apply(elValue.getOldValue());
											observer2
												.onCompleted(new ObservableValueEvent<>(InnerElement.this, oldValue, oldValue, elValue));
											if(innerSub[0] != null) {
												innerSub[0].run();
												innerSub[0] = null;
											}
										} else
											observer2.onNext(new ObservableValueEvent<>(InnerElement.this,
												map.apply(elValue.getOldValue()), mapped, elValue));
									}

									@Override
									public <V2 extends ObservableValueEvent<E>> void onCompleted(V2 elValue) {
										exists[0] = false;
										observer2.onCompleted(new ObservableValueEvent<>(InnerElement.this,
											map.apply(elValue
											.getOldValue()), map.apply(elValue.getValue()), elValue));
									}
								});
								if(!exists[0]) {
									return () -> {
									};
								}
								return () -> {
									innerSub[0].run();
								};
							}

							@Override
							public String toString() {
								return outerElement + "%%";
							}
						}
						ObservableElement<T> retElement = new InnerElement();
						outerElement.subscribe(new Observer<ObservableValueEvent<E>>() {
							@Override
							public <V2 extends ObservableValueEvent<E>> void onNext(V2 elValue) {
								if(!exists[0]) {
									T mapped = map.apply(elValue.getValue());
									if(mapped != null) {
										exists[0] = true;
										observer.onNext(retElement);
									}
								}
							}
						});
					}
				});
				return () -> {
					outerSub.run();
				};
			}
		}
		return new FilteredSet();
	}

	/**
	 * @param <T> The type of the argument value
	 * @param <V> The type of the new observable set
	 * @param arg The value to combine with each of this set's elements
	 * @param func The combination function to apply to this set's elements and the given value
	 * @return An observable set containing this set's elements combined with the given argument
	 */
	default <T, V> ObservableSet<V> combineC(ObservableValue<T> arg, BiFunction<? super E, ? super T, V> func) {
		return combineC(arg, ComposedObservableValue.getReturnType(func), func);
	}

	/**
	 * @param <T> The type of the argument value
	 * @param <V> The type of the new observable set
	 * @param arg The value to combine with each of this set's elements
	 * @param type The type for the new set
	 * @param func The combination function to apply to this set's elements and the given value
	 * @return An observable set containing this set's elements combined with the given argument
	 */
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

	/**
	 * @param <T> The type of the collection
	 * @param type The run-time type of the collection
	 * @param coll The collection with elements to wrap
	 * @return A collection containing the given elements that cannot be changed
	 */
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
				return ObservableValue.constant(new Type(ObservableSet.class, type), this);
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

	/**
	 * @param <T> The type of the collection
	 * @param type The run-time type of the collection
	 * @param values The array with elements to wrap
	 * @return A collection containing the given elements that cannot be changed
	 */
	public static <T> ObservableSet<T> constant(Type type, T... values) {
		return constant(type, java.util.Arrays.asList(values));
	}
}
