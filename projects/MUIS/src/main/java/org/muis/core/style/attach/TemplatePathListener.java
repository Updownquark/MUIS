package org.muis.core.style.attach;

import java.util.Arrays;
import java.util.List;

import org.muis.core.MuisAttribute;
import org.muis.core.MuisElement;
import org.muis.core.MuisTemplate;
import org.muis.core.MuisTemplate.AttachPoint;
import org.muis.core.event.AttributeChangedEvent;
import org.muis.core.event.ElementMovedEvent;
import org.muis.core.style.sheet.TemplateRole;
import org.observe.Subscription;

import prisms.util.ArrayUtils;

/** Listens to a MuisElement for any {@link TemplateRole}s that might apply to it */
public class TemplatePathListener {
	/** A listener to be notified of template roles for an element */
	public interface Listener {
		/** @param path The path that newly applies to an element */
		void pathAdded(TemplateRole path);

		/** @param path The path that no longer applies to an element */
		void pathRemoved(TemplateRole path);

		/**
		 * @param oldPath The path that no longer applies to the element
		 * @param newPath The modified path that now applies to the element
		 */
		void pathChanged(TemplateRole oldPath, TemplateRole newPath);
	}

	private static class TemplateListenerValue {
		AttachPoint role;

		MuisTemplate parent;

		TemplatePathListener parentListener;

		org.observe.Action<AttributeChangedEvent<String []>> parentGroupListener;
		Subscription<?> parentGroupSubscription;

		TemplateRole [] paths = new TemplateRole[0];

		private List<String> parentGroups;
	}

	private java.util.List<Listener> theListeners;

	private MuisElement theElement;

	private java.util.Map<MuisAttribute<AttachPoint>, TemplateListenerValue> theAttachParents;

	/** Creates the listener */
	public TemplatePathListener() {
		theListeners = new java.util.ArrayList<>();
		theAttachParents = new java.util.HashMap<>();
	}

	/**
	 * Begins listening to an element
	 *
	 * @param element The element to listen to
	 */
	public void listen(MuisElement element) {
		if(theElement != null)
			throw new IllegalStateException("This " + getClass().getSimpleName() + " is already listening to "
				+ (element == theElement ? "this" : "a different") + " element.");
		theElement = element;
		theElement.events().filterMap(AttributeChangedEvent.base).act(event -> {
			if(!(event.getAttribute().getType() instanceof MuisTemplate.TemplateStructure.RoleAttributeType))
				return;
			MuisAttribute<AttachPoint> roleAttr = (MuisAttribute<AttachPoint>) event.getAttribute();
			roleChanged(roleAttr, (AttachPoint) event.getValue());
		});
		if(theElement.getParent() != null)
			checkCurrent();
		else {
			Subscription<?> [] sub = new Subscription[1];
			sub[0] = theElement.events().filterMap(ElementMovedEvent.moved).act(event -> {
				sub[0].unsubscribe();
				checkCurrent();
			});
		}
	}

	private void checkCurrent() {
		for(MuisAttribute<?> att : theElement.atts().attributes()) {
			if(att.getType() instanceof MuisTemplate.TemplateStructure.RoleAttributeType) {
				MuisAttribute<AttachPoint> roleAttr = (MuisAttribute<AttachPoint>) att;
				roleChanged(roleAttr, theElement.atts().get(roleAttr));
			}
		}
	}

	/** Stops listening to this listener's element */
	public void unlisten() {
		MuisElement element = theElement;
		theElement = null;
		if(element == null)
			return;
	}

	/** @param listener The listener to be notified when paths on the assigned element change */
	public void addListener(Listener listener) {
		if(listener != null)
			theListeners.add(listener);
	}

	/** @param listener The listener to stop notifying */
	public void removeListener(Listener listener) {
		theListeners.remove(listener);
	}

