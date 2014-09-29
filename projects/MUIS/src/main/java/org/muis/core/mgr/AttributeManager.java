package org.muis.core.mgr;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import org.muis.core.*;
import org.muis.core.event.AttributeChangedEvent;
import org.muis.core.rx.DefaultObservableValue;
import org.muis.core.rx.ObservableValue;
import org.muis.core.rx.ObservableValueEvent;
import org.muis.core.rx.Observer;

import prisms.lang.Type;

/** Manages attribute information for an element */
public class AttributeManager {
	/**
	 * Wraps an attribute and its metadata for this manager
	 *
	 * @param <T> The type of the attribute to hold
	 */
	public class AttributeHolder<T> extends DefaultObservableValue<T> {
		private AttributeHolder<T> theParent;

		private final MuisAttribute<T> theAttr;

		private final Observer<ObservableValueEvent<T>> theController;

		private IdentityHashMap<Object, Object> theNeeders;

		private IdentityHashMap<Object, Object> theWanters;

		private boolean wasWanted;

		private volatile T theLastGoodValue;
		private volatile boolean isOVError;
		private ObservableValue<ObservableValue<? extends T>> theContainerObservable;
		private Observer<ObservableValueEvent<ObservableValue<? extends T>>> theContainerController;
		private ObservableValue<? extends T> theContainedObservable;
		private volatile int theStackChecker;

		{
			theController = control(null);
			theContainerObservable = new DefaultObservableValue<ObservableValue<? extends T>>() {
				private Type theType;

				@Override
				public Type getType() {
					if(theType == null)
						theType = new Type(DefaultObservableValue.class, new Type(ObservableValue.class, new Type(theAttr.getType()
							.getType(), true)));
					return theType;
				}

				@Override
				public ObservableValue<? extends T> get() {
					return theContainedObservable;
				}
			};
			theContainerController = ((DefaultObservableValue<ObservableValue<? extends T>>) theContainerObservable)
				.control(null);
		}

		AttributeHolder(MuisAttribute<T> attr) {
			theAttr = attr;
		}

		AttributeHolder(MuisPathedAttribute<T> attr, AttributeHolder<T> parent) {
			theParent = parent;
			theAttr = attr;
		}

		/** @return The attribute that this holder holds */
		public final MuisAttribute<T> getAttribute() {
			return theAttr;
		}

		/** @return The value of the attribute in this manager */
		@Override
		public final T get() {
			if(isOVError)
				return theLastGoodValue;
			else if(theContainedObservable == null)
				return null;
			return theContainedObservable.get();
		}

		/** @return The observable that this holder encapsulates */
		public final ObservableValue<? extends T> getContainedObservable() {
			return theContainedObservable;
		}

		@Override
		public final Type getType() {
			return theAttr.getType().getType();
		}

		/**
		 * @param valueStr The formatted value to set for the attribute
		 * @param context The context in which to parse and evaluate the attribute value string
		 * @return The parsed value
		 * @throws MuisException If the value cannot be parsed or cannot be set for the attribute
		 */
		public final T set(String valueStr, MuisParseEnv context) throws MuisException {
			ObservableValue<? extends T> value = theAttr.getType().parse(context, valueStr);
			setContainedObservable(value);
			return value.get();
		}

		/**
		 * @param value The value to set for the attribute
		 * @throws MuisException If the value cannot be set for the attribute
		 */
		public final void set(T value) throws MuisException {
			setContainedObservable(ObservableValue.constant(value));
		}

		/**
		 * @param observable The new observable for this attribute's value to reflect
		 * @throws MuisException If the observable's current value is incompatible with this attribute's type
		 */
		public final void setContainedObservable(ObservableValue<? extends T> observable) throws MuisException {
			T value = observable == null ? null : observable.get();
			try {
				checkValue(value);
			} catch(MuisException e) {
				isOVError = true;
				theController.onError(e);
				throw e;
			}
			isOVError = false;
			T oldValue = theLastGoodValue;
			ObservableValue<? extends T> oldObservable = theContainedObservable;
			theContainedObservable = observable;
			theLastGoodValue = value;
			fire(oldValue, value);
			theContainerController.onNext(new ObservableValueEvent<>(theContainerObservable, oldObservable, theContainedObservable, null));
			observable.takeUntil(theContainerObservable).act(evt -> {
				try {
					checkValue(evt.getValue());
				} catch(MuisException e) {
					isOVError = true;
					theController.onError(e);
					return;
				}
				isOVError = false;
				T oldEventValue = theLastGoodValue;
				theLastGoodValue = evt.getValue();
				fire(oldEventValue, evt.getValue());
			});
		}

