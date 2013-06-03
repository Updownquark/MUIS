package org.muis.core.model;

/** Listens for a MUIS action */
public interface MuisActionListener {
	/** @return Whether this action is enabled or not */
	boolean isEnabled();

	/** @param event The action event representing the action */
	void actionPerformed(MuisActionEvent event);
}
