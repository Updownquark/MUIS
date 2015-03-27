package org.muis.core.model;

import org.observe.DefaultObservableValue;
import org.observe.ObservableValue;
import org.observe.ObservableValueEvent;
import org.observe.Observer;

import prisms.lang.Type;

/** A MUIS action listener that propagates the events it gets to one or more other listeners */
public class AggregateActionListener implements MuisActionListener, prisms.util.Sealable {
	private boolean isSealed;

	private java.util.List<MuisActionListener> theListeners;

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
				for(MuisActionListener listener : theListeners)
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
	public void actionPerformed(MuisActionEvent event) {
		for(MuisActionListener listener : theListeners)
			listener.actionPerformed(event);
	}

	/** @param listener The listener to be notified when this aggregate listener receives an action event */
	public void addListener(MuisActionListener listener) {
		if(isSealed)
			throw new SealedException(this);
		if(listener != null) {
			boolean oldEnabled = isEnabled.get();
			theListeners.add(listener);
			if(!oldEnabled && listener.isEnabled().get())
				theEnabledController.onNext(new ObservableValueEvent<>(isEnabled, false, true, null));
		}
	}

	/** @param listener The listener to remove from notification */
	public void removeListener(MuisActionListener listener) {
		if(isSealed)
			throw new SealedException(this);
		boolean fire = listener.isEnabled().get();
		if(fire) {
			for(MuisActionListener action : theListeners)
				if(action != listener && action.isEnabled().get()) {
					fire = false;
					break;
				}
		}
		theListeners.remove(listener);
		if(fire)
			theEnabledController.onNext(new ObservableValueEvent<>(isEnabled, true, false, null));
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