		private void checkValue(T value) throws MuisException {
			if(value == null && isRequired())
				throw new MuisException("Attribute " + theAttr + " is required--cannot be set to null");
			if(value != null) {
				T newValue = theAttr.getType().cast(value);
				if(newValue == null)
					throw new MuisException("Value " + value + ", type " + value.getClass().getName() + " is not valid for atribute "
						+ theAttr);
				if(theAttr.getValidator() != null)
					theAttr.getValidator().assertValid(value);
			}
		}

		private void fire(T oldValue, T value) {
			theStackChecker++;
			final int stackCheck = theStackChecker;
			AttributeChangedEvent<T> evt = new AttributeChangedEvent<T>(theElement, this, theAttr, theAttr.getType().cast(oldValue), value,
				null) {
				@Override
				public boolean isOverridden() {
					return stackCheck != theStackChecker;
				}
			};
			theController.onNext(evt);
			theElement.events().fire(evt);
		}

		synchronized void addWanter(Object wanter, boolean isNeeder) {
			if(theParent != null)
				throw new IllegalStateException("Only root attributes can be wanted");
			wasWanted = true;
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
		public final boolean isRequired() {
			if(theParent != null)
				return false;
			IdentityHashMap<Object, Object> needers = theNeeders;
			return needers != null && !needers.isEmpty();
		}

		final boolean isWanted() {
			if(theParent != null)
				return theParent.isWanted();
			if(isRequired())
				return true;
			IdentityHashMap<Object, Object> wanters = theWanters;
			return wanters != null && !wanters.isEmpty();
		}

		final boolean wasWanted() {
			if(theParent != null)
				return theParent.wasWanted();
			return wasWanted;
		}

		synchronized final void unrequire(Object wanter) {
			if(theParent != null)
				throw new IllegalStateException("Non-root attributes cannot be unrequired");
			if(theNeeders != null)
				theNeeders.remove(wanter);
			if(theWanters == null)
				theWanters = new IdentityHashMap<>();
			theWanters.put(wanter, wanter);
		}

		synchronized final void reject(Object rejecter) {
			if(theParent != null)
				throw new IllegalStateException("Non-root attributes cannot be rejected");
			if(theNeeders != null)
				theNeeders.remove(rejecter);
			if(theWanters != null)
				theWanters.remove(rejecter);
		}

		@Override
		public String toString() {
			return theAttr.toString() + (isRequired() ? " (required)" : " (optional)");
		}
	}

	private static class RawAttributeValue {
		final String value;
		final MuisParseEnv context;

		RawAttributeValue(String val, MuisParseEnv ctx) {
			value = val;
			context = ctx;
		}
	}

	private ConcurrentHashMap<MuisAttribute<?>, AttributeHolder<?>> theAcceptedAttrs;
	private ConcurrentHashMap<String, MuisAttribute<?>> theAttributesByName;
	private ConcurrentHashMap<String, RawAttributeValue> theRawAttributes;

	private MuisElement theElement;

	/** @param element The element to manage attribute information for */
	public AttributeManager(MuisElement element) {
		theAcceptedAttrs = new ConcurrentHashMap<>();
		theAttributesByName = new ConcurrentHashMap<>();
		theRawAttributes = new ConcurrentHashMap<>();
		theElement = element;
		theElement.life().runWhen(() -> {
			setReady();
		}, MuisConstants.CoreStage.STARTUP.toString(), 0);
	}

