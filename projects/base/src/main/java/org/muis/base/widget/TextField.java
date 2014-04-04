package org.muis.base.widget;

import static org.muis.base.layout.TextEditLayout.charLengthAtt;
import static org.muis.base.layout.TextEditLayout.charRowsAtt;

import org.muis.core.event.AttributeChangedEvent;
import org.muis.core.event.MuisEventListener;
import org.muis.core.model.*;
import org.muis.core.tags.Template;

/** A simple widget that takes and displays text input from the user */
@Template(location = "../../../../text-field.muis")
public class TextField extends org.muis.core.MuisTemplate implements DocumentedElement {
	private org.muis.core.model.WidgetRegistration theRegistration;

	private org.muis.core.model.MuisModelValueListener<Object> theValueListener;

	/** Creates a text field */
	public TextField() {
		theValueListener = new org.muis.core.model.MuisModelValueListener<Object>() {
			private boolean theEventLock;

			@Override
			public void valueChanged(MuisModelValueEvent<? extends Object> evt) {
				if(!theEventLock && getDocumentModel() instanceof MutableDocumentModel) {
					theEventLock = true;
					try {
						((MutableDocumentModel) getDocumentModel()).setText(getTextFor(evt.getNewValue()));
					} finally {
						theEventLock = false;
					}
				}
			}
		};
		life().runWhen(new Runnable() {
			@Override
			public void run() {
				atts().accept(this, charLengthAtt);
				atts().accept(this, charRowsAtt);
				atts().accept(this, ModelAttributes.value);
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
				events().listen(AttributeChangedEvent.att(charLengthAtt), new MuisEventListener<AttributeChangedEvent<Long>>() {
					@Override
					public void eventOccurred(AttributeChangedEvent<Long> event) {
						try {
							getElement(getTemplate().getAttachPoint("text")).atts().set(event.getAttribute(), event.getValue());
						} catch(org.muis.core.MuisException e) {
							msg().error("Could not pass on " + event.getAttribute(), e);
						}
					}
				}).listen(AttributeChangedEvent.att(charRowsAtt), new MuisEventListener<AttributeChangedEvent<Long>>(){
					@Override
					public void eventOccurred(AttributeChangedEvent<Long> event) {
						try {
							getElement(getTemplate().getAttachPoint("text")).atts().set(event.getAttribute(), event.getValue());
						} catch(org.muis.core.MuisException e) {
							msg().error("Could not pass on " + event.getAttribute(), e);
						}
					}
					})
					.listen(AttributeChangedEvent.att(ModelAttributes.value),
						new MuisEventListener<AttributeChangedEvent<MuisModelValue<?>>>() {
					@Override
					public void eventOccurred(AttributeChangedEvent<MuisModelValue<?>> event) {
						modelValueChanged(event.getOldValue(), event.getValue());
					}
				});
				modelValueChanged(null, atts().get(ModelAttributes.value));
				getDocumentModel().addContentListener(new MuisDocumentModel.ContentListener() {
					@Override
					public void contentChanged(MuisDocumentModel.ContentChangeEvent evt) {
						MuisModelValue<?> modelValue = atts().get(ModelAttributes.value);
						if(modelValue != null && modelValue.getType() == String.class)
							((MuisModelValue<String>) modelValue).set(getDocumentModel().toString(), null);
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
		new org.muis.base.model.SimpleTextEditing().install(this);
	}

	@Override
	public MuisDocumentModel getDocumentModel() {
		return getValueElement().getDocumentModel();
	}

	/** @param model The document model for this text field */
	public void setDocumentModel(MutableSelectableDocumentModel model) {
		getValueElement().setDocumentModel(model);
	}

	private void modelValueChanged(MuisModelValue<?> oldValue, MuisModelValue<?> newValue) {
		if(theRegistration != null)
			theRegistration.unregister();
		if(oldValue instanceof MutableSelectableDocumentModel) {
			if(!(newValue instanceof MuisDocumentModel))
				setDocumentModel(null);
		} else if(oldValue != null)
			oldValue.removeListener(theValueListener);
		if(newValue instanceof org.muis.core.model.WidgetRegister)
			theRegistration = ((org.muis.core.model.WidgetRegister) newValue).register(TextField.this);
		if(newValue instanceof MuisDocumentModel) {
			if(newValue instanceof MutableSelectableDocumentModel)
				setDocumentModel((MutableSelectableDocumentModel) newValue);
			else
				throw new IllegalArgumentException("Document model for a text field must be a "
					+ MutableSelectableDocumentModel.class.getName());
		} else if(newValue != null) {
			newValue.addListener(theValueListener);
			if(getDocumentModel() instanceof MutableDocumentModel)
				((MutableDocumentModel) getDocumentModel()).setText(getTextFor(newValue.get()));
		}
	}

	private static String getTextFor(Object value) {
		return value == null ? null : value.toString();
	}
}
