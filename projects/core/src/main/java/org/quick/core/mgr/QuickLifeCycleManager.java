package org.quick.core.mgr;

import org.qommons.ArrayUtils;
import org.quick.core.QuickElement;

/** Manages the life cycle of an element */
public class QuickLifeCycleManager {
	/** Allows control over a life cycle manager */
	public interface Controller {
		/** @param toStage The stage to advance to */
		void advance(String toStage);
	}

	/** Accepts control of a new life cycle manager */
	public interface ControlAcceptor {
		/** @param controller The controller for the new life cycle manager */
		void setController(Controller controller);
	}

	private final QuickElement theQuickElement;

	private volatile String [] theStages;

	private int theCurrentStage;

	private volatile LifeCycleListener [] theLifeCycleListeners;

	private final Object theListenerLock;

	/**
	 * Creates a new life cycle manager. This should only be called from {@link QuickElement#QuickElement()}.
	 *
	 * @param quickElement The element that this manager will manage the life cycle of
	 * @param acceptor The acceptor to accept control of the new life cycle manager
	 * @param initStages The initial stages for the life cycle. At last one stage must be provided. The last value here will always be the
	 *            last stage in this life cycle. No stage may be added after it.
	 */
	public QuickLifeCycleManager(QuickElement quickElement, ControlAcceptor acceptor, String... initStages) {
		if(initStages.length == 0)
			throw new IllegalArgumentException("At least one initial stage (the final stage) must be provided"
				+ " to the life cycle manager constructor");
		theQuickElement = quickElement;
		theStages = initStages;
		theLifeCycleListeners = new LifeCycleListener[0];
		theListenerLock = new Object();
		acceptor.setController(toStage -> {
			advanceLifeCycle(toStage);
		});
	}

	/** @return The current stage in the element's life cycle */
	public String getStage() {
		return theStages.length == 0 ? null : theStages[theCurrentStage];
	}

	/** @return All stages in this life cycle, past, present, and future */
	public String [] getStages() {
		return theStages.clone();
	}

	/**
	 * @param stage The stage to check
	 * @return <0 if the current stage is before the given stage, 0, if the current stage is the given stage, or >0 if the current stage is
	 *         after the given stage
	 */
	public int isAfter(String stage) {
		int index = ArrayUtils.indexOf(theStages, stage);
		if(index < 0)
			throw new IllegalArgumentException("Unrecognized life cycle stage \"" + stage + "\"");
		return theCurrentStage - index;
	}

	/** @param listener The listener to be notified when the life cycle stage changes */
	public void addListener(LifeCycleListener listener) {
		synchronized(theListenerLock) {
			int idx = ArrayUtils.indexOf(theLifeCycleListeners, listener);
			if(idx < 0)
				theLifeCycleListeners = ArrayUtils.add(theLifeCycleListeners, listener);
		}
	}

	/** @param listener The listener to remove from notification */
	public void removeListener(LifeCycleListener listener) {
		synchronized(theListenerLock) {
			int idx = ArrayUtils.indexOf(theLifeCycleListeners, listener);
			if(idx >= 0)
				theLifeCycleListeners = ArrayUtils.remove(theLifeCycleListeners, idx);
		}
	}

	/**
	 * Runs a task at or after the given stage. If this life cycle is already past the given stage, the task will be run inline before
	 * returning. Otherwise, the task will be saved and run after the transition to the given stage is complete.
	 *
	 * @param task The task to run
	 * @param stage The stage to run the task for
	 * @param transition When to run the task relative to the given stage:
	 *            <ul>
	 *            <li><b>&lt;0</b> just before the transition takes place</li>
	 *            <li><b>0</b> after the transition is complete</li>
	 *            <li><b>1</b> just before transitioning away from the stage</li>
	 *            <li><b>&gt;1</b> after transitioning away from the stage</li>
	 *            </ul>
	 */
	public void runWhen(final Runnable task, final String stage, final int transition) {
		if(isAfter(stage) >= 0)
			task.run();
		else
			addListener(new LifeCycleListener() {
				@Override
				public void preTransition(String fromStage, String toStage) {
					if(transition < 0 && toStage.equals(stage))
						run();
					else if(transition == 1 && fromStage.equals(stage))
						run();
				}

				@Override
				public void postTransition(String oldStage, String newStage) {
					if(transition == 0 && newStage.equals(stage))
						run();
					else if(transition > 1 && oldStage.equals(stage))
						run();
				}

				void run() {
					task.run();
					removeListener(this);
				}
			});
	}

	/**
	 * @param stage The stage to add to this life cycle
	 * @param afterStage The stage (already registered in this life cycle manager) to add the new stage after
	 */
	public void addStage(String stage, String afterStage) {
		if(theCurrentStage > 0)
			theQuickElement.msg().error("Life cycle stages may not be added after the " + theStages[0] + " stage", "stage", stage);
		if(afterStage == null && theStages.length > 1) {
			theQuickElement.msg().error("afterStage must not be null--stages cannot be inserted before " + theStages[0], "stage", stage);
			return;
		}
		if(ArrayUtils.contains(theStages, stage))
			throw new IllegalArgumentException("Life cycle stage \"" + stage + "\" already exists in this life cycle manager");
		if(theStages.length == 1) {
			theStages = new String[] {stage, theStages[0]};
			return;
		}
		int idx = org.qommons.ArrayUtils.indexOf(theStages, afterStage);
		if(idx < 0) {
			theQuickElement.msg().error("afterStage \"" + afterStage + "\" not found. Cannot add stage.", "stage", stage);
			return;
		}
		if(idx == theStages.length - 1) {
			theQuickElement.msg().error(
				"afterStage \"" + afterStage + "\" is the last stage in this life cycle" + "--no stage can be added after it.", "stage",
				stage);
			return;
		}
		theStages = org.qommons.ArrayUtils.add(theStages, stage, idx + 1);
	}

	/** Advances the life cycle stage of the element to the given stage. Called from QuickElement. */
	private void advanceLifeCycle(String toStage) {
		String [] stages = theStages;
		LifeCycleListener [] listeners = theLifeCycleListeners;
		int goal = ArrayUtils.indexOf(stages, toStage);
		if(goal <= theCurrentStage) {
			theQuickElement.msg().error("Stage " + toStage + " has already been transitioned", "stage", toStage);
			return;
		}
		while(theCurrentStage < stages.length - 1 && !stages[theCurrentStage].equals(toStage)) {
			// Transition one stage forward
			String oldStage = stages[theCurrentStage];
			String newStage = stages[theCurrentStage + 1];
			/*
			 * Call listeners for the pre-transition in reverse order so that the first listener added gets notified just before the
			 * transition actually occurs so nobody has a chance to override its actions
			 */
			for(int L = listeners.length - 1; L >= 0; L--)
				listeners[L].preTransition(oldStage, newStage);
			theCurrentStage++;
			for(LifeCycleListener listener : listeners)
				listener.postTransition(oldStage, newStage);
		}
		if(theCurrentStage == theStages.length - 1) {
			// Can dispose of resources, since this instance is now effectively immutable
			theLifeCycleListeners = null;
		}
	}
}
