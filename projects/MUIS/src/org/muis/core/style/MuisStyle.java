package org.muis.core.style;

import org.muis.core.event.MuisEventListener;

import prisms.util.ArrayUtils;

/** Governs the set of properties that define how MUIS elements of different types render themselves */
public abstract class MuisStyle implements Iterable<StyleAttribute<?>> {
	private final java.util.concurrent.ConcurrentHashMap<StyleAttribute<?>, Object> theAttributes;

	private MuisStyle [] theDependents;

	private MuisEventListener<Void> [] theListeners;

	/** Creates a MUIS style */
	public MuisStyle() {
		theAttributes = new java.util.concurrent.ConcurrentHashMap<StyleAttribute<?>, Object>();
		theDependents = new MuisStyle[0];
		theListeners = new MuisEventListener[0];
	}

	/** @return The parent style that this style gets attributes from when the attributes are not set in this style directly */
	public abstract MuisStyle getParent();

	/** @return The styles, in order, that this style depends on for attributes not set directly in this style */
	public MuisStyle [] getDependencies() {
		MuisStyle parent = getParent();
		if(parent != null)
			return new MuisStyle[] {parent};
		else
			return new MuisStyle[0];
	}

	/**
	 * @param attr The attribute to check
	 * @return Whether the attribute is set directly in this style
	 */
	public boolean isSet(StyleAttribute<?> attr) {
		return theAttributes.containsKey(attr);
	}

	/**
	 * @param <T> The type of attribute to get the value of
	 * @param attr The attribute to get the value of
	 * @return The value of the attribute set directly in this style, or null if it is not set
	 */
	public <T> T getLocal(StyleAttribute<T> attr) {
		return (T) theAttributes.get(attr);
	}

	/**
	 * Gets the value of the attribute in this style or its dependencies. This style is checked first, then dependencies are checked. If the
	 * attribute is not set in this style or its dependencies, then the attribute's default value is returned.
	 *
	 * @param <T> The type of attribute to get the value of
	 * @param attr The attribute to get the value of
	 * @return The value of the attribute in this style's scope
	 */
	public <T> T get(StyleAttribute<T> attr) {
		T ret = (T) theAttributes.get(attr);
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
		if(!attr.getType().getType().isInstance(value))
			throw new ClassCastException(value.getClass().getName() + " instance " + value + " cannot be set for attribute " + attr
				+ " of type " + attr.getType().getType().getName());
		if(attr.getValidator() != null)
			try {
				attr.getValidator().assertValid(value);
			} catch(org.muis.core.MuisException e) {
				throw new IllegalArgumentException(e.getMessage());
			}
		theAttributes.put(attr, value);
		fireEvent(attr, value);
	}

	/**
	 * Clears the value of an attribute from this style directly, such that {@link #isSet(StyleAttribute)} returns false after this.
	 *
	 * @param attr The attribute to clear
	 */
	public void clear(StyleAttribute<?> attr) {
		if(attr == null)
			return;
		theAttributes.remove(attr);
		fireEvent(attr, null);
	}

	private <T> void fireEvent(StyleAttribute<T> attr, T value) {
		fireEvent(new StyleAttributeEvent<T>(this, attr, value));
	}

	/** @param event The style change event to fire */
	protected void fireEvent(StyleAttributeEvent<?> event) {
		for(MuisEventListener<Void> listener : theListeners)
			listener.eventOccurred(event, null);
		for(MuisStyle dependent : theDependents)
			dependent.fireEvent(event);
	}

	/**
	 * Adds a listener for style changes to this style
	 *
	 * @param listener The listener to add
	 */
	public void addListener(MuisEventListener<Void> listener) {
		if(listener != null && !ArrayUtils.contains(theListeners, listener))
			theListeners = ArrayUtils.add(theListeners, listener);
	}

	/**
	 * Removes a listener for style changes from this style
	 *
	 * @param listener The listener to remove
	 */
	public void removeListener(MuisEventListener<Void> listener) {
		theListeners = ArrayUtils.remove(theListeners, listener);
	}

	void addDependent(MuisStyle dependent) {
		if(dependent != null && !ArrayUtils.contains(theDependents, dependent))
			theDependents = ArrayUtils.add(theDependents, dependent);
	}

	void removeDependent(MuisStyle dependent) {
		theDependents = ArrayUtils.remove(theDependents, dependent);
	}

	/**
	 * Returns an iterator of attributes set in this style or any of its dependencies
	 *
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public java.util.Iterator<StyleAttribute<?>> iterator() {
		return new AttributeIterator(this, getDependencies());
	}

	/**
	 * @return An iterable for attributes set locally in this style
	 */
	public Iterable<StyleAttribute<?>> localAttributes() {
		return new Iterable<StyleAttribute<?>>() {
			@Override
			public java.util.Iterator<StyleAttribute<?>> iterator() {
				return new AttributeIterator(MuisStyle.this, new MuisStyle[0]);
			}
		};
	}

	/** An iterator for style attributes in a style and its dependencies */
	protected static class AttributeIterator implements java.util.Iterator<StyleAttribute<?>> {
		private final StyleAttribute<?> [] theLocalAttribs;

		private final java.util.Iterator<StyleAttribute<?>> [] theDependencies;

		private int index;

		private int childIndex;

		/**
		 * @param style The style for local attributes
		 * @param dependencies The set of the style's dependencies
		 */
		protected AttributeIterator(MuisStyle style, MuisStyle [] dependencies) {
			theLocalAttribs = style.theAttributes.keySet().toArray(new StyleAttribute[0]);
			theDependencies = new java.util.Iterator[dependencies.length];
			for(int d = 0; d < dependencies.length; d++)
				theDependencies[d] = dependencies[d].iterator();
			childIndex = -1;
		}

		@Override
		public boolean hasNext() {
			if(index < theLocalAttribs.length)
				return true;
			if(childIndex < 0)
				childIndex = 0;
			while(childIndex < theDependencies.length && !theDependencies[childIndex].hasNext())
				childIndex++;
			return childIndex < theDependencies.length;
		}

		@Override
		public StyleAttribute<?> next() {
			StyleAttribute<?> ret;
			if(index < theLocalAttribs.length) {
				ret = theLocalAttribs[index];
				index++;
			} else {
				if(childIndex < 0)
					childIndex = 0;
				while(childIndex < theDependencies.length && !theDependencies[childIndex].hasNext())
					childIndex++;
				if(childIndex < theDependencies.length)
					ret = theDependencies[childIndex].next();
				else
					throw new java.util.NoSuchElementException();
			}
			return ret;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Style iterators are immutable");
		}
	}
}
