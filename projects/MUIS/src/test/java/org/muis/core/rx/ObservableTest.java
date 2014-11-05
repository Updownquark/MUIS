package org.muis.core.rx;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import prisms.lang.Type;

/** Tests observable classes in the org.muis.core.rx package */
public class ObservableTest {
	/** Tests simple {@link SimpleSettableValue} functionality */
	@Test
	public void settableValue() {
		SimpleSettableValue<Integer> obs = new SimpleSettableValue<>(Integer.TYPE, false);
		obs.set(0, null);
		int [] received = new int[] {0};
		obs.act(value -> received[0] = value.getValue());
		for(int i = 1; i < 10; i++) {
			obs.set(i, null);
			assertEquals(i, received[0]);
		}
	}

	/** Tests {@link ObservableValue#mapV(java.util.function.Function)} */
	@Test
	public void valueMap() {
		SimpleSettableValue<Integer> obs = new SimpleSettableValue<>(Integer.TYPE, false);
		obs.set(0, null);
		int [] received = new int[] {0};
		obs.mapV(value -> value * 10).act(value -> received[0] = value.getValue());

		for(int i = 1; i < 10; i++) {
			obs.set(i, null);
			assertEquals(i * 10, received[0]);
		}
	}

	/** Tests {@link Observable#filter(java.util.function.Function)} */
	@Test
	public void filter() {
		DefaultObservable<Integer> obs = new DefaultObservable<>();
		Observer<Integer> controller = obs.control(null);
		int [] received = new int[] {0};
		obs.filter(value -> value % 3 == 0).act(value -> received[0] = value);

		for(int i = 1; i < 30; i++) {
			controller.onNext(i);
			assertEquals((i / 3) * 3, received[0]);
		}
	}

	/** Tests {@link Observable#take(int)} */
	@Test
	public void takeNumber() {
		DefaultObservable<Integer> obs = new DefaultObservable<>();
		Observer<Integer> controller = obs.control(null);
		int [] received = new int[] {0};
		obs.take(10).act(value -> received[0] = value);

		for(int i = 1; i < 30; i++) {
			controller.onNext(i);
			if(i <= 10)
				assertEquals(i, received[0]);
			else
				assertEquals(10, received[0]);
		}
	}

	/** Tests {@link Observable#takeUntil(Observable)} */
	@Test
	public void takeUntil() {
		DefaultObservable<Integer> obs = new DefaultObservable<>();
		Observer<Integer> controller = obs.control(null);
		DefaultObservable<Boolean> stop = new DefaultObservable<>();
		Observer<Boolean> stopControl = stop.control(null);
		int [] received = new int[] {0};
		obs.takeUntil(stop).act(value -> received[0] = value);

		for(int i = 1; i < 30; i++) {
			controller.onNext(i);
			assertEquals(i, received[0]);
		}
		stopControl.onNext(true);
		final int finalValue = received[0];
		for(int i = 1; i < 30; i++) {
			controller.onNext(i);
			assertEquals(finalValue, received[0]);
		}
	}

	/**
	 * Tests {@link Subscription} as an observable to ensure that it is closed (and its observers notified) when
	 * {@link Subscription#unsubscribe()} is called
	 */
	@Test
	public void subSubscription() {
		DefaultObservable<Integer> obs = new DefaultObservable<>();
		Observer<Integer> controller = obs.control(null);
		int [] received = new int[] {0};
		Subscription<Integer> sub = obs.act(value -> received[0] = value);
		int [] received2 = new int[] {0};
		Subscription<Integer> sub2 = sub.act(value -> received2[0] = value);
		int [] received3 = new int[] {0};
		sub2.act(value -> received3[0] = value);

		for(int i = 1; i < 30; i++) {
			controller.onNext(i);
			assertEquals(i, received[0]);
			assertEquals(i, received2[0]);
			assertEquals(i, received3[0]);
		}
		final int finalValue = received[0];
		sub2.unsubscribe();
		for(int i = 1; i < 30; i++) {
			controller.onNext(i);
			assertEquals(i, received[0]);
			assertEquals(finalValue, received2[0]);
			assertEquals(finalValue, received3[0]);
		}
	}

