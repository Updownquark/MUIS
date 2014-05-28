package org.muis.base.model;

import org.muis.core.event.UserEvent;
import org.muis.core.model.*;
import org.muis.core.rx.DefaultObservableValue;
import org.muis.core.rx.ObservableValueEvent;
import org.muis.core.rx.Observer;

import prisms.lang.Type;

/**
 * A model that manages a set of boolean-valued widgets, such as toggle buttons or radio buttons, only one of which may be selected at a
 * time
 */
public class MuisButtonGroup extends DefaultObservableValue<String> implements MuisAppModel, MuisModelValue<String> {
	private volatile String theValue;

	private java.util.Map<String, CaseModelValue> theButtonValues;

	private final Observer<ObservableValueEvent<String>> theController;

	/** Creates the button group */
	public MuisButtonGroup() {
		theButtonValues = new java.util.concurrent.ConcurrentHashMap<>(4);
		theController = control(null);
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
		if(name.equals("value")) {
			if(!type.isAssignableFrom(String.class))
				throw new IllegalArgumentException("This value is of type String--not " + type.getName());
			return (MuisModelValue<T>) this;
		} else {
			if(!type.isAssignableFrom(Boolean.class) && !type.isAssignableFrom(Boolean.TYPE))
				throw new IllegalArgumentException("This value is of type Boolean--not " + type.getName());
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
	public Type getType() {
		return new Type(String.class);
	}

	@Override
	public String get() {
		return theValue;
	}

	@Override
	public void set(String value, UserEvent event) throws IllegalStateException {
		if(!theButtonValues.containsKey(value))
			throw new IllegalStateException("\"" + value + "\" is not a valid value for this model value");
		String oldValue = theValue;
		theValue = value;
		MuisModelValueEvent<String> modelEvt = new MuisModelValueEvent<>(this, event, oldValue, theValue);
		theController.onNext(modelEvt);
		for(CaseModelValue buttonModel : theButtonValues.values())
			buttonModel.fireChange(oldValue, theValue, event);
	}

	/** @return All values that have models in this button group */
	public java.util.Set<String> getValues() {
		return java.util.Collections.unmodifiableSet(theButtonValues.keySet());
	}

	private class CaseModelValue extends DefaultObservableValue<Boolean> implements MuisModelValue<Boolean> {
		private final String theCaseValue;

		private final Observer<ObservableValueEvent<Boolean>> theCaseController;

		CaseModelValue(String caseValue) {
			theCaseValue = caseValue;
			theCaseController = control(null);
		}

		@Override
		public Type getType() {
			return new Type(Boolean.class);
		}

		@Override
		public Boolean get() {
			return theCaseValue.equals(theValue);
		}

		@Override
		public void set(Boolean value, UserEvent event) throws IllegalStateException {
			if(!value.equals(get()))
				MuisButtonGroup.this.set(theCaseValue, event);
		}

		void fireChange(String oldValue, String newValue, UserEvent cause) {
			MuisModelValueEvent<Boolean> buttonEvent = new MuisModelValueEvent<>(this, cause, theCaseValue.equals(oldValue),
				theCaseValue.equals(theValue));
			theCaseController.onNext(buttonEvent);
		}
	}
}
