package org.muis.core.model;

import org.muis.core.MuisClassView;
import org.muis.core.MuisException;
import org.muis.core.MuisProperty;
import org.muis.core.mgr.MuisMessageCenter;

/** Attributes pertaining to models */
public class ModelAttributes {
	/** The attribute to set for the user to specify an action */
	public static final MuisProperty.PropertyType<MuisActionListener> actionType;

	/** The "action" attribute on a button */
	public static final org.muis.core.MuisAttribute<MuisActionListener> action;

	static {
		actionType = new MuisProperty.PropertyType<MuisActionListener>() {
			@Override
			public <V extends MuisActionListener> Class<V> getType() {
				return (Class<V>) MuisActionListener.class;
			}

			@Override
			public <V extends MuisActionListener> V parse(MuisClassView classView, String value, MuisMessageCenter msg)
				throws MuisException {
				org.muis.core.MuisDocument doc = msg.getDocument();
				if(doc == null)
					throw new IllegalArgumentException("Cannot get actions without document context");
				String [] entries = value.split(",");
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
				String [] div = entry.split("\\.");
				if(div.length < 0) {
					msg.error("To specify an action in a model, use the format \"modelName.action\" or \"modelName.subModelName.action\". \""
						+ entry + "\" is not valid.");
					return null;
				}
				MuisAppModel model = doc.getHead().getModel(div[0]);
				String name = div[0];
				for(int i = 1; i < div.length - 1; i++) {
					if(model == null) {
						msg.error("Model \"" + name + "\" does not exist for action \"" + entry + "\".");
						return null;
					}
					model = model.getSubModel(div[i]);
					name += "." + div[i];
				}
				if(model == null) {
					msg.error("Model \"" + name + "\" does not exist for action \"" + entry + "\".");
					return null;
				}
				MuisActionListener ret = model.getAction(div[div.length - 1]);
				if(ret == null) {
					msg.error("Action \"" + div[div.length - 1] + "\" does not exist in model \"" + name + "\".");
					return null;
				}
				return ret;
			}

			@Override
			public <V extends MuisActionListener> V cast(Object value) {
				if(value instanceof MuisActionListener)
					return (V) value;
				else
					return null;
			}
		};

		action = new org.muis.core.MuisAttribute<>("action", actionType);
	}
}
