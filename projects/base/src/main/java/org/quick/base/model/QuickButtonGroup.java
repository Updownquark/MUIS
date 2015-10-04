package org.quick.base.model;

import org.observe.*;
import org.quick.core.model.QuickActionListener;
import org.quick.core.model.QuickAppModel;
import org.quick.core.model.QuickWidgetModel;

import com.google.common.reflect.TypeToken;

/**
 * A model that manages a set of boolean-valued widgets, such as toggle buttons or radio buttons, only one of which may be selected at a
 * time
 */
public class QuickButtonGroup extends DefaultObservableValue<String> implements QuickAppModel, SettableValue<String> {
	private volatile String theValue;

	private java.util.Map<String, CaseModelValue> theButtonValues;

	private final Observer<ObservableValueEvent<String>> theController;

	/** Creates the button group */
	public QuickButtonGroup() {
		theButtonValues = new java.util.concurrent.ConcurrentHashMap<>(4);
		theController = control(null);
	}

	@Override
	public QuickAppModel getSubModel(String name) {
		return null;
	}

	@Override
	public <T extends QuickWidgetModel> T getWidgetModel(String name, Class<T> modelType) throws ClassCastException {
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
	public QuickActionListener getAction(String name) {
		return null;
	}

	@Override
	public TypeToken<String> getType() {
		return TypeToken.of(String.class);
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
	public ObservableValue<Boolean> isEnabled() {
		return ObservableValue.constant(true);
	}

	@Override
	public String set(String value, Object cause) throws IllegalStateException {
		if(!theButtonValues.containsKey(value))
			throw new IllegalStateException("\"" + value + "\" is not a valid value for this model value");
		String oldValue = theValue;
		theValue = value;
		ObservableValueEvent<String> modelEvt = createChangeEvent(oldValue, theValue, cause);
		theController.onNext(modelEvt);
		for(CaseModelValue buttonModel : theButtonValues.values())
			buttonModel.fireChange(oldValue, theValue, cause);
		return oldValue;
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
		public TypeToken<Boolean> getType() {
			return TypeToken.of(Boolean.TYPE);
		}

		@Override
		public Boolean get() {
			return theCaseValue.equals(theValue);
		}

		@Override
		public Boolean set(Boolean value, Object event) throws IllegalStateException {
			if(!value.equals(get()))
				QuickButtonGroup.this.set(theCaseValue, event);
			return !value;
		}

		@Override
		public String isAcceptable(Boolean value) {
			return null;
		}

		@Override
		public ObservableValue<Boolean> isEnabled() {
			return QuickButtonGroup.this.isEnabled();
		}

		void fireChange(String oldValue, String newValue, Object cause) {
			ObservableValueEvent<Boolean> buttonEvent = createChangeEvent(theCaseValue.equals(oldValue), theCaseValue.equals(theValue),
				cause);
			theCaseController.onNext(buttonEvent);
		}
	}
}
