package org.muis.core.style.sheet;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.muis.core.MuisTemplate.AttachPoint;

/**
 * Represents a path of attach points that a widget is located at. For example, a text-field widget may have a border pane in its template
 * whose attach point is "border". A border widget may have a block in its template whose attach point is "contents". So that "contents"
 * block within the "border" border pane of a text-field has a template path of "border.contents".
 */
public class TemplatePath implements List<AttachPoint> {
	private List<AttachPoint> thePath;

	/** @param path The attach points for the path */
	public TemplatePath(List<AttachPoint> path) {
		thePath = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(path));
	}

	/** @param path The attach points for the path */
	public TemplatePath(AttachPoint... path) {
		thePath = java.util.Collections.unmodifiableList(java.util.Arrays.asList(path));
	}

	/**
	 * @param base The base template path to append onto
	 * @param append The attach point to append onto the base path
	 */
	public TemplatePath(TemplatePath base, AttachPoint append) {
		List<AttachPoint> path = new java.util.ArrayList<>(base.thePath);
		path.add(append);
		thePath = java.util.Collections.unmodifiableList(path);
	}

	/**
	 * @param path The path to test
	 * @return Whether a widget with this template path also satisfies the given path
	 */
	public boolean containsPath(TemplatePath path) {
		if(path.thePath.size() > thePath.size())
			return false;
		int sz = thePath.size();
		int pSz = path.thePath.size();
		for(int i = 0; i < sz && i < pSz; i++)
			if(thePath.get(sz - i - 1) != path.thePath.get(pSz - i - 1))
				return false;
		return true;
	}

	@Override
	public boolean equals(Object o) {
		if(!(o instanceof TemplatePath))
			return false;
		return thePath.equals(((TemplatePath) o).thePath);
	}

	@Override
	public int hashCode() {
		return thePath.hashCode();
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		ret.append(thePath.get(0).template.getDefiner().getSimpleName());
		for(AttachPoint ap : thePath)
			ret.append('#').append(ap.name);
		return thePath.toString();
	}

	@Override
	public int size() {
		return thePath.size();
	}

	@Override
	public boolean isEmpty() {
		return thePath.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		if(o instanceof TemplatePath)
			return containsPath((TemplatePath) o);
		return thePath.contains(o);
	}

	@Override
	public Iterator<AttachPoint> iterator() {
		return thePath.iterator();
	}

	@Override
	public AttachPoint [] toArray() {
		return thePath.toArray(new AttachPoint[thePath.size()]);
	}

	@Override
	public <T> T [] toArray(T [] a) {
		return thePath.toArray(a);
	}

	@Override
	public boolean add(AttachPoint e) {
		throw new UnsupportedOperationException("TemplatePath is immutable");
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException("TemplatePath is immutable");
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return thePath.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends AttachPoint> c) {
		throw new UnsupportedOperationException("TemplatePath is immutable");
	}

	@Override
	public boolean addAll(int index, Collection<? extends AttachPoint> c) {
		throw new UnsupportedOperationException("TemplatePath is immutable");
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException("TemplatePath is immutable");
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException("TemplatePath is immutable");
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException("TemplatePath is immutable");
	}

	@Override
	public AttachPoint get(int index) {
		return thePath.get(index);
	}

	@Override
	public AttachPoint set(int index, AttachPoint element) {
		throw new UnsupportedOperationException("TemplatePath is immutable");
	}

	@Override
	public void add(int index, AttachPoint element) {
		throw new UnsupportedOperationException("TemplatePath is immutable");
	}

	@Override
	public AttachPoint remove(int index) {
		throw new UnsupportedOperationException("TemplatePath is immutable");
	}

	@Override
	public int indexOf(Object o) {
		return thePath.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return thePath.lastIndexOf(o);
	}

	@Override
	public ListIterator<AttachPoint> listIterator() {
		return thePath.listIterator();
	}

	@Override
	public ListIterator<AttachPoint> listIterator(int index) {
		return thePath.listIterator(index);
	}

	@Override
	public List<AttachPoint> subList(int fromIndex, int toIndex) {
		return thePath.subList(fromIndex, toIndex);
	}
}
