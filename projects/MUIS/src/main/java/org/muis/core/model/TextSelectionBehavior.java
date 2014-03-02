package org.muis.core.model;

import java.awt.geom.Point2D;

import org.muis.core.MuisElement;
import org.muis.core.MuisTextElement;
import org.muis.core.event.KeyBoardEvent;
import org.muis.core.event.MouseEvent;

/** Implements the text-selecting feature as a user drags over a text element. Also implements keyboard copying (Ctrl+C or Ctrl+X). */
public class TextSelectionBehavior implements MuisBehavior<MuisTextElement> {
	private class MouseListener extends org.muis.core.event.MouseListener {
		MouseListener() {
			super(true);
		}

		private int theAnchor = -1;

		@Override
		public void mouseDown(MouseEvent mEvt, MuisElement element) {
			if(mEvt.getButtonType() == MouseEvent.ButtonType.LEFT) {
				int position = Math.round(((MuisTextElement) element).getDocumentModel().getPositionAt(mEvt.getX(), mEvt.getY(),
					element.bounds().getWidth()));
				SimpleDocumentModel doc = ((MuisTextElement) element).getDocumentModel();
				if(element.getDocument().isShiftPressed()) {
					theAnchor = doc.getSelectionAnchor();
				} else
					theAnchor = position;
				doc.setSelection(theAnchor, position);
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

	private class KeyListener extends org.muis.core.event.KeyBoardListener {
		public KeyListener() {
			super(true);
		}

		@Override
		public void keyPressed(KeyBoardEvent kEvt, MuisElement element) {
			MuisTextElement text = (MuisTextElement) element;
			switch (kEvt.getKeyCode()) {
			case C:
			case X:
				if(element.getDocument().isControlPressed())
					copyToClipboard(text, kEvt.getKeyCode() == KeyBoardEvent.KeyCode.X);
				kEvt.cancel();
				break;
			case LEFT_ARROW:
				left(text, element.getDocument().isShiftPressed());
				kEvt.cancel();
				break;
			case RIGHT_ARROW:
				right(text, element.getDocument().isShiftPressed());
				kEvt.cancel();
				break;
			case UP_ARROW:
				up(text, element.getDocument().isShiftPressed());
				kEvt.cancel();
				break;
			case DOWN_ARROW:
				down(text, element.getDocument().isShiftPressed());
				kEvt.cancel();
				break;
			case HOME:
				home(text, element.getDocument().isShiftPressed());
				kEvt.cancel();
				break;
			case END:
				end(text, element.getDocument().isShiftPressed());
				kEvt.cancel();
				break;
			case PAGE_UP:
				pageUp(text, element.getDocument().isShiftPressed());
				kEvt.cancel();
				break;
			case PAGE_DOWN:
				pageDown(text, element.getDocument().isShiftPressed());
				kEvt.cancel();
				break;
			default:
			}
		}
	}

	private MuisTextElement theElement;

	private int theCursorXLoc = -1;

	@Override
	public void install(MuisTextElement element) {
		if(theElement != null)
			throw new IllegalStateException(getClass().getSimpleName() + " may only be used with a single element");
		theElement = element;
		element.addListener(org.muis.core.MuisConstants.Events.MOUSE, new MouseListener());
		element.addListener(org.muis.core.MuisConstants.Events.KEYBOARD, new KeyListener());
		element.getDocumentModel().addContentListener(new MuisDocumentModel.ContentListener() {
			@Override
			public void contentChanged(MuisDocumentModel.ContentChangeEvent evt) {
				theCursorXLoc = -1;
			}
		});
		element.getDocumentModel().addSelectionListener(new SelectableDocumentModel.SelectionListener() {
			@Override
			public void selectionChanged(SelectableDocumentModel.SelectionChangeEvent evt) {
				theCursorXLoc = -1;
			}
		});
	}

	@Override
	public void uninstall(MuisTextElement element) {
		element.removeListener(org.muis.core.MuisConstants.Events.MOUSE, MouseListener.class);
		element.removeListener(org.muis.core.MuisConstants.Events.KEYBOARD, KeyListener.class);
		if(theElement == element)
			theElement = null;
	}

	private void copyToClipboard(MuisTextElement element, boolean cut) {
		SimpleDocumentModel doc = element.getDocumentModel();
		java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
			.setContents(new java.awt.datatransfer.StringSelection(doc.getSelectedText()), null);
		if(cut)
			doc.delete(doc.getSelectionAnchor(), doc.getCursor());
	}

	private void left(MuisTextElement element, boolean shift) {
		SimpleDocumentModel model = element.getDocumentModel();
		int cursor = model.getCursor() - 1;
		if(cursor < 0)
			cursor = 0;
		if(shift)
			model.setSelection(model.getSelectionAnchor(), cursor);
		else
			model.setCursor(cursor);
	}

	private void right(MuisTextElement element, boolean shift) {
		SimpleDocumentModel model = element.getDocumentModel();
		int cursor = model.getCursor() + 1;
		if(cursor > model.length())
			cursor = model.length();
		if(shift)
			model.setSelection(model.getSelectionAnchor(), cursor);
		else
			model.setCursor(cursor);
	}

	private void up(MuisTextElement element, boolean shift) {
		if(!element.getStyle().getSelf().get(org.muis.core.style.FontStyle.wordWrap))
			return;
		SimpleDocumentModel model = element.getDocumentModel();
		Point2D loc = model.getLocationAt(model.getCursor(), element.bounds().getWidth());
		if(loc.getY() == 0)
			return; // Can't go up from here.
		int cursorXLoc = theCursorXLoc;
		if(cursorXLoc < 0)
			cursorXLoc = (int) loc.getX();
		int newCursor = Math.round(model
			.getPositionAt(cursorXLoc, (float) loc.getY() - 1, element.bounds().getWidth()));

		if(shift)
			model.setSelection(model.getSelectionAnchor(), newCursor);
		else
			model.setCursor(newCursor);
		theCursorXLoc = cursorXLoc;
	}

	private void down(MuisTextElement element, boolean shift) {
		if(!element.getStyle().getSelf().get(org.muis.core.style.FontStyle.wordWrap))
			return;
		SimpleDocumentModel model = element.getDocumentModel();
		int cursor = element.getDocumentModel().getCursor();
		int cursorXLoc = theCursorXLoc;
		if(cursorXLoc < 0) {
			Point2D loc = model.getLocationAt(cursor, element.bounds().getWidth());
			cursorXLoc = (int) loc.getX();
		}
		int y = -1;
		for(MuisDocumentModel.StyledSequenceMetric metric : model.metrics(cursor, element.bounds().getWidth())) {
			if(metric.isNewLine()) {
				y = (int) metric.getTop();
				break;
			}
		}
		if(y < 0)
			return; // No newline after cursor. Can't go down.

		int newCursor = Math.round(model.getPositionAt(cursorXLoc, y, element.bounds().getWidth()));

		if(shift)
			model.setSelection(model.getSelectionAnchor(), newCursor);
		else
			model.setCursor(newCursor);
		theCursorXLoc = cursorXLoc;
	}

	private void home(MuisTextElement element, boolean shift) {
		SimpleDocumentModel model = element.getDocumentModel();
		int newCursor;
		if(!element.getStyle().getSelf().get(org.muis.core.style.FontStyle.wordWrap))
			newCursor = 0;
		else {
			Point2D loc = model.getLocationAt(model.getCursor(), element.bounds().getWidth());
			newCursor = (int) model.getPositionAt(0, (float) loc.getY(), element.bounds().getWidth());
		}
		if(shift)
			model.setSelection(model.getSelectionAnchor(), newCursor);
		else
			model.setCursor(newCursor);
	}

	private void end(MuisTextElement element, boolean shift) {
		SimpleDocumentModel model = element.getDocumentModel();
		int newCursor;
		if(!element.getStyle().getSelf().get(org.muis.core.style.FontStyle.wordWrap))
			newCursor = model.length();
		else {
			Point2D loc = model.getLocationAt(model.getCursor(), element.bounds().getWidth());
			newCursor = (int) model.getPositionAt(element.bounds().getWidth(), (float) loc.getY(),
				element.bounds().getWidth());
		}
		if(shift)
			model.setSelection(model.getSelectionAnchor(), newCursor);
		else
			model.setCursor(newCursor);
	}

	private void pageUp(MuisTextElement element, boolean shift) {
		// TODO Auto-generated method stub
	}

	private void pageDown(MuisTextElement element, boolean shift) {
		// TODO Auto-generated method stub
	}
}
