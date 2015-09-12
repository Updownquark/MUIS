package org.quick.core.model;

import org.observe.ObservableValue;

/** Listens for a MUIS action */
public interface QuickActionListener {
	/** @return Whether this action is enabled or not */
	default ObservableValue<Boolean> isEnabled() {
		return ObservableValue.constant(true);
	};

	/** @param event The action event representing the action */
	void actionPerformed(QuickActionEvent event);
}
