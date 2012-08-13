package org.muis.core.mgr;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import org.muis.core.MuisAttribute;
import org.muis.core.MuisElement;
import org.muis.core.MuisElement.CoreStage;
import org.muis.core.MuisException;
import org.muis.core.event.MuisEvent;

/** Manages attribute information for an element */
public class AttributeManager {
	/** Wraps an attribute and its metadata for this manager */
	public class AttributeHolder {
		private final MuisAttribute<?> attr;

		private IdentityHashMap<Object, Object> theNeeders;

		private IdentityHashMap<Object, Object> theWanters;

		Object theValue;

		AttributeHolder(MuisAttribute<?> anAttr) {
			attr = anAttr;
		}

		/** @return The attribute that this holder holds */
		public MuisAttribute<?> getAttribute() {
			return attr;
		}

		/** @return The value of the attribute in this manager */
		public Object getValue() {
			return theValue;
		}

		synchronized void addWanter(Object wanter, boolean isNeeder) {
			IdentityHashMap<Object, Object> set = isNeeder ? theNeeders : theWanters;
			if(set == null) {
				set = new IdentityHashMap<>();
				if(isNeeder)
					theNeeders = set;
				else
					theWanters = set;
			}
			set.put(wanter, wanter);
		}

		/** @return Whether this attribute is required in this manager */
		public boolean isRequired() {
			IdentityHashMap<Object, Object> needers = theNeeders;
			return needers != null && !needers.isEmpty();
		}

		boolean isWanted() {
			if(isRequired())
				return true;
			IdentityHashMap<Object, Object> wanters = theWanters;
			return wanters != null && wanters.isEmpty();
		}

		synchronized void unrequire(Object wanter) {
			if(theNeeders != null)
				theNeeders.remove(wanter);
			if(theWanters == null)
				theWanters = new IdentityHashMap<>();
			theWanters.put(wanter, wanter);
		}

		synchronized void reject(Object rejecter) {
			if(theNeeders != null)
				theNeeders.remove(rejecter);
			if(theWanters != null)
				theWanters.remove(rejecter);
		}

		/**
		 * Validates an attribute value
		 *
		 * @param attr The attribute to validate
		 * @param value The non-null attribute value to validate
		 * @param el The element that the attribute is for
		 * @param required Whether the attribute is required or not
		 * @return Null if the attribute value is valid for this attribute; an error string describing why the value is not valid otherwise
		 */
		private String validate(String value) {
			String val = attr.type.validate(theElement.getClassView(), value);
			if(val == null)
				return null;
			else
				return (isRequired() ? "Required attribute " : "Attribute ") + attr.name + " " + val;
		}

		@Override
		public String toString() {
			return attr.toString() + (isRequired() ? " (required)" : " (optional)");
		}
	}

	private ConcurrentHashMap<String, AttributeHolder> theAcceptedAttrs;

	private ConcurrentHashMap<String, String> theRawAttributes;

	private MuisElement theElement;

	/** @param element The element to manage attribute information for */
	public AttributeManager(MuisElement element) {
		theAcceptedAttrs = new ConcurrentHashMap<>();
		theRawAttributes = new ConcurrentHashMap<>();
		theElement = element;
		theElement.life().addListener(new LifeCycleListener() {
			@Override
			public void preTransition(String fromStage, String toStage) {
			}

			@Override
			public void postTransition(String oldStage, String newStage) {
				if(newStage.equals(CoreStage.STARTUP)) {
					setReady();
					theElement.life().removeListener(this);
				}
			}
		});
	}

	/**
	 * Sets an attribute typelessly
	 *
	 * @param attr The name of the attribute to set
	 * @param value The string representation of the attribute's value
	 * @return The parsed value for the attribute, or null if the element has not been initialized
	 * @throws MuisException If the attribute is not accepted in the element, the value is null and the attribute is required, or the
	 *             element has already been initialized and the value is not valid for the given attribute
	 */
	public final Object set(String attr, String value) throws MuisException {
		AttributeHolder holder = theAcceptedAttrs.get(attr);
		if(holder == null) {
			if(theElement.life().isAfter(CoreStage.STARTUP.toString()) >= 0)
				throw new MuisException("Attribute " + attr + " is not accepted in this element");
			theRawAttributes.put(attr, value);
			return null;
		}
		return set(holder.attr, value);
	}

