package org.muis.core.model;

import java.lang.reflect.*;

import org.muis.core.event.MuisEvent;

/** An implementation of MuisAppModel that wraps a POJO and makes its values and models available via reflection */
public class MuisWrappingModel implements MuisAppModel {
	private static final java.util.Set<Class<?>> VALUE_TYPES;

	static {
		java.util.Set<Class<?>> valueTypes = new java.util.HashSet<>();
		valueTypes.add(Byte.TYPE);
		valueTypes.add(Byte.class);
		valueTypes.add(Short.TYPE);
		valueTypes.add(Short.class);
		valueTypes.add(Integer.TYPE);
		valueTypes.add(Integer.class);
		valueTypes.add(Long.TYPE);
		valueTypes.add(Long.class);
		valueTypes.add(Float.TYPE);
		valueTypes.add(Float.class);
		valueTypes.add(Double.TYPE);
		valueTypes.add(Double.class);
		valueTypes.add(Boolean.TYPE);
		valueTypes.add(Boolean.class);
		valueTypes.add(Character.TYPE);
		valueTypes.add(Character.class);

		VALUE_TYPES = java.util.Collections.unmodifiableSet(valueTypes);
	}

	private final Object theWrapped;

	private final java.util.Map<String, Object> theData;

	/** @param wrap The POJO model to wrap */
	public MuisWrappingModel(Object wrap) {
		theWrapped = wrap;
		theData = new java.util.HashMap<>();
		buildReflectiveModel();
	}

	private void buildReflectiveModel() {
		for(Field f : theWrapped.getClass().getFields()) {
			if(!f.isAccessible() || (f.getModifiers() & Modifier.STATIC) != 0)
				continue;
			if(isValue(f, f.getType()))
				theData.put(f.getName(), new MuisMemberValue<>(theWrapped, f));
			else if(MuisWidgetModel.class.isAssignableFrom(f.getType()))
				theData.put(f.getName(), new MuisMemberAccessor<>(theWrapped, f));
			else if(isAppModel(f, f.getType()))
				theData.put(f.getName(), new MuisMemberAppModel<>(theWrapped, f));
		}
		for(Method m : theWrapped.getClass().getMethods()) {
			if(!m.isAccessible() || !m.getName().startsWith("get") || m.getParameterTypes().length > 0
				|| (m.getModifiers() & Modifier.STATIC) != 0)
				continue;
			String name = normalize(m.getName().substring(3));
			if(isValue(m, m.getReturnType()))
				theData.put(name, new MuisMemberValue<>(theWrapped, m));
			else if(MuisWidgetModel.class.isAssignableFrom(m.getReturnType()))
				theData.put(name, new MuisMemberAccessor<>(theWrapped, m));
			else if(isAppModel(m, m.getReturnType()))
				theData.put(name, new MuisMemberAppModel<>(theWrapped, m));
		}
	}

	@Override
	public MuisAppModel getSubModel(String name) {
		Object value = theData.get(name);
		if(!(value instanceof MuisAppModel))
			return null;
		return (MuisAppModel) value;
	}

	@Override
	public <T extends MuisWidgetModel> T getWidgetModel(String name, Class<T> modelType) {
		Object value = theData.get(name);
		if(!(value.getClass().equals(MuisMemberAccessor.class)))
			return null;
		MuisMemberAccessor<?> accessor = (MuisMemberAccessor<?>) value;
		if(modelType.isAssignableFrom(accessor.getType()))
			return ((MuisMemberAccessor<T>) accessor).get();
		else
			throw new ClassCastException("Widget model \"" + name + "\" is of type " + accessor.getType().getName() + ", not "
				+ modelType.getName());
	}

	@Override
	public <T> MuisModelValue<? extends T> getValue(String name, Class<T> type) throws ClassCastException {
		Object value = theData.get(name);
		if(!(value instanceof MuisModelValue))
			return null;
		MuisModelValue<?> modelValue = (MuisModelValue<?>) value;
		if(type.isAssignableFrom(modelValue.getType()))
			return (MuisModelValue<? extends T>) value;
		else
			throw new ClassCastException("Model value \"" + name + "\" is of type " + modelValue.getType().getName() + ", not "
				+ type.getName());
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

	private static boolean isValue(AccessibleObject member, Class<?> type) {
		if(VALUE_TYPES.contains(type))
			return true;
		else if(member.getAnnotation(MuisValue.class) != null)
			return true;
		else if(type.getAnnotation(MuisValue.class) != null)
			return true;
		else
			return false;
	}

	private static boolean isAppModel(AccessibleObject member, Class<?> type) {
	}

	private static class MuisMemberAccessor<T> implements WidgetRegister {
		private final Object theAppModel;

		private final Member theMember;

		MuisMemberAccessor(Object appModel, Member member) {
			theAppModel = appModel;
			theMember = member;
		}

		Class<T> getType() {
			if(theMember instanceof Field)
				return (Class<T>) ((Field) theMember).getType();
			else
				return (Class<T>) ((Method) theMember).getReturnType();
		}

		T get() {
			try {
				if(theMember instanceof Field)
					return (T) ((Field) theMember).get(theAppModel);
				else
					return (T) ((Method) theMember).invoke(theAppModel);
			} catch(IllegalArgumentException | IllegalAccessException e) {
				throw new IllegalStateException(e.getClass().getSimpleName() + " should not have been thrown here", e);
			} catch(InvocationTargetException e) {
				throw new IllegalStateException("Model's value getter threw exception", e);
			}
		}

		void set(T value) {
			// TODO
		}
	}

	private static class MuisMemberValue<T> extends MuisMemberAccessor<T> implements MuisModelValue<T> {
		MuisMemberValue(Object appModel, Member member) {
			super(appModel, member);
		}

		@Override
		public Class<T> getType() {
			return super.getType();
		}

		@Override
		public T get() {
			return super.get();
		}

		@Override
		public void set(T value, MuisEvent<?> event) {
			// TODO Auto-generated method stub

		}

		@Override
		public void addListener(MuisModelValueListener<? super T> listener) {
			// TODO Auto-generated method stub

		}

		@Override
		public void removeListener(MuisModelValueListener<? super T> listener) {
			// TODO Auto-generated method stub

		}
	}

	private static class MuisMemberAppModel<T> extends MuisMemberAccessor<T> implements MuisAppModel {
		MuisMemberAppModel(Object appModel, Member member) {
			super(appModel, member);
		}

		@Override
		public MuisAppModel getSubModel(String name) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T extends MuisWidgetModel> T getWidgetModel(String name, Class<T> modelType) throws ClassCastException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T> MuisModelValue<? extends T> getValue(String name, Class<T> type) throws ClassCastException {
			// TODO Auto-generated method stub
			return null;
		}

	}
}
