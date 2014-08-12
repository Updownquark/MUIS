package org.muis.base.model;

import org.muis.core.model.MuisActionListener;
import org.muis.core.model.MuisAppModel;
import org.muis.core.model.MuisWidgetModel;
import org.muis.core.rx.*;

import prisms.lang.Type;

/**
 * A model that manages a set of boolean-valued widgets, such as toggle buttons or radio buttons, only one of which may be selected at a
 * time
 */
public class MuisButtonGroup extends DefaultObservableValue<String> implements MuisAppModel, SettableValue<String> {
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
	public <T> SettableValue<? extends T> getValue(String name, Class<T> type) throws ClassCastException {
		if(name.equals("value")) {
			if(type != null && !type.isAssignableFrom(String.class))
				throw new IllegalArgumentException("This value is of type String--not " + type.getName());
			return (SettableValue<T>) this;
		} else {
			if(type != null && !type.isAssignableFrom(Boolean.class) && !type.isAssignableFrom(Boolean.TYPE))
				throw new IllegalArgumentException("This value is of type Boolean--not " + type.getName());
			CaseModelValue btnValue = theButtonValues.get(name);
			if(btnValue == null) {
				btnValue = new CaseModelValue(name);
				theButtonValues.put(name, btnValue);
			}
			return (SettableValue<T>) btnValue;
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
	public String isAcceptable(String value) {
		if(!theButtonValues.containsKey(value))
			return "\"" + value + "\" is not a valid value for this model value";
		return null;
	}

	@Override
	public MuisButtonGroup set(String value, Object cause) throws IllegalStateException {
		if(!theButtonValues.containsKey(value))
			throw new IllegalStateException("\"" + value + "\" is not a valid value for this model value");
		String oldValue = theValue;
		theValue = value;
		ObservableValueEvent<String> modelEvt = new ObservableValueEvent<>(this, oldValue, theValue, cause);
		theController.onNext(modelEvt);
		for(CaseModelValue buttonModel : theButtonValues.values())
			buttonModel.fireChange(oldValue, theValue, cause);
		return this;
	}

	/** @return All values that have models in this button group */
	public java.util.Set<String> getValues() {
		return java.util.Collections.unmodifiableSet(theButtonValues.keySet());
	}

	private class CaseModelValue extends DefaultObservableValue<Boolean> implements SettableValue<Boolean> {
		private final String theCaseValue;

		private final Observer<ObservableValueEvent<Boolean>> theCaseController;

		CaseModelValue(String caseValue) {
			theCaseValue = caseValue;
			theCaseController = control(null);
		}

		@Override
		public Type getType() {
			return new Type(Boolean.TYPE);
		}

		@Override
		public Boolean get() {
			return theCaseValue.equals(theValue);
		}

		@Override
		public CaseModelValue set(Boolean value, Object event) throws IllegalStateException {
			if(!value.equals(get()))
				MuisButtonGroup.this.set(theCaseValue, event);
			return this;
		}

		@Override
		public String isAcceptable(Boolean value) {
			return null;
		}

		void fireChange(String oldValue, String newValue, Object cause) {
			ObservableValueEvent<Boolean> buttonEvent = new ObservableValueEvent<>(this, theCaseValue.equals(oldValue),
				theCaseValue.equals(theValue), cause);
			theCaseController.onNext(buttonEvent);
		}
	}
}
