package org.muis.util;

import java.util.ArrayList;
import java.util.HashMap;

import org.muis.core.MuisAttribute;
import org.muis.core.MuisElement;
import org.muis.core.MuisException;
import org.muis.core.event.AttributeChangedEvent;
import org.muis.core.event.ChildEvent;
import org.muis.core.event.MuisEventListener;
import org.muis.core.style.StyleAttribute;
import org.muis.core.style.StyleAttributeEvent;
import org.muis.core.style.StyleDomain;

/**
 * <p>
 * A convenient utility that uses invocation chaining to allow code to accept/require attributes on an element and its children and perform
 * actions when they change in a very easy way.
 * </p>
 *
 *
 * <p>
 * As an example, take the SimpleListener layout in org.muis.base.layout. This layout can accept one attribute, max-inf, from the parent
 * container and several dimension parameters (left, right, height, width, etc.) from each of the children to be layed out. The layout class
 * could accept all the attributes in the {@link org.muis.core.MuisLayout#initChildren(MuisElement, MuisElement[])} method as well as in the
 * childAdded method, making sure it cleaned itself up properly in the childRemoved and remove methods. Instead, a
 * {@link MultiElementCompoundListener} is created in the constructor and initialized with the acceptable attributes once:
 * </p>
 *
 *
 * <p>
 *
 * <pre>
 * 	theListener = CompoundListener.create(this);<br>
 * 	theListener.accept(LayoutConstants.maxInf).onChange(CompoundListener.layout);<br>
 * 	theListener.child().acceptAll(left, right, top, bottom, width, minWidth, maxWidth, height, minHeight, maxHeight)
 * 		.onChange(CompoundListener.layout);
 * </pre>
 *
 * </p>
 *
 *
 * Then each parent element that the layout services is added to the listener in the initChildren method:
 *
 *
 * <p>
 * <code>
 * 		theListener.listenerFor(parent);
 * </code>
 * </p>
 *
 *
 *
 * <p>
 * The listener ensures that the parent and its children all accept and require the correct attributes and that the correct actions are
 * taken when the attributes change, and much work is saved by the author of the layout.
 *
 * </p>
 *
 * <p>
 * This functionality is sufficient for the vast majority of cases, but in some circumstances, individual children may need to accept
 * different attributes based on some custom condition. This utility supports this functionality also. Take {@link java.awt.BorderLayout}
 * for example. Every child of a parent whose layout is a border layout takes the region parameter to determine which region the child will
 * be in, but each region has a different set of sizing attributes that may apply. For instance, an element in the top region should not
 * specify a width, since the definition of its region says that the element should span the entire container's width. The layout could
 * simply accept all possible layout attributes and just ignore invalid ones, but we can do better using the
 * {@link IndividualElementListener} interface.
 * </p>
 *
 * <p>
 * Here is BorderLayout's usage of CompoundListener
 * </p>
 *
 * <pre>
 * theListener.child().accept(region).onChange(theListener.individualChecker(false)).onChange(CompoundListener.layout);
 * theListener.eachChild(new CompoundListener.IndividualElementListener() {
 * 	&#064;Override
 * 	public void individual(MuisElement element, CompoundElementListener listener) {
 * 		listener.chain(Region.left.name()).acceptAll(width, minWidth, maxWidth, right, minRight, maxRight)
 * 			.onChange(CompoundListener.layout);
 * 		listener.chain(Region.right.name()).acceptAll(width, minWidth, maxWidth, left, minLeft, maxLeft).onChange(CompoundListener.layout);
 * 		listener.chain(Region.top.name()).acceptAll(height, minHeight, maxHeight, bottom, minBottom, maxBottom)
 * 			.onChange(CompoundListener.layout);
 * 		listener.chain(Region.bottom.name()).acceptAll(height, minHeight, maxHeight, top, minTop, maxTop).onChange(CompoundListener.layout);
 * 		update(element, listener);
 * 	}
 *
 * 	&#064;Override
 * 	public void update(MuisElement element, CompoundElementListener listener) {
 * 		listener.chain(Region.left.name()).setActive(element.getAttribute(region) == Region.left);
 * 		listener.chain(Region.right.name()).setActive(element.getAttribute(region) == Region.right);
 * 		listener.chain(Region.top.name()).setActive(element.getAttribute(region) == Region.top);
 * 		listener.chain(Region.bottom.name()).setActive(element.getAttribute(region) == Region.bottom);
 * 	}
 * });
 * </pre>
 *
 * <p>
 * First the border layout tells the compound listener that every child should accept the region attribute, an if it is changed, each child
 * needs to be individually evaluated by the listener and a layout operation will be performed on the parent. Then the layout adds an
 * individual listener to alter attributes for each child. This individual listener creates 4 named chains, one for each border region and
 * calls the update method, which is also called by the individualChecker call above whenever the region changes. The update method makes
 * exactly one of the chains active at a time, depending on which region is assigned to the child. This keeps the attribute state
 * consistent.
 * </p>
 *
 * <p>
 * Style attributes may also be monitored and controlled using this class. See the {@link #watch(StyleAttribute)},
 * {@link #watchAll(StyleAttribute...)}, {@link #watchAll(StyleDomain)} and {@link #onStyleChange(MuisEventListener)} methods.
 * </p>
 *
 * @param <T> The type of the listener
 */