	/** Tests {@link Observable#completed()} */
	@Test
	public void completed() {
		DefaultObservable<Integer> obs = new DefaultObservable<>();
		Observer<Integer> controller = obs.control(null);
		int [] received = new int[] {0};
		obs.completed().act(value -> received[0] = value);

		for(int i = 1; i < 30; i++) {
			controller.onNext(i);
			assertEquals(0, received[0]);
		}
		controller.onCompleted(10);
		assertEquals(10, received[0]);
	}

	/** Tests {@link ObservableValue#combineV(TriFunction, ObservableValue, ObservableValue)} */
	@Test
	public void combine() {
		int [] events = new int[1];
		SimpleSettableValue<Integer> obs1 = new SimpleSettableValue<>(Integer.TYPE, false);
		obs1.set(0, null);
		SimpleSettableValue<Integer> obs2 = new SimpleSettableValue<>(Integer.TYPE, false);
		obs2.set(10, null);
		SimpleSettableValue<Integer> obs3 = new SimpleSettableValue<>(Integer.TYPE, false);
		obs3.set(0, null);
		int [] received = new int[] {0};
		obs1.combineV((arg1, arg2, arg3) -> arg1 * arg2 + arg3, obs2, obs3).act(event -> {
			events[0]++;
			received[0] = event.getValue();
		});
		for(int i = 1; i < 10; i++) {
			obs1.set(i + 3, null);
			obs2.set(i * 10, null);
			obs3.set(i, null);
			assertEquals(i * 3 + 1, events[0]);
			assertEquals((i + 3) * i * 10 + i, received[0]);
		}
	}

	/** Tests {@link ObservableValue#flatten(Type, ObservableValue)} */
	@Test
	public void observableValueFlatten() {
		SimpleSettableValue<ObservableValue<Integer>> outer = new SimpleSettableValue<>(new Type(ObservableValue.class, new Type(
			Integer.TYPE)), false);
		SimpleSettableValue<Integer> inner1 = new SimpleSettableValue<>(Integer.TYPE, false);
		inner1.set(1, null);
		outer.set(inner1, null);
		SimpleSettableValue<Integer> inner2 = new SimpleSettableValue<>(Integer.TYPE, false);
		inner2.set(2, null);
		int [] received = new int[1];
		ObservableValue.flatten(new Type(Integer.TYPE), outer).act(value -> received[0] = value.getValue());

		assertEquals(1, received[0]);
		inner1.set(3, null);
		assertEquals(3, received[0]);
		inner2.set(4, null);
		assertEquals(3, received[0]);
		outer.set(inner2, null);
		assertEquals(4, received[0]);
		inner1.set(5, null);
		assertEquals(4, received[0]);
		inner2.set(6, null);
		assertEquals(6, received[0]);
	}

	/** Tests basic {@link ObservableSet} functionality */
	@Test
	public void observableSet() {
		DefaultObservableSet<Integer> set = new DefaultObservableSet<>(new Type(Integer.TYPE));
		Set<Integer> controller = set.control(null);
		Set<Integer> compare1 = new TreeSet<>();
		Set<Integer> correct = new TreeSet<>();
		Subscription<?> sub = set.act(element -> {
			element.subscribe(new Observer<ObservableValueEvent<Integer>>() {
				@Override
				public <V extends ObservableValueEvent<Integer>> void onNext(V value) {
					compare1.add(value.getValue());
				}

				@Override
				public <V extends ObservableValueEvent<Integer>> void onCompleted(V value) {
					compare1.remove(value.getValue());
				}
			});
		});

		for(int i = 0; i < 30; i++) {
			controller.add(i);
			correct.add(i);
			assertEquals(new TreeSet<>(set), compare1);
			assertEquals(correct, compare1);
		}
		for(int i = 0; i < 30; i++) {
			controller.remove(i);
			correct.remove(i);
			assertEquals(new TreeSet<>(set), compare1);
			assertEquals(correct, compare1);
		}

		for(int i = 0; i < 30; i++) {
			controller.add(i);
			correct.add(i);
			assertEquals(new TreeSet<>(set), compare1);
			assertEquals(correct, compare1);
		}
		sub.unsubscribe();
		for(int i = 30; i < 50; i++) {
			controller.add(i);
			assertEquals(correct, compare1);
		}
		for(int i = 0; i < 30; i++) {
			controller.remove(i);
			assertEquals(correct, compare1);
		}
	}