	private <T> AttributeHolder<T> getHolder(MuisAttribute<T> attr, boolean add) throws MuisException {
		AttributeHolder<T> holder = (AttributeHolder<T>) theAcceptedAttrs.get(attr);
		if(holder == null && add) {
			if(theRawAttributes != null)
				theRawAttributes.remove(attr.getName());
			if(attr instanceof MuisPathedAttribute) {
				MuisPathedAttribute<T> pathed = (MuisPathedAttribute<T>) attr;
				holder = (AttributeHolder<T>) theAcceptedAttrs.get(pathed.getBase());
				if(holder == null) {
					if(theElement.life().isAfter(MuisConstants.CoreStage.STARTUP.toString()) >= 0)
						throw new MuisException("Attribute " + attr + " is not accepted in this element");
					holder = new AttributeHolder<>(pathed.getBase());
					theAcceptedAttrs.put(attr, holder);
				}
				AttributeHolder<T> pathedHolder = new AttributeHolder<>(pathed, holder);
				theAcceptedAttrs.put(attr, pathedHolder);
				theAttributesByName.put(attr.getName(), attr);
				holder = pathedHolder;
			} else {
				if(theElement.life().isAfter(MuisConstants.CoreStage.STARTUP.toString()) >= 0)
					throw new MuisException("Attribute " + attr + " is not accepted in this element");
				holder = new AttributeHolder<>(attr);
				theAcceptedAttrs.put(attr, holder);
				theAttributesByName.put(attr.getName(), attr);
			}
		}
		return holder;
	}

	/**
	 * @param <T> The type of the attribute
	 * @param attr The attribute to get the holder for
	 * @return The attribute holder for the given attribute, or null if the given attribute is not accepted in this manager
	 */
	public <T> AttributeHolder<T> getHolder(MuisAttribute<T> attr) {
		try {
			return getHolder(attr, false);
		} catch(MuisException e) {
			throw new IllegalStateException("Should not be thrown here", e);
		}
	}

	/**
	 * Sets an attribute typelessly
	 *
	 * @param attr The name of the attribute to set
	 * @param value The string representation of the attribute's value
	 * @param context The context in which to parse and evaluate the attribute value string
	 * @return The parsed value for the attribute, or null if the element has not been initialized
	 * @throws MuisException If the attribute is not accepted in the element, the value is null and the attribute is required, or the
	 *             element has already been initialized and the value is not valid for the given attribute
	 */
	public final Object set(String attr, String value, MuisParseEnv context) throws MuisException {
		MuisAttribute<?> attrObj = theAttributesByName.get(attr);
		if(attrObj != null)
			return set(attrObj, value, context);
		String baseName = attr;
		int dotIdx = baseName.indexOf('.');
		if(dotIdx >= 0)
			baseName = baseName.substring(0, dotIdx);
		attrObj = theAttributesByName.get(baseName);
		AttributeHolder<?> holder;
		if(attrObj != null) {
			holder = theAcceptedAttrs.get(attrObj);
			if(holder.theAttr.getPathAccepter() == null)
				throw new MuisException("Attribute " + attr + " is not hierarchical");
			String [] path = attr.substring(dotIdx + 1).split("\\.");
			if(!holder.theAttr.getPathAccepter().accept(theElement, path))
				throw new MuisException("Attribute " + attr + " does not accept path \"" + attr.substring(dotIdx + 1) + "\"");
			return set(new MuisPathedAttribute<>(holder.theAttr, theElement, path), value, context);
		} else {
			if(theElement.life().isAfter(MuisConstants.CoreStage.STARTUP.toString()) >= 0)
				throw new MuisException("Attribute " + attr + " is not accepted in this element");
			if(value == null)
				theRawAttributes.remove(attr);
			else
				theRawAttributes.put(attr, new RawAttributeValue(value, context));
			return null;
		}
	}

	/**
	 * Sets the value of an attribute for the element. If the element has not been fully initialized (by {@link MuisElement#postCreate()},
	 * the attribute's value will be validated and parsed during {@link MuisElement#postCreate()}. If the element has been initialized, the
	 * value will be validated immediately and a {@link MuisException} will be thrown if the value is not valid.
	 *
	 * @param <T> The type of the attribute to set
	 * @param attr The attribute to set
	 * @param value The value for the attribute
	 * @param context The context in which to parse and evaluate the attribute value string
	 * @return The parsed value for the attribute, or null if the element has not been initialized
	 * @throws MuisException If the attribute is not accepted in the element, the value is null and the attribute is required, or the
	 *             element has already been initialized and the value is not valid for the given attribute
	 */
	public final <T> T set(MuisAttribute<T> attr, String value, MuisParseEnv context) throws MuisException {
		return getHolder(attr, true).set(value, context);
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
		getHolder(attr, true).set(value);
	}

