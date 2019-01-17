package org.quick.base.widget;

import static org.quick.base.BaseConstants.States.ENABLED;
import static org.quick.core.QuickConstants.States.CLICK;
import static org.quick.core.QuickConstants.States.FOCUS;
import static org.quick.core.layout.LayoutUtils.addRadius;
import static org.quick.core.layout.LayoutUtils.removeRadius;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.observe.ObservableAction;
import org.observe.ObservableValueEvent;
import org.quick.base.BaseConstants;
import org.quick.base.style.ButtonStyle;
import org.quick.core.Point;
import org.quick.core.QuickConstants;
import org.quick.core.event.KeyBoardEvent;
import org.quick.core.event.MouseEvent;
import org.quick.core.layout.SizeGuide;
import org.quick.core.mgr.StateEngine;
import org.quick.core.model.ModelAttributes;
import org.quick.core.style.BackgroundStyle;
import org.quick.core.style.Size;
import org.quick.core.tags.AcceptAttribute;
import org.quick.core.tags.QuickElementType;
import org.quick.core.tags.State;
import org.quick.motion.Animation;

/**
 * Implements a button. Buttons can be set to toggle mode or normal mode. Buttons are containers that may have any type of content in them.
 */
@QuickElementType(//
	attributes = { //
		@AcceptAttribute(declaringClass = ModelAttributes.class, field = "action", required = true)//
	}, states = { //
		@State(name = BaseConstants.States.DEPRESSED_NAME, priority = BaseConstants.States.DEPRESSED_PRIORITY), //
		@State(name = BaseConstants.States.ENABLED_NAME, priority = BaseConstants.States.ENABLED_PRIORITY)//
	})
public class Button extends SimpleContainer {
	interface ClickControl {
		void release(Object cause);

		void cancel(Object cause);
	}

	private final StateEngine.StateController theDepressedController;
	private final StateEngine.StateController theEnabledController;

	private ObservableAction<?> theAction;

	private AtomicReference<ClickControlImpl> theCurrentClick;

	/** Creates a button */
	public Button() {
		theDepressedController = state().control(BaseConstants.States.DEPRESSED);
		theEnabledController = state().control(ENABLED);
		theEnabledController.setActive(true, null);
		theCurrentClick = new AtomicReference<>();
		setFocusable(true);
		life().runWhen(() -> {
			theAction = createAction();
			theAction.isEnabled().changes().act(event -> {
				theEnabledController.setActive(event.getNewValue() == null, event);
				if (event.getNewValue() != null)
					theDepressedController.setActive(false, event);
			});
			state().observe(CLICK).changes().noInit().act(new Consumer<ObservableValueEvent<Boolean>>() {
				private Point theClickLocation;
				private ClickControl theMouseClick;

				@Override
				public void accept(ObservableValueEvent<Boolean> event) {
					if (!state().is(ENABLED))
						return;
					MouseEvent cause = event.getCauseLike(c -> c instanceof MouseEvent ? (MouseEvent) c : null);
					if (event.getNewValue()) {
						if (cause != null) {
							theClickLocation = cause.getPosition(Button.this);
							theMouseClick = press(cause);
						} else {
							theClickLocation = null;
							theMouseClick = cancel(theMouseClick, event);
						}
					} else if (theMouseClick != null) {
						Point click = theClickLocation;
						theClickLocation = null;
						checkDepressed(cause);
						if (cause.getType() != MouseEvent.MouseEventType.released) {
							theMouseClick = cancel(theMouseClick, event);
							return;
						}
						if (state().is(BaseConstants.States.DEPRESSED)) {
							theMouseClick = cancel(theMouseClick, event);
							return;
						}
						Point unclick = cause.getPosition(Button.this);
						int dx = click.x - unclick.x;
						int dy = click.y - unclick.y;
						double tol = Button.this.getStyle().get(org.quick.base.style.ButtonStyle.clickTolerance).get();
						if (dx > tol || dy > tol) {
							theMouseClick = cancel(theMouseClick, event);
							return;
						}
						double dist2 = dx * dx + dy * dy;
						if (dist2 > tol * tol) {
							theMouseClick = cancel(theMouseClick, event);
							return;
						}
						try {
							theMouseClick = release(theMouseClick, event);
						} catch (RuntimeException e) {
							msg().error("Action listener threw exception", e);
						}
					}
				}
			});
			state().observe(FOCUS).changes().noInit().act(evt -> {
				cancel(theCurrentClick.get(), evt);
			});
			events().filterMap(KeyBoardEvent.key).act(new Consumer<KeyBoardEvent>() {
				private ClickControl theKeyClick;

				@Override
				public void accept(KeyBoardEvent event) {
					if (event.wasPressed()) {
						theKeyClick = cancel(theKeyClick, event);
						if (event.getKeyCode() != KeyBoardEvent.KeyCode.SPACE && event.getKeyCode() != KeyBoardEvent.KeyCode.ENTER)
							return;
						if (!state().is(ENABLED))
							return;
						theKeyClick = press(event);
					} else if (theKeyClick != null) {
						if (event.getKeyCode() != KeyBoardEvent.KeyCode.SPACE && event.getKeyCode() != KeyBoardEvent.KeyCode.ENTER) {
							theKeyClick = cancel(theKeyClick, event);
							return;
						}
						if (!state().is(ENABLED)) {
							theKeyClick = cancel(theKeyClick, event);
							return;
						}
						checkDepressed(event); // Unsets the depressed state if appropriate
						if (state().is(BaseConstants.States.DEPRESSED)) {
							theKeyClick = cancel(theKeyClick, event);
							return;
						}
						try {
							theKeyClick = release(theKeyClick, event);
						} catch (RuntimeException e) {
							msg().error("Action listener threw exception", e);
						}
					}
				}
			});
			getStyle().get(BackgroundStyle.cornerRadius).changes().noInit().act(event -> relayout(false));
		}, QuickConstants.CoreStage.INITIALIZED.toString(), 1);
	}

