package org.muis.util;

import java.util.ArrayList;

import org.muis.core.MuisAttribute;
import org.muis.core.MuisElement;
import org.muis.core.MuisException;
import org.muis.core.event.AttributeChangedListener;
import org.muis.core.event.MuisEvent;
import org.muis.core.event.MuisEventListener;

/**
 * A convenient utility that uses invocation chaining to allow code to accept/require attributes on an element and its children and perform
 * actions when they change in a very easy way
 *
 * @param <T> The type of the listener
 */
public abstract class CompoundListener<T>
{
	public static interface ChangeListener
	{
		void changed(MuisElement element);
	}

	public static final ChangeListener layout = new ChangeListener() {
		@Override
		public void changed(MuisElement element)
		{
			element.relayout(false);
		}
	};

	/**
	 * @param attr The attribute to accept in the element(s) that this listener applies to
	 * @return The listener for chaining
	 */
	public abstract <A> CompoundListener<A> accept(MuisAttribute<A> attr);

	/**
	 * @param attr The attribute to accept in the element(s) that this listener applies to
	 * @param value The initial value for the attribute (if it is not already set)
	 * @return The listener for chaining
	 * @throws IllegalArgumentException If {@link MuisElement#acceptAttribute(Object, MuisAttribute, Object)} throws a {@link MuisException}
	 */
	public abstract <A, V extends A> CompoundListener<A> accept(MuisAttribute<A> attr, V value) throws IllegalArgumentException;

	/**
	 * @param attr The attribute to require in the element(s) that this listener applies to
	 * @return The listener for chaining
	 */
	public abstract <A> CompoundListener<A> require(MuisAttribute<A> attr);

	/**
	 * @param attr The attribute to require in the element(s) that this listener applies to
	 * @param value The initial value for the attribute (if it is not already set)
	 * @return The listener for chaining
	 * @throws IllegalArgumentException If {@link MuisElement#acceptAttribute(Object, MuisAttribute, Object)} throws a {@link MuisException}
	 */
	public abstract <A, V extends A> CompoundListener<A> require(MuisAttribute<A> attr, V value) throws IllegalArgumentException;

	/**
	 * @param run The runnable to execute when any attribute in the current chain changes
	 * @return The listener for chaining
	 */
	public abstract CompoundListener<?> onChange(Runnable run);

	/**
	 * @param run The runnable to execute when any attribute in the current chain changes
	 * @return The listener for chaining
	 */
	public abstract CompoundListener<?> onChange(ChangeListener listener);

	/**
	 * @param listener The listener to execute when the last attribute in the current chain changes
	 * @return The listener for chaining
	 */
	public abstract CompoundListener<T> onChange(AttributeChangedListener<? super T> listener);

	/** @return Creates a {@link MultiElementCompoundListener} for creating compound listeners on multiple elements */
	public static MultiElementCompoundListener create()
	{
		return new MultiElementCompoundListener();
	}

	/**
	 * @param element The element to create the compound listener for
	 * @return The compound listener for the given element
	 */
	public static CompoundElementListener create(MuisElement element)
	{
		return new CompoundElementListener(element);
	}

	/** A compound listener for an element */
	public static class CompoundElementListener extends CompoundListener<Object>
	{
		private final MuisElement theElement;

		private ChildCompoundListener theChildListener;

		private ArrayList<ChainedCompoundListener<?>> theChains;

		private boolean isDropped;

		CompoundElementListener(MuisElement element)
		{
			theElement = element;
			theChildListener = new ChildCompoundListener(this);
			theChains = new ArrayList<>();
		}

		/** @return The element that this listener is for */
		public MuisElement getElement()
		{
			return theElement;
		}

		/**
		 * @return A compound listener that allows accepting/requiring attributes and listening to attribute changes on all this listener's
		 *         element's children
		 */
		public CompoundListener<?> child()
		{
			if(isDropped)
				throw new IllegalStateException("This listener is already dropped");
			theChildListener.setLastFinal();
			if(!theChains.isEmpty())
				theChains.get(theChains.size() - 1).setFinal();
			return theChildListener;
		}

		/** Releases all resources and requirements associated with this compound listener */
		public void drop()
		{
			if(isDropped)
				return;
			isDropped = true;
			theChildListener.drop();
			for(ChainedCompoundListener<?> chain : theChains)
			{
				theElement.removeListener(chain);
				chain.drop();
			}
		}

