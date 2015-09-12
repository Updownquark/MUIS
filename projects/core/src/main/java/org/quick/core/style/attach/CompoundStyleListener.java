package org.quick.core.style.attach;

import org.observe.Action;
import org.observe.collect.ObservableSet;
import org.quick.core.QuickElement;
import org.quick.core.mgr.QuickState;
import org.quick.core.style.QuickStyle;
import org.quick.core.style.StyleAttribute;
import org.quick.core.style.StyleAttributeEvent;
import org.quick.core.style.StyleDomain;
import org.quick.core.style.stateful.StatefulStyleSample;

import prisms.util.ArrayUtils;

/** Listens for changes to the style of an element */
public class CompoundStyleListener {
	private final QuickElement theElement;

	private Action<StyleAttributeEvent<?>> theStyleListener;
	private Action<GroupMemberEvent> theGroupListener;
	private org.observe.DefaultObservable<QuickElement> theRemoveObservable = new org.observe.DefaultObservable<>();
	private org.observe.Observer<QuickElement> theRemoveController = theRemoveObservable.control(null);

	private StyleDomain [] theDomains;

	private StyleAttribute<?> [] theAttributes;

	private boolean isDetailed;

	private boolean isAdded;

	/**
	 * Creates a StyleListener
	 *
	 * @param element The element to listen for style changes to
	 */
	public CompoundStyleListener(QuickElement element) {
		theElement = element;
		theDomains = new StyleDomain[0];
		theAttributes = new StyleAttribute[0];
		theStyleListener = event -> {
			CompoundStyleListener.this.eventOccurred(event);
		};
		theGroupListener = event -> {
			CompoundStyleListener.this.eventOccurred(event);
		};
	}

	/** Adds this listener to the element */
	public void add() {
		if(isAdded)
			return;
		isAdded = true;
		theElement.events().filterMap(StyleAttributeEvent.base).takeUntil(theRemoveObservable).act(theStyleListener);
		theElement.events().filterMap(GroupMemberEvent.groups).takeUntil(theRemoveObservable).act(theGroupListener);
	}

	/** Removes this listener from the element. */
	public void remove() {
		theRemoveController.onNext(theElement);
	}

	/**
	 * @return Whether this listener call the {@link #attributeChanged(StyleAttribute, Object)} method for each attribute changed when a
	 *         group is added or removed.
	 */
	public boolean isDetailed() {
		return isDetailed;
	}

	/**
	 * @param detailed Whether this listener should call the {@link #attributeChanged(StyleAttribute, Object)} method for each attribute
	 *            changed when a group is added or removed. Leaving this as false will improve performance somewhat.
	 * @return This listener, for chaining
	 */
	public CompoundStyleListener setDetailed(boolean detailed) {
		isDetailed = detailed;
		return this;
	}

	/**
	 * @param domain The domain to listen for changes to
	 * @return This listener, for chaining
	 */
	public CompoundStyleListener addDomain(StyleDomain domain) {
		if(domain != null && !ArrayUtils.contains(theDomains, domain))
			theDomains = ArrayUtils.add(theDomains, domain);
		return this;
	}

	/** @param domain The domain to cease listening for changes to */
	public void removeDomain(StyleDomain domain) {
		theDomains = ArrayUtils.remove(theDomains, domain);
	}

	/**
	 * @param attr The attribute to listen for changes to
	 * @return This listener, for chaining
	 */
	public CompoundStyleListener addAttribute(StyleAttribute<?> attr) {
		if(attr != null && !ArrayUtils.contains(theAttributes, attr))
			theAttributes = ArrayUtils.add(theAttributes, attr);
		return this;
	}

	/** @param attr The attribute to cease listening for changes to */
	public void removeAttribute(StyleAttribute<?> attr) {
		theAttributes = ArrayUtils.remove(theAttributes, attr);
	}

