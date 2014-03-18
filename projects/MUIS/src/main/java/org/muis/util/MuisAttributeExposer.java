package org.muis.util;

import org.muis.core.MuisAttribute;
import org.muis.core.MuisConstants.Events;
import org.muis.core.MuisElement;
import org.muis.core.MuisException;
import org.muis.core.event.MuisEvent;
import org.muis.core.event.MuisEventListener;

/** Synchronizes attributes from a source to a destination */
public class MuisAttributeExposer implements AutoCloseable {
	/** The set of attributes that are never exposed by this class */
	public static final java.util.Set<MuisAttribute<?>> EXCLUDED;

	static {
		java.util.Set<MuisAttribute<?>> excluded = new java.util.HashSet<>();
		excluded.add(org.muis.core.style.attach.StyleAttributeType.STYLE_ATTRIBUTE);
		excluded.add(org.muis.core.style.attach.GroupPropertyType.attribute);
		EXCLUDED = java.util.Collections.unmodifiableSet(excluded);
	}

	private MuisElement theSource;

	private MuisElement theDest;

	private org.muis.core.mgr.MuisMessageCenter theMessageCenter;

	private boolean isActive;

	private MuisEventListener<MuisAttribute<?>> [] theListeners;

	/**
	 * @param source The element whose attributes to synchronize to another element
	 * @param dest The element to synchronize the source's attributes into
	 * @param msg The message center to record errors setting attributes in the destination
	 */
	public MuisAttributeExposer(MuisElement source, MuisElement dest, org.muis.core.mgr.MuisMessageCenter msg) {
		theSource = source;
		theDest = dest;
		theListeners = new MuisEventListener[2];
		theListeners[0] = new MuisEventListener<MuisAttribute<?>>() {
			@Override
			public void eventOccurred(MuisEvent<MuisAttribute<?>> event, MuisElement element) {
				org.muis.core.event.AttributeAcceptedEvent aae = (org.muis.core.event.AttributeAcceptedEvent) event;
				if(aae.isAccepted())
					try {
						theSource.atts().accept(this, aae.isRequired(), (MuisAttribute<Object>) aae.getValue(), aae.getInitialValue());
					} catch(MuisException e) {
						theMessageCenter.error(
							"Attribute synchronization failed: Source " + theSource + " cannot accept attribute " + aae.getValue(), e);
					}
				else if(!theDest.atts().isAccepted(aae.getValue()))
					theSource.atts().reject(this, aae.getValue());
			}
		};
		theListeners[1] = new MuisEventListener<MuisAttribute<?>>() {
			@Override
			public void eventOccurred(MuisEvent<MuisAttribute<?>> event, MuisElement element) {
				MuisAttribute<?> att = event.getValue();
				if(!EXCLUDED.contains(att) && !(att.getType() instanceof org.muis.core.MuisTemplate.TemplateStructure.RoleAttributeType)
					&& theDest.atts().isAccepted(att))
					try {
						theDest.atts().set((MuisAttribute<Object>) att, theSource.atts().get(att));
					} catch(MuisException e) {
						theMessageCenter.error("Attribute synchronization failed: Destination " + theDest + " cannot accept value "
							+ theSource.atts().get(att) + " for attribute " + att, e);
					}
			}
		};
		theDest.addListener(Events.ATTRIBUTE_ACCEPTED, theListeners[0]);
		for(org.muis.core.mgr.AttributeManager.AttributeHolder<?> holder : theDest.atts().holders()) {
			MuisAttribute<?> att = holder.getAttribute();
			if(!EXCLUDED.contains(att) && !(att.getType() instanceof org.muis.core.MuisTemplate.TemplateStructure.RoleAttributeType)) {
				try {
					theSource.atts().accept(this, holder.isRequired(), (MuisAttribute<Object>) att, holder.getValue());
				} catch(MuisException e) {
					theMessageCenter.error("Attribute synchronization failed: Source " + theSource + " cannot accept attribute " + att, e);
				}
				Object newVal = theSource.atts().get(att);
				if(!java.util.Objects.equals(holder.getValue(), newVal))
					try {
						theDest.atts().set((MuisAttribute<Object>) att, newVal);
					} catch(MuisException e) {
						theMessageCenter.error("Attribute synchronization failed: Destination " + theDest + " cannot accept value "
							+ theSource.atts().get(att) + " for attribute " + att, e);
					}
			}
		}
		theSource.addListener(Events.ATTRIBUTE_SET, theListeners[1]);
	}

	/** @return The element that is the source of the attributes being synchronized */
	public MuisElement getSource() {
		return theSource;
	}

	/** @return The element that is the destination of the attributes being synchronized */
	public MuisElement getDest() {
		return theDest;
	}

	/** @return Whether this synchronizer is currently synchronizing attributes */
	public boolean isActive() {
		return isActive;
	}

	@Override
	public void close() {
		if(!isActive)
			return;
		theDest.removeListener(theListeners[0]);
		theSource.removeListener(theListeners[1]);
		for(org.muis.core.mgr.AttributeManager.AttributeHolder<?> holder : theDest.atts().holders())
			theSource.atts().reject(this, holder.getAttribute());
		isActive = false;
	}
}