		private CompoundListener<?> chain()
		{
			if(isDropped)
				throw new IllegalStateException("This listener is already dropped");
			theChildListener.setLastFinal();
			if(!theChains.isEmpty())
				theChains.get(theChains.size() - 1).setFinal();
			ChainedCompoundListener<?> ret = new ChainedCompoundListener<Object>(this);
			theChains.add(ret);
			theElement.addListener(MuisElement.ATTRIBUTE_CHANGED, ret);
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

	private static class ChildCompoundListener extends CompoundListener<Object> implements MuisEventListener<Object>
	{
		private final CompoundElementListener theElListener;

		private MuisEventListener<MuisElement> theAddedListener;

		private MuisEventListener<MuisElement> theRemovedListener;

		private java.util.concurrent.CopyOnWriteArrayList<ChildChainedCompoundListener<?>> theChains;

		ChildCompoundListener(CompoundElementListener elListener)
		{
			theElListener = elListener;
			theChains = new java.util.concurrent.CopyOnWriteArrayList<>();
			theAddedListener = new MuisEventListener<MuisElement>() {
				@Override
				public void eventOccurred(MuisEvent<MuisElement> event, MuisElement element)
				{
					for(ChildChainedCompoundListener<?> chain : theChains)
						chain.childAdded(event.getValue());
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
					for(ChildChainedCompoundListener<?> chain : theChains)
						chain.childRemoved(event.getValue());
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

		private CompoundListener<?> chain()
		{
			if(!theChains.isEmpty())
				theChains.get(theChains.size() - 1).setFinal();
			ChildChainedCompoundListener<?> ret = new ChildChainedCompoundListener<Object>(this);
			theChains.add(ret);
			return ret;
		}

		void setLastFinal()
		{
			if(!theChains.isEmpty())
				theChains.get(theChains.size() - 1).setFinal();
		}

		void drop()
		{
			theElListener.getElement().removeListener(this);
			theElListener.getElement().removeListener(theAddedListener);
			theElListener.getElement().removeListener(theRemovedListener);
			for(ChildChainedCompoundListener<?> chain : theChains)
				chain.drop();
		}

		@Override
		public void eventOccurred(MuisEvent<Object> event, MuisElement element)
		{
			for(ChildChainedCompoundListener<?> chain : theChains)
				chain.eventOccurred(event, element);
		}

		@Override
		public boolean isLocal()
		{
			return true;
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
		public CompoundListener<Object> onChange(AttributeChangedListener<? super Object> listener)
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

	/**
	 * TODO Need a base class for all the chained compound listeners
	 */
	private static class ChainedCompoundListener<T> extends CompoundListener<T> implements org.muis.core.event.MuisEventListener<Object>
	{
		private final CompoundElementListener theElListener;

		private java.util.concurrent.CopyOnWriteArraySet<MuisAttribute<?>> theChained;

		private java.util.concurrent.CopyOnWriteArrayList<ChangeListener> theListeners;

		private MuisAttribute<?> theLastAttr;

		private java.util.HashMap<MuisAttribute<?>, AttributeChangedListener<?>> theSpecificListeners;

		private boolean isFinal;

		private long theLastAddTime;

		private boolean isDropped;

		ChainedCompoundListener(CompoundElementListener elListener)
		{
			theElListener = elListener;
			theChained = new java.util.concurrent.CopyOnWriteArraySet<>();
			theListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
			theSpecificListeners = new java.util.HashMap<>();
			theLastAddTime = System.currentTimeMillis();
		}

		void setFinal()
		{
			isFinal = true;
		}

		void drop()
		{
			if(isDropped)
				return;
			isDropped = true;
			MuisAttribute<?> [] attrs = theChained.toArray(new MuisAttribute[0]);
			for(int a = attrs.length - 1; a >= 0; a--)
				theElListener.getElement().rejectAttribute(theElListener, attrs[a]);
		}

		@Override
		public void eventOccurred(MuisEvent<Object> event, MuisElement element)
		{
			org.muis.core.event.AttributeChangedEvent<?> ace = (org.muis.core.event.AttributeChangedEvent<?>) event;
			if(theChained.contains(ace.getAttribute()))
			{
				for(ChangeListener listener : theListeners)
					listener.changed(theElListener.getElement());
			}
			AttributeChangedListener<Object> listener;
			if(isFinal)
				listener = (AttributeChangedListener<Object>) theSpecificListeners.get(ace.getAttribute());
			else if(System.currentTimeMillis() - theLastAddTime > 3000)
			{
				isFinal = true;
				listener = (AttributeChangedListener<Object>) theSpecificListeners.get(ace.getAttribute());
			}
			else
			{
				synchronized(theSpecificListeners)
				{
					listener = (AttributeChangedListener<Object>) theSpecificListeners.get(ace.getAttribute());
				}
			}
			if(listener != null)
				listener.attributeChanged((org.muis.core.event.AttributeChangedEvent<Object>) ace);
		}

		@Override
		public boolean isLocal()
		{
			return true;
		}

		@Override
		public <A> CompoundListener<A> accept(MuisAttribute<A> attr)
		{
			theElListener.getElement().acceptAttribute(theElListener, attr);
			theChained.add(attr);
			theLastAttr = attr;
			return (CompoundListener<A>) this;
		}

		@Override
		public <A, V extends A> CompoundListener<A> accept(MuisAttribute<A> attr, V value)
		{
			try
			{
				theElListener.getElement().acceptAttribute(theElListener, attr, value);
			} catch(MuisException e)
			{
				throw new IllegalArgumentException(e.getMessage(), e);
			}
			theChained.add(attr);
			theLastAttr = attr;
			return (CompoundListener<A>) this;
		}

		@Override
		public <A> CompoundListener<A> require(MuisAttribute<A> attr)
		{
			theElListener.getElement().requireAttribute(theElListener, attr);
			theChained.add(attr);
			theLastAttr = attr;
			return (CompoundListener<A>) this;
		}

		@Override
		public <A, V extends A> CompoundListener<A> require(MuisAttribute<A> attr, V value)
		{
			try
			{
				theElListener.getElement().requireAttribute(theElListener, attr, value);
			} catch(MuisException e)
			{
				throw new IllegalArgumentException(e.getMessage(), e);
			}
			theChained.add(attr);
			theLastAttr = attr;
			return (CompoundListener<A>) this;
		}

		@Override
		public CompoundListener<?> onChange(Runnable run)
		{
			theLastAttr = null;
			theListeners.add(new RunnableChangeListener(run));
			return theElListener;
		}

		@Override
		public CompoundListener<?> onChange(ChangeListener listener)
		{
			theLastAttr = null;
			theListeners.add(listener);
			return theElListener;
		}

		@Override
		public CompoundListener<T> onChange(AttributeChangedListener<? super T> listener)
		{
			if(theLastAttr == null)
				throw new IllegalStateException("No attribute to listen to");
			synchronized(theSpecificListeners)
			{
				theSpecificListeners.put(theLastAttr, listener);
			}
			return this;
		}
	}

	private static class AttributeHolder
	{
		final MuisAttribute<?> theAttr;

		boolean isRequired;

		Object theInitValue;

		AttributeHolder(MuisAttribute<?> attr)
		{
			theAttr = attr;
		}
	}

	private static class ChildChainedCompoundListener<T> extends CompoundListener<T> implements
		org.muis.core.event.MuisEventListener<Object>
	{
		private final ChildCompoundListener theChildListener;

		private java.util.HashMap<MuisAttribute<?>, AttributeHolder> theChained;

		private java.util.concurrent.CopyOnWriteArrayList<ChangeListener> theListeners;

		private MuisAttribute<?> theLastAttr;

		private java.util.HashMap<MuisAttribute<?>, AttributeChangedListener<?>> theSpecificListeners;

		private MuisElement [] theChildren;

		private boolean isFinal;

		private long theLastAddTime;

		private boolean isDropped;

		ChildChainedCompoundListener(ChildCompoundListener childListener)
		{
			theChildListener = childListener;
			theChained = new java.util.HashMap<>();
			theListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
			theSpecificListeners = new java.util.HashMap<>();
			theChildren = childListener.getElementListener().getElement().getChildren();
			theLastAddTime = System.currentTimeMillis();
		}

		void setFinal()
		{
			isFinal = true;
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
			for(int a = attrs.length - 1; a >= 0; a--)
				for(MuisElement child : theChildren)
					child.rejectAttribute(theChildListener.getElementListener(), attrs[a]);
		}

		void childAdded(MuisElement child)
		{
			theChildren = prisms.util.ArrayUtils.add(theChildren, child);
			AttributeHolder [] holders;
			if(isFinal)
				holders = theChained.values().toArray(new AttributeHolder[theChained.size()]);
			else
			{
				synchronized(theChained)
				{
					holders = theChained.values().toArray(new AttributeHolder[theChained.size()]);
				}
			}
			for(AttributeHolder holder : holders)
				try
				{
					child.acceptAttribute(theChildListener.getElementListener(), holder.isRequired, (MuisAttribute<Object>) holder.theAttr,
						holder.theInitValue);
				} catch(MuisException e)
				{
					child.error("Bad initial value!", e, "attribute", holder.theAttr, "value", holder.theInitValue);
				}
		}

		void childRemoved(MuisElement child)
		{
			theChildren = prisms.util.ArrayUtils.remove(theChildren, child);
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
			for(int a = attrs.length - 1; a >= 0; a--)
				child.rejectAttribute(theChildListener.getElementListener(), attrs[a]);
		}

		@Override
		public void eventOccurred(MuisEvent<Object> event, MuisElement element)
		{
			org.muis.core.event.AttributeChangedEvent<?> ace = (org.muis.core.event.AttributeChangedEvent<?>) event;
			AttributeChangedListener<Object> listener;
			boolean contains;
			if(isFinal)
			{
				contains = theChained.containsKey(ace.getAttribute());
				listener = (AttributeChangedListener<Object>) theSpecificListeners.get(ace.getAttribute());
			}
			else if(System.currentTimeMillis() - theLastAddTime > 3000)
			{
				isFinal = true;
				contains = theChained.containsKey(ace.getAttribute());
				listener = (AttributeChangedListener<Object>) theSpecificListeners.get(ace.getAttribute());
			}
			else
			{
				synchronized(theChained)
				{
					contains = theChained.containsKey(ace.getAttribute());
				}
				synchronized(theSpecificListeners)
				{
					listener = (AttributeChangedListener<Object>) theSpecificListeners.get(ace.getAttribute());
				}
			}
			if(contains)
			{
				for(ChangeListener run : theListeners)
					run.changed(theChildListener.getElementListener().getElement());
			}
			if(listener != null)
				listener.attributeChanged((org.muis.core.event.AttributeChangedEvent<Object>) ace);
		}

		@Override
		public boolean isLocal()
		{
			return true;
		}

		private void apply(AttributeHolder holder) throws IllegalArgumentException
		{
			for(MuisElement child : theChildren)
				try
				{
					child.acceptAttribute(theChildListener.getElementListener(), holder.isRequired, (MuisAttribute<Object>) holder.theAttr,
						holder.theInitValue);
				} catch(MuisException e)
				{
					throw new IllegalArgumentException(e.getMessage(), e);
				}
		}

		@Override
		public <A> CompoundListener<A> accept(MuisAttribute<A> attr)
		{
			AttributeHolder holder = new AttributeHolder(attr);
			theChained.put(attr, holder);
			apply(holder);
			theLastAttr = attr;
			return (CompoundListener<A>) this;
		}

		@Override
		public <A, V extends A> CompoundListener<A> accept(MuisAttribute<A> attr, V value)
		{
			AttributeHolder holder = new AttributeHolder(attr);
			holder.theInitValue = value;
			theChained.put(attr, holder);
			apply(holder);
			theLastAttr = attr;
			return (CompoundListener<A>) this;
		}

		@Override
		public <A> CompoundListener<A> require(MuisAttribute<A> attr)
		{
			AttributeHolder holder = new AttributeHolder(attr);
			holder.isRequired = true;
			theChained.put(attr, holder);
			apply(holder);
			theLastAttr = attr;
			return (CompoundListener<A>) this;
		}

		@Override
		public <A, V extends A> CompoundListener<A> require(MuisAttribute<A> attr, V value)
		{
			AttributeHolder holder = new AttributeHolder(attr);
			holder.isRequired = true;
			holder.theInitValue = value;
			theChained.put(attr, holder);
			apply(holder);
			theLastAttr = attr;
			return (CompoundListener<A>) this;
		}

		@Override
		public CompoundListener<?> onChange(Runnable run)
		{
			theLastAttr = null;
			theListeners.add(new RunnableChangeListener(run));
			return theChildListener;
		}

		@Override
		public CompoundListener<?> onChange(ChangeListener listener)
		{
			theLastAttr = null;
			theListeners.add(listener);
			return theChildListener;
		}

		@Override
		public CompoundListener<T> onChange(AttributeChangedListener<? super T> listener)
		{
			if(theLastAttr == null)
				throw new IllegalStateException("No attribute to listen to");
			synchronized(theSpecificListeners)
			{
				theSpecificListeners.put(theLastAttr, listener);
			}
			return this;
		}
	}

	/**
	 * Controls compound listeners for multiple elements at once
	 *
	 * TODO Make this class a compound listener that can only be adjusted before the listenerFor(MuisElement) method is called such that the
	 * listeners returned from listenFor are initialized to this listener's settings
	 */
	public static class MultiElementCompoundListener extends CompoundListener<Object>
	{
		private java.util.HashMap<MuisElement, CompoundElementListener> theListeners;

		MultiElementCompoundListener()
		{
			theListeners = new java.util.HashMap<>();
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
					ret = new CompoundElementListener(element);
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
}
