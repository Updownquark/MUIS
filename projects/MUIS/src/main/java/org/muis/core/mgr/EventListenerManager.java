package org.muis.core.mgr;

import org.muis.core.event.MuisEvent;
import org.muis.core.event.MuisEventCondition;
import org.muis.core.event.MuisEventListener;
import org.muis.core.event.boole.TypedPredicate;

public interface EventListenerManager {
	public <T extends MuisEvent> void listen(MuisEventCondition<T> condition, MuisEventListener<T>... listeners);

	public <T extends MuisEvent> void remove(MuisEventCondition<T> condition, MuisEventListener<T>... listeners);

	public <T extends MuisEvent> void listen(TypedPredicate<MuisEvent, T> condition, MuisEventListener<T>... listeners);

	public <T extends MuisEvent> void remove(TypedPredicate<MuisEvent, T> condition, MuisEventListener<T>... listeners);
}
