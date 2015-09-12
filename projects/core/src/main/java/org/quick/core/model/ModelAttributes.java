package org.quick.core.model;

import org.observe.ObservableValue;
import org.quick.core.*;
import org.quick.core.parser.QuickParseException;

import prisms.lang.Type;

/** Attributes pertaining to models */
public class ModelAttributes {
	/** The type of attribute to set for the user to specify an action */
	public static final QuickProperty.PropertyType<QuickActionListener> actionType;

	/** The type of attribute to set for the user to specify a value */
	public static final QuickProperty.PropertyType<ObservableValue<?>> valueType;

	/** The type of attribute to set for the user to specify a model */
	public static final QuickProperty.PropertyType<QuickAppModel> modelType;

	/** The "action" attribute on an actionable widget */
	public static final QuickAttribute<QuickActionListener> action;

	/** The "value" attribute on a value-modeled widget */
	public static final QuickAttribute<ObservableValue<?>> value;

	/** The "model" attribute on a complex-modeled widget */
	public static final QuickAttribute<QuickAppModel> model;

	static {
		actionType = new QuickProperty.PropertyType<QuickActionListener>() {
			@Override
			public Type getType() {
				return new Type(QuickActionListener.class);
			}

			@Override
			public ObservableValue<? extends QuickActionListener> parse(QuickParseEnv env, String attValue) throws QuickException {
				QuickDocument doc = env.msg().getDocument();
				if(doc == null)
					throw new IllegalArgumentException("Cannot get actions without document context");
				String [] entries = attValue.split(",");
				if(entries.length == 0)
					return ObservableValue.constant(new Type(QuickActionListener.class), (QuickActionListener) null);
				if(entries.length == 1)
					return ObservableValue.constant(getModelActionListener(doc, entries[0].trim()));
				else {
					AggregateActionListener agg = new AggregateActionListener();
					for(String entry : entries)
						agg.addListener(getModelActionListener(doc, entry.trim()));
					return ObservableValue.constant(agg);
				}
			}

			@Override
			public boolean canCast(Type type) {
				return type.canAssignTo(QuickActionListener.class);
			}

			@Override
			public <V extends QuickActionListener> V cast(Type type, Object attValue) {
				if(attValue instanceof QuickActionListener)
					return (V) attValue;
				else
					return null;
			}

			@Override
			public String toString() {
				return "action";
			}
		};
		valueType = new QuickProperty.PrismsParsedPropertyType<ObservableValue<?>>(new Type(ObservableValue.class, new Type(new Type(
			Object.class), true))) {
			@Override
			public String toString() {
				return "model value";
			}
		};
		modelType = new QuickProperty.PrismsParsedPropertyType<QuickAppModel>(new Type(QuickAppModel.class, new Type(new Type(
			QuickAppModel.class), true))) {
			@Override
			public String toString() {
				return "model";
			}
		};

		action = new QuickAttribute<>("action", actionType);
		value = new QuickAttribute<>("value", valueType);
		model = new QuickAttribute<>("model", modelType);
	}

	/**
	 * Parses the app model out of a model reference formatted like "modelName(.subModelName)*.valueOrAction"
	 *
	 * @param doc The document to get the models from
	 * @param att The attribute value to parse
	 * @param type The name of the final type to be parsed (action or value)
	 * @param name An empty StringBuilder to append the name of the app model to
	 * @return The parsed app model, or null if parsing failed (errors reported to the message center)
	 * @throws QuickParseException If the model cannot be parsed from the value
	 */
	public static QuickAppModel getModel(QuickDocument doc, String att, String type, StringBuilder name) throws QuickParseException {
		String [] split = att.split("\\.");
		if(split.length < 2) {
			throw new QuickParseException("To specify a" + (type.startsWith("a") ? "n " : " ") + type
				+ " in a model, use the format \"modelName." + type + "\" or \"modelName.subModelName." + type + "\". \"" + att
				+ "\" is not valid.");
		}
		QuickAppModel appModel = doc.getHead().getModel(split[0]);
		name.append(split[0]);
		for(int i = 1; i < split.length - 1; i++) {
			if(appModel == null) {
				throw new QuickParseException("Model \"" + name + "\" does not exist for " + type + " \"" + att + "\".");
			}
			appModel = appModel.getSubModel(split[i]);
			name.append('.').append(split[i]);
		}
		if(appModel == null) {
			throw new QuickParseException("Model \"" + name + "\" does not exist for " + type + " \"" + att + "\".");
		}
		return appModel;
	}

	/**
	 * Parses a model value out of a reference formatted like "modelName(.subModelName)*.value"
	 *
	 * @param doc The document to get the models from
	 * @param att The attribute value to parse
	 * @return The parsed model value, or null if parsing failed (errors reported to the message center)
	 * @throws QuickParseException If the model cannot be parsed from the value
	 */
	public static ObservableValue<?> getModelValue(QuickDocument doc, String att) throws QuickParseException {
		StringBuilder name = new StringBuilder();
		QuickAppModel appModel = getModel(doc, att, "value", name);
		if(appModel == null)
			return null;
		String [] div = att.split("\\.");
		ObservableValue<? extends Object> ret = appModel.getValue(div[div.length - 1], Object.class);
		if(ret == null) {
			throw new QuickParseException("Value \"" + div[div.length - 1] + "\" does not exist in model \"" + name + "\".");
		}
		return ret;
	}

	/**
	 * Parses a model action listener out of a reference formatted like "modelName(.subModelName)*.action"
	 *
	 * @param doc The document to get the models from
	 * @param att The attribute value to parse
	 * @return The parsed action listener, or null if parsing failed (errors reported to the message center)
	 * @throws QuickParseException If the model cannot be parsed from the value
	 */
	public static QuickActionListener getModelActionListener(QuickDocument doc, String att) throws QuickParseException {
		StringBuilder name = new StringBuilder();
		QuickAppModel appModel = getModel(doc, att, "action", name);
		String [] div = att.split("\\.");
		QuickActionListener ret = appModel.getAction(div[div.length - 1]);
		if(ret == null) {
			throw new QuickParseException("Action \"" + div[div.length - 1] + "\" does not exist in model \"" + name + "\".");
		}
		return ret;
	}
}
