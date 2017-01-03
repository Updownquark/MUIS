package org.quick.base.model;

import java.util.*;

import org.quick.core.model.QuickAppModel;

/** A model that extends one or more other models and potentially adds its own fields */
public class QuickAppModelExtension implements QuickAppModel {
	private final List<QuickAppModel> theParents;
	private final Map<String, Object> theFields;

	private QuickAppModelExtension(List<QuickAppModel> parents, Map<String, Object> fields) {
		theParents = parents;
		theFields = fields;
	}

	@Override
	public Set<String> getFields() {
		LinkedHashSet<String> fields = new LinkedHashSet<>(theFields.keySet());
		for (QuickAppModel parent : theParents)
			fields.addAll(parent.getFields());
		return Collections.unmodifiableSet(fields);
	}

	@Override
	public Object getField(String name) {
		Object value = theFields.get(name);
		for (int i = 0; value == null && i < theParents.size(); i++)
			value = theParents.get(i).getField(name);
		return value;
	}

	/**
	 * Builds a new extended model
	 * 
	 * @param parent The first parent for the model
	 * @return The builder
	 */
	public static Builder build(QuickAppModel parent) {
		return new Builder().withParent(parent);
	}

	/** Builds a {@link QuickAppModelExtension} */
	public static class Builder {
		private final List<QuickAppModel> theParents;
		private final Map<String, Object> theFields;

		private Builder() {
			theParents = new LinkedList<>();
			theFields = new LinkedHashMap<>();
		}

		/**
		 * @param parent Another parent for the model to inherit fields from
		 * @return This builder
		 */
		public Builder withParent(QuickAppModel parent) {
			theParents.add(parent);
			return this;
		}

		/**
		 * @param name The name of the field
		 * @param value The value for the field
		 * @return This builder
		 */
		public Builder withField(String name, Object value) {
			theFields.put(name, value);
			return this;
		}

		/** @return The built model */
		public QuickAppModelExtension build() {
			return new QuickAppModelExtension(theParents, theFields);
		}
	}
}
