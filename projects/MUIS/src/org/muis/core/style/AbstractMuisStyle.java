package org.muis.core.style;

import prisms.util.ArrayUtils;

/** A more full partial implementation of MuisStyle */
public class AbstractMuisStyle extends MuisStyle {
	private static final Object NULL = new Object();

	private final java.util.concurrent.ConcurrentHashMap<StyleAttribute<?>, Object> theAttributes;

	private final java.util.concurrent.ConcurrentLinkedQueue<StyleListener> theListeners;

	private AbstractMuisStyle [] theDependencies;

	private final StyleListener theDependencyListener;

	/**
	 * Creates an abstract MUIS style
	 *
	 * @param dependencies The initial set of dependencies for this style
	 */
	public AbstractMuisStyle(AbstractMuisStyle... dependencies) {
		theAttributes = new java.util.concurrent.ConcurrentHashMap<StyleAttribute<?>, Object>();
		theListeners = new java.util.concurrent.ConcurrentLinkedQueue<>();
		theDependencies = dependencies;
		theDependencyListener = new StyleListener() {
			@Override
			public void eventOccurred(StyleAttributeEvent<?> event) {
				if(isSet(event.getAttribute()))
					return;
				int idx = ArrayUtils.indexOf(theDependencies, event.getRootStyle());
				if(idx < 0)
					return;
				for(int i = 0; i < idx; i++)
					if(theDependencies[i].isSetDeep(event.getAttribute()))
						return;
				Object value = event.getRootStyle().isSetDeep(event.getAttribute()) ? event.getValue() : get(event.getAttribute());
				styleChanged(event.getAttribute(), value, event.getRootStyle());
			}
		};
		for(AbstractMuisStyle dep : theDependencies)
			dep.addListener(theDependencyListener);
	}

	@Override
	public final MuisStyle [] getDependencies() {
		return theDependencies;
	}

	/**
	 * @param depend The dependency to add
	 * @param after The dependency to add the new dependency after, or null to add it as the first dependency
	 */
	protected void addDependency(AbstractMuisStyle depend, AbstractMuisStyle after) {
		int idx;
		if(after == null)
			idx = 0;
		else
			idx = ArrayUtils.indexOf(theDependencies, after);
		if(idx < 0)
			throw new IllegalArgumentException(after + " is not a dependency of " + this);
		theDependencies = ArrayUtils.add(theDependencies, depend, idx);
		depend.addListener(theDependencyListener);
		for(StyleAttribute<?> attr : depend) {
			if(isSet(attr))
				continue;
			for(int i = 0; i < idx; i++)
				if(theDependencies[i].isSetDeep(attr))
					continue;
			styleChanged(attr, depend.get(attr), null);
		}
	}

	/**
	 * Adds a dependency as the last dependency
	 *
	 * @param depend The dependency to add
	 */
	protected void addDependency(AbstractMuisStyle depend) {
		theDependencies = ArrayUtils.add(theDependencies, depend);
		depend.addListener(theDependencyListener);
		for(StyleAttribute<?> attr : depend) {
			if(isSet(attr))
				continue;
			for(int i = 0; i < theDependencies.length - 1; i++)
				if(theDependencies[i].isSetDeep(attr))
					continue;
			styleChanged(attr, depend.get(attr), null);
		}
	}

	/** @param depend The dependency to remove */
	protected void removeDependency(AbstractMuisStyle depend) {
		int idx = ArrayUtils.indexOf(theDependencies, depend);
		if(idx < 0)
			return;
		depend.removeListener(theDependencyListener);
		theDependencies = ArrayUtils.remove(theDependencies, idx);
		for(StyleAttribute<?> attr : depend) {
			if(isSet(attr))
				continue;
			for(int i = 0; i < idx; i++)
				if(theDependencies[i].isSetDeep(attr))
					continue;
			styleChanged(attr, get(attr), null);
		}
	}

	/**
	 * @param toReplace The dependency to replace
	 * @param depend The dependency to add in place of the given dependency to replace
	 */
	protected void replaceDependency(AbstractMuisStyle toReplace, AbstractMuisStyle depend) {
		int idx = ArrayUtils.indexOf(theDependencies, depend);
		if(idx < 0)
			throw new IllegalArgumentException(toReplace + " is not a dependency of " + this);
		toReplace.removeListener(theDependencyListener);
		theDependencies[idx] = depend;
		java.util.HashSet<StyleAttribute<?>> attrs = new java.util.HashSet<>();
		for(StyleAttribute<?> attr : toReplace) {
			if(isSet(attr))
				continue;
			for(int i = 0; i < idx; i++)
				if(theDependencies[i].isSetDeep(attr))
					continue;
			attrs.add(attr);
		}
		depend.addListener(theDependencyListener);
		for(StyleAttribute<?> attr : depend) {
			if(isSet(attr))
				continue;
			for(int i = 0; i < idx; i++)
				if(theDependencies[i].isSetDeep(attr))
					continue;
			attrs.remove(attr);
			styleChanged(attr, depend.get(attr), null);
		}
		for(StyleAttribute<?> attr : attrs)
			styleChanged(attr, get(attr), null);
	}

	@Override
	public boolean isSet(StyleAttribute<?> attr) {
		return theAttributes.containsKey(attr);
	}

	@Override
	public boolean isSetDeep(StyleAttribute<?> attr) {
		if(isSet(attr))
			return true;
		for(MuisStyle dep : theDependencies)
			if(dep.isSetDeep(attr))
				return true;
		return false;
	}

	@Override
	public <T> T getLocal(StyleAttribute<T> attr) {
		Object ret = theAttributes.get(attr);
		if(ret == NULL)
			ret = null;
		return (T) ret;
	}

	@Override
	public <T> void setValue(StyleAttribute<T> attr, T value) {
		if(value == null)
			value = (T) NULL;
		theAttributes.put(attr, value);
		styleChanged(attr, value, null);
	}

	@Override
	public void clear(StyleAttribute<?> attr) {
		if(theAttributes.containsKey(attr)) {
			theAttributes.remove(attr);
			styleChanged(attr, get(attr), null);
		}
	}

	@Override
	public Iterable<StyleAttribute<?>> localAttributes() {
		return ArrayUtils.immutableIterable(theAttributes.keySet());
	}

	void styleChanged(StyleAttribute<?> attr, Object value, MuisStyle root) {
		StyleAttributeEvent<Object> event = new StyleAttributeEvent<Object>(root == null ? this : root, this,
			(StyleAttribute<Object>) attr, value);
		for(StyleListener listener : theListeners)
			listener.eventOccurred(event);
	}

	/** @param listener The listener to be notified when the effective value of any style attribute in this style changes */
	public void addListener(StyleListener listener) {
		if(listener != null)
			theListeners.add(listener);
	}

	/** @param listener The listener to remove */
	public void removeListener(StyleListener listener) {
		theListeners.remove(listener);
	}
}
