package org.muis.core.rx;

import java.util.function.Function;

import prisms.lang.Type;

/** Utility methods for observables */
public class ObservableUtils {
	public static <T> ObservableList<T> flatten(Type type, ObservableList<? extends ObservableValue<T>> list) {
		class FlattenedList extends java.util.AbstractList<T> implements ObservableList<T> {
			@Override
			public Type getType() {
				return type;
			}

			@Override
			public Runnable internalSubscribe(Observer<? super ObservableElement<T>> observer) {
				Runnable listSub = list.internalSubscribe(new Observer<ObservableElement<? extends ObservableValue<T>>>() {
					@Override
					public <V extends ObservableElement<? extends ObservableValue<T>>> void onNext(V element) {
						ObservableListElement<? extends ObservableValue<T>> listElement = (ObservableListElement<? extends ObservableValue<T>>) element;
						class FlattenedElement implements ObservableListElement<T>{
							@Override
							public ObservableValue<T> persistent() {
								return listElement.get();
							}

							@Override
							public Type getType() {
								return type;
							}

							@Override
							public T get() {
								return listElement.get().get();
							}

							@Override
							public Runnable internalSubscribe(Observer<? super ObservableValueEvent<T>> observer) {
								Runnable elSub=element.internalSubscribe(new Observer<ObservableValueEvent<? extends ObservableValue<T>>>(){
									Runnable sub;
									@Override
									public <V2 extends ObservableValueEvent<? extends ObservableValue<T>>> void onNext(V2 value) {
										sub=value.getValue().takeUntil(element).internalSubscribe(event->
										// TODO Auto-generated method stub

									}

									@Override
									public void onError(Throwable e){
										observer.onError(e);
									}
								});
								// TODO Auto-generated method stub
								return null;
							}

							@Override
							public int getIndex() {
								return listElement.getIndex();
							}
						};
					}

					@Override
					public void onError(Throwable e) {
						observer.onError(e);
					}
				});
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public T get(int index) {
				return list.get(index).get();
			}

			@Override
			public int size() {
				return list.size();
			}
		}
		return new FlattenedList();
	}

	/**
	 * This method differst from {@link ObservableList#find(Type, Function)} in that the elements of the list argument for this function are
	 * themselves observable values.
	 *
	 * @param <T> The type of observable values in the list
	 * @param <V> The type of the mapped value to return
	 * @param type The run-time type of the mapped value to return
	 * @param list The list of observable values
	 * @param map The mapping function
	 * @return An observable value which is the first non-null mapped value in the list, or null if there is nones
	 */
	public static <T, V> ObservableValue<V> first(Type type, ObservableList<? extends ObservableValue<T>> list, Function<T, V> map) {
		return new ObservableValue<V>() {
			@Override
			public Type getType() {
				return type;
			}

			@Override
			public V get() {
				for(ObservableValue<T> value : list) {
					V ret = map.apply(value.get());
					if(ret != null)
						return ret;
				}
				return null;
			}

			@Override
			public Runnable internalSubscribe(Observer<? super ObservableValueEvent<V>> observer) {
				ObservableValue<V> outer = this;
				boolean [] initialized = new boolean[1];
				Runnable ret = list.internalSubscribe(new Observer<ObservableElement<? extends ObservableValue<T>>>() {
					int theIndex = -1;
					V oldValue = null;

					@Override
					public <V2 extends ObservableElement<? extends ObservableValue<T>>> void onNext(V2 element) {
						ObservableListElement<? extends ObservableValue<T>> listEl = (ObservableListElement<? extends ObservableValue<T>>) element;
						if(listEl.getIndex() <= theIndex)
							theIndex++;
						element.subscribe(new Observer<ObservableValueEvent<? extends ObservableValue<T>>>() {
							@Override
							public <V3 extends ObservableValueEvent<? extends ObservableValue<T>>> void onNext(V3 value) {
								value.getValue().takeUntil(element).value().act(elVal -> {
									if(!initialized[0] || (theIndex >= 0 && listEl.getIndex() > theIndex))
										return;
									V mapped = map.apply(elVal);
									if(mapped == null && listEl.getIndex() == theIndex) {
										for(int i = theIndex + 1; i < list.size(); i++) {
											mapped = map.apply(list.get(i).get());
											if(mapped != null) {
												theIndex = i;
												break;
											}
										}
										if(listEl.getIndex() == theIndex) {
											theIndex = -1;
											mapped = null;
										}
										observer.onNext(new ObservableValueEvent<>(outer, oldValue, mapped, value));
									}
								});
							}

							@Override
							public <V3 extends ObservableValueEvent<? extends ObservableValue<T>>> void onCompleted(V3 value) {
								if(listEl.getIndex() < theIndex) {
									theIndex--;
									return;
								}
								if(listEl.getIndex() != theIndex)
									return;
								V mapped = null;
								for(int i = theIndex; i < list.size(); i++) {
									mapped = map.apply(list.get(i).get());
									if(mapped != null) {
										theIndex = i;
										break;
									}
								}
								if(listEl.getIndex() == theIndex) {
									theIndex = -1;
									mapped = null;
								}
								observer.onNext(new ObservableValueEvent<>(outer, oldValue, mapped, value));
							}
						});
					}
				});
				initialized[0] = true;
				return ret;
			}
		};
	}
}
