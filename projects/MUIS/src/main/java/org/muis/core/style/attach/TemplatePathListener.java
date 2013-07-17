package org.muis.core.style.attach;

import org.muis.core.*;
import org.muis.core.MuisConstants.Events;
import org.muis.core.MuisTemplate.AttachPoint;
import org.muis.core.event.MuisEvent;
import org.muis.core.event.MuisEventListener;
import org.muis.core.style.sheet.TemplatePath;

public class TemplatePathListener {
	public interface Listener {
		void pathAdded(TemplatePath path);

		void pathRemoved(TemplatePath path);

		void pathChanged(TemplatePath oldPath, TemplatePath newPath);
	}

	private java.util.List<Listener> theListeners;

	private MuisElement theElement;

	private java.util.Map<MuisAttribute<AttachPoint>, MuisElement> theAttachParents;

	public TemplatePathListener() {
		theListeners = new java.util.ArrayList<>();
		theAttachParents = new java.util.HashMap<>();
	}

	public void listen(MuisElement element) {
		if(theElement != null)
			throw new IllegalStateException("This " + getClass().getSimpleName() + " is already listening to "
				+ (element == theElement ? "this" : "a different") + " element.");
		theElement.addListener(Events.ATTRIBUTE_SET, new MuisEventListener<MuisAttribute<?>>() {
			@Override
			public void eventOccurred(MuisEvent<MuisAttribute<?>> event, MuisElement evtElement) {
				if(!(event.getValue().getType() instanceof MuisTemplate.TemplateStructure.RoleAttributeType))
					return;
				MuisAttribute<AttachPoint> roleAttr = (MuisAttribute<AttachPoint>) event.getValue();
				roleChanged(roleAttr, theElement.atts().get(roleAttr));
			}

			@Override
			public boolean isLocal() {
				return true;
			}
		});
		for(MuisAttribute<?> att : theElement.atts().attributes()) {
			if(att.getType() instanceof MuisTemplate.TemplateStructure.RoleAttributeType) {
				MuisAttribute<AttachPoint> roleAttr = (MuisAttribute<AttachPoint>) att;
				roleChanged(roleAttr, theElement.atts().get(roleAttr));
			}
		}
	}

	public void unlisten() {
		MuisElement element = theElement;
		theElement = null;
		if(element == null)
			return;
	}

	public void addListener(Listener listener) {
		if(listener != null)
			theListeners.add(listener);
	}

	public void removeListener(Listener listener) {
		theListeners.remove(listener);
	}

	private void roleChanged(MuisAttribute<MuisTemplate.AttachPoint> roleAttr, AttachPoint role) {
		MuisElement newAttachParent = getAttachParent(role);
		MuisElement oldAttachParent;
		if(newAttachParent != null)
			oldAttachParent = theAttachParents.put(roleAttr, newAttachParent);
		else
			oldAttachParent = theAttachParents.remove(roleAttr);
		if(oldAttachParent == newAttachParent)
			return;
		if(oldAttachParent != null) {
			// TODO Remove the template path listener from old attach parent. Remove template paths that it contributed to,
			// replacing them with one-element paths. Fire listeners.
		}
		if(newAttachParent != null) {
			// Add a template path listener to the new attach parent. Add template paths that the new parent contributes to. Fire listeners.
		}
		int todo;
	}

	private MuisElement getAttachParent(AttachPoint role) {
	}
}
