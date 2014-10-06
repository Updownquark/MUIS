package org.muis.core.style.attach;

import java.util.Collection;

import org.muis.core.MuisAttribute;
import org.muis.core.MuisException;
import org.muis.core.MuisParseEnv;
import org.muis.core.MuisProperty;
import org.muis.core.rx.ObservableValue;

import prisms.lang.Type;

/** Parses an attribute as a comma-separated list of strings to be applied as named style groups on an element */
public class GroupPropertyType implements MuisProperty.PrintablePropertyType<String []> {
	/** Avoids having to instantiate multiple copies */
	public static final GroupPropertyType instance = new GroupPropertyType();

	/** The group attribute */
	public static final MuisAttribute<String []> attribute = new MuisAttribute<>("group", instance);

	@Override
	public Type getType() {
		return new Type(String [].class);
	}

	@Override
	public boolean canCast(Type type) {
		return type.canAssignTo(String.class) || type.isArray() && type.getComponentType().canAssignTo(String.class);
	}

	@Override
	public String [] cast(Object value) {
		if(value instanceof String [])
			return (String []) value;
		else if(value instanceof String)
			return new String[] {(String) value};
		return null;
	}

	@Override
	public ObservableValue<String []> parse(MuisParseEnv env, String value) throws MuisException {
		ObservableValue<?> ret = MuisProperty.parseExplicitObservable(env, value, false);
		if(ret != null) {
			if(ret.getType().canAssignTo(ObservableValue.class))
				ret = ObservableValue.flatten(null, (ObservableValue<? extends ObservableValue<?>>) ret);
			if(ret.getType().canAssignTo(String [].class)) {
				return (ObservableValue<String []>) ret;
			} else if(ret.getType().canAssignTo(CharSequence [].class)) {
				return ((ObservableValue<CharSequence []>) ret).mapV(seq -> {
					String [] str = new String[seq.length];
					for(int i = 0; i < seq.length; i++)
						str[i] = seq[i].toString();
					return str;
				});
			} else if(new Type(Collection.class, new Type(String.class)).isAssignable(ret.getType())) {
				return ((ObservableValue<Collection<String>>) ret).mapV(seq -> {
					return seq.toArray(new String[seq.size()]);
				});
			} else if(new Type(Collection.class, new Type(new Type(CharSequence.class), true)).isAssignable(ret.getType())) {
				return ((ObservableValue<Collection<? extends CharSequence>>) ret).mapV(seq -> {
					String [] str = new String[seq.size()];
					int i = 0;
					for(CharSequence s : seq)
						str[i++] = s.toString();
					return str;
				});
			} else if(ret.getType().canAssignTo(CharSequence.class)) {
				return ((ObservableValue<? extends CharSequence>) ret).mapV(seq -> {
					return new String[] {seq.toString()};
				});
			} else if(ret != null)
				throw new MuisException("Model value " + value + " is not of type string: " + ret.getType());
		}

		String [] split = value.split(",");
		ObservableValue<?> [] splitObs = new ObservableValue[split.length];
		for(int i = 0; i < split.length; i++) {
			split[i] = split[i].trim();
			splitObs[i] = MuisProperty.parseExplicitObservable(env, split[i], false);
			if(splitObs[i] != null) {
				if(splitObs[i].getType().canAssignTo(ObservableValue.class))
					splitObs[i] = ObservableValue.flatten(null, (ObservableValue<? extends ObservableValue<?>>) splitObs[i]);
				if(splitObs[i].getType().canAssignTo(String.class)) {
				} else if(splitObs[i].getType().canAssignTo(CharSequence.class)) {
					splitObs[i] = splitObs[i].mapV(seq -> {
						return seq.toString();
					});
				} else
					throw new MuisException("Model value " + split[i] + " is not compatible with string: " + splitObs[i].getType());
			} else
				splitObs[i] = ObservableValue.constant(split[i]);
		}
		if(split.length == 0)
			return ObservableValue.constant(new String[0]);
		else
			return new org.muis.core.rx.ComposedObservableValue<>(new Type(String [].class), args -> {
				String [] str = new String[args.length];
				for(int i = 0; i < str.length; i++)
					str[i] = (String) args[i];
				return str;
			}, splitObs);
	}

	@Override
	public String toString(String [] value) {
		StringBuilder ret = new StringBuilder();
		for(int i = 0; i < value.length; i++) {
			if(i > 0)
				ret.append(",");
			ret.append(value[i]);
		}
		return ret.toString();
	}
}