	private void roleChanged(MuisAttribute<MuisTemplate.AttachPoint> roleAttr, AttachPoint role) {
		MuisTemplate newAttachParent = getAttachParent(role);
		final TemplateListenerValue newValue;
		if(newAttachParent != null) {
			newValue = new TemplateListenerValue();
			newValue.role = role;
			newValue.parent = newAttachParent;
			String [] groups = newValue.parent.atts().get(org.muis.core.style.attach.GroupPropertyType.attribute);
			if(groups == null)
				newValue.parentGroups = new java.util.ArrayList<>(0);
			else
				newValue.parentGroups = Arrays.asList(groups);
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
			oldValue.parentGroupSubscription.unsubscribe();
			oldValue.parentListener.unlisten();
			for(int i = oldValue.paths.length - 1; i >= 0; i++)
				notifyPathRemoved(new TemplateRole(oldValue.role, oldValue.parentGroups, oldValue.parent.getClass(), oldValue.paths[i]));
			notifyPathRemoved(new TemplateRole(oldValue.role, oldValue.parentGroups, oldValue.parent.getClass(), null));
		}
		if(newValue != null) {
			// Add a template path listener to the new attach parent. Add template paths that the new parent contributes to. Fire listeners.
			newValue.parentListener = new TemplatePathListener();
			newValue.parentListener.addListener(new Listener() {
				@Override
				public void pathAdded(TemplateRole path) {
					TemplateRole added = new TemplateRole(newValue.role, newValue.parentGroups, newValue.parent.getClass(), path);
					newValue.paths = ArrayUtils.add(newValue.paths, added);
					notifyPathAdded(added);
				}

				@Override
				public void pathRemoved(TemplateRole path) {
					TemplateRole removed = new TemplateRole(newValue.role, newValue.parentGroups, newValue.parent.getClass(), path);
					newValue.paths = ArrayUtils.remove(newValue.paths, removed);
					notifyPathRemoved(removed);
				}

				@Override
				public void pathChanged(TemplateRole oldPath, TemplateRole newPath) {
					TemplateRole removed = new TemplateRole(newValue.role, newValue.parentGroups, newValue.parent.getClass(), oldPath);
					TemplateRole added = new TemplateRole(newValue.role, newValue.parentGroups, newValue.parent.getClass(), newPath);
					newValue.paths = ArrayUtils.add(newValue.paths, added);
					newValue.paths = ArrayUtils.remove(newValue.paths, removed);
					notifyPathReplaced(removed, added);
				}
			});
			TemplateRole singlet = new TemplateRole(role, newValue.parentGroups, newValue.parent.getClass(), null);
			newValue.paths = ArrayUtils.add(newValue.paths, singlet);
			notifyPathAdded(singlet);
			newValue.parentListener.listen(newAttachParent);
			newValue.parentGroupListener = event -> {
				List<String> oldGroups = newValue.parentGroups;
				newValue.parentGroups = Arrays.asList(event.getValue());
				for(int i = newValue.paths.length - 1; i >= 0; i--) {
					notifyPathReplaced(new TemplateRole(newValue.role, oldGroups, newValue.parent.getClass(), newValue.paths[i]),
						new TemplateRole(newValue.role, newValue.parentGroups, newValue.parent.getClass(), newValue.paths[i]));
				}
				notifyPathReplaced(new TemplateRole(newValue.role, oldGroups, newValue.parent.getClass(), null), new TemplateRole(
					newValue.role, newValue.parentGroups, newValue.parent.getClass(), null));
			};
			newValue.parentGroupSubscription = newValue.parent.events().filterMap(AttributeChangedEvent.att(GroupPropertyType.attribute))
				.act(newValue.parentGroupListener);
		}
	}

	private MuisTemplate getAttachParent(AttachPoint role) {
		Class<? extends MuisElement> definer = role.template.getDefiner();
		MuisElement parent = theElement.getParent();
		while(parent != null && !definer.isInstance(parent))
			parent = parent.getParent();
		return (MuisTemplate) parent;
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

	@Override
	public String toString() {
		return getClass().getSimpleName() + " for " + theElement;
	}
}