	/** Tests basic {@link ObservableList} functionality */
	@Test
	public void observableList() {
		DefaultObservableList<Integer> list = new DefaultObservableList<>(new Type(Integer.TYPE));
		List<Integer> controller = list.control(null);
		List<Integer> compare1 = new ArrayList<>();
		List<Integer> correct = new ArrayList<>();
		Subscription<?> sub = list.act(element -> {
			ObservableListElement<Integer> listEl = (ObservableListElement<Integer>) element;
			element.subscribe(new Observer<ObservableValueEvent<Integer>>() {
				@Override
				public <V extends ObservableValueEvent<Integer>> void onNext(V value) {
					compare1.add(listEl.getIndex(), value.getValue());
				}

				@Override
				public <V extends ObservableValueEvent<Integer>> void onCompleted(V value) {
					compare1.remove(listEl.getIndex());
				}
			});
		});

		for(int i = 0; i < 30; i++) {
			controller.add(i);
			correct.add(i);
			assertEquals(new ArrayList<>(list), compare1);
			assertEquals(correct, compare1);
		}
		for(int i = 0; i < 30; i++) {
			controller.remove((Integer) i);
			correct.remove((Integer) i);
			assertEquals(new ArrayList<>(list), compare1);
			assertEquals(correct, compare1);
		}

		for(int i = 0; i < 30; i++) {
			controller.add(i);
			correct.add(i);
			assertEquals(new ArrayList<>(list), compare1);
			assertEquals(correct, compare1);
		}
		sub.unsubscribe();
		for(int i = 30; i < 50; i++) {
			controller.add(i);
			assertEquals(correct, compare1);
		}
		for(int i = 0; i < 30; i++) {
			controller.remove((Integer) i);
			assertEquals(correct, compare1);
		}
	}

	/** Tests {@link ObservableSet#mapC(java.util.function.Function)} */
	@Test
	public void observableSetMap() {
		DefaultObservableSet<Integer> set = new DefaultObservableSet<>(new Type(Integer.TYPE));
		Set<Integer> controller = set.control(null);
		Set<Integer> compare1 = new TreeSet<>();
		Set<Integer> correct = new TreeSet<>();
		set.mapC(value -> value * 10).act(element -> {
			element.subscribe(new Observer<ObservableValueEvent<Integer>>() {
				@Override
				public <V extends ObservableValueEvent<Integer>> void onNext(V value) {
					compare1.add(value.getValue());
				}

				@Override
				public <V extends ObservableValueEvent<Integer>> void onCompleted(V value) {
					compare1.remove(value.getValue());
				}
			});
		});

		for(int i = 0; i < 30; i++) {
			controller.add(i);
			correct.add(i * 10);
			assertEquals(correct, compare1);
		}
		for(int i = 0; i < 30; i++) {
			controller.remove(i);
			correct.remove(i * 10);
			assertEquals(correct, compare1);
		}
	}

	/** Tests {@link ObservableSet#filterC(java.util.function.Function)} */
	@Test
	public void observableSetFilter() {
		DefaultObservableSet<Integer> set = new DefaultObservableSet<>(new Type(Integer.TYPE));
		Set<Integer> controller = set.control(null);
		Set<Integer> compare1 = new TreeSet<>();
		Set<Integer> correct = new TreeSet<>();
		set.filterC(value -> value != null && value % 2 == 0).act(element -> {
			element.subscribe(new Observer<ObservableValueEvent<Integer>>() {
				@Override
				public <V extends ObservableValueEvent<Integer>> void onNext(V value) {
					compare1.add(value.getValue());
				}

				@Override
				public <V extends ObservableValueEvent<Integer>> void onCompleted(V value) {
					compare1.remove(value.getValue());
				}
			});
		});

		for(int i = 0; i < 30; i++) {
			controller.add(i);
			if(i % 2 == 0)
				correct.add(i);
			assertEquals(correct, compare1);
		}
		for(int i = 0; i < 30; i++) {
			controller.remove(i);
			if(i % 2 == 0)
				correct.remove(i);
			assertEquals(correct, compare1);
		}
	}

