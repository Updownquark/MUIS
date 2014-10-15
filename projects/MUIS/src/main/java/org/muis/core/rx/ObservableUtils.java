package org.muis.core.rx;

import java.util.function.Function;

import prisms.lang.Type;

/** Utility methods for observables */
public class ObservableUtils {
	/**
	 * @param <T> The type of
	 * @param <V>
	 * @param type
	 * @param list
	 * @param map
	 * @return
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
