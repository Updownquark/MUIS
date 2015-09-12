package org.quick.core.model;

import java.awt.geom.Point2D;

import org.observe.DefaultObservable;
import org.observe.Observer;
import org.quick.core.QuickTextElement;
import org.quick.core.event.KeyBoardEvent;
import org.quick.core.event.MouseEvent;

/** Implements the text-selecting feature as a user drags over a text element. Also implements keyboard copying (Ctrl+C or Ctrl+X). */
public class TextSelectionBehavior implements QuickBehavior<QuickTextElement> {
	private class MouseListener implements org.observe.Action<MouseEvent> {
		MouseListener() {
			super();
		}

		private int theAnchor = -1;

		@Override
		public void act(MouseEvent event) {
			QuickTextElement element = (QuickTextElement) event.getElement();
			if(!(element.getDocumentModel() instanceof SelectableDocumentModel))
				return;
			switch (event.getType()) {
			case pressed:
				if(event.getButton() == MouseEvent.ButtonType.left) {
					SelectableDocumentModel doc = (SelectableDocumentModel) element.getDocumentModel();
					switch ((event.getClickCount() - 1) % 3) {
					case 0: // Single-click
						int position = Math.round(element.getDocumentModel().getPositionAt(event.getX(), event.getY(),
							element.bounds().getWidth()));
						if(event.isShiftPressed()) {
							theAnchor = doc.getSelectionAnchor();
						} else
							theAnchor = position;
						doc.setSelection(theAnchor, position);
						break;
					case 1: // Double-click. Select word.
						theAnchor = selectWord(doc,
							Math.round(element.getDocumentModel().getPositionAt(event.getX(), event.getY(), element.bounds().getWidth())));
						break;
					case 2: // Triple-click. Select line.
						if(!Boolean.TRUE.equals(element.atts().get(QuickTextElement.multiLine))) {
							theAnchor = 0;
							doc.setSelection(0, doc.length());
						} else
							theAnchor = selectLine(doc, Math.round(element.getDocumentModel().getPositionAt(event.getX(), event.getY(),
								element.bounds().getWidth())));
						break;
					}
				}
				break;
			case released:
				if(event.getButton() == MouseEvent.ButtonType.left)
					theAnchor = -1;
				break;
			case moved:
				if(theAnchor < 0)
					return;
				if(!element.getDocument().isButtonPressed(MouseEvent.ButtonType.left)) {
					theAnchor = -1;
					return;
				}
				int cursor = Math.round(element.getDocumentModel().getPositionAt(event.getX(), event.getY(), element.bounds().getWidth()));
				((SelectableDocumentModel) element.getDocumentModel()).setSelection(theAnchor, cursor);
				break;
			default:
			}
		}
	}

	private class KeyListener implements org.observe.Action<KeyBoardEvent> {
		@Override
		public void act(KeyBoardEvent event) {
			QuickTextElement text = (QuickTextElement) event.getElement();
			switch (event.getKeyCode()) {
			case C:
			case X:
				if(event.isControlPressed())
					copyToClipboard(text, event.getKeyCode() == KeyBoardEvent.KeyCode.X);
				event.use();
				break;
			case LEFT_ARROW:
				left(text, event.isShiftPressed());
				event.use();
				break;
			case RIGHT_ARROW:
				right(text, event.isShiftPressed());
				event.use();
				break;
			case UP_ARROW:
				up(text, event.isShiftPressed());
				event.use();
				break;
			case DOWN_ARROW:
				down(text, event.isShiftPressed());
				event.use();
				break;
			case HOME:
				home(text, event.isShiftPressed());
				event.use();
				break;
			case END:
				end(text, event.isShiftPressed());
				event.use();
				break;
			case PAGE_UP:
				pageUp(text, event.isShiftPressed());
				event.use();
				break;
			case PAGE_DOWN:
				pageDown(text, event.isShiftPressed());
				event.use();
				break;
			default:
			}
		}
	}

	private QuickTextElement theElement;

