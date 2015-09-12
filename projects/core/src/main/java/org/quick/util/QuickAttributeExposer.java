package org.quick.util;

import org.observe.Action;
import org.observe.Subscription;
import org.quick.core.QuickAttribute;
import org.quick.core.QuickElement;
import org.quick.core.QuickException;
import org.quick.core.event.AttributeAcceptedEvent;
import org.quick.core.event.AttributeChangedEvent;

import prisms.util.ArrayUtils;

/** Synchronizes attributes from a source to a destination */
public class QuickAttributeExposer {
	/** The set of attributes that are never exposed by this class */
	public static final java.util.Set<QuickAttribute<?>> EXCLUDED;

	static {
		java.util.Set<QuickAttribute<?>> excluded = new java.util.HashSet<>();
		excluded.add(org.quick.core.style.attach.StyleAttributeType.STYLE_ATTRIBUTE);
		excluded.add(org.quick.core.style.attach.GroupPropertyType.attribute);
		EXCLUDED = java.util.Collections.unmodifiableSet(excluded);
	}

	private QuickElement theSource;

	private QuickElement theDest;

	private QuickAttribute<?> [] theAttributes;

	private org.quick.core.mgr.QuickMessageCenter theMessageCenter;

	private boolean isActive;

	private Action<AttributeAcceptedEvent> theAttAcceptListener;
	private Subscription theAttAcceptSubscription;
	private Action<AttributeChangedEvent<?>> theAttChangeListener;
	private Subscription theAttChangeSubscription;

	/**
	 * @param source The element whose attributes to synchronize to another element
	 * @param dest The element to synchronize the source's attributes into
	 * @param msg The message center to record errors setting attributes in the destination
	 * @param atts The attributes to expose. If empty, this utility will expose all attributes except style and group.
	 */
	public QuickAttributeExposer(QuickElement source, QuickElement dest, org.quick.core.mgr.QuickMessageCenter msg, QuickAttribute<?>... atts) {
		theSource = source;
		theDest = dest;
		theAttributes = atts;
		theAttAcceptListener = event -> {
			if(theAttributes.length > 0 && !ArrayUtils.contains(theAttributes, event.getAttribute()))
				return;
			if(event.isAccepted())
				try {
					if(!theSource.atts().isAccepted(event.getAttribute()))
						theSource.atts().accept(this, event.isRequired(), (QuickAttribute<Object>) event.getAttribute(),
							event.getInitialValue());
				} catch(QuickException e) {
					theMessageCenter.error(
						"Attribute synchronization failed: Source " + theSource + " cannot accept attribute " + event.getAttribute(), e);
				}
			else if(!theDest.atts().isAccepted(event.getAttribute()))
				theSource.atts().reject(this, event.getAttribute());
		};
		theAttChangeListener = event -> {
			if(theAttributes.length > 0 && !ArrayUtils.contains(theAttributes, event.getAttribute()))
				return;
			QuickAttribute<?> att = event.getAttribute();
			if(!EXCLUDED.contains(att) && !(att.getType() instanceof org.quick.core.QuickTemplate.TemplateStructure.RoleAttributeType)
				&& theDest.atts().isAccepted(att))
				try {
					theDest.atts().set((QuickAttribute<Object>) att, event.getValue());
				} catch(QuickException e) {
					theMessageCenter.error("Attribute synchronization failed: Destination " + theDest + " cannot accept value "
						+ theSource.atts().get(att) + " for attribute " + att, e);
				}
		};
		theAttAcceptSubscription = theDest.events().filterMap(AttributeAcceptedEvent.attAccept).act(theAttAcceptListener);
		for(org.quick.core.mgr.AttributeManager.AttributeHolder<?> holder : theDest.atts().holders()) {
			QuickAttribute<?> att = holder.getAttribute();
			if(theAttributes.length > 0 && !ArrayUtils.contains(theAttributes, att))
				continue;
			if(!EXCLUDED.contains(att) && !(att.getType() instanceof org.quick.core.QuickTemplate.TemplateStructure.RoleAttributeType)) {
				try {
					if(!theSource.atts().isAccepted(att))
						theSource.atts().accept(this, holder.isRequired(), (QuickAttribute<Object>) att, holder.get());
				} catch(QuickException e) {
					theMessageCenter.error("Attribute synchronization failed: Source " + theSource + " cannot accept attribute " + att, e);
				}
				Object newVal = theSource.atts().get(att);
				if(!java.util.Objects.equals(holder.get(), newVal))
					try {
						theDest.atts().set((QuickAttribute<Object>) att, newVal);
					} catch(QuickException e) {
						theMessageCenter.error("Attribute synchronization failed: Destination " + theDest + " cannot accept value "
							+ theSource.atts().get(att) + " for attribute " + att, e);
					}
			}
		}
		theAttChangeSubscription = theSource.events().filterMap(AttributeChangedEvent.base).act(theAttChangeListener);
	}

	/** @return The element that is the source of the attributes being synchronized */
	public QuickElement getSource() {
		return theSource;
	}

	/** @return The element that is the destination of the attributes being synchronized */
	public QuickElement getDest() {
		return theDest;
	}

	/** @return Whether this synchronizer is currently synchronizing attributes */
	public boolean isActive() {
		return isActive;
	}

	/** Undoes the exposure of this utility's attributes */
	public void close() {
		if(!isActive)
			return;
		theAttAcceptSubscription.unsubscribe();
		theAttChangeSubscription.unsubscribe();
		for(org.quick.core.mgr.AttributeManager.AttributeHolder<?> holder : theDest.atts().holders())
			theSource.atts().reject(this, holder.getAttribute());
		isActive = false;
	}
}
