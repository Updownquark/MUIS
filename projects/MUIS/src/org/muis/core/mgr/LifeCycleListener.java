package org.muis.core.mgr;

/** A listener to be notified when the life cycle of an element transitions to a new stage */
public interface LifeCycleListener
{
	/**
	 * @param fromStage The stage that is being concluded and transitioned out of
	 * @param toStage The stage to be transitioned into
	 */
	void preTransition(String fromStage, String toStage);

	/**
	 * @param oldStage The stage that is concluded and transitioned out of
	 * @param newStage The stage that has just begun
	 */
	void postTransition(String oldStage, String newStage);
}