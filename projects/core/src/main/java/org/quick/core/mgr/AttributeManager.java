package org.quick.core.mgr;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import org.observe.*;
import org.observe.assoc.ObservableMap;
import org.observe.assoc.impl.ObservableMapImpl;
import org.observe.collect.ObservableSet;
import org.quick.core.QuickConstants;
import org.quick.core.QuickElement;
import org.quick.core.QuickException;
import org.quick.core.QuickParseEnv;
import org.quick.core.event.AttributeChangedEvent;
import org.quick.core.prop.QuickAttribute;
import org.quick.core.prop.QuickPropertyType;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;

/** Manages attribute information for an element */
public class AttributeManager {
	/**
	 * Wraps an attribute and its metadata for this manager
	 *
	 * @param <T> The type of the attribute to hold
	 */
	public class AttributeHolder<T> extends DefaultObservableValue<T> {
		private final QuickAttribute<T> theAttr;

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
				private TypeToken<ObservableValue<? extends T>> theType;

				@Override
				public TypeToken<ObservableValue<? extends T>> getType() {
					if (theType == null)
						theType = new TypeToken<ObservableValue<? extends T>>() {}.where(new TypeParameter<T>() {},
							theAttr.getType().getType());
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

		AttributeHolder(QuickAttribute<T> attr) {
			theAttr = attr;
		}

		/** @return The attribute that this holder holds */
		public final QuickAttribute<T> getAttribute() {
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

		/** @return The holder of values for this attribute */
		public final ObservableValue<ObservableValue<? extends T>> getContainer() {
			return theContainerObservable;
		}

		@Override
		public final TypeToken<T> getType() {
			return theAttr.getType().getType();
		}

		/**
		 * Creates a {@link SettableValue} that reflect's this attribute's model value. The {@link SettableValue#isEnabled()} field will
		 * only be true if the content of this this attribute is an enabled settable value. Calling
		 * {@link SettableValue#set(Object, Object)} on the value will call the set method on that contained value. The set method will
		 * never change the actual content of the attribute and will be disabled if the content is a constant value or a non-settable
		 * observable.
		 *
		 * @return A settable value for setting this attribute's content value
		 */
		public final SettableValue<T> asSettable() {
			return new SettableValue.SettableFlattenedObservableValue<T>(theContainerObservable, () -> AttributeHolder.this.get()) {
				@Override
				public T get() {
					return AttributeHolder.this.get();
				}

				@Override
				public Subscription subscribe(Observer<? super ObservableValueEvent<T>> observer) {
					return AttributeHolder.this.subscribe(observer);
				}

				@Override
				public boolean isSafe() {
					return AttributeHolder.this.isSafe();
				}

				@Override
				public <V extends T> String isAcceptable(V value) {
					if (isOVError)
						return "Attribute is in an error state";
					else if (theContainedObservable == null)
						return "Attribute has no value";
					else if (theContainedObservable instanceof SettableValue)
						return ((SettableValue<T>) theContainedObservable).isAcceptable(value);
					else
						return "Attribute is not settable";
				}

				@Override
				public ObservableValue<String> isEnabled() {
					return ObservableValue.flatten(theContainerObservable.mapV(ob -> {
						if (isOVError)
							return ObservableValue.constant("Attribute is in an error state");
						else if (ob == null)
							return ObservableValue.constant("Attribute has no value");
						else if (ob instanceof SettableValue)
							return ((SettableValue<T>) ob).isEnabled();
						else
							return ObservableValue.constant("Attribute is not settable");
					}));
				}
			};
		}

		/**
		 * @param valueStr The formatted value to set for the attribute
		 * @param context The context in which to parse and evaluate the attribute value string
		 * @return The parsed value
		 * @throws QuickException If the value cannot be parsed or cannot be set for the attribute
		 */
		public final T set(String valueStr, QuickParseEnv context) throws QuickException {
			ObservableValue<? extends T> value = theElement.getDocument().getEnvironment().getPropertyParser().parseProperty(theAttr,
				context, valueStr);
			setContainedObservable(value);
			return value.get();
		}

		/**
		 * @param value The value to set for the attribute
		 * @throws QuickException If the value cannot be set for the attribute
		 */
		public final void set(T value) throws QuickException {
			setContainedObservable(ObservableValue.constant(value));
		}

		/**
		 * @param observable The new observable for this attribute's value to reflect
		 * @throws QuickException If the observable's current value is incompatible with this attribute's type
		 */
		public final void setContainedObservable(ObservableValue<? extends T> observable) throws QuickException {
			T value = observable == null ? null : observable.get();
			try {
				checkValue(value);
			} catch(QuickException e) {
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
			theContainerController.onNext(theContainerObservable.createChangeEvent(oldObservable, theContainedObservable, null));
			observable.noInit().takeUntil(theContainerObservable.noInit()).act(evt -> {
				try {
					checkValue(evt.getValue());
				} catch(QuickException e) {
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

		private void checkValue(T value) throws QuickException {
			if(value == null && isRequired())
				throw new QuickException("Attribute " + theAttr + " is required--cannot be set to null");
			if(value != null) {
				T newValue = theAttr.getType().cast((TypeToken<T>) TypeToken.of(value.getClass()), value);
				if(newValue == null)
					throw new QuickException("Value " + value + ", type " + value.getClass().getName() + " is not valid for atribute "
						+ theAttr);
				if(theAttr.getValidator() != null)
					theAttr.getValidator().assertValid(value);
			}
		}

		private void fire(T oldValue, T value) {
			theStackChecker++;
			final int stackCheck = theStackChecker;
			AttributeChangedEvent<T> evt;
			try {
				T old = oldValue == null ? null : theAttr.getType().cast((TypeToken<T>) TypeToken.of(oldValue.getClass()), oldValue);
				evt = new AttributeChangedEvent<T>(theElement, this, theAttr, false, old, value, null) {
					@Override
					public boolean isOverridden() {
						return stackCheck != theStackChecker;
					}
				};
			} catch(Exception e) {
				throw new IllegalStateException(toString() + ": " + e, e);
			}
			theController.onNext(evt);
			theElement.events().fire(evt);
		}

		synchronized void addWanter(Object wanter, boolean isNeeder) {
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
			IdentityHashMap<Object, Object> needers = theNeeders;
			return needers != null && !needers.isEmpty();
		}

		final boolean isWanted() {
			if(isRequired())
				return true;
			IdentityHashMap<Object, Object> wanters = theWanters;
			return wanters != null && !wanters.isEmpty();
		}

		final boolean wasWanted() {
			return wasWanted;
		}

		synchronized final void unrequire(Object wanter) {
			if(theNeeders != null)
				theNeeders.remove(wanter);
			if(theWanters == null)
				theWanters = new IdentityHashMap<>();
			theWanters.put(wanter, wanter);
		}

		synchronized final void reject(Object rejecter) {
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
		final QuickParseEnv context;

		RawAttributeValue(String val, QuickParseEnv ctx) {
			value = val;
			context = ctx;
		}

		@Override
		public String toString() {
			return value;
		}
	}

	private ObservableMap<QuickAttribute<?>, AttributeHolder<?>> theAcceptedAttrs;
	private ConcurrentHashMap<String, QuickAttribute<?>> theAttributesByName;
	private ConcurrentHashMap<String, RawAttributeValue> theRawAttributes;

	private QuickElement theElement;

	/** @param element The element to manage attribute information for */
	public AttributeManager(QuickElement element) {
		theAcceptedAttrs = new ObservableMapImpl<>(new TypeToken<QuickAttribute<?>>() {}, new TypeToken<AttributeHolder<?>>() {});
		theAttributesByName = new ConcurrentHashMap<>();
		theRawAttributes = new ConcurrentHashMap<>();
		theElement = element;
		theElement.life().runWhen(() -> {
			setReady();
		}, QuickConstants.CoreStage.STARTUP.toString(), 0);
	}

	private <T> AttributeHolder<T> getHolder(QuickAttribute<T> attr, boolean add) throws QuickException {
		AttributeHolder<T> holder = (AttributeHolder<T>) theAcceptedAttrs.get(attr);
		if(holder == null && add) {
			if(theRawAttributes != null)
				theRawAttributes.remove(attr.getName());
			if (theElement.life().isAfter(QuickConstants.CoreStage.STARTUP.toString()) >= 0)
				throw new QuickException("Attribute " + attr + " is not accepted in this element");
			holder = new AttributeHolder<>(attr);
			theAcceptedAttrs.put(attr, holder);
			theAttributesByName.put(attr.getName(), attr);
		}
		return holder;
	}

	/** @return All attributes accepted in this manager */
	public ObservableSet<QuickAttribute<?>> getAllAttributes() {
		return theAcceptedAttrs.keySet().immutable();
	}

	/** @return A map of all attributes accepted in this manager to their current values */
	public ObservableMap<QuickAttribute<?>, ?> getAllValues() {
		return ObservableMap.flatten(theAcceptedAttrs).immutable();
	}

	/**
	 * @param <T> The type of the attribute
	 * @param attr The attribute to watch
	 * @return An observable value for the given attribute in this manager, even if the attribute is not accepted
	 */
	public <T> ObservableValue<T> observe(QuickAttribute<T> attr) {
		return ObservableValue.flatten(theAcceptedAttrs.observe(attr).mapV(
			new TypeToken<AttributeHolder<T>>() {}.where(new TypeParameter<T>() {}, attr.getType().getType()),
			h -> (AttributeHolder<T>) h));
	}

	/**
	 * @param <T> The type of the attribute
	 * @param attr The attribute to get the holder for
	 * @return The attribute holder for the given attribute, or null if the given attribute is not accepted in this manager
	 */
	public <T> AttributeHolder<T> getHolder(QuickAttribute<T> attr) {
		try {
			return getHolder(attr, false);
		} catch(QuickException e) {
			throw new IllegalStateException("Should not be thrown here", e);
		}
	}

	/**
	 * @param attrs The attributes to watch
	 * @return An observable that fires a {@link AttributeChangedEvent} for every change affecting the given attribute values in this
	 *         element
	 */
	public Observable<AttributeChangedEvent<?>> watch(QuickAttribute<?>... attrs) {
		return org.observe.collect.ObservableCollection
			.fold(org.observe.collect.ObservableSet.constant(new TypeToken<QuickAttribute<?>>() {}, attrs).map(attr -> getHolder(attr)))
			.map(event -> (AttributeChangedEvent<?>) event);
	}

	/**
	 * Sets an attribute typelessly
	 *
	 * @param attr The name of the attribute to set
	 * @param value The string representation of the attribute's value
	 * @param context The context in which to parse and evaluate the attribute value string
	 * @return The parsed value for the attribute, or null if the element has not been initialized
	 * @throws QuickException If the attribute is not accepted in the element, the value is null and the attribute is required, or the
	 *             element has already been initialized and the value is not valid for the given attribute
	 */
	public final Object set(String attr, String value, QuickParseEnv context) throws QuickException {
		QuickAttribute<?> attrObj = theAttributesByName.get(attr);
		if(attrObj != null)
			return set(attrObj, value, context);
		else {
			if(theElement.life().isAfter(QuickConstants.CoreStage.STARTUP.toString()) >= 0)
				throw new QuickException("Attribute " + attr + " is not accepted in this element");
			if(value == null)
				theRawAttributes.remove(attr);
			else
				theRawAttributes.put(attr, new RawAttributeValue(value, context));
			return null;
		}
	}

	/**
	 * Sets the value of an attribute for the element. If the element has not been fully initialized (by {@link QuickElement#postCreate()},
	 * the attribute's value will be validated and parsed during {@link QuickElement#postCreate()}. If the element has been initialized, the
	 * value will be validated immediately and a {@link QuickException} will be thrown if the value is not valid.
	 *
	 * @param <T> The type of the attribute to set
	 * @param attr The attribute to set
	 * @param value The value for the attribute
	 * @param context The context in which to parse and evaluate the attribute value string
	 * @return The parsed value for the attribute, or null if the element has not been initialized
	 * @throws QuickException If the attribute is not accepted in the element, the value is null and the attribute is required, or the
	 *             element has already been initialized and the value is not valid for the given attribute
	 */
	public final <T> T set(QuickAttribute<T> attr, String value, QuickParseEnv context) throws QuickException {
		AttributeHolder<T> holder = getHolder(attr, false);
		if (holder != null)
			return holder.set(value, context);
		else
			throw new QuickException("Attribute " + attr.getName() + " not accepted");
	}

	/**
	 * Sets an attribute's type-correct value
	 *
	 * @param <T> The type of the attribute to set
	 * @param attr The attribute to set
	 * @param value The value to set for the attribute in this element
	 * @throws QuickException If the attribute is not accepted in this element or the value is not valid
	 */
	public final <T> void set(QuickAttribute<T> attr, T value) throws QuickException {
		AttributeHolder<T> holder = getHolder(attr, false);
		if (holder != null)
			holder.set(value);
		else
			throw new QuickException("Attribute " + attr.getName() + " not accepted");
	}

	/**
	 * @param name The name of the attribute to get
	 * @return The value of the named attribute
	 */
	public final Object get(String name) {
		QuickAttribute<?> attr = theAttributesByName.get(name);
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
		QuickAttribute<?> attr = theAttributesByName.get(name);
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
	public final boolean isSet(QuickAttribute<?> attr) {
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
	public final <T> T get(QuickAttribute<T> attr) {
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
	public final <T> T get(QuickAttribute<T> attr, T def) {
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
	 * @throws QuickException If the given value is not acceptable for the given attribute
	 */
	public final <T, V extends T> AttributeHolder<T> require(Object needer, QuickAttribute<T> attr, V initValue) throws QuickException {
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
	public final <T> AttributeHolder<T> require(Object needer, QuickAttribute<T> attr) {
		try {
			return accept(needer, true, attr, null);
		} catch(QuickException e) {
			throw new IllegalStateException("Should not throw QuickException with null initValue");
		}
	}

	/**
	 * Specifies required attributes for this element
	 *
	 * @param needer The object that needs the attributes
	 * @param attrs The attributes that must be specified for this element
	 * @return This attribute manager
	 */
	public final AttributeManager require(Object needer, QuickAttribute<?>... attrs) {
		for(QuickAttribute<?> attr : attrs)
			try {
				accept(needer, true, attr, null);
			} catch(QuickException e) {
				throw new IllegalStateException("Should not throw QuickException with null initValue");
			}
		return this;
	}

	/**
	 * Marks accepted attributes as not required
	 *
	 * @param wanter The object that cares about the attributes
	 * @param attrs The attributes to accept but not require
	 * @return This attribute manager
	 */
	public final AttributeManager unrequire(Object wanter, QuickAttribute<?>... attrs) {
		for(QuickAttribute<?> attr : attrs) {
			AttributeHolder<?> holder = theAcceptedAttrs.get(attr);
			if(holder != null)
				holder.unrequire(wanter);
		}
		return this;
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
	 * @throws QuickException If the given value is not acceptable for the given attribute
	 */
	public final <T, V extends T> AttributeHolder<T> accept(Object wanter, QuickAttribute<T> attr, V initValue) throws QuickException {
		return accept(wanter, false, attr, initValue);
	}

	/**
	 * Specifies an optional attribute for this element
	 *
	 * @param <T> The type of the attribute to accept
	 * @param wanter The object that cares about the attribute
	 * @param attr The attribute that may be specified for this element
	 * @return The attribute holder for the attribute in this manager
	 */
	public final <T> AttributeHolder<T> accept(Object wanter, QuickAttribute<T> attr) {
		try {
			return accept(wanter, false, attr, null);
		} catch(QuickException e) {
			throw new IllegalStateException("Should not throw QuickException with null initValue");
		}
	}

	/**
	 * Specifies optional attributes for this element
	 *
	 * @param wanter The object that cares about the attributes
	 * @param attrs The attributes that may be specified for this element
	 * @return This attribute manager
	 */
	public final AttributeManager accept(Object wanter, QuickAttribute<?>... attrs) {
		for(QuickAttribute<?> attr : attrs)
			try {
				accept(wanter, false, attr, null);
			} catch(QuickException e) {
				throw new IllegalStateException("Should not throw QuickException with null initValue");
			}
		return this;
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
	 * @throws QuickException If the given value is not acceptable for the given attribute
	 */
	public final <T, V extends T> AttributeHolder<T> accept(Object wanter, boolean require, QuickAttribute<T> attr, V initValue)
		throws QuickException {
		if(require && initValue == null && theElement.life().isAfter(QuickConstants.CoreStage.STARTUP.toString()) > 0)
			throw new IllegalStateException("Attributes may not be required without an initial value after an element is initialized");
		AttributeHolder<T> holder = (AttributeHolder<T>) theAcceptedAttrs.get(attr);
		if(holder != null) {
			holder.addWanter(wanter, require); // The attribute is already accepted
		} else {
			holder = new AttributeHolder<>(attr);
			holder.addWanter(wanter, require);
			theAcceptedAttrs.put(attr, holder);
			theAttributesByName.put(attr.getName(), attr);
			RawAttributeValue strVal = theRawAttributes.remove(attr.getName());
			if(strVal != null) {
				try {
					holder.set(strVal.value, strVal.context);
				} catch(QuickException e) {
					theElement.msg().error("Could not parse pre-set value \"" + strVal + "\" of attribute " + attr.getName(), e,
						"attribute", attr);
				}
			}
		}
		if(initValue != null && holder.get() == null)
			holder.set(initValue);
		return holder;
	}

	/**
	 * Undoes acceptance of one or more attributes. This method does not remove any attribute value associated with this element. It merely
	 * disables the attribute. If the attribute is accepted on this element later, this element's value of that attribute will be preserved.
	 *
	 * @param wanter The object that used to care about the attribute
	 * @param attrs The attributes to not allow in this element
	 */
	public final void reject(Object wanter, QuickAttribute<?>... attrs) {
		for(QuickAttribute<?> attr : attrs) {
			AttributeHolder<?> holder = theAcceptedAttrs.get(attr);
			if(holder != null) {
				holder.reject(wanter);
				if(!holder.isWanted()) {
					theAcceptedAttrs.remove(attr);
					namedAttrRemoved(attr);
				}
			}
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
	public final boolean isAccepted(QuickAttribute<?> attr) {
		AttributeHolder<?> holder = theAcceptedAttrs.get(attr);
		return holder != null;
	}

	/**
	 * @param attr The attribute to check
	 * @return Whether the given attribute is required in this element
	 */
	public final boolean isRequired(QuickAttribute<?> attr) {
		AttributeHolder<?> holder = theAcceptedAttrs.get(attr);
		return holder != null && holder.isRequired();
	}

	/** @return An iterable to iterate through all accepted attributes in this manager */
	public Iterable<QuickAttribute<?>> attributes() {
		return () -> {
			return new Iterator<QuickAttribute<?>>() {
				private final Iterator<AttributeHolder<?>> theWrapped = holders().iterator();

				@Override
				public boolean hasNext() {
					return theWrapped.hasNext();
				}

				@Override
				public QuickAttribute<?> next() {
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
			theElement.msg().error("No attribute named " + attr.getKey() + " is accepted in this element", "value",
				attr.getValue().value);
		theRawAttributes = null;
	}

	private void namedAttrRemoved(QuickAttribute<?> attr) {
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
		for(QuickAttribute<?> att : theAcceptedAttrs.keySet())
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
			String value = "\"" + org.jdom2.output.Format.escapeAttribute(strategy,
				((QuickPropertyType<Object>) holder.getAttribute().getType()).toString(holder.get())) + "\"";
			ret.append(value);
		}
		return ret.toString();
	}
}
