package org.quick.widget.base;

import static org.quick.base.BaseConstants.States.ENABLED;
import static org.quick.core.QuickConstants.States.CLICK;
import static org.quick.core.QuickConstants.States.FOCUS;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.observe.ObservableAction;
import org.observe.ObservableValueEvent;
import org.quick.base.BaseConstants;
import org.quick.base.style.ButtonStyle;
import org.quick.base.widget.Button;
import org.quick.core.Point;
import org.quick.core.QuickConstants;
import org.quick.core.layout.LayoutGuideType;
import org.quick.core.layout.Orientation;
import org.quick.core.mgr.StateEngine;
import org.quick.core.model.ModelAttributes;
import org.quick.core.style.BackgroundStyle;
import org.quick.core.style.Size;
import org.quick.motion.Animation;
import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.QuickWidgetDocument;
import org.quick.widget.core.event.KeyBoardEvent;
import org.quick.widget.core.event.MouseEvent;
import org.quick.widget.core.layout.LayoutUtils;
import org.quick.widget.core.layout.SizeGuide;

public class ButtonWidget extends SimpleContainerWidget {
	interface ClickControl {
		void release(Object cause);

		void cancel(Object cause);
	}

	private final StateEngine.StateController theDepressedController;
	private final StateEngine.StateController theEnabledController;

	private AtomicReference<ClickControlImpl> theCurrentClick;

	private ObservableAction<?> theAction;

	public ButtonWidget(QuickWidgetDocument doc, Button element, QuickWidget parent) {
		super(doc, element, parent);
		theDepressedController = getElement().state().control(BaseConstants.States.DEPRESSED);
		theEnabledController = getElement().state().control(ENABLED);
		theEnabledController.setActive(true, null);
		theCurrentClick = new AtomicReference<>();
		getElement().life().runWhen(() -> {
			theAction = createAction();
			theAction.isEnabled().changes().act(event -> {
				if (!event.isInitial() && (event.getOldValue() == null) == (event.getNewValue() == null))
					return;
				theEnabledController.setActive(event.getNewValue() == null, event);
				if (event.getNewValue() != null)
					theDepressedController.setActive(false, event);
			});
			getElement().state().observe(CLICK).changes().noInit().act(new Consumer<ObservableValueEvent<Boolean>>() {
				private Point theClickLocation;
				private ClickControl theMouseClick;

				@Override
				public void accept(ObservableValueEvent<Boolean> event) {
					if (!getElement().state().is(ENABLED))
						return;
					MouseEvent cause = event.getCauseLike(c -> c instanceof MouseEvent ? (MouseEvent) c : null);
					if (event.getNewValue()) {
						if (cause != null) {
							theClickLocation = cause.getPosition(ButtonWidget.this);
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
						if (getElement().state().is(BaseConstants.States.DEPRESSED)) {
							theMouseClick = cancel(theMouseClick, event);
							return;
						}
						Point unclick = cause.getPosition(ButtonWidget.this);
						int dx = click.x - unclick.x;
						int dy = click.y - unclick.y;
						double tol = getElement().getStyle().get(org.quick.base.style.ButtonStyle.clickTolerance).get();
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
							getElement().msg().error("Action listener threw exception", e);
						}
					}
				}
			});
			getElement().state().observe(FOCUS).changes().noInit().act(evt -> {
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
						if (!getElement().state().is(ENABLED))
							return;
						theKeyClick = press(event);
					} else if (theKeyClick != null) {
						if (event.getKeyCode() != KeyBoardEvent.KeyCode.SPACE && event.getKeyCode() != KeyBoardEvent.KeyCode.ENTER) {
							theKeyClick = cancel(theKeyClick, event);
							return;
						}
						if (!getElement().state().is(ENABLED)) {
							theKeyClick = cancel(theKeyClick, event);
							return;
						}
						checkDepressed(event); // Unsets the depressed state if appropriate
						if (getElement().state().is(BaseConstants.States.DEPRESSED)) {
							theKeyClick = cancel(theKeyClick, event);
							return;
						}
						try {
							theKeyClick = release(theKeyClick, event);
						} catch (RuntimeException e) {
							getElement().msg().error("Action listener threw exception", e);
						}
					}
				}
			});
			getElement().getStyle().get(BackgroundStyle.cornerRadius).changes().noInit().act(event -> relayout(false));
		}, QuickConstants.CoreStage.INITIALIZED.toString(), 1);
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
		return ObservableAction.flatten(getElement().atts().get(ModelAttributes.action));
	}

	@Override
	public Button getElement() {
		return (Button) super.getElement();
	}

	public BlockWidget getContentPane() {
		return (BlockWidget) getChild(getElement().getContentPane());
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
	 * @param cause The cause of the press action
	 * @return The controller to control the button click
	 */
	public ClickControl press(Object cause) {
		if (!getElement().state().is(ENABLED))
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

	@Override
	public void doLayout() {
		Size radius = getElement().getStyle().get(BackgroundStyle.cornerRadius).get();
		int w = bounds().getWidth();
		int h = bounds().getHeight();
		int contentW = LayoutUtils.removeRadius(w, radius);
		int contentH = LayoutUtils.removeRadius(h, radius);
		BlockWidget content = getContentPane();
		int lOff = (w - contentW) / 2;
		int tOff = (h - contentH) / 2;
		content.bounds().setBounds(lOff, tOff, w - lOff - lOff, h - tOff - tOff);
		super.doLayout();
	}

	@Override
	public SizeGuide getSizer(Orientation orientation) {
		final Size radius = getElement().getStyle().get(BackgroundStyle.cornerRadius).get();
		return new RadiusAddSizePolicy(getContentPane().getSizer(orientation), radius);
	}

	private void checkDepressed(Object cause) {
		boolean pressed;
		if (!getElement().state().is(FOCUS) || !getElement().state().is(BaseConstants.States.ENABLED))
			pressed = false;
		else if (getElement().state().is(CLICK) || getDocument().isKeyPressed(KeyBoardEvent.KeyCode.SPACE)
			|| getDocument().isKeyPressed(KeyBoardEvent.KeyCode.ENTER))
			pressed = true;
		else
			pressed = false;
		if (!pressed)
			theDepressedController.setActive(pressed, cause);
	}

	private static class RadiusAddSizePolicy implements SizeGuide.GenericSizeGuide {
		private final SizeGuide theWrapped;

		private Size theRadius;

		RadiusAddSizePolicy(SizeGuide wrap, Size rad) {
			theWrapped = wrap;
			theRadius = rad;
		}

		@Override
		public int get(LayoutGuideType type, int crossSize, boolean csMax) {
			int ret = LayoutUtils.addRadius(theWrapped.get(type, LayoutUtils.removeRadius(crossSize, theRadius), csMax), theRadius);
			return ret;
		}

		@Override
		public int getBaseline(int size) {
			int remove = size - LayoutUtils.removeRadius(size, theRadius);
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
			theDelay = getElement().getStyle().get(ButtonStyle.actionRepeatDelay).get().toMillis();
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
			return getElement().getStyle().get(ButtonStyle.actionRepeatFrequency).get().toMillis();
		}

		void stop() {
			isRunning = false;
		}
	}
}
