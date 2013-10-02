package org.muis.base.widget;

import org.muis.core.model.SimpleDocumentModel;
import org.muis.core.tags.Template;

/** A simple widget that takes and displays text input from the user */
@Template(location = "../../../../text-field.muis")
public class TextField extends org.muis.core.MuisTemplate {
	private SimpleDocumentModel theDocument;

	public TextField() {
		life().runWhen(new Runnable() {
			@Override
			public void run() {
				theDocument = ((org.muis.core.MuisTextElement) getElement(getTemplate().getAttachPoint("value"))).getDocumentModel();
			}
		}, org.muis.core.MuisConstants.CoreStage.INIT_CHILDREN.toString(), 1);
	}
}