	private MouseListener theMouseListener = new MouseListener();
	private KeyListener theKeyListener = new KeyListener();
	private DefaultObservable<QuickTextElement> theUnsubscribeObservable = new DefaultObservable<>();
	private Observer<QuickTextElement> theUnsubscribeController = theUnsubscribeObservable.control(null);

	private int theCursorXLoc = -1;

	@Override
	public void install(QuickTextElement element) {
		if(theElement != null)
			throw new IllegalStateException(getClass().getSimpleName() + " may only be used with a single element");
		theElement = element;
		element.events().filterMap(MouseEvent.mouse).takeUntil(theUnsubscribeObservable.filter(el -> {
			return el == element;
		})).act(theMouseListener);
		element.events().filterMap(KeyBoardEvent.key.press()).takeUntil(theUnsubscribeObservable.filter(el -> {
			return el == element;
		})).act(theKeyListener);
		element.getDocumentModel().addContentListener(evt -> {
			theCursorXLoc = -1;
		});
		element.addTextSelectionListener(evt -> {
			theCursorXLoc = -1;
		});
	}

	@Override
	public void uninstall(QuickTextElement element) {
		theUnsubscribeController.onNext(element);
		if(theElement == element)
			theElement = null;
	}

	private void copyToClipboard(QuickTextElement element, boolean cut) {
		if(!(element.getDocumentModel() instanceof SelectableDocumentModel))
			return;
		SelectableDocumentModel doc = (SelectableDocumentModel) element.getDocumentModel();
		java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
			.setContents(new java.awt.datatransfer.StringSelection(doc.getSelectedText()), null);
		if(cut && element.getDocumentModel() instanceof MutableDocumentModel)
			((MutableDocumentModel) doc).delete(doc.getSelectionAnchor(), doc.getCursor());
	}

	private void left(QuickTextElement element, boolean shift) {
		if(!(element.getDocumentModel() instanceof SelectableDocumentModel))
			return;
		SelectableDocumentModel doc = (SelectableDocumentModel) element.getDocumentModel();
		int cursor = doc.getCursor() - 1;
		if(cursor < 0)
			cursor = 0;
		if(shift)
			doc.setSelection(doc.getSelectionAnchor(), cursor);
		else
			doc.setCursor(cursor);
	}

	private void right(QuickTextElement element, boolean shift) {
		if(!(element.getDocumentModel() instanceof SelectableDocumentModel))
			return;
		SelectableDocumentModel doc = (SelectableDocumentModel) element.getDocumentModel();
		int cursor = doc.getCursor() + 1;
		if(cursor > doc.length())
			cursor = doc.length();
		if(shift)
			doc.setSelection(doc.getSelectionAnchor(), cursor);
		else
			doc.setCursor(cursor);
	}

	private void up(QuickTextElement element, boolean shift) {
		if(!element.getStyle().getSelf().get(org.quick.core.style.FontStyle.wordWrap).get())
			return;
		if(!(element.getDocumentModel() instanceof SelectableDocumentModel))
			return;
		SelectableDocumentModel doc = (SelectableDocumentModel) element.getDocumentModel();
		Point2D loc = doc.getLocationAt(doc.getCursor(), element.bounds().getWidth());
		if(loc.getY() == 0)
			return; // Can't go up from here.
		int cursorXLoc = theCursorXLoc;
		if(cursorXLoc < 0)
			cursorXLoc = (int) loc.getX();
		int newCursor = Math.round(doc.getPositionAt(cursorXLoc, (float) loc.getY() - 1, element.bounds().getWidth()));

		if(shift)
			doc.setSelection(doc.getSelectionAnchor(), newCursor);
		else
			doc.setCursor(newCursor);
		theCursorXLoc = cursorXLoc;
	}

