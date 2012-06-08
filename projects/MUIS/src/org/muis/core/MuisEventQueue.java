package org.muis.core;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.concurrent.ConcurrentLinkedQueue;

/** The event queue in MUIS which makes sure elements's states stay up-to-date */
public class MuisEventQueue
{
	private static MuisEventQueue theInstance = new MuisEventQueue();

	/** @return The instance of the queue to use to schedule core events */
	public static MuisEventQueue getInstance()
	{
		return theInstance;
	}

	private ConcurrentLinkedQueue<MuisCoreEvent> theEvents;

	private final Object theLock;

	private volatile Thread theThread;

	private volatile boolean isShuttingDown;

	private long theFrequency;

	private long thePaintDirtyTolerance;

	private long theLayoutDirtyTolerance;

	private MuisEventQueue()
	{
		theEvents = new ConcurrentLinkedQueue<>();
		theLock = new Object();
		theFrequency = 50;
		thePaintDirtyTolerance = 10;
		theLayoutDirtyTolerance = 10;
	}

	/**
	 * Schedules a core event
	 *
	 * @param event The event that MUIS needs to take action on
	 * @param fireNow Whether to take action on the event immediately and wait with this thread until the event has been acted on
	 */
	public void scheduleEvent(MuisCoreEvent event, boolean fireNow)
	{
		java.util.Iterator<MuisCoreEvent> iter = theEvents.iterator();
		while(iter.hasNext())
		{
			MuisCoreEvent evt = iter.next();
			if(evt.type != event.type || evt.isActed())
				continue;
			switch (event.type)
			{
			case paint:
				if(!MuisUtils.isAncestor(evt.element, event.element))
					continue;
				if(evt.area == null)
					return; // Element will be repainted with its ancestor
				Rectangle area = MuisUtils.relative(evt.area, evt.element, event.element);
				Rectangle area2 = event.area;
				if(area2 != null)
				{
					if(area.x < 0 && area.y < area2.x && area.width + area.x >= area2.x + area2.width
						&& area.height + area.y >= area2.y + area2.height)
						return; // Element's area will be repainted with its ancestor
				}
				else if(area.x < 0 && area.y < 0 && area.width + area.x >= event.element.getWidth()
					&& area.height + area.y >= event.element.getHeight())
					return; // Element will be repainted with its ancestor
				else if(event.element == evt.element)
					if(event.area == null || event.area.contains(evt.area))
					{
						iter.remove();
						addEvent(event, fireNow);
					}
					else
					{
						iter.remove();
						java.awt.Rectangle union = evt.area.union(event.area);
						if(!evt.isDone())
						{
							event.area.setBounds(union);
							addEvent(event, fireNow);
						}
						else
							addEvent(event, fireNow);
					}
				break;
			case layout:
				if(evt.element == event.element)
					return; // Element already scheduled to be layed out
			}
		}
		addEvent(event, fireNow);
	}

	private void addEvent(MuisCoreEvent event, boolean fireNow)
	{
		theEvents.add(event);
		start();
		if(fireNow)
		{
			theThread.interrupt();
			while(!event.isDone())
				try
				{
					Thread.sleep(10);
				} catch(InterruptedException e)
				{
				}
		}
	}

	private void start()
	{
		if(theThread != null)
			return;
		new EventQueueThread().start();
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

	/** @return The frequency with which this event queue handles its events */
	public long getFrequency()
	{
		return theFrequency;
	}

	/**
	 * @return The amount of time for which this queue will let paint events rest until {@link MuisElement#repaint(Rectangle, boolean)}
	 *         stops being called repeatedly
	 */
	public long getPaintDirtyTolerance()
	{
		return thePaintDirtyTolerance;
	}

	/**
	 * @return The amount of time for which this queue will let layout events rest until {@link MuisElement#relayout(boolean)} stops being
	 *         called repeatedly
	 */
	public long getLayoutDirtyTolerance()
	{
		return theLayoutDirtyTolerance;
	}

	private class EventQueueThread extends Thread
	{
		/**
		 * The queue thread’s action is to go through the events in the queue. When an event is come to, the dirty paint or layout state of
		 * the element is checked. If not dirty, remove the event and do nothing. If the dirty time is <=10ms ago, skip the event so that it
		 * is checked again the next 50ms. Heavy, multiple-op processes on elements from external threads will cause few layout/redraw
		 * actions this way, but after ops finish, layout/redraw will happen within 60ms, average 35ms.
		 */
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
					java.util.Iterator<MuisCoreEvent> iter = theEvents.iterator();
					final long pdt = getPaintDirtyTolerance();
					final long ldt = getLayoutDirtyTolerance();
					long now = System.currentTimeMillis();
					while(iter.hasNext())
					{
						MuisCoreEvent evt = iter.next();
						switch (evt.type)
						{
						case paint:
							if(now - evt.element.getPaintDirtyTime() < pdt)
								continue;
							evt.act();
							repaint(evt);
							evt.done();
							now = System.currentTimeMillis(); // Update the time, since the repaint may have taken some
							break;
						case layout:
							if(now - evt.element.getLayoutDirtyTime() < ldt)
								continue;
							evt.act();
							relayout(evt);
							evt.done();
							now = System.currentTimeMillis(); // Update the time, since the relayout may have taken some
							break;
						}
					}
					Thread.sleep(getFrequency());
				} catch(InterruptedException e)
				{
				}
			theThread = null;
			isShuttingDown = false;
		}
	}

	/**
	 * Takes action on a {@link MuisCoreEvent.CoreEventType#paint paint} event
	 *
	 * @param event The paint event to fulfill
	 */
	protected void repaint(MuisCoreEvent event)
	{
		Point trans = MuisUtils.relative(new Point(0, 0), null, event.element);
		java.awt.Graphics2D graphics = event.element.getDocument().getGraphics();
		graphics.translate(-trans.x, -trans.y);
		event.element.paint(graphics, event.area);
	}

	/**
	 * Takes action on a {@link MuisCoreEvent.CoreEventType#layout layout} event
	 *
	 * @param event The layout event to fulfill
	 */
	protected void relayout(MuisCoreEvent event)
	{
		event.element.doLayout();
	}
}
