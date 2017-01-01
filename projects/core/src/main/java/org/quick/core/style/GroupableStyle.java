package org.quick.core.style;

/** A style which can also be modified by adding groups to it */
public interface GroupableStyle extends MutableStyle {
	/**
	 * @param group The group to add to this style
	 * @return This style
	 */
	GroupableStyle addGroup(String group);

	/**
	 * @param group The group to add to this style
	 * @return This style
	 */
	GroupableStyle removeGroup(String group);
}
