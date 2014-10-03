package org.muis.core.rx;

import java.util.function.Function;

import prisms.lang.Type;

public class ObservableUtils {
	public static <T, V> ObservableValue<V> first(Type type, ObservableList<? extends ObservableValue<T>> list, Function<T, V> map) {
		return new DefaultObservableValue<V>() {
			private V theValue;

			private int theIndex;

			private Observer<ObservableValueEvent<V>> theController;

			{
				theController = control(null);
				theIndex = -1;
				list.act(element -> {
					element.subscribe(new Observer<ObservableValue<T>>() {
						@Override
						public <V2 extends ObservableValue<T>> void onNext(V2 value) {
							{
								int listIndex = list.indexOf(value);
								if(theIndex < 0 || listIndex <= theIndex) {
									T val = value.get();
									V mapped = map.apply(val);
									if(mapped != null)
										newBest(mapped, listIndex);
								}
							}
							value.takeUntil(element).takeUntil(element.completed()).act(event -> {
								int listIndex = list.indexOf(value);
								if(theIndex < 0 || listIndex <= theIndex) {
									V mapped = map.apply(event.getValue());
									if(mapped != null)
										newBest(mapped, listIndex);
								}
							});
						}

						@Override
						public <V2 extends ObservableValue<T>> void onCompleted(V2 value) {
							int listIndex = list.indexOf(value);
							if(listIndex == theIndex) {
								boolean found = false;
								for(int i = listIndex; i < list.size(); i++) {
									T val = list.get(i).get();
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
}
