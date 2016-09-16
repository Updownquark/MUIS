package org.quick.base.widget;

import java.awt.Point;

import org.observe.Action;
import org.observe.ObservableAction;
import org.quick.base.BaseConstants;
import org.quick.core.QuickConstants;
import org.quick.core.event.*;
import org.quick.core.layout.SizeGuide;
import org.quick.core.mgr.StateEngine;
import org.quick.core.model.ModelAttributes;
import org.quick.core.style.BackgroundStyle;
import org.quick.core.tags.State;
import org.quick.core.tags.StateSupport;
import org.quick.core.tags.Template;

/** Implements a button. Buttons can be set to toggle mode or normal mode. Buttons are containers that may have any type of content in them. */
@Template(location = "../../../../simple-container.qml")
@StateSupport({@State(name = BaseConstants.States.DEPRESSED_NAME, priority = BaseConstants.States.DEPRESSED_PRIORITY),
		@State(name = BaseConstants.States.ENABLED_NAME, priority = BaseConstants.States.ENABLED_PRIORITY)})
public class Button extends org.quick.core.QuickTemplate {
	private final StateEngine.StateController theDepressedController;

	private final StateEngine.StateController theEnabledController;

	private final ObservableAction<?> theAction;

	/** Creates a button */
	public Button() {
		theAction = createAction();
		theDepressedController = state().control(BaseConstants.States.DEPRESSED);
		theEnabledController = state().control(BaseConstants.States.ENABLED);
		theEnabledController.set(true, null);
		setFocusable(true);
		life().runWhen(() -> {
			theAction.isEnabled().act(event -> {
				theEnabledController.set(event.getValue() == null, event);
				if (event.getValue() != null)
					theDepressedController.set(false, event);
			});
			events().filterMap(StateChangedEvent.state(QuickConstants.States.CLICK)).act(new Action<StateChangedEvent>() {
				private Point theClickLocation;

				@Override
				public void act(StateChangedEvent event) {
					if(!state().is(BaseConstants.States.ENABLED))
						return;
					QuickEvent cause = event.getCause();
					if(event.getValue()) {
						if(cause instanceof MouseEvent) {
							theClickLocation = ((MouseEvent) cause).getPosition(Button.this);
							theDepressedController.set(true, cause);
						} else
							theClickLocation = null;
					} else {
						Point click = theClickLocation;
						theClickLocation = null;
						checkDepressed(cause);
						if(click == null || !(cause instanceof MouseEvent)
							|| ((MouseEvent) cause).getType() != MouseEvent.MouseEventType.released) {
							return;
						}
						if(state().is(BaseConstants.States.DEPRESSED))
							return;
						Point unclick = ((MouseEvent) cause).getPosition(Button.this);
						int dx = click.x - unclick.x;
						int dy = click.y - unclick.y;
						double tol = Button.this.getStyle().get(org.quick.base.style.ButtonStyle.clickTolerance).get();
						if(dx > tol || dy > tol)
							return;
						double dist2 = dx * dx + dy * dy;
						if(dist2 > tol * tol)
							return;
						try {
							theAction.act(event);
						} catch (RuntimeException e) {
							msg().error("Action listener threw exception", e);
						}
					}
				}
			});
			events().filterMap(KeyBoardEvent.key).act(event -> {
				if(event.wasPressed()) {
					if(event.getKeyCode() != KeyBoardEvent.KeyCode.SPACE && event.getKeyCode() != KeyBoardEvent.KeyCode.ENTER)
						return;
					if(!state().is(BaseConstants.States.ENABLED))
						return;
					theDepressedController.set(true, event);
				} else {
					if(event.getKeyCode() != KeyBoardEvent.KeyCode.SPACE && event.getKeyCode() != KeyBoardEvent.KeyCode.ENTER)
						return;
					if(!state().is(BaseConstants.States.ENABLED))
						return;
					checkDepressed(event); // Unsets the depressed state if appropriate
					if(state().is(BaseConstants.States.DEPRESSED))
						return;
					try {
						theAction.act(event);
					} catch (RuntimeException e) {
						msg().error("Action listener threw exception", e);
					}
				}
			});
			getStyle().get(BackgroundStyle.cornerRadius).act(event -> relayout(false));
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
		return ObservableAction.flatten(atts().accept(new Object(), ModelAttributes.action));
	}

	/** @return The panel containing the contents of this button */
	public Block getContentPane() {
		return (Block) getElement(getTemplate().getAttachPoint("contents")).get();
	}

	@Override
	public void doLayout() {
		org.quick.core.style.Size radius = getStyle().get(BackgroundStyle.cornerRadius).get();
		int w = bounds().getWidth();
		int h = bounds().getHeight();
		int lOff = radius.evaluate(w);
		int tOff = radius.evaluate(h);
		getContentPane().bounds().setBounds(lOff, tOff, w - lOff - lOff, h - tOff - tOff);
	}

	@Override
	public SizeGuide getWSizer() {
		final org.quick.core.style.Size radius = getStyle().get(BackgroundStyle.cornerRadius).get();
		return new RadiusAddSizePolicy(getContentPane().getWSizer(), radius);
	}

	@Override
	public SizeGuide getHSizer() {
		final org.quick.core.style.Size radius = getStyle().get(BackgroundStyle.cornerRadius).get();
		return new RadiusAddSizePolicy(getContentPane().getHSizer(), radius);
	}

	private void checkDepressed(QuickEvent cause) {
		boolean pressed;
		if(!state().is(QuickConstants.States.FOCUS) || !state().is(BaseConstants.States.ENABLED))
			pressed = false;
		else if(state().is(QuickConstants.States.CLICK) || getDocument().isKeyPressed(KeyBoardEvent.KeyCode.SPACE)
			|| getDocument().isKeyPressed(KeyBoardEvent.KeyCode.ENTER))
			pressed = true;
		else
			pressed = false;
		if(!pressed)
			theDepressedController.set(pressed, cause);
	}

	private static class RadiusAddSizePolicy extends org.quick.core.layout.AbstractSizeGuide {
		private final SizeGuide theWrapped;

		private org.quick.core.style.Size theRadius;

		RadiusAddSizePolicy(SizeGuide wrap, org.quick.core.style.Size rad) {
			theWrapped = wrap;
			theRadius = rad;
		}

		@Override
		public int getMinPreferred(int crossSize, boolean csMax) {
			return addRadius(theWrapped.getMinPreferred(crossSize, csMax));
		}

		@Override
		public int getMaxPreferred(int crossSize, boolean csMax) {
			return addRadius(theWrapped.getMaxPreferred(crossSize, csMax));
		}

		@Override
		public int getMin(int crossSize, boolean csMax) {
			return addRadius(theWrapped.getMin(removeRadius(crossSize), csMax));
		}

		@Override
		public int getPreferred(int crossSize, boolean csMax) {
			return addRadius(theWrapped.getPreferred(removeRadius(crossSize), csMax));
		}

		@Override
		public int getMax(int crossSize, boolean csMax) {
			return addRadius(theWrapped.getMax(removeRadius(crossSize), csMax));
		}

		@Override
		public int getBaseline(int size) {
			int remove = size - removeRadius(size);
			int ret = theWrapped.getBaseline(size - remove * 2);
			return ret + remove;
		}

		int addRadius(int size) {
			switch (theRadius.getUnit()) {
			case pixels:
			case lexips:
				size += theRadius.getValue() * 2;
				break;
			case percent:
				float radPercent = theRadius.getValue() * 2;
				if(radPercent >= 100)
					radPercent = 90;
				size = Math.round(size * 100 / (100f - radPercent));
				break;
			}
			if(size < 0)
				return Integer.MAX_VALUE;
			return size;
		}

		int removeRadius(int size) {
			return size - theRadius.evaluate(size);
		}
	}
}
