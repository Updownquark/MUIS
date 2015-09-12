package org.quick.core.model;

import org.observe.DefaultObservableValue;
import org.observe.ObservableValue;
import org.observe.ObservableValueEvent;
import org.observe.Observer;

import prisms.lang.Type;

/** A MUIS action listener that propagates the events it gets to one or more other listeners */
public class AggregateActionListener implements QuickActionListener, org.qommons.Sealable {
	private boolean isSealed;

	private java.util.List<QuickActionListener> theListeners;

	private DefaultObservableValue<Boolean> isEnabled;
	private Observer<ObservableValueEvent<Boolean>> theEnabledController;

	/** Creates an aggregate listener */
	public AggregateActionListener() {
		theListeners = new java.util.ArrayList<>(2);
		isEnabled = new DefaultObservableValue<Boolean>() {
			@Override
			public Type getType() {
				return new Type(Boolean.class);
			}

			@Override
			public Boolean get() {
				boolean ret = false;
				for(QuickActionListener listener : theListeners)
					if(listener.isEnabled().get()) {
						ret = true;
						break;
					}
				return ret;
			}
		};
		theEnabledController = isEnabled.control(null);
	}

	@Override
	public ObservableValue<Boolean> isEnabled() {
		return isEnabled;
	}

	@Override
	public void actionPerformed(QuickActionEvent event) {
		for(QuickActionListener listener : theListeners)
			listener.actionPerformed(event);
	}

	/** @param listener The listener to be notified when this aggregate listener receives an action event */
	public void addListener(QuickActionListener listener) {
		if(isSealed)
			throw new SealedException(this);
		if(listener != null) {
			boolean oldEnabled = isEnabled.get();
			theListeners.add(listener);
			if(!oldEnabled && listener.isEnabled().get())
				theEnabledController.onNext(isEnabled.createChangeEvent(false, true, null));
		}
	}

	/** @param listener The listener to remove from notification */
	public void removeListener(QuickActionListener listener) {
		if(isSealed)
			throw new SealedException(this);
		boolean fire = listener.isEnabled().get();
		if(fire) {
			for(QuickActionListener action : theListeners)
				if(action != listener && action.isEnabled().get()) {
					fire = false;
					break;
				}
		}
		theListeners.remove(listener);
		if(fire)
			theEnabledController.onNext(isEnabled.createChangeEvent(true, false, null));
	}

	@Override
	public boolean isSealed() {
		return isSealed;
	}

	@Override
	public void seal() {
		isSealed = true;
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		ret.append('{');
		for(int i = 0; i < theListeners.size(); i++) {
			ret.append(theListeners.get(i));
			if(i > 0)
				ret.append(", ");
		}
		ret.append('}');
		return ret.toString();
	}
}