	/**
	 * Sets the value of an attribute for the element. If the element has not been fully initialized (by {@link MuisElement#postCreate()},
	 * the attribute's value will be validated and parsed during {@link MuisElement#postCreate()}. If the element has been initialized, the
	 * value will be validated immediately and a {@link MuisException} will be thrown if the value is not valid.
	 *
	 * @param <T> The type of the attribute to set
	 * @param attr The attribute to set
	 * @param value The value for the attribute
	 * @return The parsed value for the attribute, or null if the element has not been initialized
	 * @throws MuisException If the attribute is not accepted in the element, the value is null and the attribute is required, or the
	 *             element has already been initialized and the value is not valid for the given attribute
	 */
	public final <T> T set(MuisAttribute<T> attr, String value) throws MuisException {
		if(theRawAttributes != null)
			theRawAttributes.remove(attr.name);
		AttributeHolder holder = theAcceptedAttrs.get(attr.name);
		if(holder == null) {
			if(theElement.life().isAfter(CoreStage.STARTUP.toString()) >= 0)
				throw new MuisException("Attribute " + attr + " is not accepted in this element");
			holder = new AttributeHolder(attr);
			theAcceptedAttrs.put(attr.name, holder);
			holder.theValue = value;
		}
		else {
			if(value == null && holder.isRequired())
				throw new MuisException("Attribute " + attr + " is required--cannot be set to null");
			T ret;
			if(value == null)
				ret = null;
			else {
				String valError = holder.validate(value);
				if(valError != null)
					throw new MuisException(valError);
				ret = attr.type.parse(theElement.getClassView(), value);
			}
			set(attr, ret);
			return ret;
		}
		return null;
	}

	/**
	 * Sets an attribute's type-correct value
	 *
	 * @param <T> The type of the attribute to set
	 * @param attr The attribute to set
	 * @param value The value to set for the attribute in this element
	 * @throws MuisException If the attribute is not accepted in this element or the value is not valid
	 */
	public final <T> void set(MuisAttribute<T> attr, T value) throws MuisException {
		if(theRawAttributes != null)
			theRawAttributes.remove(attr.name);
		AttributeHolder holder = theAcceptedAttrs.get(attr.name);
		if(holder == null)
			throw new MuisException("Attribute " + attr + " is not accepted in this element");
		if(value == null && holder.isRequired())
			throw new MuisException("Attribute " + attr + " is required--cannot be set to null");
		if(value != null) {
			T newValue = attr.type.cast(value);
			if(newValue == null)
				throw new MuisException("Value " + value + ", type " + value.getClass().getName() + " is not valid for atribute " + attr);
		}
		Object old = holder.theValue;
		holder.theValue = value;
		theElement.fireEvent(new MuisEvent<MuisAttribute<?>>(MuisElement.ATTRIBUTE_SET, attr), false, false);
		theElement.fireEvent(new org.muis.core.event.AttributeChangedEvent<T>(attr, attr.type.cast(old), value), false, false);
	}

	/**
	 * @param name The name of the attribute to get
	 * @return The value of the named attribute
	 */
	public final Object get(String name) {
		AttributeHolder holder = theAcceptedAttrs.get(name);
		if(holder == null)
			return null;
		return holder.theValue;
	}

	/**
	 * Gets the value of an attribute in this element
	 *
	 * @param <T> The type of the attribute to get
	 * @param attr The attribute to get the value of
	 * @return The value of the attribute in this element
	 */
	public final <T> T get(MuisAttribute<T> attr) {
		AttributeHolder storedAttr = theAcceptedAttrs.get(attr.name);
		if(storedAttr != null && !storedAttr.attr.equals(attr))
			return null; // Same name, but different attribute
		Object stored = storedAttr.theValue;
		if(stored == null)
			return null;
		if(theElement.life().isAfter(CoreStage.STARTUP.toString()) < 0 && stored instanceof String)
			try {
				T ret = attr.type.parse(theElement.getClassView(), (String) stored);
				storedAttr.theValue = ret;
				return ret;
			} catch(MuisException e) {
				if(storedAttr != null && storedAttr.isRequired())
					theElement.msg().fatal("Required attribute " + attr + " could not be parsed from " + stored, e, "attribute", attr,
						"value", stored);
				else
					theElement.msg().error("Attribute " + attr + " could not be parsed from " + stored, e, "attribute", attr, "value",
						stored);
				return null;
			}
		else
			return (T) stored;
	}

