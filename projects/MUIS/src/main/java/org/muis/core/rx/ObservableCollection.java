package org.muis.core.rx;

import java.util.AbstractCollection;
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

	/** @return An observable that returns this collection whenever any elements in it change */
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
			public Runnable internalSubscribe(Observer<? super ObservableValueEvent<ObservableCollection<E>>> observer) {
				ObservableValue<ObservableCollection<E>> obsVal = this;
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

	/** @return An observable that passes along only events for removal of elements from the collection */
	default Observable<ObservableValueEvent<E>> removes() {
		ObservableCollection<E> coll = this;
		return new Observable<ObservableValueEvent<E>>() {
			@Override
			public Runnable internalSubscribe(Observer<? super ObservableValueEvent<E>> observer) {
				return coll.internalSubscribe(new Observer<ObservableElement<E>>() {
					@Override
					public <V extends ObservableElement<E>> void onNext(V element) {
						element.completed().act(value -> observer.onNext(value));
					}
				});
			}

			@Override
			public String toString() {
				return "removes(" + coll + ")";
			}
		};
	}

	/**
	 * @param <T> The type of the new collection
	 * @param map The mapping function
	 * @return An observable collection of a new type backed by this collection and the mapping function
	 */
	default <T> ObservableCollection<T> mapC(Function<? super E, T> map) {
		return mapC(ComposedObservableValue.getReturnType(map), map);
	}

	/**
	 * @param <T> The type of the mapped collection
	 * @param type The run-time type for the mapped collection (may be null)
	 * @param map The mapping function to map the elements of this collection
	 * @return The mapped collection
	 */
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
			public Runnable internalSubscribe(Observer<? super ObservableElement<T>> observer) {
				Runnable sub = outerColl.internalSubscribe(new Observer<ObservableElement<E>>() {
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
		return new MappedObservableCollection();
	}

	/**
	 * @param filter The filter function
	 * @return A collection containing all elements passing the given test
	 */
	default ObservableCollection<E> filterC(Function<? super E, Boolean> filter) {
		return filterMapC(value -> {
			return (value != null && filter.apply(value)) ? value : null;
		});
	}

	/**
	 * @param <T> The type of the mapped collection
	 * @param filterMap The mapping function
	 * @return An observable collection of a new type backed by this collection and the mapping function
	 */
	default <T> ObservableCollection<T> filterMapC(Function<? super E, T> filterMap) {
		return filterMapC(ComposedObservableValue.getReturnType(filterMap), filterMap);
	}

	/**
	 * @param <T> The type of the mapped collection
	 * @param type The run-time type of the mapped collection
	 * @param map The mapping function
	 * @return A collection containing every element in this collection for which the mapping function returns a non-null value
	 */
	default <T> ObservableCollection<T> filterMapC(Type type, Function<? super E, T> map) {
		ObservableCollection<E> outer = this;
		class FilteredCollection extends AbstractCollection<T> implements ObservableCollection<T> {
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
					private T nextVal;

					@Override
					public boolean hasNext() {
						while(nextVal == null && backing.hasNext()) {
							nextVal = map.apply(backing.next());
						}
						return nextVal != null;
					}

					@Override
					public T next() {
						if(nextVal == null && !hasNext())
							throw new java.util.NoSuchElementException();
						T ret = nextVal;
						nextVal = null;
						return ret;
					}
				};
			}

			@Override
			public Runnable internalSubscribe(Observer<? super ObservableElement<T>> observer) {
				Runnable listSub = outer.internalSubscribe(new Observer<ObservableElement<E>>() {
					@Override
					public <V extends ObservableElement<E>> void onNext(V el) {
						FilteredElement<T, E> retElement = new FilteredElement<>(el, map, type);
						el.act(elValue -> {
							if(!retElement.isIncluded()) {
								T mapped = map.apply(elValue.getValue());
								if(mapped != null)
									observer.onNext(retElement);
							}
						});
					}

					@Override
					public void onError(Throwable e) {
						observer.onError(e);
					}
				});
				return () -> {
					listSub.run();
				};
			}
		}
		return new FilteredCollection();
	}

	/**
	 * @param observable The observable to re-fire events on
	 * @return A collection whose elements fire additional value events when the given observable fires
	 */
	default ObservableCollection<E> refireWhen(Observable<?> observable) {
		ObservableCollection<E> outer = this;
		return new org.muis.util.ObservableCollectionWrapper<E>(this) {
			@Override
			public Runnable internalSubscribe(Observer<? super ObservableElement<E>> observer) {
				return outer.internalSubscribe(new Observer<ObservableElement<E>>() {
					@Override
					public <V extends ObservableElement<E>> void onNext(V value) {
						observer.onNext(value.refireWhen(observable));
					}
				});
			}
		};
	}

	/**
	 * @param <T> An observable collection that contains all elements in all collections in the wrapping collection
	 * @param coll The collection to flatten
	 * @return A collection containing all elements of all collections in the outer collection
	 */
	public static <T> ObservableCollection<T> flatten(ObservableCollection<? extends ObservableCollection<T>> coll) {
		class ComposedObservableCollection extends AbstractCollection<T> implements ObservableCollection<T> {
			@Override
			public Type getType() {
				return coll.getType().getParamTypes().length == 0 ? new Type(Object.class) : coll.getType().getParamTypes()[0];
			}

			@Override
			public int size() {
				int ret = 0;
				for(ObservableCollection<T> subColl : coll)
					ret += subColl.size();
				return ret;
			}

			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					private Iterator<? extends ObservableCollection<T>> outerBacking = coll.iterator();
					private Iterator<? extends T> innerBacking;

					@Override
					public boolean hasNext() {
						while((innerBacking == null || !innerBacking.hasNext()) && outerBacking.hasNext())
							innerBacking = outerBacking.next().iterator();
						return innerBacking != null && innerBacking.hasNext();
					}

					@Override
					public T next() {
						if(!hasNext())
							throw new java.util.NoSuchElementException();
						return innerBacking.next();
					}
				};
			}

			@Override
			public Runnable internalSubscribe(Observer<? super ObservableElement<T>> observer) {
				return coll.internalSubscribe(new Observer<ObservableElement<? extends ObservableCollection<T>>>() {
					private java.util.Map<ObservableCollection<T>, Subscription<ObservableElement<T>>> subCollSubscriptions;

					{
						subCollSubscriptions = new org.muis.util.ConcurrentIdentityHashMap<>();
					}

					@Override
					public <V extends ObservableElement<? extends ObservableCollection<T>>> void onNext(V subColl) {
						subColl.subscribe(new Observer<ObservableValueEvent<? extends ObservableCollection<T>>>() {
							@Override
							public <V2 extends ObservableValueEvent<? extends ObservableCollection<T>>> void onNext(V2 subCollEvent) {
								if(subCollEvent.getOldValue() != null && subCollEvent.getOldValue() != subCollEvent.getValue()) {
									Subscription<ObservableElement<T>> subCollSub = subCollSubscriptions.get(subCollEvent.getOldValue());
									if(subCollSub != null)
										subCollSub.unsubscribe();
								}
								Subscription<ObservableElement<T>> subCollSub = subCollEvent.getValue().subscribe(
									new Observer<ObservableElement<T>>() {
										@Override
										public <V3 extends ObservableElement<T>> void onNext(V3 subElement) {
											observer.onNext(new FlattenedElement<>(subElement, subColl));
										}

										@Override
										public void onError(Throwable e) {
											observer.onError(e);
										}
									});
								subCollSubscriptions.put(subCollEvent.getValue(), subCollSub);
							}

							@Override
							public <V2 extends ObservableValueEvent<? extends ObservableCollection<T>>> void onCompleted(V2 subCollEvent) {
								subCollSubscriptions.remove(subCollEvent.getValue()).unsubscribe();
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
		return new ComposedObservableCollection();
	}

	/**
	 * @param <T> The type of the folded observable
	 * @param coll The collection to fold
	 * @return An observable that is notified for every event on any observable in the collection
	 */
	public static <T> Observable<T> fold(ObservableCollection<? extends Observable<T>> coll) {
		return new Observable<T>() {
			@Override
			public Runnable internalSubscribe(Observer<? super T> observer) {
				return coll.internalSubscribe(new Observer<ObservableElement<? extends Observable<T>>>() {
					@Override
					public <V extends ObservableElement<? extends Observable<T>>> void onNext(V element) {
						element.subscribe(new Observer<ObservableValueEvent<? extends Observable<T>>>() {
							private Subscription<?> elSub;

							@Override
							public <V2 extends ObservableValueEvent<? extends Observable<T>>> void onNext(V2 value) {
								if(elSub != null)
									elSub.unsubscribe();
								elSub = value.getValue().takeUntil(element.completed()).subscribe(new Observer<T>() {
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
			}

			@Override
			public String toString() {
				return "fold(" + coll + ")";
			}
		};
	}

	/**
	 * The type of elements returned from {@link ObservableCollection#filterMapC(Function)}
	 *
	 * @param <T> The type of this element
	 * @param <E> The type of element being wrapped
	 */
	public static class FilteredElement<T, E> implements ObservableElement<T> {
		private final ObservableElement<E> theWrappedElement;
		private final Function<? super E, T> theMap;
		private final Type theType;
		private boolean isIncluded;

		/**
		 * @param wrapped The element to wrap
		 * @param map The mapping function to filter on
		 * @param type The type of the element
		 */
		protected FilteredElement(ObservableElement<E> wrapped, Function<? super E, T> map, Type type) {
			theWrappedElement = wrapped;
			theMap = map;
			theType = type;
		}

		@Override
		public ObservableValue<T> persistent() {
			return theWrappedElement.mapV(theMap);
		}

		@Override
		public Type getType() {
			return theType;
		}

		@Override
		public T get() {
			return theMap.apply(theWrappedElement.get());
		}

		/** @return The element that this filtered element wraps */
		protected ObservableElement<E> getWrapped() {
			return theWrappedElement;
		}

		/** @return The mapping function used by this element */
		protected Function<? super E, T> getMap() {
			return theMap;
		}

		/** @return Whether this element is currently included in the filtered collection */
		protected boolean isIncluded() {
			return isIncluded;
		}

		@Override
		public Runnable internalSubscribe(Observer<? super ObservableValueEvent<T>> observer2) {
			Runnable [] innerSub = new Runnable[1];
			innerSub[0] = theWrappedElement.internalSubscribe(new Observer<ObservableValueEvent<E>>() {
				@Override
				public <V2 extends ObservableValueEvent<E>> void onNext(V2 elValue) {
					T mapped = theMap.apply(elValue.getValue());
					if(mapped == null) {
						isIncluded = false;
						T oldValue = theMap.apply(elValue.getOldValue());
						observer2.onCompleted(new ObservableValueEvent<>(FilteredElement.this, oldValue, oldValue, elValue));
						if(innerSub[0] != null) {
							innerSub[0].run();
							innerSub[0] = null;
						}
					} else {
						isIncluded = true;
						observer2.onNext(new ObservableValueEvent<>(FilteredElement.this, theMap.apply(elValue.getOldValue()), mapped,
							elValue));
					}
				}

				@Override
				public <V2 extends ObservableValueEvent<E>> void onCompleted(V2 elValue) {
					T oldVal, newVal;
					if(elValue != null) {
						oldVal = theMap.apply(elValue.getOldValue());
						newVal = theMap.apply(elValue.getValue());
					} else {
						oldVal = get();
						newVal = oldVal;
					}
					observer2.onCompleted(new ObservableValueEvent<>(FilteredElement.this, oldVal, newVal, elValue));
				}
			});
			if(!isIncluded) {
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

	/**
	 * An element in a {@link ObservableCollection#flatten(ObservableCollection) flattened} collection
	 *
	 * @param <T> The type of the element
	 */
	public class FlattenedElement<T> implements ObservableElement<T> {
		private final ObservableElement<T> subElement;
		private final ObservableElement<? extends ObservableCollection<T>> subCollectionEl;
		private boolean isRemoved;

		/**
		 * @param subEl The sub-collection element to wrap
		 * @param subColl The element containing the sub-collection
		 */
		protected FlattenedElement(ObservableElement<T> subEl, ObservableElement<? extends ObservableCollection<T>> subColl) {
			if(subEl == null)
				throw new NullPointerException();
			subElement = subEl;
			subCollectionEl = subColl;
			subColl.completed().act(value -> isRemoved = true);
		}

		/** @return The element in the outer collection containing the inner collection that contains this element's wrapped element */
		protected ObservableElement<? extends ObservableCollection<T>> getSubCollectionElement() {
			return subCollectionEl;
		}

		/** @return The wrapped sub-collection element */
		protected ObservableElement<T> getSubElement() {
			return subElement;
		}

		@Override
		public ObservableValue<T> persistent() {
			return subElement;
		}

		/** @return Whether this element has been removed or not */
		protected boolean isRemoved() {
			return isRemoved;
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
		public Runnable internalSubscribe(Observer<? super ObservableValueEvent<T>> observer2) {
			return subElement.takeUntil(subCollectionEl.completed()).internalSubscribe(observer2);
		}

		@Override
		public String toString() {
			return "flattened(" + subElement.toString() + ")";
		}
	}
}