	/** Tests {@link ObservableSet#combineC(ObservableValue, java.util.function.BiFunction)} */
	@Test
	public void observableSetCombine() {
		DefaultObservableSet<Integer> set = new DefaultObservableSet<>(new Type(Integer.TYPE));
		SimpleSettableValue<Integer> value1 = new SimpleSettableValue<>(Integer.TYPE, false);
		value1.set(1, null);
		Set<Integer> controller = set.control(null);
		List<Integer> compare1 = new ArrayList<>();
		Set<Integer> correct = new TreeSet<>();
		set.combineC(value1, (v1, v2) -> v1 * v2).filterC(value -> value != null && value % 3 == 0).act(element -> {
			element.subscribe(new Observer<ObservableValueEvent<Integer>>() {
				@Override
				public <V extends ObservableValueEvent<Integer>> void onNext(V event) {
					if(event.getOldValue() != null)
						compare1.remove(event.getOldValue());
					compare1.add(event.getValue());
				}

				@Override
				public <V extends ObservableValueEvent<Integer>> void onCompleted(V event) {
					compare1.remove(event.getValue());
				}
			});
		});

		for(int i = 0; i < 30; i++) {
			controller.add(i);
			int value = i * value1.get();
			if(value % 3 == 0)
				correct.add(value);
			assertEquals(correct, new TreeSet<>(compare1));
			assertEquals(correct.size(), compare1.size());
		}

		value1.set(3, null);
		correct.clear();
		for(int i = 0; i < 30; i++) {
			int value = i * value1.get();
			if(value % 3 == 0)
				correct.add(value);
		}
		assertEquals(correct, new TreeSet<>(compare1));
		assertEquals(correct.size(), compare1.size());

		value1.set(10, null);
		correct.clear();
		for(int i = 0; i < 30; i++) {
			int value = i * value1.get();
			if(value % 3 == 0)
				correct.add(value);
		}
		assertEquals(correct, new TreeSet<>(compare1));
		assertEquals(correct.size(), compare1.size());

		for(int i = 0; i < 30; i++) {
			controller.remove(i);
			int value = i * value1.get();
			if(value % 3 == 0)
				correct.remove(value);
			assertEquals(correct, new TreeSet<>(compare1));
			assertEquals(correct.size(), compare1.size());
		}
	}