	/**
	 * @param name The name of the attribute to get
	 * @return The value of the named attribute
	 */
	public final Object get(String name) {
		MuisAttribute<?> attr = theAttributesByName.get(name);
		if(attr == null)
			return null;
		AttributeHolder<?> holder = theAcceptedAttrs.get(attr);
		if(holder == null)
			return null;
		return holder.get();
	}

	/**
	 * @param name The name of the attribute to check
	 * @return Whether an attribute with the given name is set in this attribute manager
	 */
	public final boolean isSet(String name) {
		MuisAttribute<?> attr = theAttributesByName.get(name);
		if(attr == null)
			return false;
		AttributeHolder<?> holder = theAcceptedAttrs.get(attr);
		if(holder != null && holder.get() != null)
			return true;
		if(theRawAttributes != null && theRawAttributes.get(name) != null)
			return true;
		return false;
	}

	/**
	 * @param attr The attribute to check
	 * @return Whether a value is set in this attribute manager for the given attribute
	 */
	public final boolean isSet(MuisAttribute<?> attr) {
		AttributeHolder<?> holder = theAcceptedAttrs.get(attr);
		return holder != null && holder.get() != null;
	}

	/**
	 * Gets the value of an attribute in this manager
	 *
	 * @param <T> The type of the attribute to get
	 * @param attr The attribute to get the value of
	 * @return The value of the attribute in this manager, or null if the attribute is not set
	 */
	public final <T> T get(MuisAttribute<T> attr) {
		return get(attr, null);
	}

	/**
	 * Gets the value of an attribute in this manager, returning a default value if the attribute is not set
	 *
	 * @param <T> The type of the attribute to get
	 * @param attr The attribute to get the value of
	 * @param def The default value to return if the attribute is not set in this manager
	 * @return The value of the attribute in this manager, or <code>def</code> if the attribute is not set
	 */
	public final <T> T get(MuisAttribute<T> attr, T def) {
		AttributeHolder<T> storedAttr = (AttributeHolder<T>) theAcceptedAttrs.get(attr);
		if(storedAttr == null)
			return def;
		if(storedAttr.get() == null)
			return def; // Same name, but different attribute
		return storedAttr.get();
	}

	/**
	 * Specifies a required attribute for this element
	 *
	 * @param <T> The type of the attribute to require
	 * @param <V> The type of the value for the attribute
	 * @param needer The object that needs the attribute
	 * @param attr The attribute that must be specified for this element
	 * @param initValue The value to set for the attribute if a value is not set already
	 * @return The attribute holder for the attribute in this manager
	 * @throws MuisException If the given value is not acceptable for the given attribute
	 */
	public final <T, V extends T> AttributeHolder<T> require(Object needer, MuisAttribute<T> attr, V initValue) throws MuisException {
		return accept(needer, true, attr, initValue);
	}

