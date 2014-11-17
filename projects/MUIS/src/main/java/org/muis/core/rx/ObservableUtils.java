package org.muis.core.rx;

import prisms.lang.Type;

/** Utility methods for observables */
public class ObservableUtils {
	/**
	 * Turns a list of observable values into a list composed of those holders' values
	 *
	 * @param <T> The type of elements held in the values
	 * @param type The run-time type of elements held in the values
	 * @param list The list to flatten
	 * @return The flattened list
	 */
	public static <T> ObservableList<T> flatten(Type type, ObservableList<? extends ObservableValue<T>> list) {
		class FlattenedList extends java.util.AbstractList<T> implements ObservableList<T> {
			@Override
			public Type getType() {
				return type;
			}

			@Override
			public Runnable internalSubscribe(Observer<? super ObservableElement<T>> observer) {
				return list.internalSubscribe(new Observer<ObservableElement<? extends ObservableValue<T>>>() {
					@Override
					public <V extends ObservableElement<? extends ObservableValue<T>>> void onNext(V element) {
						ObservableListElement<? extends ObservableValue<T>> listElement = (ObservableListElement<? extends ObservableValue<T>>) element;
						observer.onNext(new ObservableListElement<T>() {
							@Override
							public Type getType() {
								return type != null ? type : element.get().getType();
							}

							@Override
							public T get() {
								return get(element.get());
							}

							@Override
							public int getIndex() {
								return listElement.getIndex();
							}

							@Override
							public ObservableValue<T> persistent() {
								return ObservableValue.flatten(getType(), element.persistent());
							}

							private T get(ObservableValue<? extends T> value) {
								return value == null ? null : value.get();
							}

							@Override
							public Runnable internalSubscribe(Observer<? super ObservableValueEvent<T>> observer2) {
								ObservableListElement<T> retObs = this;
								Runnable [] innerSub = new Runnable[1];
								Runnable outerSub = element
									.internalSubscribe(new Observer<ObservableValueEvent<? extends ObservableValue<? extends T>>>() {
										@Override
										public <V2 extends ObservableValueEvent<? extends ObservableValue<? extends T>>> void onNext(
											V2 value) {
											if(innerSub[0] != null) {
												innerSub[0].run();
												innerSub[0] = null;
											}
											T old = get(value.getOldValue());
											if(value.getValue() != null) {
												boolean [] init = new boolean[] {true};
												innerSub[0] = value.getValue().internalSubscribe(
													new Observer<ObservableValueEvent<? extends T>>() {
														@Override
														public <V3 extends ObservableValueEvent<? extends T>> void onNext(V3 value2) {
															T innerOld;
															if(init[0]) {
																init[0] = false;
																innerOld = old;
															} else
																innerOld = value2.getValue();
															observer2.onNext(new ObservableValueEvent<>(retObs, innerOld,
																value2.getValue(), value2.getCause()));
														}

														@Override
														public void onError(Throwable e) {
															observer.onError(e);
														}
													});
											} else
												observer2.onNext(new ObservableValueEvent<>(retObs, old, null, value.getCause()));
										}

										@Override
										public <V2 extends ObservableValueEvent<? extends ObservableValue<? extends T>>> void onCompleted(
											V2 value) {
											if(innerSub[0] != null) {
												innerSub[0].run();
												innerSub[0] = null;
											}
											observer2.onCompleted(new ObservableValueEvent<>(retObs, get(value.getOldValue()), get(value
												.getValue()), value.getCause()));
										}

										@Override
										public void onError(Throwable e) {
											observer.onError(e);
										}
									});
								return () -> {
									outerSub.run();
									if(innerSub[0] != null) {
										innerSub[0].run();
										innerSub[0] = null;
									}
								};
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
}