	/**
	 * Utility method to cancel a click controller that may be null
	 *
	 * @param control The controller to cancel
	 * @param cause The cause of the cancellation
	 * @return null
	 */
	public static ClickControl cancel(ClickControl control, Object cause) {
		if (control != null)
			control.cancel(cause);
		return null;
	}

	/**
	 * Utility method to release a click controller that may be null
	 *
	 * @param control The controller to cancel
	 * @param cause The cause of the release
	 * @return null
	 */
	public static ClickControl release(ClickControl control, Object cause) {
		if (control != null)
			control.release(cause);
		return null;
	}

	/**
	 * Called by the constructor once to get the action for this button to perform.
	 *
	 * By default, this method accepts the {@link ModelAttributes#action} attribute and returns that value. Override this method to use a
	 * different action for the button.
	 *
	 * @return The action that this button will perform when it is clicked
	 */
	protected ObservableAction<?> createAction() {
		return ObservableAction.flatten(atts().get(ModelAttributes.action));
	}

	/**
	 * @param cause The cause of the press action
	 * @return The controller to control the button click
	 */
	public ClickControl press(Object cause) {
		if (!state().is(ENABLED))
			return new ClickControl() {
				@Override
				public void release(Object innerCause) {}

				@Override
				public void cancel(Object innerCause) {}
			};
		RepeatPressAnimation anim = new RepeatPressAnimation(cause);
		ClickControlImpl control = new ClickControlImpl(anim);
		theDepressedController.setActive(true, cause);
		org.quick.motion.AnimationManager.get().start(anim);
		return control;
	}

	/** @return The panel containing the contents of this button */
	public Block getContentPane() {
		return (Block) getElement(getTemplate().getAttachPoint("contents")).get();
	}

	@Override
	public void doLayout() {
		Size radius = getStyle().get(BackgroundStyle.cornerRadius).get();
		int w = bounds().getWidth();
		int h = bounds().getHeight();
		int contentW = removeRadius(w, radius);
		int contentH = removeRadius(h, radius);
		Block content = getContentPane();
		int lOff = (w - contentW) / 2;
		int tOff = (h - contentH) / 2;
		content.bounds().setBounds(lOff, tOff, w - lOff - lOff, h - tOff - tOff);
		super.doLayout();
	}

