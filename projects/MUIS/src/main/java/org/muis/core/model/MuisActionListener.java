package org.muis.core.model;

/** Listens for a MUIS action */
public interface MuisActionListener {
	/** @param event The user event that sparked the action */
	void actionPerformed(org.muis.core.event.UserEvent event);
}
