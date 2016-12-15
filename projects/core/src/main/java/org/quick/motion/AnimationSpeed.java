package org.quick.motion;

/** Represents a rate of progress for an animation */
public interface AnimationSpeed
{
	/** @return The total amount of time (in ms) that an animation of this speed takes */
	long getTotalTime();

	/**
	 * @param time The amount of time since the start of the animation
	 * @return The "completeness" of the animation, with 0 being just started and 1 being complete.
	 */
	float getProgress(long time);
}
