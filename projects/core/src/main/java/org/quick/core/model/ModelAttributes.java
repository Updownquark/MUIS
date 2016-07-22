package org.quick.core.model;

import org.observe.ObservableAction;
import org.quick.core.prop.QuickAttribute;
import org.quick.core.prop.QuickPropertyType;

import com.google.common.reflect.TypeToken;

/** Attributes pertaining to models */
public class ModelAttributes {
	/** The type of attribute to set for the user to specify an action */
	public static final QuickPropertyType<ObservableAction> actionType = QuickPropertyType
		.build("action", TypeToken.of(ObservableAction.class)).build();

	/** The type of attribute to set for the user to specify a value */
	public static final QuickPropertyType<Object> valueType = QuickPropertyType.build("value", TypeToken.of(Object.class)).build();

	public static final QuickPropertyType<Boolean> selectedType = QuickPropertyType.build("selected", TypeToken.of(Boolean.class)).build();

	/** The "action" attribute on an actionable widget */
	public static final QuickAttribute<ObservableAction> action = QuickAttribute.build("action", actionType).build();

	/** The "value" attribute on a value-modeled widget */
	public static final QuickAttribute<Object> value = QuickAttribute.build("value", valueType).build();

	public static final QuickAttribute<Boolean> selected = QuickAttribute.build("selected", selectedType).build();
}
