package org.muis.core.event;

import org.muis.core.event.boole.TypedPredicate;

public interface MuisEventCondition<E extends MuisEvent> {
	TypedPredicate<MuisEvent, E> getTester();
}
