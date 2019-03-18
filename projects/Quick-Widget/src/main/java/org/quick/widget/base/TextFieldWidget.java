package org.quick.widget.base;

import static org.quick.core.QuickTextElement.multiLine;

import org.observe.ObservableValue;
import org.quick.base.BaseConstants;
import org.quick.base.widget.TextField;
import org.quick.core.QuickConstants.CoreStage;
import org.quick.core.QuickDefinedWidget;
import org.quick.core.QuickException;
import org.quick.core.model.QuickDocumentModel;
import org.quick.widget.core.QuickTemplateWidget;
import org.quick.widget.core.QuickTextWidget;
import org.quick.widget.core.QuickWidgetDocument;
import org.quick.widget.core.RenderableDocumentModel;
import org.quick.widget.core.event.FocusEvent;
import org.quick.widget.core.event.KeyBoardEvent;
import org.quick.widget.core.model.DocumentedElement;

public class TextFieldWidget<E extends TextField> extends QuickTemplateWidget<E> implements DocumentedElement {
	private final SimpleTextEditing theTextEditing;

	public TextFieldWidget() {
		theTextEditing = new SimpleTextEditing();
	}

	@Override
	public void init(QuickWidgetDocument document, E element, QuickDefinedWidget<QuickWidgetDocument, ?> parent) throws QuickException {
		super.init(document, element, parent);

		getElement().life().runWhen(() -> {
			theTextEditing.install(this); // Installs the text editing behavior

			// Set up the cursor overlay
			QuickTextWidget valueW = getValueWidget();
			DocCursorOverlayWidget<?> cursor = getDocCursorOverlay();
			cursor.setEditor(TextFieldWidget.this, valueW);

			// When the user leaves this widget, flush--either modify the value or reset the document
			events().filterMap(FocusEvent.blur).act(event -> {
				if (getElement().state().is(BaseConstants.States.ERROR))
					getElement().resetDocument(event);
				else
					getElement().pushChanges(event);
			});
			// When the user presses enter (CTRL+enter is required for a multi-line text field), push the changes
			// When the user presses escape, reset the document
			events().filterMap(KeyBoardEvent.key.press()).act(event -> {
				if (event.getKeyCode() == KeyBoardEvent.KeyCode.ENTER) {
					if (getElement().atts().getValue(multiLine, false) && !event.isControlPressed())
						return;
					getElement().pushChanges(event);
				} else if (event.getKeyCode() == KeyBoardEvent.KeyCode.ESCAPE)
					getElement().resetDocument(event);
			});
		}, CoreStage.STARTUP, 1);
	}

	@Override
	public ObservableValue<QuickDocumentModel> getDocumentModel() {
		return getValueWidget().getDocumentModel();
	}

	@Override
	public RenderableDocumentModel getRenderableDocument() {
		return getValueWidget().getRenderableDocument();
	}

	public QuickTextWidget getValueWidget() {
		return (QuickTextWidget) getChild(getElement().getValueElement());
	}

	protected DocCursorOverlayWidget<?> getDocCursorOverlay() {
		return (DocCursorOverlayWidget<?>) getChild(getElement().getCursorOverlay());
	}
}