	/** Tests {@link ObservableCollection#flatten(ObservableCollection)} */
	@Test
	public void observableSetFlatten() {
		DefaultObservableSet<Integer> set1 = new DefaultObservableSet<>(new Type(Integer.TYPE));
		DefaultObservableSet<Integer> set2 = new DefaultObservableSet<>(new Type(Integer.TYPE));
		DefaultObservableSet<Integer> set3 = new DefaultObservableSet<>(new Type(Integer.TYPE));
		DefaultObservableSet<ObservableSet<Integer>> outer = new DefaultObservableSet<>(new Type(ObservableSet.class,
			new Type(Integer.TYPE)));
		Set<ObservableSet<Integer>> outerControl = outer.control(null);
		outerControl.add(set1);
		outerControl.add(set2);
		ObservableSet<Integer> flat = ObservableCollection.flatten(outer);
		List<Integer> compare1 = new ArrayList<>();
		List<Integer> filtered = new ArrayList<>();
		flat.act(element -> {
			element.subscribe(new Observer<ObservableValueEvent<Integer>>() {
				@Override
				public <V extends ObservableValueEvent<Integer>> void onNext(V event) {
					if(event.getOldValue() != null)
						compare1.remove(event.getOldValue());
					compare1.add(event.getValue());
				}

				@Override
				public <V extends ObservableValueEvent<Integer>> void onCompleted(V event) {
					compare1.remove(event.getValue());
				}
			});
		});
		flat.filterC(value -> value!=null && value % 3 == 0).act(element -> {
			element.subscribe(new Observer<ObservableValueEvent<Integer>>() {
				@Override
				public <V extends ObservableValueEvent<Integer>> void onNext(V event) {
					if(event.getOldValue() != null)
						filtered.remove(event.getOldValue());
					filtered.add(event.getValue());
				}

				@Override
				public <V extends ObservableValueEvent<Integer>> void onCompleted(V event) {
					filtered.remove(event.getValue());
				}
			});
		});
		Set<Integer> controller1 = set1.control(null);
		Set<Integer> controller2 = set2.control(null);
		Set<Integer> controller3 = set3.control(null);

		Set<Integer> correct = new TreeSet<>();
		Set<Integer> filteredCorrect = new TreeSet<>();

		for(int i = 0; i < 30; i++) {
			controller1.add(i);
			controller2.add(i * 10);
			controller3.add(i * 100);
			correct.add(i);
			correct.add(i * 10);
			if(i % 3 == 0) {
				filteredCorrect.add(i);
				filteredCorrect.add(i * 10);
			}
			assertEquals(new TreeSet<>(flat), new TreeSet<>(compare1));
			assertEquals(flat.size(), compare1.size());
			assertEquals(correct, new TreeSet<>(compare1));
			assertEquals(correct.size(), compare1.size());
			assertEquals(filteredCorrect, new TreeSet<>(filtered));
			assertEquals(filteredCorrect.size(), filtered.size());
		}

		outerControl.add(set3);
		for(int i = 0; i < 30; i++) {
			correct.add(i * 100);
			if(i % 3 == 0) {
				filteredCorrect.add(i * 100);
			}
		}
		assertEquals(new TreeSet<>(flat), new TreeSet<>(compare1));
		assertEquals(flat.size(), compare1.size());
		assertEquals(correct, new TreeSet<>(compare1));
		assertEquals(correct.size(), compare1.size());
		assertEquals(filteredCorrect, new TreeSet<>(filtered));
		assertEquals(filteredCorrect.size(), filtered.size());

		outerControl.remove(set2);
		correct.clear();
		filteredCorrect.clear();
		for(int i = 0; i < 30; i++) {
			correct.add(i);
			correct.add(i * 100);
			if(i % 3 == 0) {
				filteredCorrect.add(i);
				filteredCorrect.add(i * 100);
			}
		}
		assertEquals(new TreeSet<>(flat), new TreeSet<>(compare1));
		assertEquals(flat.size(), compare1.size());
		assertEquals(correct, new TreeSet<>(compare1));
		assertEquals(correct.size(), compare1.size());
		assertEquals(filteredCorrect, new TreeSet<>(filtered));
		assertEquals(filteredCorrect.size(), filtered.size());
	}

	/** Tests {@link ObservableCollection#fold(ObservableCollection)} */
	@Test
	public void observableSetFold() {
		DefaultObservable<Integer> obs1 = new DefaultObservable<>();
		DefaultObservable<Integer> obs2 = new DefaultObservable<>();
		DefaultObservable<Integer> obs3 = new DefaultObservable<>();
		Observer<Integer> controller1 = obs1.control(null);
		Observer<Integer> controller2 = obs2.control(null);
		Observer<Integer> controller3 = obs3.control(null);
		DefaultObservableSet<Observable<Integer>> set = new DefaultObservableSet<>(new Type(Observable.class, new Type(
			Integer.TYPE)));
		Set<Observable<Integer>> controller = set.control(null);
		controller.add(obs1);
		controller.add(obs2);
		Observable<Integer> folded = ObservableCollection.fold(set);
		int [] received = new int[1];
		folded.act(value -> received[0] = value);

		controller1.onNext(1);
		assertEquals(1, received[0]);
		controller2.onNext(2);
		assertEquals(2, received[0]);
		controller3.onNext(3);
		assertEquals(2, received[0]);
		set.add(obs3);
		controller3.onNext(4);
		assertEquals(4, received[0]);
		set.remove(obs2);
		controller2.onNext(5);
		assertEquals(4, received[0]);
	}

