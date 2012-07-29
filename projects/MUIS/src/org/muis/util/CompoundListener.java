package org.muis.util;

import java.util.ArrayList;

import org.muis.core.MuisAttribute;
import org.muis.core.MuisElement;
import org.muis.core.MuisException;
import org.muis.core.event.AttributeChangedListener;
import org.muis.core.event.MuisEvent;
import org.muis.core.event.MuisEventListener;

/**
 * <p>
 * A convenient utility that uses invocation chaining to allow code to accept/require attributes on an element and its children and perform
 * actions when they change in a very easy way.
 * </p>
 * 
 * <p>
 * As an example, take the SimpleListener layout in org.muis.base.layout. This layout can accept one attribute, max-inf, from the parent
 * container and several dimension parameters (left, right, height, width, etc.) from each of the children to be layed out. The layout class
 * could accept all the attributes in the {@link org.muis.core.MuisLayout#initChildren(MuisElement, MuisElement[])} method as well as in the
 * childAdded method, making sure it cleaned itself up properly in the childRemoved and remove methods. Instead, a
 * {@link MultiElementCompoundListener} is created in the constructor and initialized with the acceptable attributes once:
 * </p>
 * 
 * <p>
 * <code>
 * 		theListener = CompoundListener.create(this);<br>
 * 		theListener.accept(LayoutConstants.maxInf).onChange(CompoundListener.layout);<br>
 * 		theListener.child().acceptAll(left, right, top, bottom, width, minWidth, maxWidth, height, minHeight, maxHeight).onChange(CompoundListener.layout);
 * </code>
 * </p>
 * 
 * Then each parent element that the layout services is added to the listener in the initChildren method:
 * 
 * <p>
 * <code>
 * 		theListener.listenerFor(parent);
 * </code>
 * </p>
 * 
 * The listener ensures that the parent and its children all accept and require the correct attributes and that the correct actions are
 * taken when the attributes change, and much work is saved by the author of the layout.
 * 
 * @param <T> The type of the listener
 */
public abstract class CompoundListener<T>
{
	/** A change listener to be called when any attribute in a chain changes on an element or an element's children */
	public static interface ChangeListener
	{
		/**
		 * Called when any attribute in the chain this listener was given to changes in the element or, if the chain was on the child
		 * listener, the element's children
		 *
		 * @param element The element that the listener was for. This will be the element that the attribute changed on unless this listener
		 *            was given to the child listener, in which case it will be the parent of the element that the attribute changed on.
		 */
		void changed(MuisElement element);
	}

	/** A utility change listener to cause a layout action in the element or parent element */
	public static final ChangeListener layout = new ChangeListener() {
		@Override
		public void changed(MuisElement element)
		{
			element.relayout(false);
		}
	};

	/**
	 * @param <A> The type of the attribute to accept
	 * @param attr The attribute to accept in the element(s) that this listener applies to
	 * @return The listener for chaining
	 */
	public abstract <A> CompoundListener<A> accept(MuisAttribute<A> attr);

	/**
	 * @param <A> The type of the attribute to accept
	 * @param <V> The type of the initial value to set for the attribute
	 * @param attr The attribute to accept in the element(s) that this listener applies to
	 * @param value The initial value for the attribute (if it is not already set)
	 * @return The listener for chaining
	 * @throws IllegalArgumentException If {@link MuisElement#acceptAttribute(Object, MuisAttribute, Object)} throws a {@link MuisException}
	 */
	public abstract <A, V extends A> CompoundListener<A> accept(MuisAttribute<A> attr, V value) throws IllegalArgumentException;

	/**
	 * @param <A> The type of the attribute to accept
	 * @param attr The attribute to require in the element(s) that this listener applies to
	 * @return The listener for chaining
	 */
	public abstract <A> CompoundListener<A> require(MuisAttribute<A> attr);

	/**
	 * @param <A> The type of the attribute to accept
	 * @param <V> The type of the initial value to set for the attribute
	 * @param attr The attribute to require in the element(s) that this listener applies to
	 * @param value The initial value for the attribute (if it is not already set)
	 * @return The listener for chaining
	 * @throws IllegalArgumentException If {@link MuisElement#acceptAttribute(Object, MuisAttribute, Object)} throws a {@link MuisException}
	 */
	public abstract <A, V extends A> CompoundListener<A> require(MuisAttribute<A> attr, V value) throws IllegalArgumentException;

