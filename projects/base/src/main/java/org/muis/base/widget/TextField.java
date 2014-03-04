package org.muis.base.widget;

import static org.muis.base.layout.TextEditLayout.charLengthAtt;
import static org.muis.base.layout.TextEditLayout.charRowsAtt;

import org.muis.core.event.AttributeChangedEvent;
import org.muis.core.model.MutableSelectableDocumentModel;
import org.muis.core.tags.Template;

/** A simple widget that takes and displays text input from the user */
@Template(location = "../../../../text-field.muis")
public class TextField extends org.muis.core.MuisTemplate implements SimpleTextWidget {
	private MutableSelectableDocumentModel theDocument;

	/** Creates a text field */
	public TextField() {
		life().runWhen(new Runnable() {
			@Override
			public void run() {
				atts().accept(this, charLengthAtt);
				atts().accept(this, charRowsAtt);
			}
		}, org.muis.core.MuisConstants.CoreStage.INIT_SELF.toString(), 1);
		life().runWhen(new Runnable() {
			@Override
			public void run() {
				initDocument();
				DocumentCursorOverlay cursor = (DocumentCursorOverlay) getElement(getTemplate().getAttachPoint("cursor-overlay"));
				cursor.setTextElement(getValueElement());
				cursor.setStyleAnchor(getStyle().getSelf());
				org.muis.core.MuisElement textHolder = getElement(getTemplate().getAttachPoint("text"));
				try {
					textHolder.atts().set(charLengthAtt, atts().get(charLengthAtt));
					textHolder.atts().set(charRowsAtt, atts().get(charRowsAtt));
				} catch(org.muis.core.MuisException e) {
					msg().error("Could not initialize text layout attributes", e);
				}
				addListener(org.muis.core.MuisConstants.Events.ATTRIBUTE_CHANGED, new org.muis.core.event.AttributeChangedListener<Long>(
					charLengthAtt) {
					@Override
					public void attributeChanged(AttributeChangedEvent<Long> event) {
						try {
							getElement(getTemplate().getAttachPoint("text")).atts().set(event.getAttribute(), event.getValue());
						} catch(org.muis.core.MuisException e) {
							msg().error("Could not pass on " + event.getAttribute(), e);
						}
					}
				});
				addListener(org.muis.core.MuisConstants.Events.ATTRIBUTE_CHANGED, new org.muis.core.event.AttributeChangedListener<Long>(
					charRowsAtt) {
					@Override
					public void attributeChanged(AttributeChangedEvent<Long> event) {
						try {
							getElement(getTemplate().getAttachPoint("text")).atts().set(event.getAttribute(), event.getValue());
						} catch(org.muis.core.MuisException e) {
							msg().error("Could not pass on " + event.getAttribute(), e);
						}
					}
				});
			}
		}, org.muis.core.MuisConstants.CoreStage.INITIALIZED.toString(), 1);
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
	public MutableSelectableDocumentModel getDocumentModel() {
		return theDocument;
	}
}
