package org.muis.core.rx;

import java.util.AbstractList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import prisms.lang.Type;

/**
 * A list whose content can be observed. This list is immutable in that none of its methods, including {@link List} methods, can modify its
 * content (List modification methods will throw {@link UnsupportedOperationException}). All {@link ObservableElement}s returned by this
 * observable will be instances of {@link ObservableListElement}.
 *
 * @param <E> The type of element in the list
 */
public interface ObservableList<E> extends ObservableCollection<E>, List<E> {
	@Override
	default ObservableValue<? extends ObservableList<E>> changes() {
		return new ObservableValue<ObservableList<E>>() {
			ObservableList<E> coll = ObservableList.this;

			@Override
			public Type getType() {
				return new Type(ObservableList.class, coll.getType());
			}

			@Override
			public ObservableList<E> get() {
				return coll;
			}

			@Override
			public Runnable internalSubscribe(Observer<? super ObservableValueEvent<ObservableList<E>>> observer) {
				ObservableValue<ObservableList<E>> obsVal = this;
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
	 * @param map The mapping function
	 * @return An observable list of a new type backed by this list and the mapping function
	 */
	@Override
	default <T> ObservableList<T> mapC(Function<? super E, T> map) {
		return mapC(ComposedObservableValue.getReturnType(map), map);
	}

	/**
	 * @param type The type for the mapped list
	 * @param map The mapping function
	 * @return An observable list of a new type backed by this list and the mapping function
	 */
	@Override
	default <T> ObservableList<T> mapC(Type type, Function<? super E, T> map) {
		ObservableList<E> outerList = this;
		class MappedObservableList extends AbstractList<T> implements ObservableList<T> {
			@Override
			public Type getType() {
				return type;
			}

			@Override
			public int size() {
				return outerList.size();
			}

			@Override
			public T get(int index) {
				return map.apply(outerList.get(index));
			}

			@Override
			public Runnable internalSubscribe(Observer<? super ObservableElement<T>> observer) {
				Runnable sub = outerList.internalSubscribe(new Observer<ObservableElement<E>>() {
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
				return () -> {
					sub.run();
				};
			}
		}
		return new MappedObservableList();
	}

	/**
	 * @param filter The filter function
	 * @return A list containing all elements of this list that pass the given test
	 */
	default ObservableList<E> filterC(Function<? super E, Boolean> filter) {
		return filterMapC(getType(), (E value) -> {
			return (value != null && filter.apply(value)) ? value : null;
		});
	}

	@Override
	default <T> ObservableList<T> filterMapC(Function<? super E, T> map) {
		return filterMapC(ComposedObservableValue.getReturnType(map), map);
	}

	@Override
	default <T> ObservableList<T> filterMapC(Type type, Function<? super E, T> map) {
		ObservableList<E> outer = this;
		class FilteredElement implements ObservableListElement<T> {
			private ObservableListElement<E> theWrappedElement;
			private List<FilteredElement> theFilteredElements;
			private boolean exists;

			FilteredElement(ObservableListElement<E> wrapped, List<FilteredElement> filteredEls) {
				theWrappedElement = wrapped;
				theFilteredElements = filteredEls;
			}

			@Override
			public ObservableValue<T> persistent() {
				return theWrappedElement.mapV(map);
			}

			@Override
			public Type getType() {
				return type;
			}

			@Override
			public T get() {
				return map.apply(theWrappedElement.get());
			}

			@Override
			public int getIndex() {
				int ret = 0;
				for(FilteredElement el : theFilteredElements) {
					if(el == this)
						break;
					else if(el.exists)
						ret++;
				}
				return ret;
			}

			boolean exists() {
				return exists;
			}

			@Override
			public Runnable internalSubscribe(Observer<? super ObservableValueEvent<T>> observer2) {
				Runnable [] innerSub = new Runnable[1];
				innerSub[0] = theWrappedElement.internalSubscribe(new Observer<ObservableValueEvent<E>>() {
					@Override
					public <V2 extends ObservableValueEvent<E>> void onNext(V2 elValue) {
						T mapped = map.apply(elValue.getValue());
						if(mapped == null) {
							exists = false;
							T oldValue = map.apply(elValue.getOldValue());
							observer2.onCompleted(new ObservableValueEvent<>(FilteredElement.this, oldValue, oldValue, elValue));
							if(innerSub[0] != null) {
								innerSub[0].run();
								innerSub[0] = null;
							}
						} else {
							exists = true;
							observer2.onNext(new ObservableValueEvent<>(FilteredElement.this, map.apply(elValue.getOldValue()), mapped,
								elValue));
						}
					}

					@Override
					public <V2 extends ObservableValueEvent<E>> void onCompleted(V2 elValue) {
						exists = false;
						T oldVal, newVal;
						if(elValue != null) {
							oldVal = map.apply(elValue.getOldValue());
							newVal = map.apply(elValue.getValue());
						} else {
							oldVal = get();
							newVal = oldVal;
						}
						observer2.onCompleted(new ObservableValueEvent<>(FilteredElement.this, oldVal, newVal, elValue));
					}
				});
				if(!exists) {
					return () -> {
					};
				}
				return innerSub[0];
			}

			@Override
			public String toString() {
				return "filter(" + theWrappedElement + ")";
			}
		}
		class FilteredList extends AbstractList<T> implements ObservableList<T> {
			private List<FilteredElement> theFilteredElements = new java.util.ArrayList<>();

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
			public T get(int index) {
				if(index < 0)
					throw new IndexOutOfBoundsException("" + index);
				int size = 0;
				int idx = index;
				for(E el : outer) {
					T mapped = map.apply(el);
					if(mapped != null) {
						size++;
						if(idx == 0)
							return mapped;
						else
							idx--;
					}
				}
				throw new IndexOutOfBoundsException(index + " of " + size);
			}

			@Override
			public Runnable internalSubscribe(Observer<? super ObservableElement<T>> observer) {
				Runnable listSub = outer.internalSubscribe(new Observer<ObservableElement<E>>() {
					@Override
					public <V extends ObservableElement<E>> void onNext(V el) {
						ObservableListElement<E> outerElement = (ObservableListElement<E>) el;
						FilteredElement retElement = new FilteredElement(outerElement, theFilteredElements);
						theFilteredElements.add(outerElement.getIndex(), retElement);
						outerElement.act(elValue -> {
							if(!retElement.exists()) {
								T mapped = map.apply(elValue.getValue());
								if(mapped != null)
									observer.onNext(retElement);
							}
						});
					}
				});
				return () -> {
					listSub.run();
				};
			}
		}
		return new FilteredList();
	}

	/**
	 * @param <T> The type of the argument value
	 * @param <V> The type of the new observable list
	 * @param arg The value to combine with each of this list's elements
	 * @param func The combination function to apply to this list's elements and the given value
	 * @return An observable list containing this list's elements combined with the given argument
	 */
	default <T, V> ObservableList<V> combineC(ObservableValue<T> arg, BiFunction<? super E, ? super T, V> func) {
		return combineC(arg, ComposedObservableValue.getReturnType(func), func);
	}

	/**
	 * @param <T> The type of the argument value
	 * @param <V> The type of the new observable list
	 * @param arg The value to combine with each of this list's elements
	 * @param type The type for the new list
	 * @param func The combination function to apply to this list's elements and the given value
	 * @return An observable list containing this list's elements combined with the given argument
	 */
	default <T, V> ObservableList<V> combineC(ObservableValue<T> arg, Type type, BiFunction<? super E, ? super T, V> func) {
		ObservableList<E> outerList = this;
		class CombinedObservableList extends AbstractList<V> implements ObservableList<V> {
			@Override
			public Type getType() {
				return type;
			}

			@Override
			public int size() {
				return outerList.size();
			}

			@Override
			public V get(int index) {
				return func.apply(outerList.get(index), arg.get());
			}

			@Override
			public Runnable internalSubscribe(Observer<? super ObservableElement<V>> observer) {
				boolean [] complete = new boolean[1];
				Runnable listSub = outerList.internalSubscribe(new Observer<ObservableElement<E>>() {
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
						listSub.run();
					}
				});
				if(complete[0])
					return () -> {
					};
				return () -> {
					listSub.run();
					argSub.run();
				};
			}
		}
		return new CombinedObservableList();
	}

	/**
	 * @param <V> Type of the observable value to return
	 * @param type The run-time type of the value to return
	 * @param map The mapping function
	 * @return The first non-null mapped value in this list, or null if none of this list's elements map to a non-null value
	 */
	default <V> ObservableValue<V> find(Type type, Function<E, V> map) {
		final Type fType;
		if(type != null)
			fType = type;
		else
			fType = ComposedObservableValue.getReturnType(map);
		return new ObservableValue<V>() {
			@Override
			public Type getType() {
				return fType;
			}

			@Override
			public V get() {
				for(E element : ObservableList.this) {
					V mapped = map.apply(element);
					if(mapped != null)
						return mapped;
				}
				return null;
			}

			@Override
			public Runnable internalSubscribe(Observer<? super ObservableValueEvent<V>> observer) {
				ObservableValue<V> observableVal = this;
				return ObservableList.this.internalSubscribe(new Observer<ObservableElement<E>>() {
					private V theValue;
					private int theIndex;

					@Override
					public <V2 extends ObservableElement<E>> void onNext(V2 element) {
						element.subscribe(new Observer<ObservableValueEvent<E>>() {
							@Override
							public <V3 extends ObservableValueEvent<E>> void onNext(V3 value) {
								{
									int listIndex = indexOf(value);
									if(theIndex < 0 || listIndex <= theIndex) {
										V mapped = map.apply(value.getValue());
										if(mapped != null)
											newBest(mapped, listIndex);
									}
								}
							}

							@Override
							public <V3 extends ObservableValueEvent<E>> void onCompleted(V3 value) {
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
					}

					void newBest(V value, int index) {
						V oldValue = theValue;
						theValue = value;
						theIndex = index;
						observer.onNext(new ObservableValueEvent<>(observableVal, oldValue, theValue, null));
					}
				});
			}
		};
	}

	/**
	 * @param <T> The type of the value to wrap
	 * @param type The type of the elements in the list
	 * @param list The list of items for the new list
	 * @return An observable list whose contents are given and never changes
	 */
	public static <T> ObservableList<T> constant(Type type, List<T> list) {
		class ConstantObservableElement implements ObservableListElement<T> {
			private final Type theType;
			private final T theValue;
			private final int theIndex;

			public ConstantObservableElement(T value, int index) {
				theType = type;
				theValue = value;
				theIndex = index;
			}

			@Override
			public Runnable internalSubscribe(Observer<? super ObservableValueEvent<T>> observer) {
				observer.onNext(new ObservableValueEvent<>(this, theValue, theValue, null));
				return () -> {
				};
			}

			@Override
			public ObservableValue<T> persistent() {
				return this;
			}

			@Override
			public int getIndex() {
				return theIndex;
			}

			@Override
			public Type getType() {
				return theType;
			}

			@Override
			public T get() {
				return theValue;
			}

			@Override
			public String toString() {
				return "" + theValue;
			}
		}
		List<T> constList = java.util.Collections.unmodifiableList(list);
		List<ObservableElement<T>> obsEls = new java.util.ArrayList<>();
		for(int i = 0; i < constList.size(); i++)
			obsEls.add(new ConstantObservableElement(constList.get(i), i));
		class ConstantObservableList extends AbstractList<T> implements ObservableList<T> {
			@Override
			public Type getType() {
				return type;
			}

			@Override
			public Runnable internalSubscribe(Observer<? super ObservableElement<T>> observer) {
				for(ObservableElement<T> ob : obsEls)
					observer.onNext(ob);
				return () -> {
				};
			}

			@Override
			public ObservableValue<ObservableList<T>> changes() {
				return ObservableValue.constant(new Type(ObservableList.class, type), this);
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
	 * @param <T> The type of the elements
	 * @param type The type of the elements in the list
	 * @param list The array of items for the new list
	 * @return An observable list whose contents are given and never changes
	 */
	public static <T> ObservableList<T> constant(Type type, T... list) {
		return constant(type, java.util.Arrays.asList(list));
	}

	/**
	 * @param <T> The super-type of all lists in the wrapping list
	 * @param list The list to flatten
	 * @return A list containing all elements of all lists in the outer list
	 */
	public static <T> ObservableList<T> flatten(ObservableList<? extends ObservableList<T>> list) {
		class ComposedObservableList extends AbstractList<T> implements ObservableList<T> {
			@Override
			public Type getType() {
				return list.getType().getParamTypes().length == 0 ? new Type(Object.class) : list.getType().getParamTypes()[0];
			}

			@Override
			public T get(int index) {
				int idx = index;
				for(ObservableList<T> subList : list) {
					if(idx < subList.size())
						return subList.get(idx);
					else
						idx -= subList.size();
				}
				throw new IndexOutOfBoundsException(index + " out of " + size());
			}

			@Override
			public int size() {
				int ret = 0;
				for(ObservableList<T> subList : list)
					ret += subList.size();
				return ret;
			}

			@Override
			public Runnable internalSubscribe(Observer<? super ObservableElement<T>> observer) {
				return list.internalSubscribe(new Observer<ObservableElement<? extends ObservableList<T>>>() {
					private java.util.Map<ObservableList<T>, Subscription<ObservableElement<T>>> subListSubscriptions;

					{
						subListSubscriptions = new java.util.IdentityHashMap<>();
					}

					@Override
					public <V extends ObservableElement<? extends ObservableList<T>>> void onNext(V subList) {
						subList.subscribe(new Observer<ObservableValueEvent<? extends ObservableList<T>>>() {
							@Override
							public <V2 extends ObservableValueEvent<? extends ObservableList<T>>> void onNext(V2 subListEvent) {
								if(subListEvent.getOldValue() != null && subListEvent.getOldValue() != subListEvent.getValue()) {
									Subscription<ObservableElement<T>> subListSub = subListSubscriptions.get(subListEvent.getOldValue());
									if(subListSub != null)
										subListSub.unsubscribe();
								}
								Subscription<ObservableElement<T>> subListSub = subListEvent.getValue().subscribe(
									new Observer<ObservableElement<T>>() {
										@Override
										public <V3 extends ObservableElement<T>> void onNext(V3 subElement) {
											observer.onNext(new ObservableListElement<T>() {
												@Override
												public ObservableValue<T> persistent() {
													return subElement;
												}

												@Override
												public Type getType() {
													return subElement.getType();
												}

												@Override
												public T get() {
													return subElement.get();
												}

												@Override
												public int getIndex() {
													int subListIndex = ((ObservableListElement<?>) subList).getIndex();
													int ret = 0;
													for(int i = 0; i < subListIndex; i++)
														ret += list.get(i).size();
													ret += ((ObservableListElement<T>) subElement).getIndex();
													return ret;
												}

												@Override
												public Runnable internalSubscribe(Observer<? super ObservableValueEvent<T>> observer2) {
													return subElement.takeUntil(subList.completed()).internalSubscribe(observer2);
												}
											});
										}
									});
								subListSubscriptions.put(subListEvent.getValue(), subListSub);
							}

							@Override
							public <V2 extends ObservableValueEvent<? extends ObservableList<T>>> void onCompleted(V2 subListEvent) {
								subListSubscriptions.remove(subListEvent.getValue()).unsubscribe();
							}

							@Override
							public void onError(Throwable e) {
								observer.onError(e);
							}
						});
					}

					@Override
					public void onError(Throwable e) {
						observer.onError(e);
					}
				});
			}
		}
		return new ComposedObservableList();
	}
}