	/**
	 * @param <A> The type of the attribute to accept
	 * @param <V> The type of the initial value to set for the attribute
	 * @param attr The attribute to accept in the element(s) that this listener applies to
	 * @param required Whether the attribute should be required or just accepted
	 * @param value The initial value for the attribute (if it is not already set)
	 * @return The listener for chaining
	 * @throws IllegalArgumentException If {@link MuisElement#acceptAttribute(Object, MuisAttribute, Object)} throws a {@link MuisException}
	 */
	public abstract <A, V extends A> CompoundListener<A> accept(MuisAttribute<A> attr, boolean required, V value)
		throws IllegalArgumentException;

	/**
	 * A utility method for accepting multiple attributes at once that do not require specific listeners (
	 * {@link #onChange(AttributeChangedListener)} will throw an exception if called immediately after this).
	 *
	 * @param attrs The attributes to accept
	 * @return The listener for chaining
	 */
	public abstract CompoundListener<?> acceptAll(MuisAttribute<?>... attrs);

	/**
	 * A utility method for requiring multiple attributes at once that do not require specific listeners (
	 * {@link #onChange(AttributeChangedListener)} will throw an exception if called immediately after this).
	 *
	 * @param attrs The attributes to require
	 * @return The listener for chaining
	 */
	public abstract CompoundListener<?> requireAll(MuisAttribute<?>... attrs);

	/**
	 * @param run The runnable to execute when any attribute in the current chain changes
	 * @return The listener for chaining
	 */
	public abstract CompoundListener<?> onChange(Runnable run);

	/**
	 * @param listener The listener to call when any attribute in the current chain changes
	 * @return The listener for chaining
	 */
	public abstract CompoundListener<?> onChange(ChangeListener listener);

	/**
	 * @param listener The listener to execute when the last attribute in the current chain changes
	 * @return The listener for chaining
	 */
	public abstract CompoundListener<T> onChange(AttributeChangedListener<? super T> listener);

	/**
	 * @param wanter The object that cares about the attributes that will be listened for on the elements
	 * @return Creates a {@link MultiElementCompoundListener} for creating compound listeners on multiple elements
	 */
	public static MultiElementCompoundListener create(Object wanter)
	{
		return new MultiElementCompoundListener(wanter);
	}

	/**
	 * @param element The element to create the compound listener for
	 * @param wanter The object that cares about the attributes that will be listened for on the element
	 * @return The compound listener for the given element
	 */
	public static CompoundElementListener create(MuisElement element, Object wanter)
	{
		return new CompoundElementListener(element, wanter);
	}

	/** A compound listener for an element */
	public static class CompoundElementListener extends ChainableCompoundListener
	{
		private final MuisElement theElement;

		private final Object theWanter;

		private ChildCompoundListener theChildListener;

		CompoundElementListener(MuisElement element, Object wanter)
		{
			theElement = element;
			theWanter = wanter;
			theChildListener = new ChildCompoundListener(this);
		}

		/** @return The element that this listener is for */
		public MuisElement getElement()
		{
			return theElement;
		}

		/** @return The object that cares about the attributes listened for by this listener */
		public Object getWanter()
		{
			return theWanter;
		}

		@Override
		void setLastFinal()
		{
			super.setLastFinal();
			theChildListener.setLastFinal();
		}

		@Override
		ChainedCompoundListener<?> createChain()
		{
			ChainedCompoundListener<?> ret = new SelfChainedCompoundListener<Object>(this);
			theElement.addListener(MuisElement.ATTRIBUTE_CHANGED, ret);
			return ret;
		}

		/**
		 * @return A compound listener that allows accepting/requiring attributes and listening to attribute changes on all this listener's
		 *         element's children
		 */
		public CompoundListener<?> child()
		{
			setLastFinal();
			return theChildListener;
		}

		/** Releases all resources and requirements associated with this compound listener */
		@Override
		public void drop()
		{
			for(ChainedCompoundListener<?> chain : getChains())
				theElement.removeListener(chain);
			super.drop();
			theChildListener.drop();
		}
	}

