package org.muis.core.style.attach;

import org.muis.core.MuisAttribute;
import org.muis.core.MuisException;
import org.muis.core.MuisParseEnv;
import org.muis.core.MuisProperty;

/** Parses an attribute as a comma-separated list of strings to be applied as named style groups on an element */
public class GroupPropertyType implements MuisProperty.PrintablePropertyType<String []> {
	/** Avoids having to instantiate multiple copies */
	public static final GroupPropertyType instance = new GroupPropertyType();

	/** The group attribute */
	public static final MuisAttribute<String []> attribute = new MuisAttribute<>("group", instance);

	@Override
	public Class<String []> getType() {
		return String [].class;
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
	public String [] parse(MuisParseEnv env, String value) throws MuisException {
		String [] ret = value.split(",");
		for(int i = 0; i < ret.length; i++) {
			ret[i] = ret[i].trim();
			if(env.getModelParser().getNextMVR(ret[i], 0) == 0) {
				org.muis.core.model.MuisModelValue<String> modelValue = (org.muis.core.model.MuisModelValue<String>) env.getModelParser()
					.parseMVR(ret[i]);
				if(modelValue.getType().canAssignTo(String.class))
					ret[i] = modelValue.get();
				else
					throw new MuisException("Model value " + ret[i] + " is not compatible with string");
			}
		}
		return ret;
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
