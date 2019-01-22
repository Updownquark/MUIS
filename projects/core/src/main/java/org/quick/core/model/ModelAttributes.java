package org.quick.core.model;

import org.observe.Observable;
import org.observe.ObservableAction;
import org.observe.collect.ObservableCollection;
import org.observe.util.TypeTokens;
import org.quick.core.prop.QuickAttribute;
import org.quick.core.prop.QuickPropertyType;

import com.google.common.reflect.TypeToken;

/** Attributes pertaining to models */
public class ModelAttributes {
	/** The type of attribute to set for the user to specify an action */
	public static final QuickPropertyType<ObservableAction<?>> actionType = QuickPropertyType
		.build("action", TypeTokens.get().keyFor(ObservableAction.class).<ObservableAction<?>> parameterized()).build();

	/** The type of attribute to set for the user to specify a value */
	public static final QuickPropertyType<Object> valueType = QuickPropertyType.build("value", TypeTokens.get().OBJECT).build();

	/** The type of attribute for whether an element is marked as selected or not */
	public static final QuickPropertyType<Boolean> selectedType = QuickPropertyType.build("selected", TypeTokens.get().BOOLEAN).build();

	/** The type of attribute to set for the user to specify an event */
	public static final QuickPropertyType<Observable<?>> eventType = QuickPropertyType.build("event", new TypeToken<Observable<?>>() {})
		.build();

	/** A collection model attribute */
	public static final QuickPropertyType<ObservableCollection<?>> collectionType = QuickPropertyType
		.build("collection", TypeTokens.get().keyFor(ObservableCollection.class).<ObservableCollection<?>> parameterized()).build();

	/** The "action" attribute on an actionable widget */
	public static final QuickAttribute<ObservableAction<?>> action = QuickAttribute.build("action", actionType).build();

	/** The "value" attribute on a value-modeled widget */
	public static final QuickAttribute<Object> value = QuickAttribute.build("value", valueType).build();

	/** The "selected" attribute on a selectable widget */
	public static final QuickAttribute<Boolean> selected = QuickAttribute.build("selected", selectedType).build();

	/** The "event" attribute on an eventing widget */
	public static final QuickAttribute<Observable<?>> event = QuickAttribute.build("event", eventType).build();
}
