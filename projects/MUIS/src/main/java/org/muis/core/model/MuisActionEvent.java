package org.muis.core.model;

import org.muis.core.event.UserEvent;

/** Represents the occurrence of an action in MUIS */
public class MuisActionEvent {
	private final String theAction;

	private final UserEvent theUserEvent;

	/**
	 * @param action The name of the action that occurred
	 * @param userEvent The user event that sparked the action. May be null.
	 */
	public MuisActionEvent(String action, UserEvent userEvent) {
		theAction = action;
		theUserEvent = userEvent;
	}

	/** @return The name of the action that occurred */
	public String getAction() {
		return theAction;
	}

	/** @return The user event that sparked the action. May be null. */
	public UserEvent getUserEvent() {
		return theUserEvent;
	}
}
