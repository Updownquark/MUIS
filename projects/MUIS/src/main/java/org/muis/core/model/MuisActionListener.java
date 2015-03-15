package org.muis.core.model;

import org.muis.rx.ObservableValue;

/** Listens for a MUIS action */
public interface MuisActionListener {
	/** @return Whether this action is enabled or not */
	default ObservableValue<Boolean> isEnabled() {
		return ObservableValue.constant(true);
	};

	/** @param event The action event representing the action */
	void actionPerformed(MuisActionEvent event);
}
