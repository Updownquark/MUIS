package org.quick.base.model;

import java.util.*;

import org.quick.core.model.QuickAppModel;

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

	public static Builder build(QuickAppModel parent) {
		return new Builder().withParent(parent);
	}

	public static class Builder {
		private final List<QuickAppModel> theParents;
		private final Map<String, Object> theFields;

		private Builder() {
			theParents = new LinkedList<>();
			theFields = new LinkedHashMap<>();
		}

		public Builder withParent(QuickAppModel parent) {
			theParents.add(parent);
			return this;
		}

		public Builder withField(String name, Object value) {
			theFields.put(name, value);
			return this;
		}

		public QuickAppModelExtension build() {
			return new QuickAppModelExtension(theParents, theFields);
		}
	}
}
