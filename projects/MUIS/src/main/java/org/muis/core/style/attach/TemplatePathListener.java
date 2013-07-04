package org.muis.core.style.attach;

import org.muis.core.MuisElement;
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
}
