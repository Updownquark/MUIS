package org.quick.core.model;

import java.util.Set;

/** Contains values backing Quick widgets */
public interface QuickAppModel {
	/** @return All fields in this model */
	Set<String> getFields();

	/**
	 * @param name The name of the field to get
	 * @return The value of the field
	 */
	Object getField(String name);
}
