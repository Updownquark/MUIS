package org.muis.core.model;

import org.muis.core.MuisElement;
import org.muis.core.MuisTextElement;
import org.muis.core.event.KeyBoardEvent;
import org.muis.core.event.MouseEvent;

/** Implements the text-selecting feature as a user drags over a text element. Also implements keyboard copying (Ctrl+C or Ctrl+X). */
public class TextSelectionBehavior implements MuisBehavior<MuisTextElement> {
	private static class MouseListener extends org.muis.core.event.MouseListener {
		MouseListener() {
			super(true);
		}

		private int theAnchor = -1;

		@Override
		public void mouseDown(MouseEvent mEvt, MuisElement element) {
			if(mEvt.getButtonType() == MouseEvent.ButtonType.LEFT) {
				theAnchor = Math.round(((MuisTextElement) element).getDocumentModel().getPositionAt(mEvt.getX(), mEvt.getY(),
					element.bounds().getWidth()));
				((MuisTextElement) element).getDocumentModel().setCursor(theAnchor);
			}
		}

		@Override
		public void mouseUp(MouseEvent mEvt, MuisElement element) {
			if(mEvt.getButtonType() == MouseEvent.ButtonType.LEFT)
				theAnchor = -1;
		}

		@Override
		public void mouseMoved(MouseEvent mEvt, MuisElement element) {
			if(theAnchor < 0)
				return;
			if(!element.getDocument().isButtonPressed(MouseEvent.ButtonType.LEFT)) {
				theAnchor = -1;
				return;
			}
			int cursor = Math.round(((MuisTextElement) element).getDocumentModel().getPositionAt(mEvt.getX(), mEvt.getY(),
				element.bounds().getWidth()));
			((MuisTextElement) element).getDocumentModel().setSelection(theAnchor, cursor);
		}
	}

	private static class KeyListener extends org.muis.core.event.KeyBoardListener {
		public KeyListener() {
			super(true);
		}

		@Override
		public void keyPressed(KeyBoardEvent kEvt, MuisElement element) {
			MuisTextElement text = (MuisTextElement) element;
			switch (kEvt.getKeyCode()) {
			case C:
			case V:
				if(element.getDocument().isControlPressed())
					copyToClipboard(text);
				break;
			case LEFT_ARROW:
				left(text, element.getDocument().isShiftPressed());
				break;
			case RIGHT_ARROW:
				right(text, element.getDocument().isShiftPressed());
				break;
			case UP_ARROW:
				up(text, element.getDocument().isShiftPressed());
				break;
			case DOWN_ARROW:
				down(text, element.getDocument().isShiftPressed());
				break;
			default:
			}
		}
	}

	@Override
	public void install(MuisTextElement element) {
		element.addListener(org.muis.core.MuisConstants.Events.MOUSE, new MouseListener());
		element.addListener(org.muis.core.MuisConstants.Events.KEYBOARD, new KeyListener());
	}

	@Override
	public void uninstall(MuisTextElement element) {
		element.removeListener(org.muis.core.MuisConstants.Events.MOUSE, MouseListener.class);
		element.removeListener(org.muis.core.MuisConstants.Events.KEYBOARD, KeyListener.class);
	}

	private static void copyToClipboard(MuisTextElement element) {
		// TODO
	}

	private static void left(MuisTextElement element, boolean shift) {
		SimpleDocumentModel model = element.getDocumentModel();
		int cursor = model.getCursor() - 1;
		if(cursor < 0)
			cursor = 0;
		if(shift)
			model.setSelection(model.getSelectionAnchor(), cursor);
		else
			model.setCursor(cursor);
	}

	private static void right(MuisTextElement element, boolean shift) {
		SimpleDocumentModel model = element.getDocumentModel();
		int cursor = model.getCursor() + 1;
		if(cursor > model.length())
			cursor = model.length();
		if(shift)
			model.setSelection(model.getSelectionAnchor(), cursor);
		else
			model.setCursor(cursor);
	}

	private static void up(MuisTextElement element, boolean shift) {
		// TODO Should really inspect for newlines before the current cursor and figure it out from there
	}

	private static void down(MuisTextElement element, boolean shift) {
		// TODO Should really inspect for newlines after the current cursor and figure it out from there
	}
}