	/**
	 * Specifies a required attribute for this element
	 *
	 * @param <T> The type of the attribute to require
	 * @param <V> The type of the value for the attribute
	 * @param needer The object that needs the attribute
	 * @param attr The attribute that must be specified for this element
	 * @param initValue The value to set for the attribute if a value is not set already
	 * @throws MuisException If the given value is not acceptable for the given attribute
	 */
	public final <T, V extends T> void require(Object needer, MuisAttribute<T> attr, V initValue) throws MuisException {
		accept(needer, true, attr, initValue);
	}

	/**
	 * Specifies a required attribute for this element
	 *
	 * @param needer The object that needs the attribute
	 * @param attr The attribute that must be specified for this element
	 */
	public final void require(Object needer, MuisAttribute<?> attr) {
		try {
			accept(needer, true, attr, null);
		} catch(MuisException e) {
			throw new IllegalStateException("Should not throw MuisException with null initValue");
		}
	}

	/**
	 * Marks an accepted attribute as not required
	 *
	 * @param wanter The object that cares about the attribute
	 * @param attr The attribute to accept but not require
	 */
	public final void unrequire(Object wanter, MuisAttribute<?> attr) {
		AttributeHolder holder = theAcceptedAttrs.get(attr.name);
		if(holder != null)
			holder.unrequire(wanter);
		else
			accept(wanter, attr);
	}

	/**
	 * Specifies an optional attribute for this element
	 *
	 * @param <T> The type of the attribute to accept
	 * @param <V> The type of the value for the attribute
	 * @param wanter The object that cares about the attribute
	 * @param attr The attribute that may be specified for this element
	 * @param initValue The value to set for the attribute if a value is not set already
	 * @throws MuisException If the given value is not acceptable for the given attribute
	 */
	public final <T, V extends T> void accept(Object wanter, MuisAttribute<T> attr, V initValue) throws MuisException {
		accept(wanter, false, attr, initValue);
	}

	/**
	 * Specifies an optional attribute for this element
	 *
	 * @param wanter The object that cares about the attribute
	 * @param attr The attribute that must be specified for this element
	 */
	public final void accept(Object wanter, MuisAttribute<?> attr) {
		try {
			accept(wanter, false, attr, null);
		} catch(MuisException e) {
			throw new IllegalStateException("Should not throw MuisException with null initValue");
		}
	}

	/**
	 * Sepcifies an optional or required attribute for this element
	 *
	 * @param <T> The type of the attribute to accept
	 * @param <V> The type of the value for the attribute
	 * @param wanter The object that cares about the attribute
	 * @param require Whether the attribute should be required or optional
	 * @param attr The attribute to accept
	 * @param initValue The value to set for the attribute if a value is not set already
	 * @throws MuisException If the given value is not acceptable for the given attribute
	 */
	public final <T, V extends T> void accept(Object wanter, boolean require, MuisAttribute<T> attr, V initValue)
		throws MuisException {
		// if(theLifeCycleManager.isAfter(CoreStage.STARTUP.toString()) > 0)
		// throw new IllegalStateException("Attributes cannot be specified after an element is initialized");
		if(require && initValue == null && theElement.life().isAfter(CoreStage.STARTUP.toString()) > 0)
			throw new IllegalStateException("Attributes may not be required without an initial value after an element is initialized");
		AttributeHolder holder = theAcceptedAttrs.get(attr.name);
		if(holder != null) {
			if(holder.attr.equals(attr))
				holder.addWanter(wanter, require); // The attribute is already required
			else
				throw new IllegalStateException("An attribute named " + attr.name + " (" + holder.attr
					+ ") is already accepted in this element");
		}
		else {
			holder = new AttributeHolder(attr);
			holder.addWanter(wanter, require);
			theAcceptedAttrs.put(attr.name, holder);
			String strVal = theRawAttributes.remove(attr.name);
			if(strVal != null) {
				String valError = holder.validate(strVal);
				if(valError != null)
					theElement.msg().error(valError, "attribute", attr);
				else
					try {
						set((MuisAttribute<Object>) attr, attr.type.parse(theElement.getClassView(), strVal));
					} catch(MuisException e) {
						theElement.msg().error("Could not parse pre-set value \"" + strVal + "\" of attribute " + attr.name, e,
							"attribute", attr);
					}
			}
		}
		if(initValue != null && holder.theValue == null)
			set(attr, initValue);
	}

