package org.muis.core.model;

/** A MUIS action listener that propagates the events it gets to one or more other listeners */
public class AggregateActionListener implements MuisActionListener, prisms.util.Sealable {
	private boolean isSealed;

	private java.util.List<MuisActionListener> theListeners;

	/** Creates an aggregate listener */
	public AggregateActionListener() {
		theListeners = new java.util.ArrayList<>(2);
	}

	@Override
	public void actionPerformed(MuisActionEvent event) {
		for(MuisActionListener listener : theListeners)
			listener.actionPerformed(event);
	}

	/** @param listener The listener to be notified when this aggregate listener receives an action event */
	public void addListener(MuisActionListener listener) {
		if(isSealed)
			throw new SealedException(this);
		if(listener != null)
			theListeners.add(listener);
	}

	/** @param listener The listener to remove from notification */
	public void removeListener(MuisActionListener listener) {
		if(isSealed)
			throw new SealedException(this);
		theListeners.remove(listener);
	}

	@Override
	public boolean isSealed() {
		return isSealed;
	}

	@Override
	public void seal() {
		isSealed = true;
	}
}
