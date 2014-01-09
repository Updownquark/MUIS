package org.muis.base.model;

import org.muis.base.widget.SimpleTextWidget;
import org.muis.core.MuisElement;
import org.muis.core.event.CharInputEvent;
import org.muis.core.model.MuisBehavior;

public class SimpleTextEditing implements MuisBehavior<SimpleTextWidget> {
	private org.muis.core.event.CharInputListener theInputListener;

	public SimpleTextEditing() {
		theInputListener = new org.muis.core.event.CharInputListener(false) {
			@Override
			public void keyTyped(CharInputEvent evt, MuisElement element) {
				charInput((SimpleTextWidget) element, evt.getChar());
			}
		};
	}

	@Override
	public void install(SimpleTextWidget element) {
		((MuisElement) element).addListener(org.muis.core.MuisConstants.Events.CHARACTER_INPUT, theInputListener);

	}

	@Override
	public void uninstall(SimpleTextWidget element) {
		((MuisElement) element).removeListener(theInputListener);

	}

	protected void charInput(SimpleTextWidget widget, char ch) {
		if(ch < ' ' && ch != '\t' && ch != '\n' && ch != '\r')
			return;
		org.muis.core.model.SimpleDocumentModel doc = widget.getDocumentModel();
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
		} else
			doc.insert(ch);
	}
}
