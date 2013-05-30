package org.muis.core.model;

/** An implementation of MuisAppModel that wraps a POJO and makes its values and models available via reflection */
public class MuisWrappingModel implements MuisAppModel {
	private final Object theWrapped;

	private final java.util.Map<String, Object> theData;

	/** @param wrap The POJO model to wrap */
	public MuisWrappingModel(Object wrap) {
		theWrapped = wrap;
		theData = new java.util.HashMap<>();
		buildReflectiveModel();
	}

	private void buildReflectiveModel() {
		for(java.lang.reflect.Field f : theWrapped.getClass().getFields()) {

		}
		// TODO
	}

	@Override
	public MuisAppModel getSubModel(String name) {
		return get(name, MuisAppModel.class);
	}

	@Override
	public <T extends MuisWidgetModel> T getWidgetModel(String name, Class<T> modelType) {
		MuisWidgetModel model = get(name, MuisWidgetModel.class);
		if(model == null)
			return null;
		else if(modelType.isInstance(model))
			return modelType.cast(model);
		else
			throw new ClassCastException("Widget model \"" + name + "\" is of type " + model.getClass().getName() + ", not "
				+ modelType.getName());
	}

	@Override
	public <T> MuisModelValue<? extends T> getValue(String name, Class<T> type) throws ClassCastException {
		MuisModelValue<?> value = get(name, MuisModelValue.class);
		if(value == null)
			return null;
		else if(type.isAssignableFrom(value.getType()))
			return (MuisModelValue<? extends T>) value;
		else
			throw new ClassCastException("Model value \"" + name + "\" is of type " + value.getType().getName() + ", not " + type.getName());
	}

	private <T> T get(String name, Class<T> type) {
		Object value = theData.get(normalize(name));
		if(type.isInstance(value))
			return type.cast(value);
		else
			return null;
	}

	private String normalize(String name) {
		if(name.length() == 0)
			return name;
		StringBuilder ret = new StringBuilder(name);
		if(Character.isUpperCase(ret.charAt(0)))
			ret.setCharAt(0, Character.toLowerCase(ret.charAt(0)));

		for(int i = 1; i < ret.length() - 1; i++)
			if(ret.charAt(i) == '-' || ret.charAt(i) == ' ' && Character.isLetter(ret.charAt(i))) {
				ret.deleteCharAt(i);
				if(Character.isLowerCase(ret.charAt(i)))
					ret.setCharAt(i, Character.toLowerCase(ret.charAt(i)));
			}
		return ret.toString();
	}
}
