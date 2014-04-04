package org.muis.core.event.boole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of {@link UnionTypedPredicate}
 *
 * @param <T> The type of objects that this predicate can test and produce
 */
public class TPOr<T> implements UnionTypedPredicate<T, T> {
	private List<TypedPredicate<? super T, ? extends T>> theComponents;

	/** @param components The components for this OR operation */
	public TPOr(Iterable<? extends TypedPredicate<? super T, ? extends T>> components) {
		ArrayList<TypedPredicate<? super T, ? extends T>> comps = new ArrayList<>();
		for(TypedPredicate<? super T, ? extends T> comp : components)
			comps.add(comp);
		theComponents = Collections.unmodifiableList(comps);
	}

	/** @param components The components for this OR operation */
	public TPOr(TypedPredicate<? super T, ? extends T>... components) {
		ArrayList<TypedPredicate<? super T, ? extends T>> comps = new ArrayList<>();
		for(TypedPredicate<? super T, ? extends T> comp : components)
			comps.add(comp);
		theComponents = Collections.unmodifiableList(comps);
	}

	@Override
	public List<TypedPredicate<? super T, ? extends T>> getComponents() {
		return theComponents;
	}

	@Override
	public T cast(T value) {
		T ret = null;
		for(TypedPredicate<? super T, ? extends T> comp : theComponents) {
			ret = comp.cast(value);
			if(ret != null)
				return ret;
		}
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == this)
			return true;
		if(!(obj instanceof TPOr))
			return false;
		TPOr<?> or = (TPOr<?>) obj;
		boolean [] hits = new boolean[theComponents.size()];
		for(int i = 0; i < or.theComponents.size(); i++) {
			int index = theComponents.indexOf(or.theComponents.get(i));
			if(index < 0)
				return false;
			hits[index] = true;
		}
		for(int i = 0; i < theComponents.size(); i++)
			if(!hits[i] && !or.theComponents.contains(theComponents.get(i)))
				return false;
		return true;
	}

	/**
	 * @param or The or operation to test against
	 * @return Whether this or operation contains all of the given operation's components; i.e. whether this operation is always true for an
	 *         object if the given operation is true for the same object
	 */
	public boolean contains(TPOr<?> or) {
		for(TypedPredicate<?, ?> comp : or.theComponents)
			if(!(theComponents.contains(comp)))
				return false;
		return true;
	}
}
