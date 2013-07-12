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

	public TemplatePathListener() {
		theListeners = new java.util.ArrayList<>();
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

	private void roleChanged(MuisAttribute<MuisTemplate.AttachPoint> roleAttr, MuisTemplate.AttachPoint role) {
		int todo; // TODO
	}
}
