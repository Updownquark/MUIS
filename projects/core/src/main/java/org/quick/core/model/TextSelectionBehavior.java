package org.quick.core.model;

import java.awt.geom.Point2D;
import java.util.function.Consumer;

import org.observe.SimpleObservable;
import org.qommons.Transaction;
import org.quick.core.QuickTextElement;
import org.quick.core.event.KeyBoardEvent;
import org.quick.core.event.MouseEvent;
import org.quick.core.model.QuickDocumentModel.ContentChangeEvent;
import org.quick.core.model.SelectableDocumentModel.SelectionChangeEvent;

/** Implements the text-selecting feature as a user drags over a text element. Also implements keyboard copying (Ctrl+C or Ctrl+X). */
public class TextSelectionBehavior implements QuickBehavior<QuickTextElement> {
	private class MouseListener implements Consumer<MouseEvent> {
		MouseListener() {
			super();
		}

		private int theAnchor = -1;

		@Override
		public void accept(MouseEvent event) {
			QuickTextElement element = (QuickTextElement) event.getElement();
			if (!(element.getDocumentModel().get() instanceof SelectableDocumentModel))
				return;
			SelectableDocumentModel selectableDoc = (SelectableDocumentModel) element.getDocumentModel().get();
			switch (event.getType()) {
			case pressed:
				if (event.getButton() == MouseEvent.ButtonType.left) {
					SelectableDocumentModel doc = (SelectableDocumentModel) element.getDocumentModel().get();
					switch ((event.getClickCount() - 1) % 3) {
					case 0: // Single-click
						int position = Math.round(selectableDoc.getPositionAt(event.getX(), event.getY(), element.bounds().getWidth()));
						if (event.isShiftPressed()) {
							theAnchor = doc.getSelectionAnchor();
						} else
							theAnchor = position;
						try (Transaction t = doc.holdForWrite(event)) {
							doc.setSelection(theAnchor, position);
						}
						break;
					case 1: // Double-click. Select word.
						int pos = Math.round(selectableDoc.getPositionAt(event.getX(), event.getY(), element.bounds().getWidth()));
						theAnchor = selectWord(doc, pos, event);
						break;
					case 2: // Triple-click. Select line.
						if (!Boolean.TRUE.equals(element.atts().get(QuickTextElement.multiLine))) {
							theAnchor = 0;
							try (Transaction t = doc.holdForWrite(event)) {
								doc.setSelection(0, doc.length());
							}
						} else {
							pos = Math.round(selectableDoc.getPositionAt(event.getX(), event.getY(), element.bounds().getWidth()));
							theAnchor = selectLine(doc, pos, event);
						}
						break;
					}
				}
				break;
			case released:
				if (event.getButton() == MouseEvent.ButtonType.left)
					theAnchor = -1;
				break;
			case moved:
				if (theAnchor < 0)
					return;
				if (!element.getDocument().isButtonPressed(MouseEvent.ButtonType.left)) {
					theAnchor = -1;
					return;
				}
				int cursor = Math.round(selectableDoc.getPositionAt(event.getX(), event.getY(), element.bounds().getWidth()));
				try (Transaction t = selectableDoc.holdForWrite(event)) {
					selectableDoc.setSelection(theAnchor, cursor);
				}
				break;
			default:
			}
		}
	}

	private class KeyListener implements Consumer<KeyBoardEvent> {
		@Override
		public void accept(KeyBoardEvent event) {
			QuickTextElement text = (QuickTextElement) event.getElement();
			if (!(text.getDocumentModel().get() instanceof SelectableDocumentModel))
				return;
			SelectableDocumentModel doc = (SelectableDocumentModel) text.getDocumentModel().get();
			try (Transaction t = doc.holdForWrite(event)) {
				switch (event.getKeyCode()) {
				case C:
				case X:
					if (event.isControlPressed()) {
						if (copyToClipboard(text, event.getKeyCode() == KeyBoardEvent.KeyCode.X))
							event.use();
					}
					break;
				case LEFT_ARROW:
					if (left(text, event.isShiftPressed()))
						event.use();
					break;
				case RIGHT_ARROW:
					if (right(text, event.isShiftPressed()))
						event.use();
					break;
				case UP_ARROW:
					if (up(text, event.isShiftPressed()))
						event.use();
					break;
				case DOWN_ARROW:
					if (down(text, event.isShiftPressed()))
						event.use();
					break;
				case HOME:
					if (home(text, event.isShiftPressed()))
						event.use();
					break;
				case END:
					if (end(text, event.isShiftPressed()))
						event.use();
					break;
				case PAGE_UP:
					if (pageUp(text, event.isShiftPressed()))
						event.use();
					break;
				case PAGE_DOWN:
					if (pageDown(text, event.isShiftPressed()))
						event.use();
					break;
				default:
				}
			}
		}
	}

