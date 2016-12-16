package org.quick.base.widget;

import static org.quick.core.QuickConstants.States.CLICK;
import static org.quick.core.layout.LayoutUtils.addRadius;
import static org.quick.core.layout.LayoutUtils.removeRadius;

import java.awt.Point;

import org.observe.Action;
import org.observe.ObservableAction;
import org.observe.ObservableValueEvent;
import org.quick.base.BaseConstants;
import org.quick.core.QuickConstants;
import org.quick.core.event.KeyBoardEvent;
import org.quick.core.event.MouseEvent;
import org.quick.core.event.QuickEvent;
import org.quick.core.layout.SizeGuide;
import org.quick.core.mgr.StateEngine;
import org.quick.core.model.ModelAttributes;
import org.quick.core.style.BackgroundStyle;
import org.quick.core.style.Size;
import org.quick.core.tags.AcceptAttribute;
import org.quick.core.tags.QuickElementType;
import org.quick.core.tags.State;

/** Implements a button. Buttons can be set to toggle mode or normal mode. Buttons are containers that may have any type of content in them. */
@QuickElementType(//
	attributes = { //
		@AcceptAttribute(declaringClass = ModelAttributes.class, field = "action", required = true)//
	}, states = { //
		@State(name = BaseConstants.States.DEPRESSED_NAME, priority = BaseConstants.States.DEPRESSED_PRIORITY), //
		@State(name = BaseConstants.States.ENABLED_NAME, priority = BaseConstants.States.ENABLED_PRIORITY)//
	})
public class Button extends SimpleContainer {
	private final StateEngine.StateController theDepressedController;
	private final StateEngine.StateController theEnabledController;

	private ObservableAction<?> theAction;

	/** Creates a button */
	public Button() {
		theDepressedController = state().control(BaseConstants.States.DEPRESSED);
		theEnabledController = state().control(BaseConstants.States.ENABLED);
		theEnabledController.set(true, null);
		setFocusable(true);
		life().runWhen(() -> {
			theAction = createAction();
			theAction.isEnabled().act(event -> {
				theEnabledController.set(event.getValue() == null, event);
				if (event.getValue() != null)
					theDepressedController.set(false, event);
			});
			state().observe(CLICK).noInit().act(new Action<ObservableValueEvent<Boolean>>() {
				private Point theClickLocation;

				@Override
				public void act(ObservableValueEvent<Boolean> event) {
					if(!state().is(BaseConstants.States.ENABLED))
						return;
					MouseEvent cause = event.getCauseLike(c -> c instanceof MouseEvent ? (MouseEvent) c : null);
					if(event.getValue()) {
						if (cause != null) {
							theClickLocation = cause.getPosition(Button.this);
							theDepressedController.set(true, cause);
						} else
							theClickLocation = null;
					} else {
						Point click = theClickLocation;
						theClickLocation = null;
						checkDepressed(cause);
						if (cause.getType() != MouseEvent.MouseEventType.released)
							return;
						if(state().is(BaseConstants.States.DEPRESSED))
							return;
						Point unclick = cause.getPosition(Button.this);
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
		return ObservableAction.flatten(atts().getHolder(ModelAttributes.action));
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
}