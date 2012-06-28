package org.muis.motion;

/** Manages animations in MUIS */
public class AnimationManager
{
	private static class AnimationHolder
	{
		final Animation animation;

		final long startTime;

		final long frequency;

		int timesRun;

		int actualTimesRun;

		AnimationHolder(Animation anim)
		{
			animation = anim;
			frequency = anim.getMaxFrequency();
			startTime = System.currentTimeMillis();
		}
	}

	private static AnimationManager theInstance = new AnimationManager();

	/** @return The instance of AnimationManager to use for running animations */
	public static AnimationManager get()
	{
		return theInstance;
	}

	private AnimationHolder [] theAnimations;

	private Object theLock;

	private AnimationThread theThread;

	private volatile boolean isShuttingDown;

	volatile boolean isInterrupted;

	private AnimationManager()
	{
		theAnimations = new AnimationHolder[0];
		theLock = new Object();
	}

	/** @param animation The animation to run */
	public void start(Animation animation)
	{
		AnimationHolder holder = new AnimationHolder(animation);
		synchronized(theLock)
		{
			theAnimations = prisms.util.ArrayUtils.add(theAnimations, holder);
		}
		start();
		isInterrupted = true;
		theThread.interrupt();
	}

	/** Starts the animation thread */
	private void start()
	{
		if(theThread != null)
			return;
		new AnimationThread().start();
	}

	private void remove(AnimationHolder animation)
	{
		synchronized(theLock)
		{
			theAnimations = prisms.util.ArrayUtils.remove(theAnimations, animation);
		}
	}

	/**
	 * Can't think why this should be called, but if it's needed we can change the modifier to public or provide some mechanism to access it
	 */
	@SuppressWarnings("unused")
	private void shutdown()
	{
		isShuttingDown = true;
	}

	/** @return Whether this event queue is currently running */
	public boolean isRunning()
	{
		return theThread != null;
	}

	/** @return Whether this event queue is shutting down. Not currently used. */
	public boolean isShuttingDown()
	{
		return isShuttingDown;
	}

	private class AnimationThread extends Thread
	{
		AnimationThread()
		{
			super("MUIS Motion");
		}

		/**
		 * The queue thread’s action is to go through the events in the queue. When an event is come to, the dirty paint or layout state of
		 * the element is checked. If not dirty, remove the event and do nothing. If the dirty time is <=10ms ago, skip the event so that it
		 * is checked again the next 50ms. Heavy, multiple-op processes on elements from external threads will cause few layout/redraw
		 * actions this way, but after ops finish, layout/redraw will happen within 60ms, average 35ms.
		 */
		@SuppressWarnings("synthetic-access")
		@Override
		public void run()
		{
			synchronized(theLock)
			{
				if(theThread != null)
					return;
				theThread = this;
			}
			while(!isShuttingDown())
				try
				{
					isInterrupted = false;
					AnimationHolder [] animations = theAnimations;
					boolean acted = false;
					long now = System.currentTimeMillis();
					long next = now + 1000;
					for(AnimationHolder anim : animations)
					{
						if(anim.actualTimesRun != 0 && (now - anim.startTime) / anim.timesRun < anim.frequency)
						{
							long animNext = anim.startTime + (anim.timesRun + 1) * anim.frequency;
							if(animNext < next)
								next = animNext;
							continue;
						}
						int times = anim.frequency == 0 ? 0 : (int) ((now - anim.startTime) / anim.frequency);
						anim.timesRun = times + 1;
						anim.actualTimesRun++;
						try
						{
							if(anim.animation.update(now - anim.startTime))
								remove(anim);
						} catch(RuntimeException | Error e)
						{
							System.err.println("Error running animation " + anim.animation);
							e.printStackTrace();
							remove(anim);
						}
						now = System.currentTimeMillis();
						if(next > now + anim.frequency)
							next = now + anim.frequency;
					}
					if(!acted && !isInterrupted && next > now)
						Thread.sleep(next - now);
				} catch(InterruptedException e)
				{
				}
			theThread = null;
			isShuttingDown = false;
		}
	}
}
