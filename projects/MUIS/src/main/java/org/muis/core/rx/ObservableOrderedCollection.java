package org.muis.core.rx;

import java.util.*;
import java.util.function.Function;

import org.muis.util.ObservableOrderedCollectionWrapper;

import prisms.lang.Type;

/**
 * An ordered collection whose content can be observed. All {@link ObservableElement}s * returned by this observable will be instances of
 * {@link OrderedObservableElement}.
 *
 * @param <E> The type of element in the collection
 */
public interface ObservableOrderedCollection<E> extends ObservableCollection<E> {
	/**
	 * @param <V> Type of the observable value to return
	 * @param type The run-time type of the value to return
	 * @param map The mapping function
	 * @return The first non-null mapped value in this list, or null if none of this list's elements map to a non-null value
	 */
	default <V> ObservableValue<V> find(Type type, Function<E, V> map) {
		if(type != null && type.isPrimitive())
			throw new IllegalArgumentException("Types passed to find() must be nullable");
		final Type fType;
		if(type == null) {
			type = ComposedObservableValue.getReturnType(map);
			if(type.isPrimitive())
				type = new Type(Type.getWrapperType(type.getBaseType()));
		}
		fType = type;
		return new ObservableValue<V>() {
			@Override
			public Type getType() {
				return fType;
			}

			@Override
			public V get() {
				for(E element : ObservableOrderedCollection.this) {
					V mapped = map.apply(element);
					if(mapped != null)
						return mapped;
				}
				return null;
			}

			@Override
			public Runnable internalSubscribe(Observer<? super ObservableValueEvent<V>> observer) {
				if(isEmpty())
					observer.onNext(new ObservableValueEvent<>(this, null, null, null));
				ObservableValue<V> observableVal = this;
				return ObservableOrderedCollection.this.internalSubscribe(new Observer<ObservableElement<E>>() {
					private V theValue;
					private int theIndex = -1;

					@Override
					public <V2 extends ObservableElement<E>> void onNext(V2 element) {
						element.subscribe(new Observer<ObservableValueEvent<E>>() {
							@Override
							public <V3 extends ObservableValueEvent<E>> void onNext(V3 value) {
								int listIndex = ((OrderedObservableElement<?>) value.getObservable()).getIndex();
								if(theIndex < 0 || listIndex <= theIndex) {
									V mapped = map.apply(value.getValue());
									if(mapped != null)
										newBest(mapped, listIndex);
									else if(listIndex == theIndex) {
										findNextBest(listIndex + 1);
									}
								}
							}

							@Override
							public <V3 extends ObservableValueEvent<E>> void onCompleted(V3 value) {
								int listIndex = ((OrderedObservableElement<?>) value.getObservable()).getIndex();
								if(listIndex == theIndex) {
									findNextBest(listIndex + 1);
								} else if(listIndex < theIndex)
									theIndex--;
							}

							private void findNextBest(int index) {
								boolean found = false;
								java.util.Iterator<E> iter = ObservableOrderedCollection.this.iterator();
								int idx = 0;
								for(idx = 0; iter.hasNext() && idx < index; idx++)
									iter.next();
								for(; iter.hasNext(); idx++) {
									E val = iter.next();
									V mapped = map.apply(val);
									if(mapped != null) {
										found = true;
										newBest(mapped, idx);
										break;
									}
								}
								if(!found)
									newBest(null, -1);
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

			@Override
			public String toString() {
				return "find " + fType + " in " + ObservableOrderedCollection.this;
			}
		};
	}

	/**
	 * @param observable The observable to re-fire events on
	 * @return A collection whose elements fire additional value events when the given observable fires
	 */
	default ObservableOrderedCollection<E> refireWhen(Observable<?> observable) {
		ObservableOrderedCollection<E> outer = this;
		return new ObservableOrderedCollectionWrapper<E>(this) {
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

	@Override
	default <T> ObservableOrderedCollection<T> mapC(Function<? super E, T> map) {
		return mapC(ComposedObservableValue.getReturnType(map), map);
	}

	@Override
	default <T> ObservableOrderedCollection<T> mapC(Type type, Function<? super E, T> map) {
		ObservableCollection<E> outerColl = this;
		class MappedObservableCollection extends java.util.AbstractCollection<T> implements ObservableOrderedCollection<T> {
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

	@Override
	default ObservableOrderedCollection<E> filterC(Function<? super E, Boolean> filter) {
		return filterMapC(value -> {
			return (value != null && filter.apply(value)) ? value : null;
		});
	}

	@Override
	default <T> ObservableOrderedCollection<T> filterMapC(Function<? super E, T> filterMap) {
		return filterMapC(ComposedObservableValue.getReturnType(filterMap), filterMap);
	}

	@Override
	default <T> ObservableOrderedCollection<T> filterMapC(Type type, Function<? super E, T> map) {
		ObservableCollection<E> outer = this;
		class FilteredCollection extends AbstractCollection<T> implements ObservableOrderedCollection<T> {
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
	 * @param <E> The type of the collection to sort
	 * @param coll The collection whose elements to sort
	 * @param compare The comparator to sort the elements
	 * @return An observable collection containing all elements in <code>coll</code> (even multiples for which <code>compare</code>
	 *         {@link Comparator#compare(Object, Object) .compare} returns 0), sorted according to <code>compare()</code>.
	 */
	public static <E> ObservableOrderedCollection<E> sort(ObservableCollection<E> coll, java.util.Comparator<? super E> compare) {
		if(compare == null) {
			if(!new Type(Comparable.class).isAssignable(coll.getType()))
				throw new IllegalArgumentException("No natural ordering for collection of type " + coll.getType());
			compare = (Comparator<? super E>) (Comparable<Comparable<?>> o1, Comparable<Comparable<?>> o2) -> o1.compareTo(o2);
		}
		return new SortedObservableCollectionWrapper<>(coll, compare);
	}

	/**
	 * Flattens a collection of ordered collections. The inner collections must be sorted according to the given comparator in order for the
	 * result to be correctly sorted.
	 *
	 * @param <E> The super-type of all collections in the wrapping collection
	 * @param list The collection to flatten
	 * @param compare The comparator to compare elements between collections
	 * @return A collection containing all elements of all collections in the outer collection, sorted according to the given comparator,
	 *         then by order in the outer collection.
	 */
	public static <E> ObservableOrderedCollection<E> flatten(ObservableOrderedCollection<? extends ObservableOrderedCollection<E>> list,
		Comparator<? super E> compare) {
		if(compare == null) {
			compare = (Comparator<? super E>) (Comparable<Comparable<?>> o1, Comparable<Comparable<?>> o2) -> o1.compareTo(o2);
		}
		Comparator<? super E> fCompare = compare;
		class FlattenedObservableOrderedCollection extends AbstractCollection<E> implements ObservableOrderedCollection<E> {
			@Override
			public Type getType() {
				return list.getType().getParamTypes().length == 0 ? new Type(Object.class) : list.getType().getParamTypes()[0];
			}

			@Override
			public int size() {
				int ret = 0;
				for(ObservableOrderedCollection<E> subList : list)
					ret += subList.size();
				return ret;
			}

			@Override
			public Iterator<E> iterator() {
				ArrayList<Iterator<? extends E>> iters = new ArrayList<>();
				for(ObservableOrderedCollection<E> subList : list)
					iters.add(subList.iterator());
				return new Iterator<E>() {
					private Object [] subValues = new Object[iters.size()];
					/** This list represents the indexes of the sub values, as they would be in a list sorted according to the comparator. */
					private prisms.util.IntList indexes = new prisms.util.IntList(subValues.length);
					private boolean initialized;

					@Override
					public boolean hasNext() {
						if(!initialized) {
							for(int i = 0; i < subValues.length; i++) {
								if(iters.get(i).hasNext()) {
									subValues[i] = iters.get(i).next();
									indexes.add(i);
								}
							}
							prisms.util.ArrayUtils.sort(subValues, new prisms.util.ArrayUtils.SortListener<Object>() {
								@Override
								public int compare(Object o1, Object o2) {
									if(o1 == null && o2 == null)
										return 0;
									else if(o1 == null)
										return -1;
									else if(o2 == null)
										return 1;
									return fCompare.compare((E) o1, (E) o2);
								}

								@Override
								public void swapped(Object o1, int idx1, Object o2, int idx2) {
									int firstIdx = indexes.get(idx1);
									indexes.set(idx1, indexes.get(idx2));
									indexes.set(idx2, firstIdx);
								}
							});
						}
						return !indexes.isEmpty();
					}

					@Override
					public E next() {
						if(indexes.isEmpty())
							throw new java.util.NoSuchElementException();
						int nextIndex = indexes.remove(0);
						E ret = (E) subValues[nextIndex];
						if(iters.get(nextIndex).hasNext()) {
							E nextVal = iters.get(nextIndex).next();
							subValues[nextIndex] = nextVal;
							int i;
							for(i = 0; i < indexes.size(); i++) {
								int comp = fCompare.compare(nextVal, (E) subValues[indexes.get(i)]);
								if(comp < 0)
									continue;
								if(comp == 0 && nextIndex > indexes.get(i))
									continue;
							}
							indexes.add(i, nextIndex);
						} else
							subValues[nextIndex] = null;
						return ret;
					}
				};
			}

			@Override
			public boolean contains(Object o) {
				for(ObservableOrderedCollection<E> subList : list)
					if(subList.contains(o))
						return true;
				return false;
			}

			@Override
			public boolean containsAll(Collection<?> c) {
				Object [] ca = c.toArray();
				BitSet contained = new BitSet(ca.length);
				for(ObservableOrderedCollection<E> subList : list) {
					for(int i = contained.nextClearBit(0); i < ca.length; i = contained.nextClearBit(i))
						if(subList.contains(ca[i]))
							contained.set(i);
					if(contained.nextClearBit(0) == ca.length)
						break;
				}
				return contained.nextClearBit(0) == ca.length;
			}

			@Override
			public Runnable internalSubscribe(Observer<? super ObservableElement<E>> observer) {
				return list.internalSubscribe(new Observer<ObservableElement<? extends ObservableOrderedCollection<E>>>() {
					private Map<ObservableOrderedCollection<E>, Subscription<ObservableElement<E>>> subListSubscriptions;

					{
						subListSubscriptions = new org.muis.util.ConcurrentIdentityHashMap<>();
					}

					@Override
					public <V extends ObservableElement<? extends ObservableOrderedCollection<E>>> void onNext(V subList) {
						class FlattenedOrderedElement extends FlattenedElement<E> implements OrderedObservableElement<E> {
							FlattenedOrderedElement(OrderedObservableElement<E> subEl) {
								super(subEl, subList);
							}

							@Override
							protected OrderedObservableElement<E> getSubElement() {
								return (OrderedObservableElement<E>) super.getSubElement();
							}

							@Override
							public int getIndex() {
								E value = get();
								int subElIdx = getSubElement().getIndex();
								int ret = 0;
								boolean passedMe = false;
								for(ObservableOrderedCollection<E> sub : list) {
									if(sub == subList.get()) {
										passedMe = true;
										ret += subElIdx;
										continue;
									}
									for(E el : sub) {
										int comp = fCompare.compare(value, el);
										if(comp < 0)
											break;
										if(passedMe && comp == 0)
											break;
										ret++;
									}
								}
								return ret;
							}
						}
						subList.subscribe(new Observer<ObservableValueEvent<? extends ObservableOrderedCollection<E>>>() {
							@Override
							public <V2 extends ObservableValueEvent<? extends ObservableOrderedCollection<E>>> void onNext(V2 subListEvent) {
								if(subListEvent.getOldValue() != null && subListEvent.getOldValue() != subListEvent.getValue()) {
									Subscription<ObservableElement<E>> subListSub = subListSubscriptions.get(subListEvent.getOldValue());
									if(subListSub != null)
										subListSub.unsubscribe();
								}
								Subscription<ObservableElement<E>> subListSub = subListEvent.getValue().subscribe(
									new Observer<ObservableElement<E>>() {
										@Override
										public <V3 extends ObservableElement<E>> void onNext(V3 subElement) {
											OrderedObservableElement<E> subListEl = (OrderedObservableElement<E>) subElement;
											FlattenedOrderedElement flatEl = new FlattenedOrderedElement(subListEl);
											observer.onNext(flatEl);
										}

										@Override
										public void onError(Throwable e) {
											observer.onError(e);
										}
									});
								subListSubscriptions.put(subListEvent.getValue(), subListSub);
							}

							@Override
							public <V2 extends ObservableValueEvent<? extends ObservableOrderedCollection<E>>> void onCompleted(
								V2 subListEvent) {
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
		return new FlattenedObservableOrderedCollection();
	}

	/**
	 * Backs elements in {@link ObservableOrderedCollection#sort(ObservableCollection, Comparator)}
	 *
	 * @param <E> The type of the element
	 */
	static class SortedElementWrapper<E> implements OrderedObservableElement<E> {
		private final SortedObservableCollectionWrapper<E> theList;
		private final ObservableElement<E> theWrapped;
		private final SortedObservableWrapperObserver<E> theParentObserver;
		SortedElementWrapper<E> theLeft;
		SortedElementWrapper<E> theRight;

		SortedElementWrapper(SortedObservableCollectionWrapper<E> list, ObservableElement<E> wrap,
			SortedObservableWrapperObserver<E> parentObs, SortedElementWrapper<E> anchor) {
			theList = list;
			theWrapped = wrap;
			theParentObserver = parentObs;
			if(anchor != null)
				findPlace(theWrapped.get(), anchor);
		}

		@Override
		public ObservableValue<E> persistent() {
			return theWrapped.persistent();
		}

		@Override
		public Type getType() {
			return theWrapped.getType();
		}

		@Override
		public E get() {
			return theWrapped.get();
		}

		@Override
		public Runnable internalSubscribe(Observer<? super ObservableValueEvent<E>> observer) {
			return theWrapped.internalSubscribe(new Observer<ObservableValueEvent<E>>() {
				@Override
				public <V extends ObservableValueEvent<E>> void onNext(V event) {
					if(theLeft != null && theList.getCompare().compare(event.getValue(), theLeft.get()) < 0) {
						observer.onCompleted(new ObservableValueEvent<>(SortedElementWrapper.this, event.getOldValue(), event.getValue(),
							event));
						theLeft.theRight = theRight;
						if(theRight != null)
							theRight.theLeft = theLeft;
						findPlace(event.getValue(), theLeft);
						theParentObserver.theOuterObserver.onNext(SortedElementWrapper.this);
					} else if(theRight != null && theList.getCompare().compare(event.getValue(), theRight.get()) > 0) {
						observer.onCompleted(new ObservableValueEvent<>(SortedElementWrapper.this, event.getOldValue(), event.getValue(),
							event));
						if(theLeft != null)
							theLeft.theRight = theRight;
						theRight.theLeft = theLeft;
						findPlace(event.getValue(), theRight);
						theParentObserver.theOuterObserver.onNext(SortedElementWrapper.this);
					} else
						observer.onNext(new ObservableValueEvent<>(SortedElementWrapper.this, event.getOldValue(), event.getValue(), event));
				}

				@Override
				public <V extends ObservableValueEvent<E>> void onCompleted(V event) {
					if(theParentObserver.theAnchor == SortedElementWrapper.this) {
						if(theLeft != null)
							theParentObserver.theAnchor = theLeft;
						else if(theRight != null)
							theParentObserver.theAnchor = theRight;
						else
							theParentObserver.theAnchor = null;
					}
					observer.onCompleted(new ObservableValueEvent<>(SortedElementWrapper.this, event.getOldValue(), event.getValue(), event));
				}
			});
		}

		@Override
		public int getIndex() {
			SortedElementWrapper<E> left = theLeft;
			int ret = 0;
			while(left != null) {
				ret++;
				left = left.theLeft;
			}
			return ret;
		}

		private void findPlace(E value, SortedElementWrapper<E> anchor) {
			SortedElementWrapper<E> test = anchor;
			int comp = theList.getCompare().compare(value, test.get());
			if(comp >= 0) {
				while(test.theRight != null && comp >= 0) {
					test = test.theRight;
					comp = theList.getCompare().compare(value, test.get());
				}

				if(comp >= 0) { // New element is right-most
					theLeft = test;
					test.theRight = this;
				} else { // New element to be inserted to the left of test
					theLeft = test.theLeft;
					theRight = test;
					test.theLeft = this;
				}
			} else {
				while(test.theLeft != null && comp < 0) {
					test = test.theLeft;
					comp = theList.getCompare().compare(value, test.get());
				}

				if(comp < 0) { // New element is left-most
					theRight = test;
					test.theLeft = this;
				} else { // New element to be inserted to the right of test
					theLeft = test;
					theRight = test.theRight;
					test.theRight = this;
				}
			}
		}
	}

	/**
	 * Backs {@link ObservableOrderedCollection#sort(ObservableCollection, Comparator)}
	 *
	 * @param <E> The type of the elements in the collection
	 */
	static class SortedObservableCollectionWrapper<E> extends AbstractCollection<E> implements ObservableOrderedCollection<E> {
		private final ObservableCollection<E> theWrapped;
		private final Comparator<? super E> theCompare;

		SortedObservableCollectionWrapper(ObservableCollection<E> wrap, Comparator<? super E> compare) {
			theWrapped = wrap;
			theCompare = compare;
		}

		Comparator<? super E> getCompare() {
			return theCompare;
		}

		@Override
		public Type getType() {
			return theWrapped.getType();
		}

		@Override
		public int size() {
			return theWrapped.size();
		}

		@Override
		public boolean isEmpty() {
			return theWrapped.isEmpty();
		}

		@Override
		public boolean contains(Object o) {
			return theWrapped.contains(o);
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			return theWrapped.containsAll(c);
		}

		@Override
		public Iterator<E> iterator() {
			ArrayList<E> list = new ArrayList<>(theWrapped);
			Collections.sort(new ArrayList<>(theWrapped), theCompare);
			return Collections.unmodifiableCollection(list).iterator();
		}

		@Override
		public Runnable internalSubscribe(Observer<? super ObservableElement<E>> observer) {
			return theWrapped.internalSubscribe(new SortedObservableWrapperObserver<>(this, observer));
		}
	}

	/**
	 * Used by {@link ObservableOrderedCollection#sort(ObservableCollection, Comparator)}
	 *
	 * @param <E> The type of the elements in the collection
	 */
	static class SortedObservableWrapperObserver<E> implements Observer<ObservableElement<E>> {
		private final SortedObservableCollectionWrapper<E> theList;
		final Observer<? super ObservableElement<E>> theOuterObserver;
		private SortedElementWrapper<E> theAnchor;

		SortedObservableWrapperObserver(SortedObservableCollectionWrapper<E> list, Observer<? super ObservableElement<E>> outerObs) {
			theList = list;
			theOuterObserver = outerObs;
		}

		@Override
		public <V extends ObservableElement<E>> void onNext(V outerEl) {
			SortedElementWrapper<E> newEl = new SortedElementWrapper<>(theList, outerEl, this, theAnchor);
			if(theAnchor == null)
				theAnchor = newEl;
			theOuterObserver.onNext(newEl);
		}

		@Override
		public void onError(Throwable e) {
			theOuterObserver.onError(e);
		}
	}
}
