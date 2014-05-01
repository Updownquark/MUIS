package org.muis.util;

import org.muis.core.MuisAttribute;
import org.muis.core.MuisElement;
import org.muis.core.MuisException;
import org.muis.core.event.AttributeAcceptedEvent;
import org.muis.core.event.AttributeChangedEvent;
import org.muis.core.event.MuisEventListener;

import prisms.util.ArrayUtils;

/** Synchronizes attributes from a source to a destination */
public class MuisAttributeExposer {
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

	private MuisAttribute<?> [] theAttributes;

	private org.muis.core.mgr.MuisMessageCenter theMessageCenter;

	private boolean isActive;

	private MuisEventListener<AttributeAcceptedEvent> theAttAcceptListener;
	private MuisEventListener<AttributeChangedEvent<?>> theAttChangeListener;

	/**
	 * @param source The element whose attributes to synchronize to another element
	 * @param dest The element to synchronize the source's attributes into
	 * @param msg The message center to record errors setting attributes in the destination
	 * @param atts The attributes to expose. If empty, this utility will expose all attributes except style and group.
	 */
	public MuisAttributeExposer(MuisElement source, MuisElement dest, org.muis.core.mgr.MuisMessageCenter msg, MuisAttribute<?>... atts) {
		theSource = source;
		theDest = dest;
		theAttributes = atts;
		theAttAcceptListener = new MuisEventListener<AttributeAcceptedEvent>() {
			@Override
			public void eventOccurred(AttributeAcceptedEvent event) {
				if(theAttributes.length > 0 && !ArrayUtils.contains(theAttributes, event.getAttribute()))
					return;
				if(event.isAccepted())
					try {
						if(!theSource.atts().isAccepted(event.getAttribute()))
							theSource.atts().accept(this, event.isRequired(), (MuisAttribute<Object>) event.getAttribute(),
								event.getInitialValue());
					} catch(MuisException e) {
						theMessageCenter.error("Attribute synchronization failed: Source " + theSource + " cannot accept attribute "
							+ event.getAttribute(), e);
					}
				else if(!theDest.atts().isAccepted(event.getAttribute()))
					theSource.atts().reject(this, event.getAttribute());
			}
		};
		theAttChangeListener = new MuisEventListener<AttributeChangedEvent<?>>() {
			@Override
			public void eventOccurred(AttributeChangedEvent<?> event) {
				if(theAttributes.length > 0 && !ArrayUtils.contains(theAttributes, event.getAttribute()))
					return;
				MuisAttribute<?> att = event.getAttribute();
				if(!EXCLUDED.contains(att) && !(att.getType() instanceof org.muis.core.MuisTemplate.TemplateStructure.RoleAttributeType)
					&& theDest.atts().isAccepted(att))
					try {
						theDest.atts().set((MuisAttribute<Object>) att, event.getValue());
					} catch(MuisException e) {
						theMessageCenter.error("Attribute synchronization failed: Destination " + theDest + " cannot accept value "
							+ theSource.atts().get(att) + " for attribute " + att, e);
					}
			}
		};
		theDest.events().listen(AttributeAcceptedEvent.attAccept, theAttAcceptListener);
		for(org.muis.core.mgr.AttributeManager.AttributeHolder<?> holder : theDest.atts().holders()) {
			MuisAttribute<?> att = holder.getAttribute();
			if(theAttributes.length > 0 && !ArrayUtils.contains(theAttributes, att))
				continue;
			if(!EXCLUDED.contains(att) && !(att.getType() instanceof org.muis.core.MuisTemplate.TemplateStructure.RoleAttributeType)) {
				try {
					if(!theSource.atts().isAccepted(att))
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
		theSource.events().listen(AttributeChangedEvent.base, theAttChangeListener);
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

	/** Undoes the exposure of this utility's attributes */
	public void close() {
		if(!isActive)
			return;
		theDest.events().remove(AttributeAcceptedEvent.attAccept, theAttAcceptListener);
		theSource.events().remove(AttributeChangedEvent.base, theAttChangeListener);
		for(org.muis.core.mgr.AttributeManager.AttributeHolder<?> holder : theDest.atts().holders())
			theSource.atts().reject(this, holder.getAttribute());
		isActive = false;
	}
}
