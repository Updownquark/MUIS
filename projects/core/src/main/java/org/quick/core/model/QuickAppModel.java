package org.quick.core.model;

import java.util.Collections;
import java.util.Set;

/** Contains values backing Quick widgets */
public interface QuickAppModel {
	/** @return This model's name */
	String getName();

	/** @return All fields in this model */
	Set<String> getFields();

	/**
	 * @param name The name of the field to get
	 * @return The value of the field
	 */
	Object getField(String name);

	/**
	 * @param name The name for the empty model
	 * @return A model with no fields
	 */
	static QuickAppModel empty(String name) {
		return new QuickAppModel() {
			@Override
			public String getName() {
				return name;
			}

			@Override
			public Set<String> getFields() {
				return Collections.emptySet();
			}

			@Override
			public Object getField(String fieldName) {
				return null;
			}
		};
	}
}
