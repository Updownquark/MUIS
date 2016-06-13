package org.quick.core.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class BuiltQuickModel {
	private final Map<String, Object> theValues;

	private BuiltQuickModel(Map<String, Object> values) {
		theValues = Collections.unmodifiableMap(new LinkedHashMap<>(values));
	}

	public Set<String> getFields() {
		return theValues.keySet();
	}

	public Object getField(String name) {
		return theValues.get(name);
	}

	public static Builder build() {
		return new Builder();
	}

	public static class Builder {
		private final Map<String, Object> theValues = new LinkedHashMap<>();

		public Builder with(String field, Object value) {
			theValues.put(field, value);
			return this;
		}

		public Object getValue(String field) {
			return theValues.get(field);
		}

		public BuiltQuickModel build() {
			return new BuiltQuickModel(theValues);
		}
	}
}