	private static class ChildCompoundListener extends ChainableCompoundListener implements MuisEventListener<Object>
	{
		private final CompoundElementListener theElListener;

		private MuisEventListener<MuisElement> theAddedListener;

		private MuisEventListener<MuisElement> theRemovedListener;

		ChildCompoundListener(CompoundElementListener elListener)
		{
			theElListener = elListener;
			theAddedListener = new MuisEventListener<MuisElement>() {
				@Override
				public void eventOccurred(MuisEvent<MuisElement> event, MuisElement element)
				{
					for(ChainedCompoundListener<?> chain : getChains())
						((ChildChainedCompoundListener<?>) chain).childAdded(event.getValue());
				}

				@Override
				public boolean isLocal()
				{
					return true;
				}
			};
			theRemovedListener = new MuisEventListener<MuisElement>() {
				@Override
				public void eventOccurred(MuisEvent<MuisElement> event, MuisElement element)
				{
					for(ChainedCompoundListener<?> chain : getChains())
						((ChildChainedCompoundListener<?>) chain).childRemoved(event.getValue());
				}

				@Override
				public boolean isLocal()
				{
					return true;
				}
			};
			theElListener.getElement().addListener(MuisElement.CHILD_ADDED, theAddedListener);
			theElListener.getElement().addListener(MuisElement.CHILD_REMOVED, theRemovedListener);
			theElListener.getElement().addChildListener(MuisElement.ATTRIBUTE_CHANGED, this);
		}

		CompoundElementListener getElementListener()
		{
			return theElListener;
		}

		@Override
		ChainedCompoundListener<?> createChain()
		{
			return new ChildChainedCompoundListener<Object>(this);
		}

		@Override
		void drop()
		{
			theElListener.getElement().removeListener(this);
			theElListener.getElement().removeListener(theAddedListener);
			theElListener.getElement().removeListener(theRemovedListener);
			super.drop();
		}

		@Override
		public void eventOccurred(MuisEvent<Object> event, MuisElement element)
		{
			for(ChainedCompoundListener<?> chain : getChains())
				chain.eventOccurred(event, element);
		}

		@Override
		public boolean isLocal()
		{
			return true;
		}
	}

	private static abstract class ChainableCompoundListener extends CompoundListener<Object>
	{
		private ArrayList<ChainedCompoundListener<?>> theChains;

		private boolean isDropped;

		ChainableCompoundListener()
		{
			theChains = new ArrayList<>();
		}

		ChainedCompoundListener<?> [] getChains()
		{
			return theChains.toArray(new ChainedCompoundListener[theChains.size()]);
		}

		/** Releases all resources and requirements associated with this compound listener */
		void drop()
		{
			if(isDropped)
				return;
			isDropped = true;
			for(ChainedCompoundListener<?> chain : theChains)
				chain.drop();
		}

		void setLastFinal()
		{
			if(!theChains.isEmpty())
				theChains.get(theChains.size() - 1).setFinal();
		}

		abstract ChainedCompoundListener<?> createChain();

		private CompoundListener<?> chain()
		{
			if(isDropped)
				throw new IllegalStateException("This listener is already dropped");
			setLastFinal();
			ChainedCompoundListener<?> ret = createChain();
			theChains.add(ret);
			return ret;
		}

		@Override
		public <A> CompoundListener<A> accept(MuisAttribute<A> attr)
		{
			return chain().accept(attr);
		}

		@Override
		public <A, V extends A> CompoundListener<A> accept(MuisAttribute<A> attr, V value) throws IllegalArgumentException
		{
			return chain().accept(attr, value);
		}

		@Override
		public <A> CompoundListener<A> require(MuisAttribute<A> attr)
		{
			return chain().require(attr);
		}

		@Override
		public <A, V extends A> CompoundListener<A> require(MuisAttribute<A> attr, V value) throws IllegalArgumentException
		{
			return chain().require(attr, value);
		}

		@Override
		public <A, V extends A> CompoundListener<A> accept(MuisAttribute<A> attr, boolean required, V value)
			throws IllegalArgumentException
		{
			return chain().accept(attr, required, value);
		}

