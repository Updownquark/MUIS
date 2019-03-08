package org.quick.widget.base;

import java.util.function.Consumer;

import org.observe.SimpleObservable;
import org.quick.core.QuickElement;
import org.quick.core.QuickTextElement;
import org.quick.core.model.DocumentedElement;
import org.quick.core.model.MutableSelectableDocumentModel;
import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.QuickWidgetBehavior;
import org.quick.widget.core.event.CharInputEvent;

/** Behavior allowing keyboard input */
public class SimpleTextEditing implements QuickWidgetBehavior<QuickWidget> {
	private Consumer<CharInputEvent> theInputListener;

	private SimpleObservable<QuickWidget> theUninstallObservable;

	private boolean isEnabled = true;

	/** Creates the behavior */
	public SimpleTextEditing() {
		theInputListener = evt -> {
			if (isEnabled)
				charInput((DocumentedElement) evt.getWidget().getElement(), evt.getChar());
		};
	}

	@Override
	public void install(QuickWidget widget) {
		theUninstallObservable = new SimpleObservable<>(null, false, widget.getElement().getAttributeLocker(), null);
		widget.events().filterMap(CharInputEvent.charInput).takeUntil(theUninstallObservable.filter(el -> {
			return el == widget;
		})).act(theInputListener);
		widget.getElement().state().observe(org.quick.base.BaseConstants.States.ENABLED).takeUntil(theUninstallObservable)
			.changes().act(event -> setEnabled(event.getNewValue()));
	}

	@Override
	public void uninstall(QuickWidget widget) {
		theUninstallObservable.onNext(widget);
	}

	/** @param enabled Whether this behavior should be acting on events */
	public void setEnabled(boolean enabled) {
		isEnabled = enabled;
	}

	/**
	 * @param widget The widget that the character is being given to
	 * @param ch The input character
	 */
	protected void charInput(DocumentedElement widget, char ch) {
		if(ch < ' ' && ch != '\t' && ch != '\n' && ch != '\r' && ch != '\b' && ch != CharInputEvent.PASTE)
			return;
		if (!(widget.getDocumentModel().get() instanceof MutableSelectableDocumentModel))
			return;
		MutableSelectableDocumentModel doc = (MutableSelectableDocumentModel) widget.getDocumentModel().get();
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
		} else if(ch == CharInputEvent.PASTE)
			pasteFromClipboard(widget);
		else if (ch == '\n' && !Boolean.TRUE.equals(((QuickElement) widget).atts().get(QuickTextElement.multiLine).get()))
			return;
		else
			doc.insert(ch);
	}

	private static void pasteFromClipboard(DocumentedElement element) {
		if (!(element.getDocumentModel().get() instanceof MutableSelectableDocumentModel))
			return;
		java.awt.datatransfer.Transferable contents = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
		if(contents == null || !contents.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.stringFlavor))
			return;
		String text;
		try {
			text = (String) contents.getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor);
		} catch(java.awt.datatransfer.UnsupportedFlavorException e) {
			((QuickElement) element).msg().error("Badly supported String data flavor", e);
			return;
		} catch(java.io.IOException e) {
			((QuickElement) element).msg().error("I/O exception pasting text", e);
			return;
		}
		MutableSelectableDocumentModel doc = (MutableSelectableDocumentModel) element.getDocumentModel().get();
		doc.insert(doc.getCursor(), text);
	}
}
