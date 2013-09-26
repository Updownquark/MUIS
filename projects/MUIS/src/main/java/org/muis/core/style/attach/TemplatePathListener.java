package org.muis.core.style.attach;

import org.muis.core.*;
import org.muis.core.MuisConstants.Events;
import org.muis.core.MuisTemplate.AttachPoint;
import org.muis.core.event.MuisEvent;
import org.muis.core.event.MuisEventListener;
import org.muis.core.style.sheet.TemplateRole;

import prisms.util.ArrayUtils;

public class TemplatePathListener {
	public interface Listener {
		void pathAdded(TemplateRole path);

		void pathRemoved(TemplateRole path);

		void pathChanged(TemplateRole oldPath, TemplateRole newPath);
	}

	private static class TemplateListenerValue {
		AttachPoint role;

		MuisElement parent;

		TemplatePathListener parentListener;

		TemplateRole [] paths = new TemplateRole[0];
	}

	private java.util.List<Listener> theListeners;

	private MuisElement theElement;

	private java.util.Map<MuisAttribute<AttachPoint>, TemplateListenerValue> theAttachParents;

	public TemplatePathListener() {
		theListeners = new java.util.ArrayList<>();
		theAttachParents = new java.util.HashMap<>();
	}

	public void listen(MuisElement element) {
		if(theElement != null)
			throw new IllegalStateException("This " + getClass().getSimpleName() + " is already listening to "
				+ (element == theElement ? "this" : "a different") + " element.");
		theElement = element;
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
		if(theElement.getParent() != null)
			checkCurrent();
		else
			theElement.addListener(MuisConstants.Events.ELEMENT_MOVED, new MuisEventListener<MuisElement>() {
				@Override
				public void eventOccurred(MuisEvent<MuisElement> event, MuisElement el) {
					theElement.removeListener(this);
					checkCurrent();
				}

				@Override
				public boolean isLocal() {
					return true;
				}
			});
	}

	private void checkCurrent() {
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
		final TemplateListenerValue newValue;
		if(newAttachParent != null) {
			newValue = new TemplateListenerValue();
			newValue.role = role;
			newValue.parent = newAttachParent;
		} else
			newValue = null;

		TemplateListenerValue oldValue;
		if(newValue != null)
			oldValue = theAttachParents.put(roleAttr, newValue);
		else
			oldValue = theAttachParents.remove(roleAttr);
		if(oldValue != null && newAttachParent == oldValue.parent)
			return;
		if(oldValue != null) {
			// Remove old listener and template paths
			oldValue.parentListener.unlisten();
			for(int i = oldValue.paths.length - 1; i >= 0; i++)
				notifyPathRemoved(new TemplateRole(oldValue.role, null, null, oldValue.paths[i]));
			notifyPathRemoved(new TemplateRole(oldValue.role, null, null, null));
		}
		if(newValue != null) {
			// Add a template path listener to the new attach parent. Add template paths that the new parent contributes to. Fire listeners.
			newValue.parentListener = new TemplatePathListener();
			newValue.parentListener.addListener(new Listener() {
				@Override
				public void pathAdded(TemplateRole path) {
					TemplateRole added = new TemplateRole(newValue.role, null, null, path);
					newValue.paths = ArrayUtils.add(newValue.paths, added);
					notifyPathAdded(added);
				}

				@Override
				public void pathRemoved(TemplateRole path) {
					TemplateRole removed = new TemplateRole(newValue.role, null, null, path);
					newValue.paths = ArrayUtils.remove(newValue.paths, removed);
					notifyPathRemoved(removed);
				}

				@Override
				public void pathChanged(TemplateRole oldPath, TemplateRole newPath) {
					TemplateRole removed = new TemplateRole(newValue.role, null, null, oldPath);
					TemplateRole added = new TemplateRole(newValue.role, null, null, newPath);
					newValue.paths = ArrayUtils.add(newValue.paths, added);
					newValue.paths = ArrayUtils.remove(newValue.paths, removed);
					notifyPathReplaced(removed, added);
				}
			});
			TemplateRole singlet = new TemplateRole(role, null, null, null);
			newValue.paths = ArrayUtils.add(newValue.paths, singlet);
			notifyPathAdded(singlet);
			newValue.parentListener.listen(newAttachParent);
		}
	}

	private MuisElement getAttachParent(AttachPoint role) {
		Class<? extends MuisElement> definer = role.template.getDefiner();
		MuisElement parent = theElement.getParent();
		while(parent != null && !definer.isInstance(parent))
			parent = parent.getParent();
		return parent;
	}

	private void notifyPathAdded(TemplateRole path) {
		for(Listener listener : theListeners)
			listener.pathAdded(path);
	}

	private void notifyPathRemoved(TemplateRole path) {
		for(Listener listener : theListeners)
			listener.pathRemoved(path);
	}

	private void notifyPathReplaced(TemplateRole oldPath, TemplateRole newPath) {
		for(Listener listener : theListeners)
			listener.pathChanged(oldPath, newPath);
	}
}