	private void down(QuickTextElement element, boolean shift) {
		if(!element.getStyle().getSelf().get(org.quick.core.style.FontStyle.wordWrap).get())
			return;
		if(!(element.getDocumentModel() instanceof SelectableDocumentModel))
			return;
		SelectableDocumentModel doc = (SelectableDocumentModel) element.getDocumentModel();
		int cursor = doc.getCursor();
		int cursorXLoc = theCursorXLoc;
		if(cursorXLoc < 0) {
			Point2D loc = doc.getLocationAt(cursor, element.bounds().getWidth());
			cursorXLoc = (int) loc.getX();
		}
		int y = -1;
		for(QuickDocumentModel.StyledSequenceMetric metric : doc.metrics(cursor, element.bounds().getWidth())) {
			if(metric.isNewLine()) {
				y = (int) metric.getTop();
				break;
			}
		}
		if(y < 0)
			return; // No newline after cursor. Can't go down.

		int newCursor = Math.round(doc.getPositionAt(cursorXLoc, y, element.bounds().getWidth()));

		if(shift)
			doc.setSelection(doc.getSelectionAnchor(), newCursor);
		else
			doc.setCursor(newCursor);
		theCursorXLoc = cursorXLoc;
	}

	private void home(QuickTextElement element, boolean shift) {
		if(!(element.getDocumentModel() instanceof SelectableDocumentModel))
			return;
		SelectableDocumentModel doc = (SelectableDocumentModel) element.getDocumentModel();
		int newCursor;
		if(!element.getStyle().getSelf().get(org.quick.core.style.FontStyle.wordWrap).get())
			newCursor = 0;
		else {
			Point2D loc = doc.getLocationAt(doc.getCursor(), element.bounds().getWidth());
			newCursor = (int) doc.getPositionAt(0, (float) loc.getY(), element.bounds().getWidth());
		}
		if(shift)
			doc.setSelection(doc.getSelectionAnchor(), newCursor);
		else
			doc.setCursor(newCursor);
	}

	private void end(QuickTextElement element, boolean shift) {
		if(!(element.getDocumentModel() instanceof SelectableDocumentModel))
			return;
		SelectableDocumentModel doc = (SelectableDocumentModel) element.getDocumentModel();
		int newCursor;
		if(!element.getStyle().getSelf().get(org.quick.core.style.FontStyle.wordWrap).get())
			newCursor = doc.length();
		else {
			Point2D loc = doc.getLocationAt(doc.getCursor(), element.bounds().getWidth());
			newCursor = (int) doc.getPositionAt(element.bounds().getWidth(), (float) loc.getY(), element.bounds().getWidth());
		}
		if(shift)
			doc.setSelection(doc.getSelectionAnchor(), newCursor);
		else
			doc.setCursor(newCursor);
	}

	private void pageUp(QuickTextElement element, boolean shift) {
		// TODO Auto-generated method stub
	}

	private void pageDown(QuickTextElement element, boolean shift) {
		// TODO Auto-generated method stub
	}

	private int selectWord(SelectableDocumentModel doc, int position) {
		int anchor;
		int cursor;
		if(position == doc.length())
			anchor = cursor = position;
		else {
			for(anchor = position; anchor > 0 && isWordChar(doc.charAt(anchor)); anchor--);
			if(anchor < position && !isWordChar(doc.charAt(anchor)))
				anchor++;
			for(cursor = position; cursor < doc.length() && isWordChar(doc.charAt(cursor)); cursor++);
		}
		doc.setSelection(anchor, cursor);
		return anchor;
	}

	private boolean isWordChar(char ch) {
		return Character.isLetterOrDigit(ch) || ch == '_' || ch == '\'';
	}

	private int selectLine(SelectableDocumentModel doc, int position) {
		int anchor = position;
		int cursor;
		if(doc.length() == 0)
			anchor = cursor = 0;
		else {
			if(position == doc.length())
				anchor--;
			for(; anchor > 0 && !isLineBreakChar(doc.charAt(anchor)); anchor--);
			if(anchor < position && isLineBreakChar(doc.charAt(anchor)))
				anchor++;
			for(cursor = position; cursor < doc.length() && !isLineBreakChar(doc.charAt(cursor)); cursor++);
			while(cursor < doc.length() && isLineBreakChar(doc.charAt(cursor + 1)))
				cursor++;
		}
		doc.setSelection(anchor, cursor);
		return anchor;
	}

	private boolean isLineBreakChar(char ch) {
		return ch == '\n' || ch == '\r';
	}
}