	/**
	 * Specifies a required attribute for this element
	 *
	 * @param <T> The type of the attribute to require
	 * @param needer The object that needs the attribute
	 * @param attr The attribute that must be specified for this element
	 * @return The attribute holder for the attribute in this manager
	 */
	public final <T> AttributeHolder<T> require(Object needer, MuisAttribute<T> attr) {
		try {
			return accept(needer, true, attr, null);
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
		AttributeHolder<?> holder = theAcceptedAttrs.get(attr);
		if(holder != null)
			holder.unrequire(wanter);
	}

	/**
	 * Specifies an optional attribute for this element
	 *
	 * @param <T> The type of the attribute to accept
	 * @param <V> The type of the value for the attribute
	 * @param wanter The object that cares about the attribute
	 * @param attr The attribute that may be specified for this element
	 * @param initValue The value to set for the attribute if a value is not set already
	 * @return The attribute holder for the attribute in this manager
	 * @throws MuisException If the given value is not acceptable for the given attribute
	 */
	public final <T, V extends T> AttributeHolder<T> accept(Object wanter, MuisAttribute<T> attr, V initValue) throws MuisException {
		return accept(wanter, false, attr, initValue);
	}

	/**
	 * Specifies an optional attribute for this element
	 *
	 * @param <T> The type of the attribute to accept
	 * @param wanter The object that cares about the attribute
	 * @param attr The attribute that must be specified for this element
	 * @return The attribute holder for the attribute in this manager
	 */
	public final <T> AttributeHolder<T> accept(Object wanter, MuisAttribute<T> attr) {
		try {
			return accept(wanter, false, attr, null);
		} catch(MuisException e) {
			throw new IllegalStateException("Should not throw MuisException with null initValue");
		}
	}

	/**
	 * Specifies an optional or required attribute for this element
	 *
	 * @param <T> The type of the attribute to accept
	 * @param <V> The type of the value for the attribute
	 * @param wanter The object that cares about the attribute
	 * @param require Whether the attribute should be required or optional
	 * @param attr The attribute to accept
	 * @param initValue The value to set for the attribute if a value is not set already
	 * @return The attribute holder for the attribute in this manager
	 * @throws MuisException If the given value is not acceptable for the given attribute
	 */
	public final <T, V extends T> AttributeHolder<T> accept(Object wanter, boolean require, MuisAttribute<T> attr, V initValue)
		throws MuisException {
		if(attr instanceof MuisPathedAttribute)
			throw new IllegalArgumentException("Pathed attributes cannot be accepted or required");
		if(require && initValue == null && theElement.life().isAfter(MuisConstants.CoreStage.STARTUP.toString()) > 0)
			throw new IllegalStateException("Attributes may not be required without an initial value after an element is initialized");
		AttributeHolder<T> holder = (AttributeHolder<T>) theAcceptedAttrs.get(attr);
		if(holder != null) {
			fireAccepted(require, attr, initValue);
			holder.addWanter(wanter, require); // The attribute is already accepted
		} else {
			holder = new AttributeHolder<>(attr);
			holder.addWanter(wanter, require);
			theAcceptedAttrs.put(attr, holder);
			theAttributesByName.put(attr.getName(), attr);
			fireAccepted(require, attr, initValue);
			RawAttributeValue strVal = theRawAttributes.remove(attr.getName());
			if(strVal != null) {
				try {
					holder.set(strVal.value, strVal.context);
				} catch(MuisException e) {
					theElement.msg().error("Could not parse pre-set value \"" + strVal + "\" of attribute " + attr.getName(), e,
						"attribute", attr);
				}
			}
		}
		if(initValue != null && holder.get() == null)
			holder.set(initValue);
		return holder;
	}

	private void fireAccepted(boolean require, MuisAttribute<?> attr, Object value) {
		theElement.events().fire(new org.muis.core.event.AttributeAcceptedEvent(theElement, attr, true, require, value));
	}

	/**
	 * Undoes acceptance of an attribute. This method does not remove any attribute value associated with this element. It merely disables
	 * the attribute. If the attribute is accepted on this element later, this element's value of that attribute will be preserved.
	 *
	 * @param wanter The object that used to care about the attribute
	 * @param attr The attribute to not allow in this element
	 */
	public final void reject(Object wanter, MuisAttribute<?> attr) {
		if(attr instanceof MuisPathedAttribute)
			throw new IllegalArgumentException("Pathed attributes cannot be rejected");
		AttributeHolder<?> holder = theAcceptedAttrs.get(attr.getName());
		if(holder != null) {
			holder.reject(holder);
			if(!holder.isWanted()) {
				theAcceptedAttrs.remove(attr);
				namedAttrRemoved(attr);
			}
			theElement.events().fire(new org.muis.core.event.AttributeAcceptedEvent(theElement, attr, false, false, null));
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
		if(attr instanceof MuisPathedAttribute)
			return isAccepted(((MuisPathedAttribute<?>) attr).getBase());
		AttributeHolder<?> holder = theAcceptedAttrs.get(attr);
		return holder != null;
	}

	/**
	 * @param attr The attribute to check
	 * @return Whether the given attribute is required in this element
	 */
	public final boolean isRequired(MuisAttribute<?> attr) {
		if(attr instanceof MuisPathedAttribute)
			return false;
		AttributeHolder<?> holder = theAcceptedAttrs.get(attr);
		return holder != null && holder.isRequired();
	}

	/** @return An iterable to iterate through all accepted attributes in this manager */
	public Iterable<MuisAttribute<?>> attributes() {
		return () -> {
			return new Iterator<MuisAttribute<?>>() {
				private final Iterator<AttributeHolder<?>> theWrapped = holders().iterator();

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
		};
	}

	/** @return An iterable to iterate through the metadata of each accepted attribute in this manager */
	public Iterable<AttributeHolder<?>> holders() {
		return () -> {
			return new Iterator<AttributeHolder<?>>() {
				private final Iterator<AttributeHolder<?>> theWrapped = theAcceptedAttrs.values().iterator();

				private AttributeHolder<?> theNext;

				private boolean calledNext = true;

				@Override
				public boolean hasNext() {
					if(!calledNext)
						return theNext != null;
					calledNext = false;
					theNext = null;
					while(theNext == null) {
						if(!theWrapped.hasNext())
							return false;
						theNext = theWrapped.next();
						if(!theNext.isWanted())
							theNext = null;
					}
					return theNext != null;
				}

				@Override
				public AttributeHolder<?> next() {
					if(calledNext && !hasNext())
						return theWrapped.next();
					calledNext = true;
					if(theNext == null)
						return theWrapped.next();
					else
						return theNext;
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		};
	}

	private void setReady() {
		Iterator<AttributeHolder<?>> holders = holders().iterator();
		while(holders.hasNext()) {
			AttributeHolder<?> holder = holders.next();
			if(!holder.wasWanted()) {
				holders.remove();
				namedAttrRemoved(holder.getAttribute());
				theElement.msg().error("Attribute " + holder.getAttribute() + " is not accepted in this element", "value", holder.get());
			} else if(holder.isRequired() && holder.get() == null) {
				theElement.msg().error("Attribute " + holder.getAttribute() + " is required but has no value in this element");
			}
		}
		for(java.util.Map.Entry<String, RawAttributeValue> attr : theRawAttributes.entrySet())
			theElement.msg().error("No attribute named " + attr.getKey() + " is not accepted in this element", "value",
				attr.getValue().value);
		theRawAttributes = null;
	}

	private void namedAttrRemoved(MuisAttribute<?> attr) {
		if(theAttributesByName.get(attr.getName()) != attr)
			return;
		/* Not sure how to tag this.  There *could* potentially be a fundamental problem with the way the attribute manager's guts are
		 * structured here.  The potential problem is that if 3 or more attributes of the same name are accepted in an element and the last
		 * one to be accepted is then rejected, the one that should be exposed by name would be the second attribute that was accepted.
		 * The manager, however, keeps record only of the latest one added, so it has a roughly even chance of picking the right one among
		 * the remaining attributes.
		 *
		 * The acceptance of multiple attributes with the same name was added to support the role attribute in templates, but that attribute
		 * should never be rejected, so it isn't a problem for that.  Hopefully it will never be a problem for anybody, as fixing it the
		 * right way in a concurrency-safe way will involve a bit more thought and perhaps a slight performance hit for locking.
		 */
		boolean found = false;
		for(MuisAttribute<?> att : theAcceptedAttrs.keySet())
			if(att != attr && att.getName().equals(attr.getName())) {
				theAttributesByName.put(attr.getName(), att);
				found = true;
				break;
			}
		if(!found)
			theAttributesByName.remove(attr.getName());
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		org.jdom2.output.EscapeStrategy strategy = ch -> {
			if(org.jdom2.Verifier.isHighSurrogate(ch)) {
				return true; // Safer this way per http://unicode.org/faq/utf_bom.html#utf8-4
			}
			return false;
		};
		for(AttributeHolder<?> holder : holders()) {
			if(!holder.isWanted() || holder.get() == null)
				continue;
			if(ret.length() > 0)
				ret.append(' ');
			ret.append(holder.getAttribute().getName()).append('=');
			String value;
			if(holder.getAttribute().getType() instanceof MuisProperty.PrintablePropertyType)
				value = "\""
					+ org.jdom2.output.Format.escapeAttribute(strategy, ((MuisProperty.PrintablePropertyType<Object>) holder.getAttribute()
						.getType()).toString(holder.get())) + "\"";
			else
				value = String.valueOf(holder.get());
			ret.append(value);
		}
		return ret.toString();
	}
}
