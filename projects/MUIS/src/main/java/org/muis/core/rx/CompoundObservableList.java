package org.muis.core.rx;

import java.util.ArrayList;
import java.util.List;

public class CompoundObservableList<E> extends DefaultObservableList<E> {
	private ArrayList<ObservableList<? extends E>> theLists;
	private prisms.util.IntList theListIndexes;
	private List<E> theController;
	private DefaultObservable<ObservableList<? extends E>> theRemoveObservable;
	private Observer<ObservableList<? extends E>> theRemoveController;

	public CompoundObservableList() {
		theController = control();
		theLists = new ArrayList<>();
		theRemoveObservable = new DefaultObservable<>();
		theRemoveController = theRemoveObservable.control(null);
	}

	public void addList(ObservableList<? extends E> list) {
		addList(theListIndexes.size(), list);
	}

	public void addList(int index, ObservableList<? extends E> list) {
		theLists.add(index, list);
		theListIndexes.add(index, index == theListIndexes.size() ? size() : theListIndexes.get(index));
		Observable<ObservableList<? extends E>> removedObs = theRemoveObservable.filter(removed -> removed == list);
		list.takeUntil(removedObs).act(element -> {
			element.takeUntil(removedObs).subscribe(new Observer<E>() {
				E theValue;

				@Override
				public <V extends E> void onNext(V value) {
					boolean replace = theValue != null;
					theValue = value;
					int listIndex = theLists.indexOf(list);
					if(replace) {
						int elIndex = theListIndexes.get(listIndex) + list.indexOf(value);
						theController.set(elIndex, value);
					} else {
						int elIndex = theListIndexes.size() == listIndex + 1 ? size() : theListIndexes.get(listIndex + 1);
						theController.add(elIndex, value);
						for(int i = listIndex + 1; i < theListIndexes.size(); i++)
							theListIndexes.set(i, theListIndexes.get(i) + 1);
					}
				}

				@Override
				public <V extends E> void onCompleted(V value) {
					int listIndex = theLists.indexOf(list);
					int start = theListIndexes.get(listIndex);
					int end = theListIndexes.size() == listIndex + 1 ? size() : theListIndexes.get(listIndex + 1);
					boolean found = false;
					for(int i = start; i < end; i++)
						if(theController.get(i).equals(value)) {
							found = true;
							theController.remove(i);
							break;
						}
					if(found) // Should always find here, not sure what to do if we don't
						for(int i = listIndex + 1; i < theListIndexes.size(); i++)
							theListIndexes.set(i, theListIndexes.get(i) - 1);
				}
			});
		});
	}

	public void removeList(ObservableList<? extends E> list) {
		int index = theLists.indexOf(list);
		if(index < 0)
			return;
		theRemoveController.onNext(list);
		int elIndex = theListIndexes.get(index);
		// Now remove the elements
		doLocked(() -> {
			for(int i = list.size() - 1; i >= 0; i--) {
				remove(elIndex + 1);
			}
		}, true);
		theLists.remove(index);
		theListIndexes.remove(index);

	}

	public void setList(int index, ObservableList<? extends E> list) {
		removeList(theLists.get(index));
		addList(index, list);
	}

	public List<ObservableList<? extends E>> getLists() {
		return java.util.Collections.unmodifiableList(theLists);
	}
}