public abstract class CompoundListener<T> {
	/** A change listener to be called when any attribute in a chain changes on an element or an element's children */
	public static interface ChangeListener {
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
	public static final ChangeListener layout = element -> {
		element.relayout(false);
	};

	/** A utility change listener to fire a {@link org.muis.core.event.SizeNeedsChangedEvent} on the element or parent element */
	public static final ChangeListener sizeNeedsChanged = element -> {
		element.events().fire(new org.muis.core.event.SizeNeedsChangedEvent(element, null));
	};

	/**
	 * When passed to {@link CompoundListener.CompoundElementListener#eachChild(IndividualElementListener)}, this listener allows code to
	 * deal with each child of an element individually, potentially accepting different attribute sets for each child.
	 */
	public static interface IndividualElementListener {
		/**
		 * Called for each child of an element and when each new child is added to the element
		 *
		 * @param element
		 * @param listener
		 */
		void individual(MuisElement element, CompoundElementListener listener);

		/**
		 * Called when a {@link CompoundListener.CompoundElementListener#individualChecker(boolean)} listener fires
		 *
		 * @param element The element to check and make sure its state is consistent with this listener's goal
		 * @param listener The element listener for the element
		 */
		void update(MuisElement element, CompoundElementListener listener);
	}

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
	 * @throws IllegalArgumentException If {@link org.muis.core.mgr.AttributeManager#accept(Object, MuisAttribute, Object)} throws a
	 *             {@link MuisException}
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
	 * @throws IllegalArgumentException If {@link org.muis.core.mgr.AttributeManager#accept(Object, MuisAttribute, Object)} throws a
	 *             {@link MuisException}
	 */
	public abstract <A, V extends A> CompoundListener<A> require(MuisAttribute<A> attr, V value) throws IllegalArgumentException;

	/**
	 * @param <A> The type of the attribute to accept
	 * @param <V> The type of the initial value to set for the attribute
	 * @param attr The attribute to accept in the element(s) that this listener applies to
	 * @param required Whether the attribute should be required or just accepted
	 * @param value The initial value for the attribute (if it is not already set)
	 * @return The listener for chaining
	 * @throws IllegalArgumentException If {@link org.muis.core.mgr.AttributeManager#accept(Object, MuisAttribute, Object)} throws a
	 *             {@link MuisException}
	 */
	public abstract <A, V extends A> CompoundListener<A> accept(MuisAttribute<A> attr, boolean required, V value)
		throws IllegalArgumentException;

	/**
	 * A utility method for accepting multiple attributes at once that do not require specific listeners (
	 * {@link #onAttChange(MuisEventListener)} will throw an exception if called immediately after this).
	 *
	 * @param attrs The attributes to accept
	 * @return The listener for chaining
	 */
	public abstract CompoundListener<?> acceptAll(MuisAttribute<?>... attrs);

	/**
	 * A utility method for requiring multiple attributes at once that do not require specific listeners (
	 * {@link #onAttChange(MuisEventListener)} will throw an exception if called immediately after this).
	 *
	 * @param attrs The attributes to require
	 * @return The listener for chaining
	 */
	public abstract CompoundListener<?> requireAll(MuisAttribute<?>... attrs);

	/**
	 * Watches a style attribute. When the attribute's value changes, any change listeners registered on this listener will fire.
	 *
	 * @param <A> The type of the attribute
	 * @param attr The style attribute to listen for
	 * @return The listener for chaining
	 */
	public abstract <A> CompoundListener<A> watch(StyleAttribute<A> attr);

	/**
	 * @param domain The style domain to watch
	 * @return The listener for chaining
	 */
	public abstract CompoundListener<?> watchAll(StyleDomain domain);

	/**
	 * @param attrs The style attributes to watch
	 * @return The listener for chaining
	 */
	public abstract CompoundListener<?> watchAll(StyleAttribute<?>... attrs);

	/**
	 * Activates or activates the effects of this listener. Effects are active by default.
	 *
	 * @param active Whether this listener's effects are active or inactive.
	 * @return The listener for chaining
	 */
	public abstract CompoundListener<?> setActive(boolean active);

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
	public abstract CompoundListener<T> onAttChange(MuisEventListener<AttributeChangedEvent<? super T>> listener);

	/**
	 * @param listener The listener to execute when the last style attribute in the current chain changes
	 * @return The listener for chaining
	 */
	public abstract CompoundListener<T> onStyleChange(MuisEventListener<StyleAttributeEvent<? super T>> listener);

	/**
	 * @param wanter The object that cares about the attributes that will be listened for on the elements
	 * @return Creates a {@link MultiElementCompoundListener} for creating compound listeners on multiple elements
	 */
	public static MultiElementCompoundListener create(Object wanter) {
		return new MultiElementCompoundListener(wanter);
	}

	/**
	 * @param element The element to create the compound listener for
	 * @param wanter The object that cares about the attributes that will be listened for on the element
	 * @return The compound listener for the given element
	 */
	public static CompoundElementListener create(MuisElement element, Object wanter) {
		return new CompoundElementListener(element, wanter);
	}

	/** A compound listener for an element */
	public static class CompoundElementListener extends ChainableCompoundListener {
		private final MuisElement theElement;

		private final Object theWanter;

		private ChildCompoundListener theChildListener;

		private final ChangeListener theAllIndividualsChecker;

		private final ChangeListener theIndividualChecker;

		CompoundElementListener(MuisElement element, Object wanter) {
			theElement = element;
			theWanter = wanter;
			theChildListener = new ChildCompoundListener(this);
			theAllIndividualsChecker = el -> {
				theChildListener.updateAllIndividuals();
			};
			theIndividualChecker = el -> {
				theChildListener.updateIndividual(el);
			};
		}

		/** @return The element that this listener is for */
		public MuisElement getElement() {
			return theElement;
		}

		/** @return The object that cares about the attributes listened for by this listener */
		public Object getWanter() {
			return theWanter;
		}

		/**
		 * @param all Whether the listener returned should call an
		 *            {@link CompoundListener.IndividualElementListener#update(MuisElement, org.muis.util.CompoundListener.CompoundElementListener)
		 *            update} on all this element's children or just the child that is passed to
		 *            {@link CompoundListener.ChangeListener#changed(MuisElement)}. If the element passed to <code>changed</code> is the
		 *            parent element, all children are checked and this parameter has no effect.
		 * @return The change listener to update individual children in this listener's element
		 */
		public ChangeListener individualChecker(boolean all) {
			return all ? theAllIndividualsChecker : theIndividualChecker;
		}

		/**
		 * @param name The name of the chain to get or create, or null to create a new anonymous chain
		 * @return The chain of the given name or a new anonymous chain. Will never be null.
		 */
		@Override
		public CompoundListener<?> chain(String name) {
			return super.chain(name);
		}

		@Override
		ChainedCompoundListener<?> createChain() {
			ChainedCompoundListener<?> ret = new SelfChainedCompoundListener<>(this);
			theElement.events().listen(AttributeChangedEvent.base, ret.attListener);
			theElement.getStyle().getSelf().addListener(ret.getStyleListener());
			return ret;
		}

		/**
		 * @return A compound listener that allows accepting/requiring attributes and listening to attribute changes on all this listener's
		 *         element's children
		 */
		public CompoundListener<?> child() {
			return theChildListener;
		}

		/** @param individual The listener to deal with each child of an element separately */
		public void eachChild(IndividualElementListener individual) {
			theChildListener.addIndividualListener(individual);
		}

		/** Releases all resources and requirements associated with this compound listener */
		@Override
		public void drop() {
			for(ChainedCompoundListener<?> chain : getAnonymousChains()) {
				theElement.events().remove(AttributeChangedEvent.base, chain.attListener);
				theElement.getStyle().getSelf().removeListener(chain.getStyleListener());
			}
			for(String name : getChainNames()) {
				ChainedCompoundListener<?> cl = (ChainedCompoundListener<?>) chain(name);
				theElement.events().remove(AttributeChangedEvent.base, cl.attListener);
				theElement.getStyle().getSelf().removeListener(cl.getStyleListener());
			}
			super.drop();
			theChildListener.drop();
		}
	}

	private static class ChildCompoundListener extends ChainableCompoundListener {
		private final CompoundElementListener theElListener;

		private MuisEventListener<ChildEvent> theAddedListener;

		private MuisEventListener<ChildEvent> theRemovedListener;

		private MuisEventListener<AttributeChangedEvent<?>> theAttListener;
		private MuisEventListener<StyleAttributeEvent<?>> theStyleListener;

		private ArrayList<IndividualElementListener> theIndividualListeners;

		private java.util.Map<MuisElement, CompoundElementListener> theElementListeners;

		ChildCompoundListener(CompoundElementListener elListener) {
			theElListener = elListener;
			theAddedListener = event -> {
				for(ChainedCompoundListener<?> chain : getAnonymousChains())
					((ChildChainedCompoundListener<?>) chain).childAdded(event.getChild());
				for(String name : getChainNames())
					((ChildChainedCompoundListener<?>) chain(name)).childAdded(event.getChild());
				synchronized(theElementListeners) {
					CompoundElementListener childListener = theElementListeners.get(event.getChild());
					if(childListener == null) {
						childListener = create(event.getChild(), theElListener.getWanter());
						theElementListeners.put(event.getChild(), childListener);
					}
					for(IndividualElementListener listener : theIndividualListeners)
						listener.individual(event.getChild(), childListener);
				}
			};
			theRemovedListener = event -> {
				for(ChainedCompoundListener<?> chain : getAnonymousChains())
					((ChildChainedCompoundListener<?>) chain).childRemoved(event.getChild());
				for(String name : getChainNames())
					((ChildChainedCompoundListener<?>) chain(name)).childRemoved(event.getChild());
				synchronized(theElementListeners) {
					CompoundElementListener childListener = theElementListeners.remove(event.getChild());
					if(childListener != null) {
						childListener.drop();
					}
				}
			};
			theAttListener = event -> {
				for(ChainedCompoundListener<?> chain : getAnonymousChains())
					chain.attListener.eventOccurred(event);
				for(String name : getChainNames())
					((ChildChainedCompoundListener<?>) chain(name)).attListener.eventOccurred(event);
			};
			theStyleListener = event -> {
				for(ChainedCompoundListener<?> chain : getAnonymousChains())
					chain.styleListener.eventOccurred(event);
				for(String name : getChainNames())
					((ChildChainedCompoundListener<?>) chain(name)).styleListener.eventOccurred(event);
			};
			theElListener.getElement().events().listen(ChildEvent.child.add(), theAddedListener);
			theElListener.getElement().events().listen(ChildEvent.child.remove(), theRemovedListener);
			theElListener.getElement().ch().events().listen(AttributeChangedEvent.base, theAttListener);
			theElListener.getElement().ch().events().listen(StyleAttributeEvent.base, theStyleListener);
			theIndividualListeners = new ArrayList<>();
			theElementListeners = new java.util.HashMap<>();
		}

		CompoundElementListener getElementListener() {
			return theElListener;
		}

		void addIndividualListener(IndividualElementListener listener) {
			if(listener != null)
				synchronized(theIndividualListeners) {
					theIndividualListeners.add(listener);
					for(MuisElement child : theElListener.getElement().getChildren()) {
						CompoundElementListener childListener = theElementListeners.get(child);
						if(childListener == null) {
							childListener = create(child, theElListener.getWanter());
							theElementListeners.put(child, childListener);
						}
						listener.individual(child, childListener);
					}
				}
		}

		void updateAllIndividuals() {
			synchronized(theIndividualListeners) {
				for(MuisElement child : theElListener.getElement().getChildren()) {
					CompoundElementListener childListener = theElementListeners.get(child);
					if(childListener != null)
						for(IndividualElementListener individualListener : theIndividualListeners)
							individualListener.update(child, childListener);
				}
			}
		}

		void updateIndividual(MuisElement child) {
			synchronized(theIndividualListeners) {
				CompoundElementListener childListener = theElementListeners.get(child);
				if(childListener != null)
					for(IndividualElementListener individualListener : theIndividualListeners)
						individualListener.update(child, childListener);
				else if(child == theElListener.getElement())
					updateAllIndividuals();
			}
		}

		@Override
		ChainedCompoundListener<?> createChain() {
			return new ChildChainedCompoundListener<>(this);
		}

		@Override
		void drop() {
			theElListener.getElement().events().remove(AttributeChangedEvent.base, theAttListener);
			theElListener.getElement().events().remove(StyleAttributeEvent.base, theStyleListener);
			theElListener.getElement().events().remove(ChildEvent.child.add(), theAddedListener);
			theElListener.getElement().events().remove(ChildEvent.child.remove(), theRemovedListener);
			for(CompoundElementListener childListener : theElementListeners.values())
				childListener.drop();
			theElementListeners.clear();
			super.drop();
		}
	}

	private static abstract class ChainableCompoundListener extends CompoundListener<Object> {
		private java.util.Map<String, ChainedCompoundListener<?>> theNamedChains;

		private ArrayList<ChainedCompoundListener<?>> theAnonymousChains;

		private ChainedCompoundListener<?> theLastChain;

		private boolean isDropped;

		ChainableCompoundListener() {
			theNamedChains = new java.util.HashMap<>();
			theAnonymousChains = new ArrayList<>();
		}

		String [] getChainNames() {
			synchronized(theAnonymousChains) {
				return theNamedChains.keySet().toArray(new String[theNamedChains.size()]);
			}
		}

		ChainedCompoundListener<?> [] getAnonymousChains() {
			synchronized(theAnonymousChains) {
				return theAnonymousChains.toArray(new ChainedCompoundListener[theAnonymousChains.size()]);
			}
		}

		/** Releases all resources and requirements associated with this compound listener */
		void drop() {
			if(isDropped)
				return;
			isDropped = true;
			for(ChainedCompoundListener<?> chain : theAnonymousChains)
				chain.drop();
			for(ChainedCompoundListener<?> chain : theNamedChains.values())
				chain.drop();
			theAnonymousChains.clear();
			theNamedChains.clear();
		}

		abstract ChainedCompoundListener<?> createChain();

		public CompoundListener<?> chain(String name) {
			if(isDropped)
				throw new IllegalStateException("This listener is already dropped");
			ChainedCompoundListener<?> ret;
			synchronized(theAnonymousChains) {
				if(name != null) {
					ret = theNamedChains.get(name);
					if(ret == null) {
						ret = createChain();
						theNamedChains.put(name, ret);
					}
				} else {
					ret = createChain();
					theAnonymousChains.add(ret);
				}
			}
			theLastChain = ret;
			return ret;
		}

		@Override
		public <A> CompoundListener<A> accept(MuisAttribute<A> attr) {
			return chain(null).accept(attr);
		}

		@Override
		public <A, V extends A> CompoundListener<A> accept(MuisAttribute<A> attr, V value) throws IllegalArgumentException {
			return chain(null).accept(attr, value);
		}

		@Override
		public <A> CompoundListener<A> require(MuisAttribute<A> attr) {
			return chain(null).require(attr);
		}

		@Override
		public <A, V extends A> CompoundListener<A> require(MuisAttribute<A> attr, V value) throws IllegalArgumentException {
			return chain(null).require(attr, value);
		}

		@Override
		public <A, V extends A> CompoundListener<A> accept(MuisAttribute<A> attr, boolean required, V value)
			throws IllegalArgumentException {
			return chain(null).accept(attr, required, value);
		}

		@Override
		public CompoundListener<?> acceptAll(MuisAttribute<?>... attrs) {
			return chain(null).acceptAll(attrs);
		}

		@Override
		public CompoundListener<?> requireAll(MuisAttribute<?>... attrs) {
			return chain(null).requireAll(attrs);
		}

		@Override
		public <A> CompoundListener<A> watch(StyleAttribute<A> attr) {
			return chain(null).watch(attr);
		}

		@Override
		public CompoundListener<?> watchAll(StyleDomain domain) {
			return chain(null).watchAll(domain);
		}

		@Override
		public CompoundListener<?> watchAll(StyleAttribute<?>... attrs) {
			return chain(null).watchAll(attrs);
		}

		@Override
		public CompoundListener<?> setActive(boolean active) {
			ChainedCompoundListener<?> [] chains = getAnonymousChains();
			for(ChainedCompoundListener<?> chain : chains) {
				chain.setActive(active);
			}
			for(String name : getChainNames())
				chain(name).setActive(active);
			return this;
		}

		@Override
		public CompoundListener<?> onChange(Runnable run) {
			synchronized(theAnonymousChains) {
				if(theLastChain == null)
					return null;
				return theLastChain.onChange(run);
			}
		}

		@Override
		public CompoundListener<?> onChange(ChangeListener listener) {
			synchronized(theAnonymousChains) {
				if(theLastChain == null)
					return null;
				return theLastChain.onChange(listener);
			}
		}

		/**
		 * @param listener The listener to execute when the last attribute in the current chain changes
		 * @return The listener for chaining
		 */
		@Override
		public CompoundListener<Object> onAttChange(MuisEventListener<AttributeChangedEvent<? super Object>> listener) {
			throw new IllegalStateException("No attributes to listen to");
		}

		/**
		 * @param listener The listener to execute when the last style attribute in the current chain changes
		 * @return The listener for chaining
		 */
		@Override
		public CompoundListener<Object> onStyleChange(MuisEventListener<StyleAttributeEvent<? super Object>> listener) {
			throw new IllegalStateException("No style attributes to listen to");
		}
	}

	private static class RunnableChangeListener implements ChangeListener {
		private final Runnable theRun;

		RunnableChangeListener(Runnable run) {
			theRun = run;
		}

		@Override
		public void changed(MuisElement element) {
			theRun.run();
		}
	}

	static class AttributeHolder {
		final MuisAttribute<?> attr;

		boolean required;

		Object initValue;

		AttributeHolder(MuisAttribute<?> att) {
			attr = att;
		}
	}

	private static abstract class ChainedCompoundListener<T> extends CompoundListener<T> {
		private java.util.Map<MuisAttribute<?>, AttributeHolder> theChained;

		private java.util.concurrent.CopyOnWriteArrayList<ChangeListener> theListeners;

		private MuisAttribute<?> theLastAttr;

		private HashMap<MuisAttribute<?>, MuisEventListener<AttributeChangedEvent<?>> []> theSpecificListeners;

		private java.util.Set<StyleAttribute<?>> theStyleAttributes;

		private java.util.Set<StyleDomain> theStyleDomains;

		private HashMap<StyleAttribute<?>, MuisEventListener<StyleAttributeEvent<?>> []> theSpecificStyleListeners;

		private StyleAttribute<?> theLastStyleAttr;

		private boolean isActive;

		private boolean isDropped;

		MuisEventListener<AttributeChangedEvent<?>> attListener;
		MuisEventListener<StyleAttributeEvent<?>> styleListener;

		ChainedCompoundListener() {
			theChained = new java.util.LinkedHashMap<>();
			theListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
			theSpecificListeners = new HashMap<>();
			theSpecificStyleListeners = new HashMap<>();
			theStyleAttributes = new java.util.HashSet<>();
			theStyleDomains = new java.util.HashSet<>();
			isActive = true;

			attListener = event -> {
				MuisEventListener<AttributeChangedEvent<?>> [] listeners;
				boolean contains;
				synchronized(theChained) {
					contains = theChained.containsKey(event.getAttribute());
				}
				synchronized(theSpecificListeners) {
					listeners = theSpecificListeners.get(event.getAttribute());
				}
				if(contains) {
					for(ChangeListener run : theListeners)
						run.changed(getElement());
				}
				if(listeners != null)
					for(MuisEventListener<AttributeChangedEvent<?>> listener : listeners)
						listener.eventOccurred(event);
			};
			styleListener = event -> {
				boolean contains;
				MuisEventListener<StyleAttributeEvent<?>> [] listeners;
				synchronized(theStyleDomains) {
					contains = theStyleDomains.contains(event.getAttribute().getDomain());
				}
				if(!contains)
					synchronized(theStyleAttributes) {
						contains = theStyleAttributes.contains(event.getAttribute());
					}
				synchronized(theSpecificStyleListeners) {
					listeners = theSpecificStyleListeners.get(event.getAttribute());
				}
				if(contains) {
					for(ChangeListener run : theListeners)
						run.changed(getElement());
				}
				if(listeners != null)
					for(MuisEventListener<StyleAttributeEvent<?>> listener : listeners)
						listener.eventOccurred(event);
			};
		}

		void drop() {
			if(isDropped)
				return;
			isDropped = true;
			MuisAttribute<?> [] attrs;
			synchronized(theChained) {
				attrs = theChained.keySet().toArray(new MuisAttribute[theChained.size()]);
			}
			for(MuisAttribute<?> attr : attrs)
				doReject(attr);
		}

		org.muis.core.style.StyleListener getStyleListener() {
			return event -> {
				MuisElement element;
				if(event.getLocalStyle() instanceof org.muis.core.style.attach.ElementStyle)
					element = ((org.muis.core.style.attach.ElementStyle) event.getLocalStyle()).getElement();
				else if(event.getLocalStyle() instanceof org.muis.core.style.attach.ElementSelfStyle)
					element = ((org.muis.core.style.attach.ElementSelfStyle) event.getLocalStyle()).getElementStyle().getElement();
				else if(event.getLocalStyle() instanceof org.muis.core.style.attach.ElementHeirStyle)
					element = ((org.muis.core.style.attach.ElementHeirStyle) event.getLocalStyle()).getElementStyle().getElement();
				else
					element = null;
				if(element != null)
					((MuisEventListener<StyleAttributeEvent<Object>>) (MuisEventListener<?>) styleListener)
						.eventOccurred((StyleAttributeEvent<Object>) event);
			};
		}

		abstract void doReject(MuisAttribute<?> attr);

		AttributeHolder [] getAllAttrProps() {
			synchronized(theChained) {
				return theChained.values().toArray(new AttributeHolder[theChained.size()]);
			}
		}

		ChangeListener [] getListeners() {
			return theListeners.toArray(new ChangeListener[theListeners.size()]);
		}

		<A> MuisEventListener<AttributeChangedEvent<? super A>> [] getSpecificListeners(MuisAttribute<A> attr) {
			synchronized(theSpecificListeners) {
				return (MuisEventListener<AttributeChangedEvent<? super A>> []) (Object []) theSpecificListeners.get(attr);
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
			throws IllegalArgumentException {
			if(isActive)
				try {
					doAccept(attr, required, value);
				} catch(MuisException e) {
					throw new IllegalArgumentException(e.getMessage(), e);
				}
			AttributeHolder holder = new AttributeHolder(attr);
			holder.required = required;
			holder.initValue = value;
			synchronized(theChained) {
				theChained.put(attr, holder);
			}
			theLastAttr = attr;
			theLastStyleAttr = null;
			return (CompoundListener<A>) this;
		}

		@Override
		public <A> CompoundListener<A> accept(MuisAttribute<A> attr) {
			return accept(attr, false, null);
		}

		@Override
		public <A, V extends A> CompoundListener<A> accept(MuisAttribute<A> attr, V value) throws IllegalArgumentException {
			return accept(attr, false, value);
		}

		@Override
		public <A> CompoundListener<A> require(MuisAttribute<A> attr) {
			return accept(attr, true, null);
		}

		@Override
		public <A, V extends A> CompoundListener<A> require(MuisAttribute<A> attr, V value) throws IllegalArgumentException {
			return accept(attr, true, value);
		}

		@Override
		public CompoundListener<?> acceptAll(MuisAttribute<?>... attrs) {
			if(isActive)
				for(MuisAttribute<?> attr : attrs)
					try {
						doAccept(attr, false, null);
					} catch(MuisException e) {
						throw new IllegalArgumentException(e.getMessage(), e);
					}
			synchronized(theChained) {
				for(MuisAttribute<?> attr : attrs) {
					AttributeHolder holder = new AttributeHolder(attr);
					holder.required = false;
					theChained.put(attr, holder);
				}
			}
			theLastStyleAttr = null;
			theLastAttr = null;
			return this;
		}

		@Override
		public CompoundListener<?> requireAll(MuisAttribute<?>... attrs) {
			if(isActive)
				try {
					for(MuisAttribute<?> attr : attrs)
						doAccept(attr, true, null);
				} catch(MuisException e) {
					throw new IllegalArgumentException(e.getMessage(), e);
				}
			synchronized(theChained) {
				for(MuisAttribute<?> attr : attrs) {
					AttributeHolder holder = new AttributeHolder(attr);
					holder.required = true;
					theChained.put(attr, holder);
				}
			}
			theLastStyleAttr = null;
			theLastAttr = null;
			return this;
		}

		@Override
		public <A> CompoundListener<A> watch(StyleAttribute<A> attr) {
			synchronized(theStyleAttributes) {
				theStyleAttributes.add(attr);
			}
			theLastAttr = null;
			theLastStyleAttr = attr;
			return (CompoundListener<A>) this;
		}

		@Override
		public CompoundListener<?> watchAll(StyleDomain domain) {
			synchronized(theStyleDomains) {
				theStyleDomains.add(domain);
			}
			theLastAttr = null;
			theLastStyleAttr = null;
			return this;
		}

		@Override
		public CompoundListener<?> watchAll(StyleAttribute<?>... attrs) {
			synchronized(theStyleAttributes) {
				for(StyleAttribute<?> attr : attrs)
					theStyleAttributes.add(attr);
			}
			theLastAttr = null;
			theLastStyleAttr = null;
			return this;
		}

		@Override
		public CompoundListener<?> setActive(boolean active) {
			if(isActive == active)
				return this;
			isActive = active;
			AttributeHolder [] attrs;
			synchronized(theChained) {
				attrs = theChained.values().toArray(new AttributeHolder[theChained.size()]);
			}
			if(isActive) {
				for(AttributeHolder attr : attrs) {
					try {
						doAccept((MuisAttribute<Object>) attr.attr, attr.required, attr.initValue);
					} catch(MuisException e) {
						throw new IllegalArgumentException(e.getMessage(), e);
					}
				}
			} else {
				for(AttributeHolder attr : attrs)
					doReject(attr.attr);
			}
			return this;
		}

		@Override
		public CompoundListener<?> onChange(Runnable run) {
			theLastAttr = null;
			theLastStyleAttr = null;
			theListeners.add(new RunnableChangeListener(run));
			return getParent();
		}

		@Override
		public CompoundListener<?> onChange(ChangeListener listener) {
			theLastAttr = null;
			theLastStyleAttr = null;
			theListeners.add(listener);
			return getParent();
		}

		@Override
		public CompoundListener<T> onAttChange(MuisEventListener<AttributeChangedEvent<? super T>> listener) {
			if(theLastAttr == null)
				throw new IllegalStateException("No attribute to listen to");
			synchronized(theSpecificListeners) {
				MuisEventListener<AttributeChangedEvent<?>> [] listeners = theSpecificListeners.get(theLastAttr);
				if(listeners == null)
					listeners = new MuisEventListener[] {listener};
				else
					listeners = (MuisEventListener<AttributeChangedEvent<?>> []) prisms.util.ArrayUtils.add(listeners, listener);
				theSpecificListeners.put(theLastAttr, listeners);
			}
			return this;
		}

		@Override
		public CompoundListener<T> onStyleChange(MuisEventListener<StyleAttributeEvent<? super T>> listener) {
			if(theLastStyleAttr == null)
				throw new IllegalStateException("No style attribute to listen to");
			synchronized(theSpecificStyleListeners) {
				MuisEventListener<StyleAttributeEvent<?>> [] listeners = theSpecificStyleListeners.get(theLastStyleAttr);
				if(listeners == null)
					listeners = new MuisEventListener[] {listener};
				else
					listeners = (MuisEventListener<StyleAttributeEvent<?>> []) prisms.util.ArrayUtils.add(listeners, listener);
				theSpecificStyleListeners.put(theLastStyleAttr, listeners);
			}
			return this;
		}
	}

	private static class SelfChainedCompoundListener<T> extends ChainedCompoundListener<T> {
		private CompoundElementListener theParent;

		SelfChainedCompoundListener(CompoundElementListener elListener) {
			theParent = elListener;
		}

		@Override
		void doReject(MuisAttribute<?> attr) {
			theParent.getElement().atts().reject(theParent.getWanter(), attr);
		}

		@Override
		MuisElement getElement() {
			return theParent.getElement();
		}

		@Override
		CompoundListener<?> getParent() {
			return theParent;
		}

		@Override
		<A, V extends A> void doAccept(MuisAttribute<A> attr, boolean required, V value) throws MuisException {
			theParent.getElement().atts().accept(theParent.getWanter(), required, attr, value);
		}
	}

	private static class ChildChainedCompoundListener<T> extends ChainedCompoundListener<T> {
		private final ChildCompoundListener theChildListener;

		private MuisElement [] theChildren;

		ChildChainedCompoundListener(ChildCompoundListener childListener) {
			theChildListener = childListener;
			theChildren = childListener.getElementListener().getElement().getChildren().toArray();
		}

		void childAdded(MuisElement child) {
			theChildren = prisms.util.ArrayUtils.add(theChildren, child);
			for(AttributeHolder holder : getAllAttrProps())
				try {
					child.atts().accept(theChildListener.getElementListener().getWanter(), holder.required,
						(MuisAttribute<Object>) holder.attr, holder.initValue);
				} catch(MuisException e) {
					child.msg().error("Bad initial value!", e, "attribute", holder.attr, "value", holder.initValue);
				}
		}

		void childRemoved(MuisElement child) {
			theChildren = prisms.util.ArrayUtils.remove(theChildren, child);
			for(AttributeHolder holder : getAllAttrProps())
				child.atts().reject(theChildListener.getElementListener().getWanter(), holder.attr);
		}

		@Override
		<A, V extends A> void doAccept(MuisAttribute<A> attr, boolean required, V initValue) throws IllegalArgumentException {
			for(MuisElement child : theChildren)
				try {
					child.atts().accept(theChildListener.getElementListener().getWanter(), required, (MuisAttribute<Object>) attr,
						initValue);
				} catch(MuisException e) {
					throw new IllegalArgumentException(e.getMessage(), e);
				}
		}

		@Override
		void doReject(MuisAttribute<?> attr) {
			for(MuisElement child : theChildren)
				child.atts().reject(theChildListener.getElementListener().getWanter(), attr);
		}

		@Override
		MuisElement getElement() {
			return theChildListener.getElementListener().getElement();
		}

		@Override
		CompoundListener<?> getParent() {
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
	public static class MultiElementCompoundListener extends ChainableCompoundListener {
		private final Object theWanter;

		private java.util.HashMap<MuisElement, CompoundElementListener> theListeners;

		private MultiElementChildCompoundListener theChildListener;

		private ArrayList<IndividualElementListener> theIndividualListeners;

		private final ChangeListener theAllIndividualsChecker;

		private final ChangeListener theIndividualChecker;

		private boolean isFinal;

		MultiElementCompoundListener(Object wanter) {
			theWanter = wanter;
			theListeners = new java.util.HashMap<>();
			theChildListener = new MultiElementChildCompoundListener();
			theIndividualListeners = new ArrayList<>();
			// Don't know why these are here anymore
			theAllIndividualsChecker = element -> {
			};
			theIndividualChecker = element -> {
			};
		}

		/** @return The listener to use to control attribute acceptance for all children that will use this listener */
		public CompoundListener<?> child() {
			if(isFinal)
				throw new IllegalStateException("MultiElementCompoundListeners may not be modified after elements are using it");
			return theChildListener;
		}

		/** @param individual The listener to deal with each child of an element separately */
		public void eachChild(IndividualElementListener individual) {
			theIndividualListeners.add(individual);
		}

		/**
		 * @param all Whether the listener returned should call an
		 *            {@link CompoundListener.IndividualElementListener#update(MuisElement, org.muis.util.CompoundListener.CompoundElementListener)
		 *            update} on all the element's children or just the child that is passed to
		 *            {@link CompoundListener.ChangeListener#changed(MuisElement)}. If the element passed to <code>changed</code> is the
		 *            parent element, all children are checked and this parameter has no effect.
		 * @return The change listener to update individual children in the listener's element
		 */
		public ChangeListener individualChecker(boolean all) {
			return all ? theAllIndividualsChecker : theIndividualChecker;
		}

		@Override
		ChainedCompoundListener<?> createChain() {
			if(isFinal)
				throw new IllegalStateException("MultiElementCompoundListeners may not be modified after elements are using it");
			return new HoldingChainedCompoundListener<>(this);
		}

		/**
		 * @param element The element to get or create the compound listener for
		 * @return A compound listener for the given element containing this listener's settings initially
		 */
		public CompoundElementListener listenerFor(MuisElement element) {
			synchronized(theListeners) {
				CompoundElementListener ret = theListeners.get(element);
				if(ret == null) {
					ret = new CompoundElementListener(element, theWanter);
					for(ChainedCompoundListener<?> chain : getAnonymousChains())
						applyChain(ret, chain, ret);
					for(String name : getChainNames())
						applyChain(ret, (ChainedCompoundListener<?>) chain(name), ret);

					for(ChainedCompoundListener<?> chain : theChildListener.getAnonymousChains())
						applyChain(ret.child(), chain, ret);
					for(String name : theChildListener.getChainNames()) {
						applyChain(ret.child(), (ChainedCompoundListener<?>) chain(name), ret);
					}
					for(IndividualElementListener individualListener : theIndividualListeners)
						ret.eachChild(individualListener);
					theListeners.put(element, ret);
				}
				return ret;
			}
		}

		private void applyChain(CompoundListener<?> applyTo, ChainedCompoundListener<?> chain, CompoundElementListener elListener) {
			CompoundListener<?> chainL = applyTo;
			for(AttributeHolder attr : chain.getAllAttrProps()) {
				chainL = chainL.accept((MuisAttribute<Object>) attr.attr, attr.required, attr.initValue);
				MuisEventListener<AttributeChangedEvent<? super Object>> [] listeners = chain
					.getSpecificListeners((MuisAttribute<Object>) attr.attr);
				if(listeners != null)
					for(MuisEventListener<AttributeChangedEvent<? super Object>> listener : listeners)
						((CompoundListener<Object>) chainL).onAttChange(listener);
			}
			for(ChangeListener listener : chain.getListeners())
				chainL.onChange(interpListener(listener, elListener));
		}

		private ChangeListener interpListener(ChangeListener listener, CompoundElementListener elListener) {
			if(listener == theAllIndividualsChecker)
				return elListener.individualChecker(true);
			else if(listener == theIndividualChecker)
				return elListener.individualChecker(false);
			else
				return listener;
		}

		/**
		 * Releases all resources and requirements of a compound listener for an element
		 *
		 * @param element The element to stop listening to
		 */
		public void dropFor(MuisElement element) {
			synchronized(theListeners) {
				CompoundElementListener ret = theListeners.remove(element);
				if(ret != null)
					ret.drop();
			}
		}
	}

	private static class MultiElementChildCompoundListener extends ChainableCompoundListener {
		@Override
		org.muis.util.CompoundListener.ChainedCompoundListener<?> createChain() {
			return new HoldingChainedCompoundListener<>(this);
		}
	}

	private static class HoldingChainedCompoundListener<T> extends ChainedCompoundListener<T> {
		private final CompoundListener<?> theParent;

		HoldingChainedCompoundListener(CompoundListener<?> parent) {
			theParent = parent;
		}

		@Override
		void doReject(MuisAttribute<?> attr) {
		}

		@Override
		MuisElement getElement() {
			return null;
		}

		@Override
		CompoundListener<?> getParent() {
			return theParent;
		}

		@Override
		<A, V extends A> void doAccept(MuisAttribute<A> attr, boolean required, V value) throws MuisException {
		}
	}
}
