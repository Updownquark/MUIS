package org.muis.core.model;

import org.muis.core.MuisDocument;
import org.muis.core.MuisException;
import org.muis.core.MuisParseEnv;
import org.muis.core.MuisProperty;
import org.muis.core.mgr.MuisMessageCenter;

/** Attributes pertaining to models */
public class ModelAttributes {
	/** The type of attribute to set for the user to specify an action */
	public static final MuisProperty.PropertyType<MuisActionListener> actionType;

	/** The tyep of attribute to set for the user to specify a value */
	public static final MuisProperty.PropertyType<MuisModelValue<?>> valueType;

	/** The "action" attribute on an actionable widget */
	public static final org.muis.core.MuisAttribute<MuisActionListener> action;

	/** The "value" attribute on a value-modeled widget */
	public static final org.muis.core.MuisAttribute<MuisModelValue<?>> value;

	static {
		actionType = new MuisProperty.PropertyType<MuisActionListener>() {
			@Override
			public <V extends MuisActionListener> Class<V> getType() {
				return (Class<V>) MuisActionListener.class;
			}

			@Override
			public <V extends MuisActionListener> V parse(MuisParseEnv env, String attValue) throws MuisException {
				MuisDocument doc = env.msg().getDocument();
				if(doc == null)
					throw new IllegalArgumentException("Cannot get actions without document context");
				String [] entries = attValue.split(",");
				if(entries.length == 0)
					return null;
				if(entries.length == 1)
					return (V) getModelActionListener(doc, entries[0].trim(), env.msg());
				else {
					AggregateActionListener agg = new AggregateActionListener();
					for(String entry : entries)
						agg.addListener(getModelActionListener(doc, entry.trim(), env.msg()));
					return (V) agg;
				}
			}

			@Override
			public <V extends MuisActionListener> V cast(Object attValue) {
				if(attValue instanceof MuisActionListener)
					return (V) attValue;
				else
					return null;
			}
		};

		valueType = new MuisProperty.PropertyType<MuisModelValue<?>>() {
			@Override
			public <V extends MuisModelValue<?>> Class<V> getType() {
				return (Class<V>) MuisModelValue.class;
			}

			@Override
			public <V extends MuisModelValue<?>> V parse(MuisParseEnv env, String attValue) throws MuisException {
				MuisDocument doc = env.msg().getDocument();
				if(doc == null)
					throw new IllegalArgumentException("Cannot get model values without document context");
				return (V) getModelValue(doc, attValue, env.msg());
			}

			@Override
			public <V extends MuisModelValue<?>> V cast(Object attValue) {
				if(attValue instanceof MuisModelValue)
					return (V) attValue;
				else
					return null;
			}
		};

		action = new org.muis.core.MuisAttribute<>("action", actionType);
		value = new org.muis.core.MuisAttribute<>("value", valueType);
	}

	/**
	 * Parses the app model out of a model reference formatted like "modelName(.subModelName)*.valueOrAction"
	 *
	 * @param doc The document to get the models from
	 * @param att The attribute value to parse
	 * @param msg The message center to report parsing errors to
	 * @param type The name of the final type to be parsed (action or value)
	 * @param name An empty StringBuilder to append the name of the app model to
	 * @return The parsed app model, or null if parsing failed (errors reported to the message center)
	 */
	public static MuisAppModel getModel(MuisDocument doc, String att, MuisMessageCenter msg, String type, StringBuilder name) {
		String [] split = att.split("\\.");
		if(split.length < 2) {
			msg.error("To specify a" + (type.startsWith("a") ? "n " : " ") + type + " in a model, use the format \"modelName." + type
				+ "\" or \"modelName.subModelName." + type + "\". \"" + att + "\" is not valid.");
			return null;
		}
		MuisAppModel model = doc.getHead().getModel(split[0]);
		name.append(split[0]);
		for(int i = 1; i < split.length - 1; i++) {
			if(model == null) {
				msg.error("Model \"" + name + "\" does not exist for " + type + " \"" + att + "\".");
				return null;
			}
			model = model.getSubModel(split[i]);
			name.append('.').append(split[i]);
		}
		if(model == null) {
			msg.error("Model \"" + name + "\" does not exist for " + type + " \"" + att + "\".");
			return null;
		}
		return model;
	}

	/**
	 * Parses a model value out of a reference formatted like "modelName(.subModelName)*.value"
	 * 
	 * @param doc The document to get the models from
	 * @param att The attribute value to parse
	 * @param msg The message center to report parsing errors to
	 * @return The parsed model value, or null if parsing failed (errors reported to the message center)
	 */
	public static MuisModelValue<?> getModelValue(MuisDocument doc, String att, MuisMessageCenter msg) {
		StringBuilder name = new StringBuilder();
		MuisAppModel model = getModel(doc, att, msg, "value", name);
		if(model == null)
			return null;
		String [] div = att.split("\\.");
		MuisModelValue<?> ret = model.getValue(div[div.length - 1], Object.class);
		if(ret == null) {
			msg.error("Value \"" + div[div.length - 1] + "\" does not exist in model \"" + name + "\".");
			return null;
		}
		return ret;
	}

	/**
	 * Parses a model action listener out of a reference formatted like "modelName(.subModelName)*.action"
	 * 
	 * @param doc The document to get the models from
	 * @param att The attribute value to parse
	 * @param msg The message center to report parsing errors to
	 * @return The parsed action listener, or null if parsing failed (errors reported to the message center)
	 */
	public static MuisActionListener getModelActionListener(MuisDocument doc, String att, MuisMessageCenter msg) {
		StringBuilder name = new StringBuilder();
		MuisAppModel model = getModel(doc, att, msg, "action", name);
		if(model == null)
			return null;
		String [] div = att.split("\\.");
		MuisActionListener ret = model.getAction(div[div.length - 1]);
		if(ret == null) {
			msg.error("Action \"" + div[div.length - 1] + "\" does not exist in model \"" + name + "\".");
			return null;
		}
		return ret;
	}
}