		@Override
		public CompoundListener<?> acceptAll(MuisAttribute<?>... attrs)
		{
			return chain().acceptAll(attrs);
		}

		@Override
		public CompoundListener<?> requireAll(MuisAttribute<?>... attrs)
		{
			return chain().requireAll(attrs);
		}

		@Override
		public CompoundListener<?> onChange(Runnable run)
		{
			if(theChains.isEmpty())
				throw new IllegalStateException("No attributes to listen to");
			return theChains.get(theChains.size() - 1).onChange(run);
		}

		@Override
		public CompoundListener<?> onChange(ChangeListener listener)
		{
			if(theChains.isEmpty())
				throw new IllegalStateException("No attributes to listen to");
			return theChains.get(theChains.size() - 1).onChange(listener);
		}

		@Override
		public CompoundListener<Object> onChange(AttributeChangedListener<Object> listener)
		{
			throw new IllegalStateException("No attributes to listen to");
		}
	}

	private static class RunnableChangeListener implements ChangeListener
	{
		private final Runnable theRun;

		RunnableChangeListener(Runnable run)
		{
			theRun = run;
		}

		@Override
		public void changed(MuisElement element)
		{
			theRun.run();
		}
	}

	static class AttributeHolder
	{
		final MuisAttribute<?> attr;

		boolean required;

		Object initValue;

		AttributeHolder(MuisAttribute<?> att)
		{
			attr = att;
		}
	}

