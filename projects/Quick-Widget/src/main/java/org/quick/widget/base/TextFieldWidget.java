package org.quick.widget.base;

import static org.quick.core.QuickTextElement.multiLine;

import org.quick.base.BaseConstants;
import org.quick.base.widget.DocumentCursorOverlay;
import org.quick.base.widget.TextField;
import org.quick.core.QuickConstants.CoreStage;
import org.quick.widget.core.QuickTemplateWidget;
import org.quick.widget.core.QuickTextWidget;
import org.quick.widget.core.QuickWidget;
import org.quick.widget.core.QuickWidgetDocument;
import org.quick.widget.core.event.FocusEvent;
import org.quick.widget.core.event.KeyBoardEvent;

public class TextFieldWidget extends QuickTemplateWidget {
	private final SimpleTextEditing theTextEditing;

	public TextFieldWidget(QuickWidgetDocument doc, TextField element, QuickWidget parent) {
		super(doc, element, parent);

		theTextEditing = new SimpleTextEditing();
		getElement().life().runWhen(() -> {
			theTextEditing.install(this); // Installs the text editing behavior

			// Set up the cursor overlay
			QuickTextWidget valueEl = (QuickTextWidget) getElement(getTemplate().getAttachPoint("value")).get();
			DocumentCursorOverlay cursor = (DocumentCursorOverlay) getElement(getTemplate().getAttachPoint("cursor-overlay")).get();
			cursor.setElement(this, getValueElement());

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
	public TextField getElement() {
		return (TextField) super.getElement();
	}

}
