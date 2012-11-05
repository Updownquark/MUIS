package org.muis.core.style.attach;

import org.muis.core.MuisAttribute;
import org.muis.core.MuisClassView;
import org.muis.core.MuisException;
import org.muis.core.MuisProperty;
import org.muis.core.mgr.MuisMessageCenter;

/** Parses an attribute as a comma-separated list of strings to be applied as named style groups on an element */
public class GroupPropertyType implements MuisProperty.PrintablePropertyType<String []> {
	/** Avoids having to instantiate multiple copies */
	public static final GroupPropertyType instance = new GroupPropertyType();

	/** The group attribute */
	public static final MuisAttribute<String []> attribute = new MuisAttribute<String []>("group", instance);

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
	public String [] parse(MuisClassView classView, String value, MuisMessageCenter msg) throws MuisException {
		String [] ret = value.split(",");
		for(int i = 0; i < ret.length; i++)
			ret[i] = ret[i].trim();
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