	private QuickTextElement theElement;

	private MouseListener theMouseListener = new MouseListener();
	private KeyListener theKeyListener = new KeyListener();
	private SimpleObservable<QuickTextElement> theUnsubscribeObservable;

	private int theCursorXLoc = -1;

	@Override
	public void install(QuickTextElement element) {
		if (theElement != null)
			throw new IllegalStateException(getClass().getSimpleName() + " may only be used with a single element");
		theElement = element;
		theUnsubscribeObservable = new SimpleObservable<>(null, false, theElement.getAttributeLocker(), null);
		element.events().filterMap(MouseEvent.mouse).takeUntil(theUnsubscribeObservable.filter(el -> {
			return el == element;
		})).act(theMouseListener);
		element.events().filterMap(KeyBoardEvent.key.press()).takeUntil(theUnsubscribeObservable.filter(el -> {
			return el == element;
		})).act(theKeyListener);
		QuickDocumentModel.flatten(element.getDocumentModel()).changes().act(evt -> {
			if (evt instanceof ContentChangeEvent || evt instanceof SelectionChangeEvent)
				theCursorXLoc = -1;
		});
	}

	@Override
	public void uninstall(QuickTextElement element) {
		theUnsubscribeObservable.onNext(element);
		if (theElement == element)
			theElement = null;
	}

	private boolean copyToClipboard(QuickTextElement element, boolean cut) {
		if (!(element.getDocumentModel().get() instanceof SelectableDocumentModel))
			return false;
		SelectableDocumentModel doc = (SelectableDocumentModel) element.getDocumentModel().get();
		if (doc.getCursor() == doc.getSelectionAnchor())
			return false;
		java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
			.setContents(new java.awt.datatransfer.StringSelection(doc.getSelectedText()), null);
		if (cut && doc instanceof MutableDocumentModel)
			((MutableDocumentModel) doc).delete(doc.getSelectionAnchor(), doc.getCursor());
		return true;
	}

	private boolean left(QuickTextElement element, boolean shift) {
		if (!(element.getDocumentModel().get() instanceof SelectableDocumentModel))
			return false;
		SelectableDocumentModel doc = (SelectableDocumentModel) element.getDocumentModel().get();
		int cursor = doc.getCursor() - 1;
		if (cursor < 0) {
			return false;
		}
		if (shift)
			doc.setSelection(doc.getSelectionAnchor(), cursor);
		else
			doc.setCursor(cursor);
		return true;
	}

	private boolean right(QuickTextElement element, boolean shift) {
		if (!(element.getDocumentModel().get() instanceof SelectableDocumentModel))
			return false;
		SelectableDocumentModel doc = (SelectableDocumentModel) element.getDocumentModel().get();
		int cursor = doc.getCursor() + 1;
		if (cursor > doc.length()) {
			return false;
		}
		if (shift)
			doc.setSelection(doc.getSelectionAnchor(), cursor);
		else
			doc.setCursor(cursor);
		return true;
	}

	private boolean up(QuickTextElement element, boolean shift) {
		if (!element.getStyle().get(org.quick.core.style.FontStyle.wordWrap).get())
			return false;
		if (!(element.getDocumentModel().get() instanceof SelectableDocumentModel))
			return false;
		SelectableDocumentModel doc = (SelectableDocumentModel) element.getDocumentModel().get();
		Point2D loc = doc.getLocationAt(doc.getCursor(), element.bounds().getWidth());
		if (loc.getY() == 0)
			return false; // Can't go up from here.
		int cursorXLoc = theCursorXLoc;
		if (cursorXLoc < 0)
			cursorXLoc = (int) loc.getX();
		int newCursor = Math.round(doc.getPositionAt(cursorXLoc, (float) loc.getY() - 1, element.bounds().getWidth()));

		if (shift)
			doc.setSelection(doc.getSelectionAnchor(), newCursor);
		else
			doc.setCursor(newCursor);
		theCursorXLoc = cursorXLoc;
		return true;
	}

