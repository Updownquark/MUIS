package org.muis.base.widget;

import org.muis.core.model.SimpleDocumentModel;
import org.muis.core.tags.Template;

/** A simple widget that takes and displays text input from the user */
@Template(location = "../../../../text-field.muis")
public class TextField extends org.muis.core.MuisTemplate implements SimpleTextWidget {
	private SimpleDocumentModel theDocument;

	/** Creates a text field */
	public TextField() {
		setFocusable(true);
		life().runWhen(new Runnable() {
			@Override
			public void run() {
				initDocument();
			}
		}, org.muis.core.MuisConstants.CoreStage.INIT_CHILDREN.toString(), 1);
	}

	/**
	 * Initializes the document for this text field. This may be overridden and used by subclasses but should never be called directly
	 * except as the super call from the extending method.
	 */
	protected void initDocument() {
		if(theDocument != null)
			throw new IllegalStateException("Document model is already initialized");
		theDocument = ((org.muis.core.MuisTextElement) getElement(getTemplate().getAttachPoint("value"))).getDocumentModel();
		new org.muis.base.model.SimpleTextEditing().install(this);
	}

	@Override
	public SimpleDocumentModel getDocumentModel() {
		return theDocument;
	}
}