	private static abstract class ChainedCompoundListener<T> extends CompoundListener<T> implements
		org.muis.core.event.MuisEventListener<Object>
	{
		private java.util.Map<MuisAttribute<?>, AttributeHolder> theChained;

		private java.util.concurrent.CopyOnWriteArrayList<ChangeListener> theListeners;

		private MuisAttribute<?> theLastAttr;

		private java.util.HashMap<MuisAttribute<?>, AttributeChangedListener<?> []> theSpecificListeners;

		private boolean isFinal;

		private volatile long theLastAddTime;

		private boolean isDropped;

		ChainedCompoundListener()
		{
			theChained = new java.util.LinkedHashMap<>();
			theListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
			theSpecificListeners = new java.util.HashMap<>();
			theLastAddTime = System.currentTimeMillis();
		}

		void setFinal()
		{
			isFinal = true;
			theLastAttr = null;
			theLastAddTime = 0;
		}

		void drop()
		{
			if(isDropped)
				return;
			isDropped = true;
			MuisAttribute<?> [] attrs;
			if(isFinal)
				attrs = theChained.keySet().toArray(new MuisAttribute[theChained.size()]);
			else
			{
				synchronized(theChained)
				{
					attrs = theChained.keySet().toArray(new MuisAttribute[theChained.size()]);
				}
			}
			for(MuisAttribute<?> attr : attrs)
				doReject(attr);
		}

		abstract void doReject(MuisAttribute<?> attr);

		@Override
		public void eventOccurred(MuisEvent<Object> event, MuisElement element)
		{
			org.muis.core.event.AttributeChangedEvent<?> ace = (org.muis.core.event.AttributeChangedEvent<?>) event;
			AttributeChangedListener<Object> [] listeners;
			boolean contains;
			if(isFinal)
			{
				contains = theChained.containsKey(ace.getAttribute());
				listeners = (AttributeChangedListener<Object> []) theSpecificListeners.get(ace.getAttribute());
			}
			else if(System.currentTimeMillis() - theLastAddTime > 3000)
			{
				isFinal = true;
				contains = theChained.containsKey(ace.getAttribute());
				listeners = (AttributeChangedListener<Object> []) theSpecificListeners.get(ace.getAttribute());
			}
			else
			{
				synchronized(theChained)
				{
					contains = theChained.containsKey(ace.getAttribute());
				}
				synchronized(theSpecificListeners)
				{
					listeners = (AttributeChangedListener<Object> []) theSpecificListeners.get(ace.getAttribute());
				}
			}
			if(contains)
			{
				for(ChangeListener run : theListeners)
					run.changed(getElement());
			}
			if(listeners != null)
				for(AttributeChangedListener<Object> listener : listeners)
					listener.attributeChanged((org.muis.core.event.AttributeChangedEvent<Object>) ace);
		}

		@Override
		public boolean isLocal()
		{
			return true;
		}

		AttributeHolder [] getAllAttrProps()
		{
			if(isFinal)
				return theChained.values().toArray(new AttributeHolder[theChained.size()]);
			else
				synchronized(theChained)
				{
					return theChained.values().toArray(new AttributeHolder[theChained.size()]);
				}
		}

		ChangeListener [] getListeners()
		{
			return theListeners.toArray(new ChangeListener[theListeners.size()]);
		}

		<A> AttributeChangedListener<? super A> [] getSpecificListeners(MuisAttribute<A> attr)
		{
			if(isFinal)
				return (AttributeChangedListener<? super A> []) theSpecificListeners.get(attr);
			else
				synchronized(theSpecificListeners)
				{
					return (AttributeChangedListener<? super A> []) theSpecificListeners.get(attr);
				}
		}

		/** The parent element that this compound listener is for */
		abstract MuisElement getElement();

		/** This chained compound listener's parent */
		abstract CompoundListener<?> getParent();

		/** Accepts an attribute on the element(s) that this compound listener is for */
		abstract <A, V extends A> void doAccept(MuisAttribute<A> attr, boolean required, V value) throws MuisException;

		@Override
		public <A, V extends A> CompoundListener<A> accept(MuisAttribute<A> attr, boolean required, V value)
			throws IllegalArgumentException
		{
			if(isFinal)
				throw new IllegalStateException("Chains may only be modified when they are created");
			try
			{
				doAccept(attr, required, value);
			} catch(MuisException e)
			{
				throw new IllegalArgumentException(e.getMessage(), e);
			}
			AttributeHolder holder = new AttributeHolder(attr);
			holder.required = required;
			holder.initValue = value;
			synchronized(theChained)
			{
				theChained.put(attr, holder);
			}
			theLastAttr = attr;
			theLastAddTime = System.currentTimeMillis();
			return (CompoundListener<A>) this;
		}

		@Override
		public <A> CompoundListener<A> accept(MuisAttribute<A> attr)
		{
			return accept(attr, false, null);
		}

		@Override
		public <A, V extends A> CompoundListener<A> accept(MuisAttribute<A> attr, V value) throws IllegalArgumentException
		{
			return accept(attr, false, value);
		}

		@Override
		public <A> CompoundListener<A> require(MuisAttribute<A> attr)
		{
			return accept(attr, true, null);
		}

		@Override
		public <A, V extends A> CompoundListener<A> require(MuisAttribute<A> attr, V value) throws IllegalArgumentException
		{
			return accept(attr, true, value);
		}

		@Override
		public CompoundListener<?> acceptAll(MuisAttribute<?>... attrs)
		{
			if(isFinal)
				throw new IllegalStateException("Chains may only be modified when they are created");
			try
			{
				for(MuisAttribute<?> attr : attrs)
					doAccept(attr, false, null);
			} catch(MuisException e)
			{
				throw new IllegalArgumentException(e.getMessage(), e);
			}
			synchronized(theChained)
			{
				for(MuisAttribute<?> attr : attrs)
				{
					AttributeHolder holder = new AttributeHolder(attr);
					holder.required = false;
					theChained.put(attr, holder);
				}
			}
			theLastAttr = null;
			theLastAddTime = System.currentTimeMillis();
			return this;
		}

		@Override
		public CompoundListener<?> requireAll(MuisAttribute<?>... attrs)
		{
			if(isFinal)
				throw new IllegalStateException("Chains may only be modified when they are created");
			try
			{
				for(MuisAttribute<?> attr : attrs)
					doAccept(attr, true, null);
			} catch(MuisException e)
			{
				throw new IllegalArgumentException(e.getMessage(), e);
			}
			synchronized(theChained)
			{
				for(MuisAttribute<?> attr : attrs)
				{
					AttributeHolder holder = new AttributeHolder(attr);
					holder.required = true;
					theChained.put(attr, holder);
				}
			}
			theLastAttr = null;
			theLastAddTime = System.currentTimeMillis();
			return this;
		}

		@Override
		public CompoundListener<?> onChange(Runnable run)
		{
			if(isFinal)
				throw new IllegalStateException("Chains may only be modified when they are created");
			theLastAttr = null;
			theListeners.add(new RunnableChangeListener(run));
			theLastAddTime = System.currentTimeMillis();
			return getParent();
		}

		@Override
		public CompoundListener<?> onChange(ChangeListener listener)
		{
			if(isFinal)
				throw new IllegalStateException("Chains may only be modified when they are created");
			theLastAttr = null;
			theListeners.add(listener);
			theLastAddTime = System.currentTimeMillis();
			return getParent();
		}

		@Override
		public CompoundListener<T> onChange(AttributeChangedListener<? super T> listener)
		{
			if(isFinal)
				throw new IllegalStateException("Chains may only be modified when they are created");
			if(theLastAttr == null)
				throw new IllegalStateException("No attribute to listen to");
			synchronized(theSpecificListeners)
			{
				AttributeChangedListener<?> [] listeners = theSpecificListeners.get(theLastAttr);
				if(listeners == null)
					listeners = new AttributeChangedListener[] {listener};
				else
					listeners = prisms.util.ArrayUtils.add(listeners, listener);
				theSpecificListeners.put(theLastAttr, listeners);
			}
			theLastAddTime = System.currentTimeMillis();
			return this;
		}
	}

