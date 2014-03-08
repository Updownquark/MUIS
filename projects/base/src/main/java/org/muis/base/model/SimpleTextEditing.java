package org.muis.base.model;

import org.muis.core.MuisElement;
import org.muis.core.event.CharInputEvent;
import org.muis.core.model.DocumentedElement;
import org.muis.core.model.MuisBehavior;
import org.muis.core.model.MutableSelectableDocumentModel;

/** Behavior allowing keyboard input */
public class SimpleTextEditing implements MuisBehavior<DocumentedElement> {
	private org.muis.core.event.CharInputListener theInputListener;

	/** Creates the behavior */
	public SimpleTextEditing() {
		theInputListener = new org.muis.core.event.CharInputListener(false) {
			@Override
			public void keyTyped(CharInputEvent evt, MuisElement element) {
				charInput((DocumentedElement) element, evt.getChar());
			}
		};
	}

	@Override
	public void install(DocumentedElement element) {
		((MuisElement) element).addListener(org.muis.core.MuisConstants.Events.CHARACTER_INPUT, theInputListener);

	}

	@Override
	public void uninstall(DocumentedElement element) {
		((MuisElement) element).removeListener(theInputListener);
	}

	/**
	 * @param widget The widget that the character is being given to
	 * @param ch The input character
	 */
	protected void charInput(DocumentedElement widget, char ch) {
		if(ch < ' ' && ch != '\t' && ch != '\n' && ch != '\r' && ch != '\b' && ch != CharInputEvent.PASTE)
			return;
		if(!(widget.getDocumentModel() instanceof MutableSelectableDocumentModel))
			return;
		MutableSelectableDocumentModel doc = (MutableSelectableDocumentModel) widget.getDocumentModel();
		boolean batchDeleted = false;
		if(doc.getSelectionAnchor() != doc.getCursor()) {
			doc.delete(doc.getSelectionAnchor(), doc.getCursor());
			batchDeleted = true;
		}
		if(ch == '\b') {
			if(!batchDeleted && doc.getCursor() > 0)
				doc.delete(doc.getCursor() - 1, doc.getCursor());
		} else if(ch == java.awt.event.KeyEvent.VK_DELETE) {
			if(!batchDeleted && doc.getCursor() < doc.length() - 1)
				doc.delete(doc.getCursor(), doc.getCursor() + 1);
		} else if(ch == CharInputEvent.PASTE) {
			pasteFromClipboard(widget);
		} else
			doc.insert(ch);
	}

	private static void pasteFromClipboard(DocumentedElement element) {
		if(!(element.getDocumentModel() instanceof MutableSelectableDocumentModel))
			return;
		java.awt.datatransfer.Transferable contents = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
		if(contents == null || !contents.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.stringFlavor))
			return;
		String text;
		try {
			text = (String) contents.getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor);
		} catch(java.awt.datatransfer.UnsupportedFlavorException e) {
			((MuisElement) element).msg().error("Badly supported String data flavor", e);
			return;
		} catch(java.io.IOException e) {
			((MuisElement) element).msg().error("I/O exception pasting text", e);
			return;
		}
		MutableSelectableDocumentModel doc = (MutableSelectableDocumentModel) element.getDocumentModel();
		doc.insert(doc.getCursor(), text);
	}
}