	@Override
	public SizeGuide getWSizer() {
		final Size radius = getStyle().get(BackgroundStyle.cornerRadius).get();
		return new RadiusAddSizePolicy(getContentPane().getWSizer(), radius);
	}

	@Override
	public SizeGuide getHSizer() {
		final Size radius = getStyle().get(BackgroundStyle.cornerRadius).get();
		return new RadiusAddSizePolicy(getContentPane().getHSizer(), radius);
	}

	private void checkDepressed(Object cause) {
		boolean pressed;
		if (!state().is(FOCUS) || !state().is(BaseConstants.States.ENABLED))
			pressed = false;
		else if (state().is(CLICK) || getDocument().isKeyPressed(KeyBoardEvent.KeyCode.SPACE)
			|| getDocument().isKeyPressed(KeyBoardEvent.KeyCode.ENTER))
			pressed = true;
		else
			pressed = false;
		if (!pressed)
			theDepressedController.setActive(pressed, cause);
	}

	private static class RadiusAddSizePolicy extends org.quick.core.layout.AbstractSizeGuide {
		private final SizeGuide theWrapped;

		private Size theRadius;

		RadiusAddSizePolicy(SizeGuide wrap, Size rad) {
			theWrapped = wrap;
			theRadius = rad;
		}

		@Override
		public int getMinPreferred(int crossSize, boolean csMax) {
			int ret = addRadius(theWrapped.getMinPreferred(removeRadius(crossSize, theRadius), csMax), theRadius);
			return ret;
		}

		@Override
		public int getMaxPreferred(int crossSize, boolean csMax) {
			int ret = addRadius(theWrapped.getMaxPreferred(removeRadius(crossSize, theRadius), csMax), theRadius);
			return ret;
		}

		@Override
		public int getMin(int crossSize, boolean csMax) {
			int ret = addRadius(theWrapped.getMin(removeRadius(crossSize, theRadius), csMax), theRadius);
			return ret;
		}

		@Override
		public int getPreferred(int crossSize, boolean csMax) {
			int ret = addRadius(theWrapped.getPreferred(removeRadius(crossSize, theRadius), csMax), theRadius);
			return ret;
		}

		@Override
		public int getMax(int crossSize, boolean csMax) {
			int ret = addRadius(theWrapped.getMax(removeRadius(crossSize, theRadius), csMax), theRadius);
			return ret;
		}

		@Override
		public int getBaseline(int size) {
			int remove = size - removeRadius(size, theRadius);
			int ret = theWrapped.getBaseline(size - remove * 2);
			return ret + remove;
		}
	}

	private class ClickControlImpl implements ClickControl {
		private final RepeatPressAnimation theAnimation;

		ClickControlImpl(RepeatPressAnimation animation) {
			theAnimation = animation;
		}

		@Override
		public void release(Object cause) {
			cancel(cause);
			if (theAction.isEnabled().get() == null)
				theAction.act(cause);
		}

		@Override
		public void cancel(Object cause) {
			theAnimation.stop();
			checkDepressed(cause);
		}

		@Override
		protected void finalize() throws Throwable {
			cancel(null);
			super.finalize();
		}
	}

	private class RepeatPressAnimation implements Animation {
		private final Object theCause;
		private final long theDelay;
		private volatile boolean isRunning = true;

		RepeatPressAnimation(Object cause) {
			theCause = cause;
			theDelay = getStyle().get(ButtonStyle.actionRepeatDelay).get().toMillis();
		}

		@Override
		public boolean update(long time) {
			if (!isRunning)
				return true;
			if (time < theDelay)
				return false;
			if (theAction.isEnabled().get() != null)
				return true;
			theAction.act(theCause);
			return false;
		}

		@Override
		public long getMaxFrequency() {
			return getStyle().get(ButtonStyle.actionRepeatFrequency).get().toMillis();
		}

		void stop() {
			isRunning = false;
		}
	}
}