	/** Tests {@link ObservableList#mapC(java.util.function.Function)} */
	@Test
	public void observableListMap() {
		DefaultObservableList<Integer> list = new DefaultObservableList<>(new Type(Integer.TYPE));
		List<Integer> controller = list.control(null);
		List<Integer> compare1 = new ArrayList<>();
		List<Integer> correct = new ArrayList<>();
		list.mapC(value -> value * 10).act(element -> {
			ObservableListElement<Integer> listEl = (ObservableListElement<Integer>) element;
			element.subscribe(new Observer<ObservableValueEvent<Integer>>() {
				@Override
				public <V extends ObservableValueEvent<Integer>> void onNext(V value) {
					if(value.getOldValue() != null)
						compare1.set(listEl.getIndex(), value.getValue());
					else
						compare1.add(listEl.getIndex(), value.getValue());
				}

				@Override
				public <V extends ObservableValueEvent<Integer>> void onCompleted(V value) {
					compare1.remove(listEl.getIndex());
				}
			});
		});

		for(int i = 0; i < 30; i++) {
			controller.add(i);
			correct.add(i * 10);
			assertEquals(correct, compare1);
		}
		for(int i = 0; i < 30; i++) {
			controller.add(i);
			correct.add(i * 10);
			assertEquals(correct, compare1);
		}
		for(int i = 0; i < 30; i++) {
			controller.remove(i);
			correct.remove(i * 10);
			assertEquals(correct, compare1);
		}
	}

	/** Tests {@link ObservableList#filter(java.util.function.Function)} */
	@Test
	public void observableListFilter() {
		DefaultObservableList<Integer> list = new DefaultObservableList<>(new Type(Integer.TYPE));
		List<Integer> controller = list.control(null);
		List<Integer> compare1 = new ArrayList<>();
		List<Integer> correct = new ArrayList<>();
		list.filterC(value -> value != null && value % 2 == 0).act(element -> {
			ObservableListElement<Integer> listEl = (ObservableListElement<Integer>) element;
			element.subscribe(new Observer<ObservableValueEvent<Integer>>() {
				@Override
				public <V extends ObservableValueEvent<Integer>> void onNext(V value) {
					if(value.getOldValue() != null)
						compare1.set(listEl.getIndex(), value.getValue());
					else
						compare1.add(listEl.getIndex(), value.getValue());
				}

				@Override
				public <V extends ObservableValueEvent<Integer>> void onCompleted(V value) {
					compare1.remove(listEl.getIndex());
				}
			});
		});

		for(int i = 0; i < 30; i++) {
			controller.add(i);
			if(i % 2 == 0)
				correct.add(i);
			assertEquals(correct, compare1);
		}
		for(int i = 0; i < 30; i++) {
			controller.add(i);
			if(i % 2 == 0)
				correct.add(i);
			assertEquals(correct, compare1);
		}
		for(int i = 0; i < 30; i++) {
			controller.remove(i);
			if(i % 2 == 0)
				correct.remove(i);
			assertEquals(correct, compare1);
		}
	}

	/** Tests {@link ObservableList#combineC(ObservableValue, java.util.function.BiFunction)} */
	@Test
	public void observableListCombine() {
		DefaultObservableList<Integer> set = new DefaultObservableList<>(new Type(Integer.TYPE));
		SimpleSettableValue<Integer> value1 = new SimpleSettableValue<>(Integer.TYPE, false);
		value1.set(1, null);
		List<Integer> controller = set.control(null);
		List<Integer> compare1 = new ArrayList<>();
		List<Integer> correct = new ArrayList<>();
		set.combineC(value1, (v1, v2) -> v1 * v2).filterC(value -> value != null && value % 3 == 0).act(element -> {
			ObservableListElement<Integer> listEl = (ObservableListElement<Integer>) element;
			element.subscribe(new Observer<ObservableValueEvent<Integer>>() {
				@Override
				public <V extends ObservableValueEvent<Integer>> void onNext(V event) {
					if(event.getOldValue() != null)
						compare1.set(listEl.getIndex(), event.getOldValue());
					else
						compare1.add(listEl.getIndex(), event.getValue());
				}

				@Override
				public <V extends ObservableValueEvent<Integer>> void onCompleted(V event) {
					compare1.remove(listEl.getIndex());
				}
			});
		});

		for(int i = 0; i < 30; i++) {
			controller.add(i);
			int value = i * value1.get();
			if(value % 3 == 0)
				correct.add(value);
			assertEquals(correct, compare1);
			assertEquals(correct.size(), compare1.size());
		}
		for(int i = 0; i < 30; i++) {
			controller.add(i);
			int value = i * value1.get();
			if(value % 3 == 0)
				correct.add(value);
			assertEquals(correct, compare1);
			assertEquals(correct.size(), compare1.size());
		}

		value1.set(3, null);
		correct.clear();
		for(int i = 0; i < 30; i++) {
			int value = i * value1.get();
			if(value % 3 == 0)
				correct.add(value);
		}
		assertEquals(correct, compare1);
		assertEquals(correct.size(), compare1.size());

		value1.set(10, null);
		correct.clear();
		for(int i = 0; i < 30; i++) {
			int value = i * value1.get();
			if(value % 3 == 0)
				correct.add(value);
		}
		assertEquals(correct, compare1);
		assertEquals(correct.size(), compare1.size());

		for(int i = 0; i < 30; i++) {
			controller.remove(i);
			int value = i * value1.get();
			if(value % 3 == 0)
				correct.remove(value);
			assertEquals(correct, compare1);
			assertEquals(correct.size(), compare1.size());
		}
	}

