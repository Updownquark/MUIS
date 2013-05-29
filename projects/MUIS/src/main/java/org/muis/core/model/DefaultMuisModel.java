package org.muis.core.model;

import java.util.Map;

public class DefaultMuisModel implements MuisModel, prisms.util.Sealable {
	private boolean isSealed;

	private Map<String, MuisModel> theModels;

	private Map<String, MuisModelValue<?>> theValues;

	public DefaultMuisModel() {
		theModels = new java.util.HashMap<>(2);
		theValues = new java.util.HashMap<>(2);
	}

	@Override
	public boolean isSealed() {
		return isSealed;
	}

	@Override
	public void seal() {
		theModels = java.util.Collections.unmodifiableMap(theModels);
		theValues = java.util.Collections.unmodifiableMap(theValues);
		isSealed = true;
	}

	public Map<String, MuisModel> models() {
		return theModels;
	}

	public Map<String, MuisModelValue<?>> values() {
		return theValues;
	}

	@Override
	public MuisModel getModel(String name) {
		return theModels.get(name);
	}

	@Override
	public <T> MuisModelValue<? extends T> getValue(String name, Class<T> type) {
		MuisModelValue<?> value = theValues.get(name);
		if(type != null && !type.isAssignableFrom(value.getType()))
			throw new ClassCastException("Value \"" + name + "\" is type \"" + value.getType().getName() + "\", not \"" + type.getName()
				+ "\"");
		return (MuisModelValue<? extends T>) value;
	}
}
