package org.muis.core.rx;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import prisms.lang.Type;

public class ObservableTest {
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

	@Test
	public void valueMap() {
		SimpleSettableValue<Integer> obs = new SimpleSettableValue<>(Integer.TYPE, false);
		obs.set(0, null);
		int [] received = new int[] {0};
		obs.mapV(value -> value * 10)
		.act(value -> received[0] = value.getValue());

		for(int i = 1; i < 10; i++) {
			obs.set(i, null);
			assertEquals(i * 10, received[0]);
		}
	}

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

	@Test
	public void combine() {
		SimpleSettableValue<Integer> obs1 = new SimpleSettableValue<>(Integer.TYPE, false);
		obs1.set(0, null);
		SimpleSettableValue<Integer> obs2 = new SimpleSettableValue<>(Integer.TYPE, false);
		obs2.set(10, null);
		SimpleSettableValue<Integer> obs3 = new SimpleSettableValue<>(Integer.TYPE, false);
		obs3.set(0, null);
		int [] received = new int[] {0};
		obs1.combineV((arg1, arg2, arg3) -> arg1 * arg2 + arg3, obs2, obs3).act(event -> received[0] = event.getValue());
		for(int i = 1; i < 10; i++) {
			obs1.set(i + 3, null);
			obs2.set(i * 10, null);
			obs3.set(i, null);
			assertEquals((i + 3) * i * 10 + i, received[0]);
		}
	}

	@Test
	public void observableSet() {
		DefaultObservableSet<Integer> set = new DefaultObservableSet<>(new Type(Integer.TYPE));
		Set<Integer> controller = set.control(null);
		Set<Integer> compare1 = new TreeSet<>();
		Set<Integer> compare2 = new TreeSet<>();
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
			compare2.add(i);
			assertEquals(new TreeSet<>(set), compare1);
			assertEquals(compare1, compare2);
		}
		for(int i = 0; i < 30; i++) {
			controller.remove(i);
			compare2.remove(i);
			assertEquals(new TreeSet<>(set), compare1);
			assertEquals(compare1, compare2);
		}

		for(int i = 0; i < 30; i++) {
			controller.add(i);
			compare2.add(i);
			assertEquals(new TreeSet<>(set), compare1);
			assertEquals(compare1, compare2);
		}
		sub.unsubscribe();
		for(int i = 30; i < 50; i++) {
			controller.add(i);
			assertEquals(compare1, compare2);
		}
		for(int i = 0; i < 30; i++) {
			controller.remove(i);
			assertEquals(compare1, compare2);
		}
	}

	@Test
	public void observableList() {
		DefaultObservableList<Integer> list = new DefaultObservableList<>(new Type(Integer.TYPE));
		List<Integer> controller = list.control(null);
		List<Integer> compare1 = new ArrayList<>();
		List<Integer> compare2 = new ArrayList<>();
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
			compare2.add(i);
			assertEquals(new ArrayList<>(list), compare1);
			assertEquals(compare1, compare2);
		}
		for(int i = 0; i < 30; i++) {
			controller.remove((Integer) i);
			compare2.remove((Integer) i);
			assertEquals(new ArrayList<>(list), compare1);
			assertEquals(compare1, compare2);
		}

		for(int i = 0; i < 30; i++) {
			controller.add(i);
			compare2.add(i);
			assertEquals(new ArrayList<>(list), compare1);
			assertEquals(compare1, compare2);
		}
		sub.unsubscribe();
		for(int i = 30; i < 50; i++) {
			controller.add(i);
			assertEquals(compare1, compare2);
		}
		for(int i = 0; i < 30; i++) {
			controller.remove((Integer) i);
			assertEquals(compare1, compare2);
		}
	}

	@Test
	public void observableSetMap() {
		DefaultObservableSet<Integer> set = new DefaultObservableSet<>(new Type(Integer.TYPE));
		Set<Integer> controller = set.control(null);
		Set<Integer> compare1 = new TreeSet<>();
		Set<Integer> compare2 = new TreeSet<>();
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
			compare2.add(i * 10);
			assertEquals(compare1, compare2);
		}
		for(int i = 0; i < 30; i++) {
			controller.remove(i);
			compare2.remove(i * 10);
			assertEquals(compare1, compare2);
		}
	}

	@Test
	public void observableSetFilter() {
		DefaultObservableSet<Integer> set = new DefaultObservableSet<>(new Type(Integer.TYPE));
		Set<Integer> controller = set.control(null);
		Set<Integer> compare1 = new TreeSet<>();
		Set<Integer> compare2 = new TreeSet<>();
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
				compare2.add(i);
			assertEquals(compare1, compare2);
		}
		for(int i = 0; i < 30; i++) {
			controller.remove(i);
			if(i % 2 == 0)
				compare2.remove(i);
			assertEquals(compare1, compare2);
		}
	}

	@Test
	public void observableSetCombine() {
		DefaultObservableSet<Integer> set = new DefaultObservableSet<>(new Type(Integer.TYPE));
		SimpleSettableValue<Integer> obs1 = new SimpleSettableValue<>(Integer.TYPE, false);
		obs1.set(1, null);
		Set<Integer> controller = set.control(null);
		List<Integer> compare1 = new ArrayList<>();
		Set<Integer> compare2 = new TreeSet<>();
		set.combineC(obs1, (v1, v2) -> v1 * v2).filterC(value -> value != null && value % 3 == 0).act(element -> {
			element.subscribe(new Observer<ObservableValueEvent<Integer>>() {
				@Override
				public <V extends ObservableValueEvent<Integer>> void onNext(V value) {
					if(value.getOldValue() != null)
						compare1.remove(value.getOldValue());
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
			int value = i * obs1.get();
			if(value % 3 == 0)
				compare2.add(value);
			assertEquals(new TreeSet<>(compare1), compare2);
		}

		obs1.set(3, null);
		compare2.clear();
		for(int i = 0; i < 30; i++) {
			int value = i * obs1.get();
			if(value % 3 == 0)
				compare2.add(value);
		}
		assertEquals(new TreeSet<>(compare1), compare2);

		obs1.set(10, null);
		compare2.clear();
		for(int i = 0; i < 30; i++) {
			int value = i * obs1.get();
			if(value % 3 == 0)
				compare2.add(value);
		}
		assertEquals(new TreeSet<>(compare1), compare2);

		for(int i = 0; i < 30; i++) {
			controller.remove(i);
			int value = i * obs1.get();
			if(value % 3 == 0)
				compare2.remove(value);
			assertEquals(new TreeSet<>(compare1), compare2);
		}
	}

	// TODO Tests for collection map, filter, combine, find, first, etc. operations
}