	/**
	 * Called when a group is added to or removed from an element style
	 *
	 * @param event The group member event representing the change
	 */
	private void eventOccurred(GroupMemberEvent event) {
		java.util.HashSet<StyleAttribute<?>> groupAttrs = new java.util.HashSet<>();
		ObservableSet<QuickState> state = theElement.getStateEngine().activeStates();
		for(StyleAttribute<?> attr : new StatefulStyleSample(event.getGroup().getGroupForType(theElement.getClass()), state).attributes())
			if(ArrayUtils.contains(theDomains, attr.getDomain()) || ArrayUtils.contains(theAttributes, attr))
				groupAttrs.add(attr);
		if(groupAttrs.isEmpty())
			return; // The group doesn't contain any attributes we care about

		for(StyleAttribute<?> attr : theElement.getStyle().localAttributes())
			groupAttrs.remove(attr);
		if(event.getRemoveIndex() < 0)
			for(TypedStyleGroup<?> g : theElement.getStyle().groups(false)) {
				if(g == event.getGroup())
					break;
				for(StyleAttribute<?> attr : new StatefulStyleSample(g, state).attributes())
					groupAttrs.remove(attr);
			}
		else {
			java.util.ListIterator<TypedStyleGroup<?>> iter;
			iter = (java.util.ListIterator<TypedStyleGroup<?>>) theElement.getStyle().groups(false).iterator();
			while(iter.hasNext()) {
				if(iter.nextIndex() <= event.getRemoveIndex())
					break;
				for(StyleAttribute<?> attr : new StatefulStyleSample(iter.next(), state).attributes())
					groupAttrs.remove(attr);
			}
		}
		if(groupAttrs.isEmpty())
			return; // Any interesting attributes are eclipsed by closer styles

		styleChanged(new StatefulStyleSample(event.getGroup(), state));
		if(isDetailed)
			for(StyleAttribute<?> attr : groupAttrs) {
				Object val = theElement.getStyle().get(attr);
				attributeChanged(attr, val);
			}
	}

	/**
	 * Called when a single attribute changes
	 *
	 * @param event The event representing the change
	 */
	public void eventOccurred(StyleAttributeEvent<?> event) {
		if(!ArrayUtils.contains(theDomains, event.getAttribute().getDomain()) && !ArrayUtils.contains(theAttributes, event.getAttribute()))
			return;
		if(event.getRootStyle() instanceof TypedStyleGroup<?>) {
			TypedStyleGroup<?> group = (TypedStyleGroup<?>) event.getRootStyle();
			if(!group.getType().isInstance(theElement))
				return;
			if(theElement.getStyle().isSet(event.getAttribute()))
				return;
			NamedStyleGroup nsg = null;
			while(group != null && !(group instanceof NamedStyleGroup))
				group = group.getParent();
			nsg = (NamedStyleGroup) group;
			if(!nsg.isMember(theElement))
				return;
			for(TypedStyleGroup<?> g : theElement.getStyle().groups(false)) {
				if(g == nsg)
					break;
				if(new StatefulStyleSample(g, theElement.getStateEngine().activeStates()).isSet(event.getAttribute()))
					return;
			}
		}
		styleChanged(event.getRootStyle());
		attributeChanged(event.getAttribute(), event.getValue());
	}

	/**
	 * Called whenever any style attribute matching this listener's domains and attributes (or possibly more than one if a style group is
	 * added or removed) is changed in the style
	 *
	 * @param style The style that was changed--may be an ElementStyle or a TypedStyleGroup
	 */
	public void styleChanged(QuickStyle style) {
	}

	/**
	 * Called for each attribute matching this listener's domains and attributes that is changed in the style. If {@link #isDetailed()} is
	 * false, this will not be called when groups are added to or removed from the style.
	 *
	 * @param attr The attribute that was changed
	 * @param newValue The new value of the attribute
	 */
	public void attributeChanged(StyleAttribute<?> attr, Object newValue) {
	}
}
