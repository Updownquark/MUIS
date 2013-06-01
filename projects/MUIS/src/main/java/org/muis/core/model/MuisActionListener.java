package org.muis.core.model;

/** Listens for a MUIS action */
public interface MuisActionListener {
	/** @param event The action event representing the action */
	void actionPerformed(MuisActionEvent event);
}
