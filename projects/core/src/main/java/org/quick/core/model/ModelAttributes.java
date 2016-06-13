package org.quick.core.model;

import org.quick.core.prop.QuickAttribute;
import org.quick.core.prop.QuickPropertyType;

import com.google.common.reflect.TypeToken;

/** Attributes pertaining to models */
public class ModelAttributes {
	/** The type of attribute to set for the user to specify an action */
	public static final QuickPropertyType<Runnable> actionType = QuickPropertyType.build("action", TypeToken.of(Runnable.class)).build();

	/** The type of attribute to set for the user to specify a value */
	public static final QuickPropertyType<Object> valueType = QuickPropertyType.build("value", TypeToken.of(Object.class)).build();

	/** The "action" attribute on an actionable widget */
	public static final QuickAttribute<Runnable> action;

	/** The "value" attribute on a value-modeled widget */
	public static final QuickAttribute<Object> value;

	static {
		action = new QuickAttribute<>("action", actionType);
		value = new QuickAttribute<>("value", valueType);
	}
}
