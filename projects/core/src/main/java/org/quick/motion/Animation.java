package org.quick.motion;

/** Represents an animation to run */
public interface Animation
{
	/**
	 * @param time The amount of time since this animation was started
	 * @return Whether the animation has finished
	 */
	boolean update(long time);

	/** @return The minimum number of milliseconds that should pass between calls to {@link #update(long)}. This is only called once. */
	long getMaxFrequency();
}
