package org.muis.core.model;

import org.muis.core.MuisClassView;
import org.muis.core.MuisException;
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
			public <V extends MuisActionListener> V parse(MuisClassView classView, String attValue, MuisMessageCenter msg)
				throws MuisException {
				org.muis.core.MuisDocument doc = msg.getDocument();
				if(doc == null)
					throw new IllegalArgumentException("Cannot get actions without document context");
				String [] entries = attValue.split(",");
				if(entries.length == 0)
					return null;
				if(entries.length == 1)
					return (V) getActionListener(doc, entries[0].trim(), msg);
				else {
					AggregateActionListener agg = new AggregateActionListener();
					for(String entry : entries)
						agg.addListener(getActionListener(doc, entry.trim(), msg));
					return (V) agg;
				}
			}

			private MuisActionListener getActionListener(org.muis.core.MuisDocument doc, String entry, MuisMessageCenter msg) {
				String [] name = new String[1];
				MuisAppModel model = getModel(doc, entry, msg, "action", name);
				if(model == null)
					return null;
				String [] div = entry.split("\\.");
				MuisActionListener ret = model.getAction(div[div.length - 1]);
				if(ret == null) {
					msg.error("Action \"" + div[div.length - 1] + "\" does not exist in model \"" + name[0] + "\".");
					return null;
				}
				return ret;
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
			public <V extends MuisModelValue<?>> V parse(MuisClassView classView, String attValue, MuisMessageCenter msg)
				throws MuisException {
				org.muis.core.MuisDocument doc = msg.getDocument();
				if(doc == null)
					throw new IllegalArgumentException("Cannot get model values without document context");
				String [] name = new String[1];
				MuisAppModel model = getModel(doc, attValue, msg, "value", name);
				if(model == null)
					return null;
				String [] div = attValue.split("\\.");
				MuisModelValue<?> ret = model.getValue(div[div.length - 1], Object.class);
				if(ret == null) {
					msg.error("Value \"" + div[div.length - 1] + "\" does not exist in model \"" + name[0] + "\".");
					return null;
				}
				return (V) ret;
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

	static MuisAppModel getModel(org.muis.core.MuisDocument doc, String att, MuisMessageCenter msg, String type, String [] name) {
		String [] split = att.split("\\.");
		if(split.length < 2) {
			msg.error("To specify a" + (type.startsWith("a") ? "n " : " ") + type + " in a model, use the format \"modelName." + type
				+ "\" or \"modelName.subModelName." + type + "\". \"" + att + "\" is not valid.");
			return null;
		}
		MuisAppModel model = doc.getHead().getModel(split[0]);
		name[0] = split[0];
		for(int i = 1; i < split.length - 1; i++) {
			if(model == null) {
				msg.error("Model \"" + name[0] + "\" does not exist for " + type + " \"" + att + "\".");
				return null;
			}
			model = model.getSubModel(split[i]);
			name[0] += "." + split[i];
		}
		if(model == null) {
			msg.error("Model \"" + name[0] + "\" does not exist for " + type + " \"" + att + "\".");
			return null;
		}
		return model;
	}
}