	/**
	 * Undoes acceptance of an attribute. This method does not remove any attribute value associated with this element. It merely disables
	 * the attribute. If the attribute is accepted on this element later, this element's value of that attribute will be preserved.
	 *
	 * @param wanter The object that used to care about the attribute
	 * @param attr The attribute to not allow in this element
	 */
	public final void reject(Object wanter, MuisAttribute<?> attr) {
		AttributeHolder holder = theAcceptedAttrs.get(attr.name);
		if(holder != null) {
			holder.reject(holder);
			if(!holder.isWanted())
				theAcceptedAttrs.remove(attr.name);
		}
	}

	/** @return The number of attributes set for this element */
	public final int size() {
		return theAcceptedAttrs.size();
	}

	/**
	 * @param attr The attribute to check
	 * @return Whether the given attribute can be set in this element
	 */
	public final boolean isAccepted(MuisAttribute<?> attr) {
		AttributeHolder holder = theAcceptedAttrs.get(attr.name);
		return holder != null && holder.attr.equals(attr);
	}

	/**
	 * @param attr The attribute to check
	 * @return Whether the given attribute is required in this element
	 */
	public final boolean isRequired(MuisAttribute<?> attr) {
		AttributeHolder holder = theAcceptedAttrs.get(attr.name);
		return holder != null && !holder.attr.equals(attr) && holder.isRequired();
	}

	/** @return An iterable to iterate through all accepted attributes in this manager */
	public Iterable<MuisAttribute<?>> attributes() {
		return new Iterable<MuisAttribute<?>>() {
			@Override
			public Iterator<MuisAttribute<?>> iterator() {
				return new Iterator<MuisAttribute<?>>() {
					private final Iterator<AttributeHolder> theWrapped = theAcceptedAttrs.values().iterator();

					@Override
					public boolean hasNext() {
						return theWrapped.hasNext();
					}

					@Override
					public MuisAttribute<?> next() {
						return theWrapped.next().getAttribute();
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	/** @return An iterable to iterate through the metadata of each accepted attribute in this manager */
	public Iterable<AttributeHolder> holders() {
		return new Iterable<AttributeHolder>() {
			@Override
			public Iterator<AttributeHolder> iterator() {
				return new Iterator<AttributeHolder>() {
					private final Iterator<AttributeHolder> theWrapped = theAcceptedAttrs.values().iterator();

					@Override
					public boolean hasNext() {
						return theWrapped.hasNext();
					}

					@Override
					public AttributeHolder next() {
						return theWrapped.next();
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	private void setReady() {
		for(AttributeHolder holder : theAcceptedAttrs.values()) {
			MuisAttribute<?> attr = holder.attr;
			boolean required = holder.isRequired();
			Object value = holder.theValue;
			if(value == null && required)
				theElement.msg().fatal("Required attribute " + attr + " not set");
			if(value instanceof String) {
				String valError = holder.validate((String) value);
				if(valError != null)
					theElement.msg().fatal(valError);
				try {
					value = attr.type.parse(theElement.getClassView(), (String) value);
					set((MuisAttribute<Object>) attr, value);
				} catch(MuisException e) {
					if(required)
						theElement.msg().fatal("Required attribute " + attr + " could not be parsed", e, "attribute", attr, "value", value);
					else
						theElement.msg().error("Attribute " + attr + " could not be parsed", e, "attribute", attr, "value", value);
					holder.theValue = null;
				}
			}
			else if(value != null && attr.type.cast(value) == null) {
				if(required)
					theElement.msg().fatal("Unrecognized value for required attribute " + attr + ": " + value, "value", value);
				else
					theElement.msg().error("Unrecognized value for attribute " + attr + ": " + value, "value", value);
				holder.theValue = null;
			}
			if(value != null)
				theElement.fireEvent(new MuisEvent<MuisAttribute<?>>(MuisElement.ATTRIBUTE_SET, attr), false, false);
		}
		for(java.util.Map.Entry<String, String> attr : theRawAttributes.entrySet())
			theElement.msg().error("Attribute " + attr.getKey() + " is not accepted in this element", "value", attr.getValue());
		theRawAttributes = null;
	}
}
