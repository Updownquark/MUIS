package org.muis.base.widget;

import static org.muis.base.layout.TextEditLayout.charLengthAtt;
import static org.muis.base.layout.TextEditLayout.charRowsAtt;

import org.muis.core.event.*;
import org.muis.core.model.*;
import org.muis.core.tags.Template;
import org.muis.util.Transaction;

/** A simple widget that takes and displays text input from the user */
@Template(location = "../../../../text-field.muis")
public class TextField extends org.muis.core.MuisTemplate implements DocumentedElement {
	private org.muis.core.model.WidgetRegistration theRegistration;

	private org.muis.core.model.MuisModelValueListener<Object> theValueListener;

	private WrappingDocumentModel theDocumentWrapper;

	private int theCallbackLock = 0;

	/** Creates a text field */
	public TextField() {
		theValueListener = new org.muis.core.model.MuisModelValueListener<Object>() {
			private boolean theEventLock;

			@Override
			public void valueChanged(MuisModelValueEvent<? extends Object> evt) {
				if(!theEventLock) {
					theEventLock = true;
					try {
						getContentModel().setText(getTextFor(evt.getNewValue()));
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
				atts().accept(this, org.muis.core.MuisTextElement.multiLine);
			}
		}, org.muis.core.MuisConstants.CoreStage.INIT_SELF.toString(), 1);
		life().runWhen(new Runnable() {
			@Override
			public void run() {
				initDocument();
				new org.muis.util.MuisAttributeExposer(TextField.this, getValueElement(), msg(), org.muis.core.MuisTextElement.multiLine);
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
				getContentModel().addContentListener(new MuisDocumentModel.ContentListener() {
					@Override
					public void contentChanged(MuisDocumentModel.ContentChangeEvent evt) {
						if(theCallbackLock > 0)
							return;
						pushToEdit();
						fireModelChange();
					}
				});

				events().listen(FocusEvent.blur, new MuisEventListener<FocusEvent>() {
					@Override
					public void eventOccurred(FocusEvent event) {
						pushToModel();
					}
				});
				events().listen(KeyBoardEvent.key.press(), new MuisEventListener<KeyBoardEvent>() {
					@Override
					public void eventOccurred(KeyBoardEvent event) {
						if(event.getKeyCode() == KeyBoardEvent.KeyCode.ENTER) {
							// TODO Uncomment these when multi-line is supported
							// if(isMultiLine() && !getDocument().isControlPressed())
							// return;
							pushToModel();
						}
					}
				});
				pushToEdit();
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
		theDocumentWrapper = new WrappingDocumentModel(new org.muis.core.model.SimpleDocumentModel(getValueElement().getStyle().getSelf()));
		new org.muis.base.model.SimpleTextEditing().install(this);
	}

	/** @return The wrapping document model that backs this text field's model */
	public MutableDocumentModel getContentModel() {
		return (MutableDocumentModel) theDocumentWrapper.getDocumentModel();
	}

	/** @return The document model that backs this text field's model */
	public MutableDocumentModel getWrappedContentModel() {
		return (MutableDocumentModel) theDocumentWrapper.getWrapped();
	}

	/** @return The document model that is edited directly by the user */
	@Override
	public MutableSelectableDocumentModel getDocumentModel() {
		return (MutableSelectableDocumentModel) getValueElement().getDocumentModel();
	}

	/** @param model The document model for this text field */
	public void setDocumentModel(MutableDocumentModel model) {
		theDocumentWrapper.setWrapped(model);
	}

	/** @param model The document model that will be edited directly by the user */
	public void setEditModel(MutableSelectableDocumentModel model) {
		getValueElement().setDocumentModel(model);
	}

	/** Pushes content from the content model to the edit model */
	private void pushToEdit() {
		MutableDocumentModel contentModel = getContentModel();
		MutableDocumentModel editModel = getDocumentModel();
		theCallbackLock++;
		try (Transaction w = editModel.holdForWrite(); Transaction r = contentModel.holdForRead()) {
			editModel.setText("");
			for(org.muis.core.model.MuisDocumentModel.StyledSequence seq : contentModel)
				editModel.append(seq);
		} finally {
			theCallbackLock--;
		}
	}

	/** Pushes content from the edit model to the content model */
	private void pushToModel() {
		MutableDocumentModel contentModel = getContentModel();
		MutableDocumentModel editModel = getDocumentModel();
		theCallbackLock++;
		try (Transaction w = contentModel.holdForWrite(); Transaction r = editModel.holdForRead()) {
			contentModel.setText("");
			for(org.muis.core.model.MuisDocumentModel.StyledSequence seq : editModel)
				contentModel.append(seq);
		} finally {
			theCallbackLock--;
		}
		fireModelChange();
	}

	private void fireModelChange() {
		MuisModelValue<?> modelValue = atts().get(ModelAttributes.value);
		if(modelValue != null && modelValue.getType() == String.class)
			((MuisModelValue<String>) modelValue).set(getContentModel().toString(), null);
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
			getContentModel().setText(getTextFor(newValue.get()));
		}
	}

	private static String getTextFor(Object value) {
		return value == null ? null : value.toString();
	}
}
