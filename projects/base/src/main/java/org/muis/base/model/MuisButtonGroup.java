package org.muis.base.model;

import org.muis.core.event.UserEvent;
import org.muis.core.model.*;

/**
 * A model that manages a set of boolean-valued widgets, such as toggle buttons or radio buttons, only one of which may be selected at a
 * time
 */
public class MuisButtonGroup implements MuisAppModel, MuisModelValue<String> {
	private String theValue;

	private java.util.Map<String, CaseModelValue> theButtonValues;

	private java.util.List<MuisModelValueListener<String>> theListeners;

	/** Creates the button group */
	public MuisButtonGroup() {
		theListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
		theButtonValues = new java.util.concurrent.ConcurrentHashMap<>(4);
	}

	@Override
	public MuisAppModel getSubModel(String name) {
		return null;
	}

	@Override
	public <T extends MuisWidgetModel> T getWidgetModel(String name, Class<T> modelType) throws ClassCastException {
		return null;
	}

	@Override
	public <T> MuisModelValue<? extends T> getValue(String name, Class<T> type) throws ClassCastException {
		if(name.equals("value"))
			return (MuisModelValue<T>) this;
		else {
			CaseModelValue btnValue = theButtonValues.get(name);
			if(btnValue == null) {
				btnValue = new CaseModelValue(name);
				theButtonValues.put(name, btnValue);
			}
			return (MuisModelValue<T>) btnValue;
		}
	}

	@Override
	public MuisActionListener getAction(String name) {
		return null;
	}

	@Override
	public Class<String> getType() {
		return String.class;
	}

	@Override
	public String get() {
		return theValue;
	}

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	public void set(String value, UserEvent event) throws IllegalStateException {
		if(!theButtonValues.containsKey(value))
			throw new IllegalStateException("\"" + value + "\" is not a valid value for this model value");
		String oldValue = theValue;
		theValue = value;
		MuisModelValueEvent<String> modelEvt = new MuisModelValueEvent<>(this, event, oldValue, theValue);
		for(MuisModelValueListener<String> listener : theListeners)
			listener.valueChanged(modelEvt);
		for(CaseModelValue buttonModel : theButtonValues.values())
			buttonModel.fireChange(oldValue, theValue, event);
	}

	@Override
	public void addListener(MuisModelValueListener<? super String> listener) {
		if(listener != null)
			theListeners.add((MuisModelValueListener<String>) listener);
	}

	@Override
	public void removeListener(MuisModelValueListener<?> listener) {
		theListeners.remove(listener);
	}

	/** @return All values that have models in this button group */
	public java.util.Set<String> getValues() {
		return java.util.Collections.unmodifiableSet(theButtonValues.keySet());
	}

	private class CaseModelValue implements MuisModelValue<Boolean> {
		private final String theCaseValue;

		private java.util.List<MuisModelValueListener<Boolean>> theButtonListeners;

		CaseModelValue(String caseValue) {
			theCaseValue = caseValue;
			theButtonListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
		}

		@Override
		public Class<Boolean> getType() {
			return Boolean.class;
		}

		@Override
		public Boolean get() {
			return theCaseValue.equals(theValue);
		}

		@Override
		public boolean isMutable() {
			return true;
		}

		@Override
		public void set(Boolean value, UserEvent event) throws IllegalStateException {
			if(!value.equals(get()))
				MuisButtonGroup.this.set(theCaseValue, event);
		}

		@Override
		public void addListener(MuisModelValueListener<? super Boolean> listener) {
			if(listener != null)
				theButtonListeners.add((MuisModelValueListener<Boolean>) listener);
		}

		@Override
		public void removeListener(MuisModelValueListener<?> listener) {
			theButtonListeners.remove(listener);
		}

		void fireChange(String oldValue, String newValue, UserEvent cause) {
			MuisModelValueEvent<Boolean> buttonEvent = new MuisModelValueEvent<>(this, cause, theCaseValue.equals(oldValue),
				theCaseValue.equals(theValue));
			for(MuisModelValueListener<Boolean> listener : theButtonListeners)
				listener.valueChanged(buttonEvent);
		}
	}
}
