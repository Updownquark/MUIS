package org.muis.core.rx;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;

import prisms.lang.Type;

/**
 * A collection whose content can be observed
 *
 * @param <E> The type of element in the collection
 */
public interface ObservableCollection<E> extends Collection<E>, Observable<ObservableElement<E>> {
	/** @return The type of elements in this collection */
	Type getType();

	default ObservableValue<? extends ObservableCollection<E>> changes() {
		return new ObservableValue<ObservableCollection<E>>() {
			ObservableCollection<E> coll = ObservableCollection.this;

			@Override
			public Type getType() {
				return new Type(ObservableCollection.class, coll.getType());
			}

			@Override
			public ObservableCollection<E> get() {
				return coll;
			}

			@Override
			public Subscription<ObservableValueEvent<ObservableCollection<E>>> subscribe(
				Observer<? super ObservableValueEvent<ObservableCollection<E>>> observer) {
				ObservableValue<ObservableCollection<E>> obsVal = this;
				Subscription<ObservableElement<E>> outerSub = coll.subscribe(new Observer<ObservableElement<E>>() {
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
				return new DefaultSubscription<ObservableValueEvent<ObservableCollection<E>>>(this) {
					@Override
					public void unsubscribeSelf() {
						outerSub.unsubscribe();
					}
				};
			}
		};
	}

	default Observable<ObservableElement<E>> removes() {
		ObservableCollection<E> coll = this;
		return new Observable<ObservableElement<E>>() {
			@Override
			public Subscription<ObservableElement<E>> subscribe(Observer<? super ObservableElement<E>> observer) {
				Subscription<ObservableElement<E>> sub = coll.subscribe(new Observer<ObservableElement<E>>() {
					@Override
					public <V extends ObservableElement<E>> void onNext(V element) {
						element.completed().act(value -> observer.onNext(element));
					}
				});
				return new DefaultSubscription<ObservableElement<E>>(this) {
					@Override
					public void unsubscribeSelf() {
						sub.unsubscribe();
					}
				};
			}
		};
	}

	/**
	 * @param map The mapping function
	 * @return An observable collection of a new type backed by this collection and the mapping function
	 */
	default <T> ObservableCollection<T> mapC(Function<? super E, T> map) {
		return mapC(ComposedObservableValue.getReturnType(map), map);
	}

	default <T> ObservableCollection<T> mapC(Type type, Function<? super E, T> map) {
		ObservableCollection<E> outerColl = this;
		class MappedObservableCollection extends java.util.AbstractCollection<T> implements ObservableCollection<T> {
			@Override
			public Type getType() {
				return type;
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
			public Subscription<ObservableElement<T>> subscribe(Observer<? super ObservableElement<T>> observer) {
				Subscription<ObservableElement<E>> sub = outerColl.subscribe(new Observer<ObservableElement<E>>() {
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
				return new DefaultSubscription<ObservableElement<T>>(this) {
					@Override
					public void unsubscribeSelf() {
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
	default <T> ObservableCollection<T> filterMapC(Function<? super E, T> filterMap) {
		return filterMapC(ComposedObservableValue.getReturnType(filterMap), filterMap);
	}

	default <T> ObservableCollection<T> filterMapC(Type type, Function<? super E, T> filterMap) {
		ObservableCollection<E> outerColl = this;
		class MappedObservableCollection extends java.util.AbstractCollection<T> implements ObservableCollection<T> {
			@Override
			public Type getType() {
				return type;
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
			public Subscription<ObservableElement<T>> subscribe(Observer<? super ObservableElement<T>> observer) {
				Subscription<ObservableElement<E>> sub = outerColl.subscribe(new Observer<ObservableElement<E>>() {
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
				return new DefaultSubscription<ObservableElement<T>>(this) {
					@Override
					public void unsubscribeSelf() {
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
	public static <T> ObservableSet<T> flatten(ObservableCollection<? extends ObservableCollection<T>> coll) {
		class ComposedObservableSet extends java.util.AbstractSet<T> implements ObservableSet<T> {
			@Override
			public Type getType() {
				return coll.getType().getParamTypes().length == 0 ? new Type(Object.class) : coll.getType().getParamTypes()[0];
			}

			@Override
			public int size() {
				int ret = 0;
				for(ObservableCollection<T> subSet : coll)
					ret += subSet.size();
				return ret;
			}

			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					private final Iterator<? extends ObservableCollection<T>> backing = coll.iterator();
					private Iterator<T> backingSub;

					@Override
					public boolean hasNext() {
						while((backingSub == null || !backingSub.hasNext()) && backing.hasNext())
							backingSub = backing.next().iterator();
						return backingSub != null && backingSub.hasNext();
					}

					@Override
					public T next() {
						if((backingSub == null || !backingSub.hasNext()) && !hasNext())
							throw new java.util.NoSuchElementException();
						return backingSub.next();
					}
				};
			}

			@Override
			public Subscription<ObservableElement<T>> subscribe(Observer<? super ObservableElement<T>> observer) {
				Subscription<? extends ObservableElement<? extends ObservableCollection<T>>> sub = coll
					.subscribe(new Observer<ObservableElement<? extends ObservableCollection<T>>>() {
						private java.util.Map<ObservableCollection<T>, Subscription<ObservableElement<T>>> subListSubscriptions;

						{
							subListSubscriptions = new java.util.IdentityHashMap<>();
						}

						@Override
						public <V extends ObservableElement<? extends ObservableCollection<T>>> void onNext(V subList) {
							subList.subscribe(new Observer<ObservableValueEvent<? extends ObservableCollection<T>>>() {
								@Override
								public <V2 extends ObservableValueEvent<? extends ObservableCollection<T>>> void onNext(V2 subListEvent) {
									if(subListEvent.getOldValue() != null && subListEvent.getOldValue() != subListEvent.getValue()) {
										Subscription<ObservableElement<T>> subListSub = subListSubscriptions.get(subListEvent.getOldValue());
										if(subListSub != null)
											subListSub.unsubscribe();
									}
									Subscription<ObservableElement<T>> subListSub = subListEvent.getValue().takeUntil(subList)
										.subscribe(new Observer<ObservableElement<T>>() {
											@Override
											public <V3 extends ObservableElement<T>> void onNext(V3 subElement) {
												observer.onNext(new ObservableElement<T>() {
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
													public Subscription<ObservableValueEvent<T>> subscribe(
														Observer<? super ObservableValueEvent<T>> observer2) {
														return subElement.subscribe(observer2);
													}
												});
											}
										});
									subListSubscriptions.put(subListEvent.getValue(), subListSub);
								}
							});
						}

						@Override
						public void onError(Throwable e) {
							observer.onError(e);
						}
					});
				return new DefaultSubscription<ObservableElement<T>>(this) {
					@Override
					public void unsubscribeSelf() {
						sub.unsubscribe();
					}
				};
			}
		}
		return new ComposedObservableSet();
	}

	public static <T> Observable<T> fold(ObservableCollection<? extends Observable<T>> coll) {
		return new Observable<T>() {
			@Override
			public Subscription<T> subscribe(Observer<? super T> observer) {
				Subscription<?> collSub = coll.subscribe(new Observer<ObservableElement<? extends Observable<T>>>() {
					@Override
					public <V extends ObservableElement<? extends Observable<T>>> void onNext(V element) {
						element.subscribe(new Observer<ObservableValueEvent<? extends Observable<T>>>() {
							private Subscription<?> elSub;

							@Override
							public <V2 extends ObservableValueEvent<? extends Observable<T>>> void onNext(V2 value) {
								if(elSub != null)
									elSub.unsubscribe();
								elSub = value.getValue().takeUntil(element).subscribe(new Observer<T>() {
									@Override
									public <V3 extends T> void onNext(V3 value3) {
										observer.onNext(value3);
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

					@Override
					public void onError(Throwable e) {
						observer.onError(e);
					}
				});
				return new DefaultSubscription<T>(this) {
					@Override
					public void unsubscribeSelf() {
						collSub.unsubscribe();
					}
				};
			}
		};
	}
}