	/** Tests {@link ObservableList#flatten(ObservableList)} */
	@Test
	public void observableListFlatten() {
		DefaultObservableList<Integer> set1 = new DefaultObservableList<>(new Type(Integer.TYPE));
		DefaultObservableList<Integer> set2 = new DefaultObservableList<>(new Type(Integer.TYPE));
		DefaultObservableList<Integer> set3 = new DefaultObservableList<>(new Type(Integer.TYPE));
		DefaultObservableList<ObservableList<Integer>> outer = new DefaultObservableList<>(new Type(ObservableList.class, new Type(
			Integer.TYPE)));
		List<ObservableList<Integer>> outerControl = outer.control(null);
		outerControl.add(set1);
		outerControl.add(set2);
		ObservableList<Integer> flat = ObservableList.flatten(outer);
		List<Integer> compare1 = new ArrayList<>();
		List<Integer> filtered = new ArrayList<>();
		flat.act(element -> {
			ObservableListElement<Integer> listEl = (ObservableListElement<Integer>) element;
			element.subscribe(new Observer<ObservableValueEvent<Integer>>() {
				@Override
				public <V extends ObservableValueEvent<Integer>> void onNext(V event) {
					if(event.getOldValue() != null)
						compare1.set(listEl.getIndex(), event.getOldValue());
					else
						compare1.add(listEl.getIndex(), event.getValue());
				}

				@Override
				public <V extends ObservableValueEvent<Integer>> void onCompleted(V event) {
					compare1.remove(listEl.getIndex());
				}
			});
		});
		flat.filterC(value -> value != null && value % 3 == 0).act(element -> {
			ObservableListElement<Integer> listEl = (ObservableListElement<Integer>) element;
			element.subscribe(new Observer<ObservableValueEvent<Integer>>() {
				@Override
				public <V extends ObservableValueEvent<Integer>> void onNext(V event) {
					if(event.getOldValue() != null)
						filtered.set(listEl.getIndex(), event.getOldValue());
					else
						filtered.add(listEl.getIndex(), event.getValue());
				}

				@Override
				public <V extends ObservableValueEvent<Integer>> void onCompleted(V event) {
					filtered.remove(listEl.getIndex());
				}
			});
		});
		List<Integer> controller1 = set1.control(null);
		List<Integer> controller2 = set2.control(null);
		List<Integer> controller3 = set3.control(null);

		List<Integer> correct = new ArrayList<>();
		List<Integer> filteredCorrect = new ArrayList<>();

		for(int i = 0; i < 30; i++) {
			controller1.add(i);
			controller2.add(i * 10);
			controller3.add(i * 100);
			correct.add(i);
			correct.add(i * 10);
			if(i % 3 == 0) {
				filteredCorrect.add(i);
				filteredCorrect.add(i * 10);
			}
			assertEquals(flat, compare1);
			assertEquals(flat.size(), compare1.size());
			assertEquals(correct, compare1);
			assertEquals(correct.size(), compare1.size());
			assertEquals(filteredCorrect, filtered);
			assertEquals(filteredCorrect.size(), filtered.size());
		}

		outerControl.add(set3);
		for(int i = 0; i < 30; i++) {
			correct.add(i * 100);
			if(i % 3 == 0) {
				filteredCorrect.add(i * 100);
			}
		}
		assertEquals(flat, compare1);
		assertEquals(flat.size(), compare1.size());
		assertEquals(correct, compare1);
		assertEquals(correct.size(), compare1.size());
		assertEquals(filteredCorrect, filtered);
		assertEquals(filteredCorrect.size(), filtered.size());

		outerControl.remove(set2);
		correct.clear();
		filteredCorrect.clear();
		for(int i = 0; i < 30; i++) {
			correct.add(i);
			correct.add(i * 100);
			if(i % 3 == 0) {
				filteredCorrect.add(i);
				filteredCorrect.add(i * 100);
			}
		}
		assertEquals(flat, compare1);
		assertEquals(flat.size(), compare1.size());
		assertEquals(correct, compare1);
		assertEquals(correct.size(), compare1.size());
		assertEquals(filteredCorrect, filtered);
		assertEquals(filteredCorrect.size(), filtered.size());
	}

