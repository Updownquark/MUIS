package org.muis.core.rx;

import java.util.LinkedHashSet;
import java.util.Set;

public class CompoundObservableSet<E> extends DefaultObservableSet<E> {
	private LinkedHashSet<ObservableCollection<? extends E>> theSets;
	private Set<E> theController;
	private DefaultObservable<ObservableCollection<? extends E>> theRemoveObservable;
	private Observer<ObservableCollection<? extends E>> theRemoveController;

	public CompoundObservableSet() {
		theController = control();
		theSets = new LinkedHashSet<>();
		theRemoveObservable = new DefaultObservable<>();
		theRemoveController = theRemoveObservable.control(null);
	}

	public void addSet(ObservableCollection<? extends E> set) {
		theSets.add(set);
		Observable<ObservableCollection<? extends E>> removedObs = theRemoveObservable.filter(removed -> removed == set);
		set.takeUntil(removedObs).act(element -> {
			element.takeUntil(removedObs).subscribe(new Observer<E>() {
				@Override
				public <V extends E> void onNext(V value) {
					theController.add(value);
				}

				@Override
				public <V extends E> void onCompleted(V value) {
					for(ObservableCollection<? extends E> otherSet : theSets) {
						if(otherSet != set && otherSet.contains(value))
							return;
					}
					theController.remove(value);
				}
			});
		});
	}

	public void removeSet(ObservableCollection<? extends E> set) {
		theRemoveController.onNext(set);
		theSets.remove(set);
		// Now remove the elements
		doLocked(() -> {
			for(E value : set) {
				boolean stillHas = false;
				for(ObservableCollection<? extends E> otherSet : theSets)
					if(otherSet.contains(value)) {
						stillHas = true;
						break;
					}
				if(!stillHas)
					theController.remove(value);
			}
		}, true);

	}

	public Set<ObservableCollection<? extends E>> getSets() {
		return java.util.Collections.unmodifiableSet(theSets);
	}
}