	private static class SelfChainedCompoundListener<T> extends ChainedCompoundListener<T>
	{
		private CompoundElementListener theParent;

		SelfChainedCompoundListener(CompoundElementListener elListener)
		{
			theParent = elListener;
		}

		@Override
		void doReject(MuisAttribute<?> attr)
		{
			theParent.getElement().rejectAttribute(theParent.getWanter(), attr);
		}

		@Override
		MuisElement getElement()
		{
			return theParent.getElement();
		}

		@Override
		CompoundListener<?> getParent()
		{
			return theParent;
		}

		@Override
		<A, V extends A> void doAccept(MuisAttribute<A> attr, boolean required, V value) throws MuisException
		{
			theParent.getElement().acceptAttribute(theParent.getWanter(), required, attr, value);
		}
	}

	private static class ChildChainedCompoundListener<T> extends ChainedCompoundListener<T>
	{
		private final ChildCompoundListener theChildListener;

		private MuisElement [] theChildren;

		ChildChainedCompoundListener(ChildCompoundListener childListener)
		{
			theChildListener = childListener;
			theChildren = childListener.getElementListener().getElement().getChildren();
		}

		void childAdded(MuisElement child)
		{
			theChildren = prisms.util.ArrayUtils.add(theChildren, child);
			for(AttributeHolder holder : getAllAttrProps())
				try
				{
					child.acceptAttribute(theChildListener.getElementListener().getWanter(), holder.required,
						(MuisAttribute<Object>) holder.attr, holder.initValue);
				} catch(MuisException e)
				{
					child.error("Bad initial value!", e, "attribute", holder.attr, "value", holder.initValue);
				}
		}

		void childRemoved(MuisElement child)
		{
			theChildren = prisms.util.ArrayUtils.remove(theChildren, child);
			for(AttributeHolder holder : getAllAttrProps())
				child.rejectAttribute(theChildListener.getElementListener().getWanter(), holder.attr);
		}

		@Override
		<A, V extends A> void doAccept(MuisAttribute<A> attr, boolean required, V initValue) throws IllegalArgumentException
		{
			for(MuisElement child : theChildren)
				try
				{
					child.acceptAttribute(theChildListener.getElementListener().getWanter(), required, (MuisAttribute<Object>) attr,
						initValue);
				} catch(MuisException e)
				{
					throw new IllegalArgumentException(e.getMessage(), e);
				}
		}

		@Override
		void doReject(MuisAttribute<?> attr)
		{
			for(MuisElement child : theChildren)
				child.rejectAttribute(theChildListener.getElementListener().getWanter(), attr);
		}

		@Override
		MuisElement getElement()
		{
			return theChildListener.getElementListener().getElement();
		}

		@Override
		CompoundListener<?> getParent()
		{
			return theChildListener;
		}
	}