	/** Tests {@link ObservableList#find(Type, java.util.function.Function)} */
	@Test
	public void observableListFind() {
		DefaultObservableList<Integer> list = new DefaultObservableList<>(new Type(Integer.TYPE));
		ObservableValue<Integer> found = list.find(new Type(Integer.TYPE), value -> value % 3 == 0 ? value : null);
		Integer [] received = new Integer[] {0};
		found.act(value -> received[0] = value.getValue());
		Integer [] correct = new Integer[] {null};

		assertEquals(correct[0], received[0]);
		assertEquals(correct[0], found.get());
		for(int i = 1; i < 30; i++) {
			list.add(i);
			if(i % 3 == 0 && correct[0] == null)
				correct[0] = i;
			assertEquals(correct[0], received[0]);
			assertEquals(correct[0], found.get());
		}
		for(int i = 1; i < 30; i++) {
			list.remove(i);
			if(i % 3 == 0) {
				correct[0] += 3;
				if(correct[0] >= 30)
					correct[0] = null;
			}
			assertEquals(correct[0], received[0]);
			assertEquals(correct[0], found.get());
		}
	}

	/** Tests {@link ObservableUtils#flatten(Type, ObservableList)} */
	@Test
	public void utilsListFlatten() {
		DefaultObservableList<ObservableValue<Integer>> list = new DefaultObservableList<>(new Type(ObservableValue.class, new Type(
			Integer.TYPE)));
		List<ObservableValue<Integer>> listControl = list.control(null);
		SimpleSettableValue<Integer> value1 = new SimpleSettableValue<>(Integer.TYPE, false);
		value1.set(1, null);
		SimpleSettableValue<Integer> value2 = new SimpleSettableValue<>(Integer.TYPE, false);
		value2.set(2, null);
		SimpleSettableValue<Integer> value3 = new SimpleSettableValue<>(Integer.TYPE, false);
		value3.set(3, null);
		SimpleSettableValue<Integer> value4 = new SimpleSettableValue<>(Integer.TYPE, false);
		value4.set(4, null);
		SimpleSettableValue<Integer> value5 = new SimpleSettableValue<>(Integer.TYPE, false);
		value5.set(9, null);
		listControl.addAll(java.util.Arrays.asList(value1, value2, value3, value4));

		Integer [] received = new Integer[1];
		ObservableUtils.flatten(new Type(Integer.TYPE), list).find(new Type(Integer.TYPE), value -> value % 3 == 0 ? value : null).value()
			.act(value -> received[0] = value);
		assertEquals(Integer.valueOf(3), received[0]);
		value3.set(4, null);
		assertEquals(null, received[0]);
		value4.set(6, null);
		assertEquals(Integer.valueOf(6), received[0]);
		listControl.remove(value4);
		assertEquals(null, received[0]);
		listControl.add(value5);
		assertEquals(Integer.valueOf(9), received[0]);
	}
}
