package org.quick.motion;

/**
 * Represents an animation that takes a certain amount of time and then completes. Typically animations of this type are used to change the
 * state something, e.g. moving or resizing an element
 */
public abstract class DiscreteAnimation implements Animation
{
	private AnimationSpeed theSpeed;

	private long theMaxFrequency;

	/**
	 * @param speed The speed function for the animation
	 * @param ticks The number of frames to take to render this animation
	 */
	public DiscreteAnimation(AnimationSpeed speed, int ticks)
	{
		theSpeed = speed;
		if(ticks == 0)
			theMaxFrequency = 0;
		else if(ticks == 1)
			theMaxFrequency = speed.getTotalTime();
		else
			theMaxFrequency = speed.getTotalTime() / (ticks - 1);
	}

	@Override
	public boolean update(long time)
	{
		float progress = theSpeed.getProgress(time);
		if(progress > 1)
			progress = 1;
		update(progress);
		return progress == 1;
	}

	/** @param progress The completeness of the animation, 0 being the very beginning and 1 being the end result of the animation */
	public abstract void update(float progress);

	@Override
	public long getMaxFrequency()
	{
		return theMaxFrequency;
	}
}