	private boolean down(QuickTextElement element, boolean shift) {
		if (!element.getStyle().get(org.quick.core.style.FontStyle.wordWrap).get())
			return false;
		if (!(element.getDocumentModel().get() instanceof SelectableDocumentModel))
			return false;
		SelectableDocumentModel doc = (SelectableDocumentModel) element.getDocumentModel().get();
		int cursor = doc.getCursor();
		int cursorXLoc = theCursorXLoc;
		if (cursorXLoc < 0) {
			Point2D loc = doc.getLocationAt(cursor, element.bounds().getWidth());
			cursorXLoc = (int) loc.getX();
		}
		int y = -1;
		for (QuickDocumentModel.StyledSequenceMetric metric : doc.metrics(cursor, element.bounds().getWidth())) {
			if (metric.isNewLine()) {
				y = (int) metric.getTop();
				break;
			}
		}
		if (y < 0)
			return false; // No newline after cursor. Can't go down.

		int newCursor = Math.round(doc.getPositionAt(cursorXLoc, y, element.bounds().getWidth()));

		if (shift)
			doc.setSelection(doc.getSelectionAnchor(), newCursor);
		else
			doc.setCursor(newCursor);
		theCursorXLoc = cursorXLoc;
		return true;
	}

	private boolean home(QuickTextElement element, boolean shift) {
		if (!(element.getDocumentModel().get() instanceof SelectableDocumentModel))
			return false;
		SelectableDocumentModel doc = (SelectableDocumentModel) element.getDocumentModel().get();
		int newCursor;
		if (!element.getStyle().get(org.quick.core.style.FontStyle.wordWrap).get())
			newCursor = 0;
		else {
			Point2D loc = doc.getLocationAt(doc.getCursor(), element.bounds().getWidth());
			newCursor = (int) doc.getPositionAt(0, (float) loc.getY(), element.bounds().getWidth());
		}
		if (doc.getSelectionAnchor() == newCursor && doc.getCursor() == newCursor)
			return false;
		if (shift)
			doc.setSelection(doc.getSelectionAnchor(), newCursor);
		else
			doc.setCursor(newCursor);
		return true;
	}

	private boolean end(QuickTextElement element, boolean shift) {
		if (!(element.getDocumentModel().get() instanceof SelectableDocumentModel))
			return false;
		SelectableDocumentModel doc = (SelectableDocumentModel) element.getDocumentModel().get();
		if (doc.getCursor() == doc.length() && doc.getSelectionAnchor() == doc.length())
			return false;
		int newCursor;
		if (!element.getStyle().get(org.quick.core.style.FontStyle.wordWrap).get())
			newCursor = doc.length();
		else {
			Point2D loc = doc.getLocationAt(doc.getCursor(), element.bounds().getWidth());
			newCursor = (int) doc.getPositionAt(element.bounds().getWidth(), (float) loc.getY(), element.bounds().getWidth());
		}
		if (doc.getSelectionAnchor() == newCursor && doc.getCursor() == newCursor)
			return false;
		if (shift)
			doc.setSelection(doc.getSelectionAnchor(), newCursor);
		else
			doc.setCursor(newCursor);
		return true;
	}

	private boolean pageUp(QuickTextElement element, boolean shift) {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean pageDown(QuickTextElement element, boolean shift) {
		// TODO Auto-generated method stub
		return false;
	}

	private int selectWord(SelectableDocumentModel doc, int position, Object cause) {
		int anchor;
		int cursor;
		if (position == doc.length())
			anchor = cursor = position;
		else {
			for (anchor = position; anchor > 0 && isWordChar(doc.charAt(anchor)); anchor--)
				;
			if (anchor < position && !isWordChar(doc.charAt(anchor)))
				anchor++;
			for (cursor = position; cursor < doc.length() && isWordChar(doc.charAt(cursor)); cursor++)
				;
		}
		doc.setSelection(anchor, cursor);
		return anchor;
	}

	private boolean isWordChar(char ch) {
		return Character.isLetterOrDigit(ch) || ch == '_' || ch == '\'';
	}

	private int selectLine(SelectableDocumentModel doc, int position, Object cause) {
		int anchor = position;
		int cursor;
		if (doc.length() == 0)
			anchor = cursor = 0;
		else {
			if (position == doc.length())
				anchor--;
			for (; anchor > 0 && !isLineBreakChar(doc.charAt(anchor)); anchor--)
				;
			if (anchor < position && isLineBreakChar(doc.charAt(anchor)))
				anchor++;
			for (cursor = position; cursor < doc.length() && !isLineBreakChar(doc.charAt(cursor)); cursor++)
				;
			while (cursor < doc.length() && isLineBreakChar(doc.charAt(cursor + 1)))
				cursor++;
		}
		doc.setSelection(anchor, cursor);
		return anchor;
	}

	private boolean isLineBreakChar(char ch) {
		return ch == '\n' || ch == '\r';
	}
}