	/**
	 * Controls compound listeners for multiple elements at once. This listener may be used to create a set of attributes needs and
	 * listeners before any elements are added (using {@link CompoundListener.MultiElementCompoundListener#listenerFor(MuisElement)}) so
	 * that any element listeners created by this listener will be initialized with those needs and listeners. The individual element
	 * listeners may be modified as needed. It is illegal to attempt to add attribute needs or listeners to this listener after elements are
	 * added.
	 */
	public static class MultiElementCompoundListener extends ChainableCompoundListener
	{
		private final Object theWanter;

		private java.util.HashMap<MuisElement, CompoundElementListener> theListeners;

		private MultiElementChildCompoundListener theChildListener;

		private boolean isFinal;

		MultiElementCompoundListener(Object wanter)
		{
			theWanter = wanter;
			theListeners = new java.util.HashMap<>();
			theChildListener = new MultiElementChildCompoundListener();
		}

		/** @return The listener to use to control attribute acceptance for all children that will use this listener */
		public CompoundListener<?> child()
		{
			if(isFinal)
				throw new IllegalStateException("MultiElementCompoundListeners may not be modified after elements are using it");
			return theChildListener;
		}

		@Override
		ChainedCompoundListener<?> createChain()
		{
			if(isFinal)
				throw new IllegalStateException("MultiElementCompoundListeners may not be modified after elements are using it");
			return new HoldingChainedCompoundListener<Object>(this);
		}

		/**
		 * @param element The element to get or create the compound listener for
		 * @return A compound listener for the given element containing this listener's settings initially
		 */
		public CompoundElementListener listenerFor(MuisElement element)
		{
			synchronized(theListeners)
			{
				CompoundElementListener ret = theListeners.get(element);
				if(ret == null)
				{
					ret = new CompoundElementListener(element, theWanter);
					CompoundListener<?> chainL = ret;
					for(ChainedCompoundListener<?> chain : getChains())
					{
						for(AttributeHolder attr : chain.getAllAttrProps())
						{
							chainL = chainL.accept((MuisAttribute<Object>) attr.attr, attr.required, attr.initValue);
							AttributeChangedListener<Object> [] listeners = chain.getSpecificListeners((MuisAttribute<Object>) attr.attr);
							if(listeners != null)
								for(AttributeChangedListener<Object> listener : listeners)
									chainL.onChange(listener);
						}
						for(ChangeListener listener : chain.getListeners())
							chainL.onChange(listener);
					}

					chainL = ret.child();
					for(ChainedCompoundListener<?> chain : theChildListener.getChains())
					{
						for(AttributeHolder attr : chain.getAllAttrProps())
						{
							chainL = chainL.accept((MuisAttribute<Object>) attr.attr, attr.required, attr.initValue);
							AttributeChangedListener<Object> [] listeners = chain.getSpecificListeners((MuisAttribute<Object>) attr.attr);
							if(listeners != null)
								for(AttributeChangedListener<Object> listener : listeners)
									chainL.onChange(listener);
						}
						for(ChangeListener listener : chain.getListeners())
							chainL.onChange(listener);
					}
					theListeners.put(element, ret);
				}
				return ret;
			}
		}

		/**
		 * Releases all resources and requirements of a compound listener for an element
		 *
		 * @param element The element to stop listening to
		 */
		public void dropFor(MuisElement element)
		{
			synchronized(theListeners)
			{
				CompoundElementListener ret = theListeners.remove(element);
				if(ret != null)
					ret.drop();
			}
		}
	}

	private static class MultiElementChildCompoundListener extends ChainableCompoundListener
	{
		@Override
		org.muis.util.CompoundListener.ChainedCompoundListener<?> createChain()
		{
			return new HoldingChainedCompoundListener<Object>(this);
		}
	}

	private static class HoldingChainedCompoundListener<T> extends ChainedCompoundListener<T>
	{
		private final CompoundListener<?> theParent;

		HoldingChainedCompoundListener(CompoundListener<?> parent)
		{
			theParent = parent;
		}

		@Override
		void doReject(MuisAttribute<?> attr)
		{
		}

		@Override
		MuisElement getElement()
		{
			return null;
		}

		@Override
		CompoundListener<?> getParent()
		{
			return theParent;
		}

		@Override
		<A, V extends A> void doAccept(MuisAttribute<A> attr, boolean required, V value) throws MuisException
		{
		}
	}
}
