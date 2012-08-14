package org.muis.core.style;

import java.util.Iterator;

/** Governs the set of properties that define how MUIS elements of different types render themselves */
public abstract class MuisStyle implements Iterable<StyleAttribute<?>> {

	/** @return The styles, in order, that this style depends on for attributes not set directly in this style */
	public abstract MuisStyle [] getDependencies();

	/**
	 * @param attr The attribute to check
	 * @return Whether the attribute is set directly in this style
	 */
	public abstract boolean isSet(StyleAttribute<?> attr);

	/**
	 * @param attr The attribute to check
	 * @return Whether the attribute is set in this style or one of its ancestors
	 */
	public abstract boolean isSetDeep(StyleAttribute<?> attr);

	/**
	 * @param <T> The type of attribute to get the value of
	 * @param attr The attribute to get the value of
	 * @return The value of the attribute set directly in this style, or null if it is not set
	 */
	public abstract <T> T getLocal(StyleAttribute<T> attr);

	/**
	 * This is called after the value is checked against the attribute, so this is just a simple set operation with no error checking
	 *
	 * @param attr The attribute to set the value of
	 * @param value The value to set for the attribute
	 */
	protected abstract <T> void setValue(StyleAttribute<T> attr, T value);

	/**
	 * Clears the value of an attribute from this style directly, such that {@link #isSet(StyleAttribute)} returns false after this.
	 *
	 * @param attr The attribute to clear
	 */
	public abstract void clear(StyleAttribute<?> attr);

	/** @return An iterable for attributes set locally in this style */
	public abstract Iterable<StyleAttribute<?>> localAttributes();

	/**
	 * Gets the value of the attribute in this style or its dependencies. This style is checked first, then dependencies are checked. If the
	 * attribute is not set in this style or its dependencies, then the attribute's default value is returned.
	 *
	 * @param <T> The type of attribute to get the value of
	 * @param attr The attribute to get the value of
	 * @return The value of the attribute in this style's scope
	 */
	public <T> T get(StyleAttribute<T> attr) {
		T ret = getLocal(attr);
		if(ret != null)
			return ret;
		for(MuisStyle dep : getDependencies()) {
			ret = dep.get(attr);
			if(ret != null)
				return ret;
		}
		return attr.getDefault();
	}

	/**
	 * Sets the value of an attribute in this style
	 *
	 * @param <T> The type of the attribute to set the value of
	 * @param attr The attribute to set the value of
	 * @param value The value to set for the attribute
	 * @throws IllegalArgumentException If the value set is not valid for the attribute
	 */
	public <T> void set(StyleAttribute<T> attr, T value) throws IllegalArgumentException {
		if(value == null) {
			clear(attr);
			return;
		}
		if(attr == null)
			throw new NullPointerException("Cannot set the value of a null attribute");
		T value2 = attr.getType().cast(value);
		if(value2 == null)
			throw new ClassCastException(value.getClass().getName() + " instance " + value + " cannot be set for attribute " + attr
				+ " of type " + attr.getType());
		value = value2;
		if(attr.getValidator() != null)
			try {
				attr.getValidator().assertValid(value);
			} catch(org.muis.core.MuisException e) {
				throw new IllegalArgumentException(e.getMessage());
			}
		setValue(attr, value);
	}

	@Override
	public Iterator<StyleAttribute<?>> iterator() {
		return new AttributeIterator(this, getDependencies());
	}

	/** An iterator for style attributes in a style and its dependencies */
	protected static class AttributeIterator implements java.util.Iterator<StyleAttribute<?>> {
		private Iterator<StyleAttribute<?>> theLocalAttribs;

		private final Iterator<StyleAttribute<?>> [] theDependencies;

		private int childIndex;

		private boolean calledNext;

		/**
		 * @param style The style for local attributes
		 * @param dependencies The set of the style's dependencies
		 */
		public AttributeIterator(MuisStyle style, MuisStyle... dependencies) {
			theLocalAttribs = style.localAttributes().iterator();
			theDependencies = new java.util.Iterator[dependencies.length];
			for(int d = 0; d < dependencies.length; d++)
				theDependencies[d] = dependencies[d].iterator();
			childIndex = -1;
		}

		@Override
		public boolean hasNext() {
			calledNext = false;
			if(theLocalAttribs != null) {
				if(theLocalAttribs.hasNext())
					return true;
				else
					theLocalAttribs = null;
			}
			if(childIndex < 0)
				childIndex = 0;
			while(childIndex < theDependencies.length && !theDependencies[childIndex].hasNext())
				childIndex++;
			return childIndex < theDependencies.length;
		}

		@Override
		public StyleAttribute<?> next() {
			if((calledNext && !hasNext()) || childIndex >= theDependencies.length)
				throw new java.util.NoSuchElementException();
			calledNext = true;
			if(theLocalAttribs != null)
				return theLocalAttribs.next();
			return theDependencies[childIndex].next();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Style iterators are immutable");
		}
	}
}
