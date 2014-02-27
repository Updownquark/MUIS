package org.muis.base.widget;

import org.muis.core.model.SimpleDocumentModel;
import org.muis.core.tags.Template;

/** A simple widget that takes and displays text input from the user */
@Template(location = "../../../../text-field.muis")
public class TextField extends org.muis.core.MuisTemplate implements SimpleTextWidget {
	private SimpleDocumentModel theDocument;

	/** Creates a text field */
	public TextField() {
		life().runWhen(new Runnable() {
			@Override
			public void run() {
				initDocument();
				// ((DocumentCursorOverlay) getElement(getTemplate().getAttachPoint("cursor-overlay"))).setTextElement(getValueElement());
			}
		}, org.muis.core.MuisConstants.CoreStage.INIT_CHILDREN.toString(), 1);
	}

	/** @return The text element containing the text that is the value of this text field */
	protected org.muis.core.MuisTextElement getValueElement() {
		return (org.muis.core.MuisTextElement) getElement(getTemplate().getAttachPoint("value"));
	}

	/**
	 * Initializes the document for this text field. This may be overridden and used by subclasses but should never be called directly
	 * except as the super call from the extending method.
	 */
	protected void initDocument() {
		if(theDocument != null)
			throw new IllegalStateException("Document model is already initialized");
		theDocument = getValueElement().getDocumentModel();
		new org.muis.base.model.SimpleTextEditing().install(this);
	}

	@Override
	public SimpleDocumentModel getDocumentModel() {
		return theDocument;
	}
}
