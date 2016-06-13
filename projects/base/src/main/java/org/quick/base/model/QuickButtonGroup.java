package org.quick.base.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.observe.*;
import org.quick.core.model.QuickAppModel;

import com.google.common.reflect.TypeToken;

/**
 * A model that manages a set of boolean-valued widgets, such as toggle buttons or radio buttons, only one of which may be selected at a
 * time
 */
public class QuickButtonGroup<T> implements QuickAppModel {
	private final Map<String, CaseModelValue> theButtonValues;
	private final Map<T, String> theCaseValues;
	private final boolean isConstrained;

	private final DefaultSettableValue<T> theSettableValue;
	private final Observer<ObservableValueEvent<T>> theController;
	private final T theDefaultValue;
	private volatile T theValue;

	/** Creates the button group */
	private QuickButtonGroup(TypeToken<T> type, T defValue, Map<String, T> caseValues, boolean constrain) {
		theButtonValues = new java.util.concurrent.ConcurrentHashMap<>(4);
		isConstrained = constrain;
		theDefaultValue=defValue;
		theValue = defValue;

		theSettableValue = new DefaultSettableValue<T>() {
			@Override
			public <V extends T> T set(V value, Object cause) throws IllegalArgumentException {
				String accept = isAcceptable(value);
				if (accept != null)
					throw new IllegalArgumentException(accept);
				T old = theValue;
				theValue = value;
				theController.onNext(createChangeEvent(old, value, cause));
				return old;
			}

			@Override
			public <V extends T> String isAcceptable(V value) {
				if (!isConstrained || (!Objects.equals(theDefaultValue, value) && theCaseValues.containsKey(value)))
					return null;
				else
					return "Value "+value+" is not valid for this value";
			}

			@Override
			public ObservableValue<Boolean> isEnabled() {
				return ObservableValue.constant(true);
			}

			@Override
			public TypeToken<T> getType() {
				return type;
			}

			@Override
			public T get() {
				return theValue;
			}
		};
		theController = theSettableValue.control(null);

		Map<String, CaseModelValue> buttonValues=new LinkedHashMap<>();
		Map<T, String> cvs=new LinkedHashMap<>();
		for(Map.Entry<String, T> caseValue : caseValues.entrySet()){
			cvs.put(caseValue.getValue(), caseValue.getKey());
			buttonValues.put(caseValue.getKey(), new CaseModelValue(caseValue.getValue()));
		}
	}

	/** @return All values that have models in this button group */
	public java.util.Set<String> getValues() {
		return java.util.Collections.unmodifiableSet(theButtonValues.keySet());
	}

	private class CaseModelValue implements SettableValue<Boolean> {
		private final T theCaseValue;

		CaseModelValue(T caseValue) {
			theCaseValue = caseValue;
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
			String accept = isAcceptable(value);
			if (accept != null)
				throw new IllegalArgumentException(accept);
			if(!value.equals(get())){
				if(value)
					theSettableValue.set(theCaseValue, event);
				else
					theSettableValue.set(theDefaultValue, event);
			}
			return !value;
		}

		@Override
		public String isAcceptable(Boolean value) {
			if (!value && Objects.equals(theCaseValue, theDefaultValue))
				return "This value cannot be un-set directly";
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
