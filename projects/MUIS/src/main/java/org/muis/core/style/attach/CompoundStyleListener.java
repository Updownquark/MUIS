package org.muis.core.style.attach;

import org.muis.core.MuisElement;
import org.muis.core.event.MuisEvent;
import org.muis.core.style.MuisStyle;
import org.muis.core.style.StyleAttribute;
import org.muis.core.style.StyleAttributeEvent;
import org.muis.core.style.StyleDomain;
import org.muis.core.style.stateful.StatefulStyleSample;

import prisms.util.ArrayUtils;

/** Listens for changes to the style of an element */
public class CompoundStyleListener implements org.muis.core.event.MuisEventListener<Object> {
	private final MuisElement theElement;

	private StyleDomain [] theDomains;

	private StyleAttribute<?> [] theAttributes;

	private boolean isDetailed;

	private boolean isAdded;

	/**
	 * Creates a StyleListener
	 *
	 * @param element The element to listen for style changes to
	 */
	public CompoundStyleListener(MuisElement element) {
		theElement = element;
		theDomains = new StyleDomain[0];
		theAttributes = new StyleAttribute[0];
	}

	/** Adds this listener to the element */
	public void add() {
		if(isAdded)
			return;
		isAdded = true;
		theElement.addListener(StyleAttributeEvent.TYPE, this);
		theElement.addListener(GroupMemberEvent.TYPE, this);
	}

	/** Removes this listener from the element. */
	public void remove() {
		theElement.removeListener(this);
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

	@Override
	public boolean isLocal() {
		return false;
	}

	@Override
	public void eventOccurred(MuisEvent<Object> event, MuisElement element) {
		MuisEvent<?> evt = event;
		if(evt instanceof GroupMemberEvent)
			eventOccurred((GroupMemberEvent) evt);
		else if(evt instanceof StyleAttributeEvent<?>)
			eventOccurred((StyleAttributeEvent<?>) evt);
		else
			return;
	}

	/**
	 * Called when a group is added to or removed from an element style
	 *
	 * @param event The group member event representing the change
	 */
	public void eventOccurred(GroupMemberEvent event) {
		java.util.HashSet<StyleAttribute<?>> groupAttrs = new java.util.HashSet<>();
		org.muis.core.mgr.MuisState[] state = theElement.getStateEngine().toArray();
		for(StyleAttribute<?> attr : new StatefulStyleSample(event.getValue().getGroupForType(theElement.getClass()), state))
			if(ArrayUtils.contains(theDomains, attr.getDomain()) || ArrayUtils.contains(theAttributes, attr))
				groupAttrs.add(attr);
		if(groupAttrs.isEmpty())
			return; // The group doesn't contain any attributes we care about

		for(StyleAttribute<?> attr : theElement.getStyle().localAttributes())
			groupAttrs.remove(attr);
		if(event.getRemoveIndex() < 0)
			for(TypedStyleGroup<?> g : theElement.getStyle().groups(false)) {
				if(g == event.getValue())
					break;
				for(StyleAttribute<?> attr : new StatefulStyleSample(g, state))
					groupAttrs.remove(attr);
			}
		else {
			java.util.ListIterator<TypedStyleGroup<?>> iter;
			iter = (java.util.ListIterator<TypedStyleGroup<?>>) theElement.getStyle().groups(false).iterator();
			while(iter.hasNext()) {
				if(iter.nextIndex() <= event.getRemoveIndex())
					break;
				for(StyleAttribute<?> attr : new StatefulStyleSample(iter.next(), state))
					groupAttrs.remove(attr);
			}
		}
		if(groupAttrs.isEmpty())
			return; // Any interesting attributes are eclipsed by closer styles

		styleChanged(new StatefulStyleSample(event.getValue(), state));
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
				if(new StatefulStyleSample(g, theElement.getStateEngine().toArray()).isSet(event.getAttribute()))
					return;
			}
		}
		styleChanged(event.getRootStyle());
		attributeChanged(event.getAttribute(), event.getNewValue());
	}

	/**
	 * Called whenever any style attribute matching this listener's domains and attributes (or possibly more than one if a style group is
	 * added or removed) is changed in the style
	 *
	 * @param style The style that was changed--may be an ElementStyle or a TypedStyleGroup
	 */
	public void styleChanged(MuisStyle style) {
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
