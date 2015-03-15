package org.muis.core.model;

import java.lang.reflect.*;
import java.util.List;

import org.muis.core.MuisElement;
import org.muis.core.event.UserEvent;
import org.muis.rx.ObservableValue;
import org.muis.rx.ObservableValueEvent;
import org.muis.rx.Observer;
import org.muis.rx.SettableValue;

import prisms.lang.Type;

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

	private final org.muis.core.mgr.MuisMessageCenter theMessageCenter;

	private final Getter<?> theWrapped;

	private final java.util.Map<String, Object> theData;

	private final java.util.Map<String, AggregateActionListener> theActions;

	/**
	 * @param wrap The POJO model getter to wrap
	 * @param msg The message center to give errors and other messages to
	 */
	public MuisWrappingModel(Getter<?> wrap, org.muis.core.mgr.MuisMessageCenter msg) {
		theMessageCenter = msg;
		theWrapped = wrap;
		theData = new java.util.HashMap<>(5);
		theActions = new java.util.HashMap<>(2);
		buildReflectiveModel();
	}

	/**
	 * @param wrap The POJO model to wrap
	 * @param msg The message center to give errors and other messages to
	 */
	public MuisWrappingModel(Object wrap, org.muis.core.mgr.MuisMessageCenter msg) {
		this(new Getter<Object>() {
			@Override
			public Class<Object> getType() {
				return (Class<Object>) wrap.getClass();
			}

			@Override
			public Object get() throws IllegalStateException {
				return wrap;
			}
		}, msg);
	}

	private void buildReflectiveModel() {
		for(Field f : theWrapped.getType().getFields()) {
			if((f.getModifiers() & Modifier.STATIC) != 0 || (f.getModifiers() & Modifier.PUBLIC) == 0)
				continue;
			try {
				if(isValue(f, f.getType()))
					theData.put(f.getName(), new MuisMemberValue<>(theWrapped, f));
				if(MuisWidgetModel.class.isAssignableFrom(f.getType()))
					theData.put(f.getName(), new MuisMemberAccessor<>(theWrapped, f));
				if(MuisAppModel.class.isAssignableFrom(f.getType()))
					try {
						theData.put(f.getName(), f.get(theWrapped.get()));
					} catch(Exception e) {
						theMessageCenter.error("Muis sub model field " + f.getName() + " cannot be used", e);
					}
				else if(isAppModel(f, f.getType()))
					try {
						theData.put(f.getName(), new MuisWrappingModel(new MemberGetter<>(theWrapped, f), theMessageCenter));
					} catch(IllegalArgumentException e) {
						if(f.getAnnotation(MuisSubModel.class) != null)
							theMessageCenter.error("Tagged sub model field " + f.getName() + " cannot be used", e);
					}
			} catch(SecurityException e) {
				theMessageCenter.warn("Could not gain access to field " + f, e);
			}
		}
		for(Method m : theWrapped.getType().getMethods()) {
			if((m.getModifiers() & Modifier.STATIC) != 0 || (m.getModifiers() & Modifier.PUBLIC) == 0)
				continue;
			try {
				if(m.getName().startsWith("get") && m.getParameterTypes().length == 0 && m.getDeclaringClass() != Object.class) {
					String name = normalize(m.getName().substring(3));
					if(isValue(m, m.getReturnType()))
						theData.put(name, new MuisMemberValue<>(theWrapped, m));
					if(MuisWidgetModel.class.isAssignableFrom(m.getReturnType()))
						theData.put(name, new MuisMemberAccessor<>(theWrapped, m));
					if(MuisAppModel.class.isAssignableFrom(m.getReturnType()))
						try {
							theData.put(name, m.invoke(theWrapped.get()));
						} catch(Exception e) {
							theMessageCenter.error("Muis sub model method " + m.getName() + " cannot be used", e);
						}
					else if(isAppModel(m, m.getReturnType()))
						try {
							theData.put(name, new MuisWrappingModel(new MemberGetter<>(theWrapped, m), theMessageCenter));
						} catch(IllegalArgumentException e) {
							if(m.getAnnotation(MuisSubModel.class) != null)
								theMessageCenter.error("Tagged sub model method " + m.getName() + " cannot be used", e);
						}
				}
				if(isAction(m)) {
					MuisAction action = m.getAnnotation(MuisAction.class);
					MethodActionListener mal = new MethodActionListener(theWrapped, m);
					if(action != null && action.actions().length > 0) {
						for(String ac : action.actions())
							addActionListener(ac, mal);
					} else
						addActionListener(m.getName(), mal);
				}
			} catch(SecurityException e) {
				theMessageCenter.warn("Could not gain access to method " + m, e);
			}
		}
	}

	/** @return The POJO model object that this wraps */
	public Object getWrapped() {
		return theWrapped.get();
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
		if(accessor.getType().canAssignTo(modelType))
			return ((MuisMemberAccessor<T>) accessor).get();
		else
			throw new ClassCastException("Widget model \"" + name + "\" is of type " + accessor.getType().getName() + ", not "
				+ modelType.getName());
	}

	@Override
	public <T> ObservableValue<? extends T> getValue(String name, Class<T> type) throws ClassCastException {
		Object value = theData.get(name);
		if(!(value instanceof ObservableValue))
			return null;
		ObservableValue<?> modelValue = (ObservableValue<?>) value;
		if(modelValue.getType().canAssignTo(type))
			return (ObservableValue<? extends T>) value;
		else
			throw new ClassCastException("Model value \"" + name + "\" is of type " + modelValue.getType().getName() + ", not "
				+ type.getName());
	}

	@Override
	public MuisActionListener getAction(String name) {
		return theActions.get(name);
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

	private boolean isAppModel(AccessibleObject member, Class<?> type) {
		if(MuisAppModel.class.isAssignableFrom(type))
			return true;
		else if(member.getAnnotation(MuisSubModel.class) != null)
			return true;
		else if(type.getAnnotation(MuisSubModel.class) != null)
			return true;
		else
			return false;
	}

	private boolean isAction(Method m) {
		if(m.getName().startsWith("set"))
			return false;
		if(!Void.TYPE.equals(m.getReturnType()))
			return false;
		if(m.getDeclaringClass() == Object.class)
			return false;
		for(Class<?> type : m.getParameterTypes()) {
			if(MuisActionEvent.class.equals(type))
				continue;
			if(String.class.equals(type))
				continue;
			if(UserEvent.class.equals(type))
				continue;
			if(MuisElement.class.equals(type))
				continue;
			return false;
		}
		return true;
	}

	private void addActionListener(String action, MuisActionListener listener) {
		AggregateActionListener agg = theActions.get(action);
		if(agg == null) {
			agg = new AggregateActionListener();
			theActions.put(action, agg);
		}
		agg.addListener(listener);
	}

	private class MuisMemberAccessor<T> implements WidgetRegister {
		private final Getter<?> theAppModel;

		private final String theName;

		private final Member theFieldGetter;

		private Method theFieldSetter;

		private List<MuisElement> theRegisteredElements;

		MuisMemberAccessor(Getter<?> appModel, Member member) {
			if(!((AccessibleObject) member).isAccessible())
				((AccessibleObject) member).setAccessible(true);
			theAppModel = appModel;
			theFieldGetter = member;
			if(theFieldGetter instanceof Method) {
				theName = normalize(theFieldGetter.getName().substring(3));
				String setterName;
				MuisValue mvAnn = ((Method) theFieldGetter).getAnnotation(MuisValue.class);
				if(mvAnn != null && mvAnn.setter().length() > 0)
					setterName = mvAnn.setter();
				else
					setterName = "set" + theFieldGetter.getName().substring(3);
				try {
					theFieldSetter = theFieldGetter.getDeclaringClass().getMethod(setterName, ((Method) theFieldGetter).getReturnType());
				} catch(NoSuchMethodException | SecurityException e) {
					if(mvAnn != null)
						theMessageCenter.error("MUIS value \"" + theName + "\"'s tagged setter method, \"" + mvAnn.setter()
							+ "\" does not exist or cannot be accessed", e);
					theFieldSetter = null;
				}
				if(!theFieldSetter.isAccessible() || (theFieldSetter.getModifiers() & Modifier.PUBLIC) == 0
					|| (theFieldSetter.getModifiers() & Modifier.STATIC) != 0) {
					if(mvAnn != null)
						theMessageCenter.error("MUIS value \"" + theName + "\"'s tagged setter method, \"" + mvAnn.setter()
							+ "\" cannot be accessed");
					theFieldSetter = null;
				}
			} else
				theName = theFieldGetter.getName();
			theRegisteredElements = new java.util.concurrent.CopyOnWriteArrayList<>();
		}

		prisms.lang.Type getType() {
			if(theFieldGetter instanceof Field)
				return new prisms.lang.Type(((Field) theFieldGetter).getType());
			else
				return new prisms.lang.Type(((Method) theFieldGetter).getReturnType());
		}

		@SuppressWarnings("unused")
		String getName() {
			return theName;
		}

		T get() {
			try {
				if(theFieldGetter instanceof Field)
					return (T) ((Field) theFieldGetter).get(theAppModel.get());
				else
					return (T) ((Method) theFieldGetter).invoke(theAppModel.get());
			} catch(IllegalArgumentException | IllegalAccessException e) {
				theMessageCenter.error(e.getClass().getSimpleName() + " should not have been thrown here", e);
				throw new IllegalStateException(e.getClass().getSimpleName() + " should not have been thrown here", e);
			} catch(InvocationTargetException e) {
				theMessageCenter.error("Model's value getter threw exception", e);
				throw new IllegalStateException("Model's value getter threw exception", e);
			}
		}

		boolean isMutable() {
			if(theFieldSetter != null)
				return true;
			else if(theFieldGetter instanceof Field && (theFieldGetter.getModifiers() & Modifier.FINAL) == 0)
				return true;
			else
				return false;
		}

		<V extends T> String isAcceptable(V value) {
			if(!isMutable())
				return "MUIS value \"" + theName + "\" is not mutable";
			if(value == null)
				return null;
			Type type;
			if(theFieldSetter != null)
				type = new Type(theFieldSetter.getGenericParameterTypes()[0]);
			else
				type = new Type(((Field) theFieldGetter).getGenericType());
			if(!type.isAssignableFrom(value.getClass()))
				return "Value of type " + value.getClass().getName() + " cannot be assigned to model of type " + type;
			return null;
		}

		void set(T value) {
			if(!isMutable())
				throw new IllegalStateException("MUIS value \"" + theName + "\" is not mutable");
			try {
				if(theFieldSetter != null)
					theFieldSetter.invoke(theAppModel.get(), value);
				else
					((Field) theFieldGetter).set(theAppModel.get(), value);
			} catch(IllegalAccessException e) {
				theMessageCenter.error("Could not access setter for MUIS value \"" + theName + "\"", e);
			} catch(IllegalArgumentException e) {
				theMessageCenter.error("Illegal argument for MUIS value \"" + theName + "\"", e);
				throw e;
			} catch(InvocationTargetException e) {
				theMessageCenter.error("Setter for MUIS value \"" + theName + "\" threw exception", e);
				throw new IllegalStateException("Setter for MUIS value \"" + theName + "\" threw exception", e);
			}
		}

		@Override
		public WidgetRegistration register(final MuisElement widget) {
			if(widget == null)
				return null;
			theRegisteredElements.add(widget);
			return new WidgetRegistration() {
				@Override
				public void unregister() {
					theRegisteredElements.remove(widget);
				}
			};
		}

		@Override
		public List<MuisElement> registered() {
			return java.util.Collections.unmodifiableList(theRegisteredElements);
		}
	}

	private class MuisMemberValue<T> extends MuisMemberAccessor<T> implements SettableValue<T> {
		private List<Observer<? super ObservableValueEvent<T>>> theListeners;

		MuisMemberValue(Getter<?> appModel, Member member) {
			super(appModel, member);
			theListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
		}

		@Override
		public prisms.lang.Type getType() {
			return super.getType();
		}

		@Override
		public T get() {
			return super.get();
		}

		@Override
		public boolean isMutable() {
			return super.isMutable();
		}

		@Override
		public ObservableValue<Boolean> isEnabled() {
			return ObservableValue.constant(isMutable());
		}

		@Override
		public <V extends T> String isAcceptable(V value) {
			return super.isAcceptable(value);
		}

		@Override
		public <V extends T> MuisMemberValue<T> set(V value, Object cause) throws IllegalArgumentException {
			T oldValue = get();
			super.set(value);
			ObservableValueEvent<T> valueEvent = new ObservableValueEvent<>(this, oldValue, value, cause);
			for(Observer<? super ObservableValueEvent<T>> listener : theListeners)
				listener.onNext(valueEvent);
			return this;
		}

		@Override
		public Runnable internalSubscribe(Observer<? super ObservableValueEvent<T>> observer) {
			theListeners.add(observer);
			return () -> {
				theListeners.remove(observer);
			};
		}
	}

	private class MethodActionListener implements MuisActionListener {
		private Getter<?> theAppModel;

		private Method theMethod;

		MethodActionListener(Getter<?> appModel, Method getter) {
			if(!((AccessibleObject) getter).isAccessible())
				((AccessibleObject) getter).setAccessible(true);
			theAppModel = appModel;
			theMethod = getter;
		}

		@Override
		public void actionPerformed(MuisActionEvent event) {
			Object [] params = new Object[theMethod.getParameterTypes().length];
			for(int p = 0; p < params.length; p++) {
				if(theMethod.getParameterTypes()[p] == MuisActionEvent.class)
					params[p] = event;
				else if(theMethod.getParameterTypes()[p] == String.class)
					params[p] = event.getAction();
				else if(theMethod.getParameterTypes()[p] == UserEvent.class)
					params[p] = event.getUserEvent();
				else if(theMethod.getParameterTypes()[p] == MuisElement.class && event.getUserEvent() != null)
					params[p] = event.getUserEvent().getElement();
			}
			try {
				theMethod.invoke(theAppModel.get(), params);
			} catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException | IllegalStateException e) {
				theMessageCenter.error("Could not invoke action listener method " + theMethod, e);
			}
		}
	}
}
