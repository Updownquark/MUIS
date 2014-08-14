package org.muis.base.widget;

import static org.muis.base.layout.TextEditLayout.charLengthAtt;
import static org.muis.base.layout.TextEditLayout.charRowsAtt;
import static org.muis.core.MuisTextElement.multiLine;

import org.muis.core.event.FocusEvent;
import org.muis.core.event.KeyBoardEvent;
import org.muis.core.model.*;
import org.muis.core.rx.ObservableValue;
import org.muis.core.tags.Template;
import org.muis.util.Transaction;

import prisms.lang.Type;

/** A simple widget that takes and displays text input from the user */
@Template(location = "../../../../text-field.muis")
public class TextField extends org.muis.core.MuisTemplate implements DocumentedElement {
	private org.muis.core.model.WidgetRegistration theRegistration;

	private WrappingDocumentModel theDocumentWrapper;

	private int theCallbackLock = 0;

	private boolean theEventLock;

	/** Creates a text field */
	public TextField() {
		life().runWhen(() -> {
			atts().accept(this, charLengthAtt).act(event -> {
				try {
					getElement(getTemplate().getAttachPoint("text")).atts().set(charLengthAtt, event.getValue());
				} catch(org.muis.core.MuisException e) {
					msg().error("Could not pass on " + charLengthAtt, e);
				}
			});
			atts().accept(this, charRowsAtt).act(event -> {
				try {
					getElement(getTemplate().getAttachPoint("text")).atts().set(charRowsAtt, event.getValue());
				} catch(org.muis.core.MuisException e) {
					msg().error("Could not pass on " + charRowsAtt, e);
				}
			});
			atts().accept(this, ModelAttributes.value).act(event -> {
				if(theRegistration != null)
					theRegistration.unregister();
				if(event.getValue() instanceof org.muis.core.model.WidgetRegister)
					theRegistration = ((org.muis.core.model.WidgetRegister) event.getValue()).register(TextField.this);
			});
			atts().accept(this, multiLine);
		}, org.muis.core.MuisConstants.CoreStage.INIT_SELF.toString(), 1);
		life().runWhen(() -> {
			initDocument();
			new org.muis.util.MuisAttributeExposer(TextField.this, getValueElement(), msg(), multiLine);
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
			ObservableValue.flatten(new Type(Object.class, true), atts().getHolder(ModelAttributes.value)).act(evt -> {
				modelValueChanged(evt.getOldValue(), evt.getValue());
			});
			getContentModel().addContentListener(evt -> {
				if(theCallbackLock > 0)
					return;
				pushToEdit();
				fireModelChange();
			});
			modelValueChanged(null, atts().get(ModelAttributes.value));

			events().filterMap(FocusEvent.blur).act(event -> {
				pushToModel();
			});
			events().filterMap(KeyBoardEvent.key.press()).act(event -> {
				if(event.getKeyCode() == KeyBoardEvent.KeyCode.ENTER) {
					if(Boolean.TRUE.equals(atts().get(multiLine)) && !event.isControlPressed())
						return;
					pushToModel();
				}
			});
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
	public void setContentModel(MutableDocumentModel model) {
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
		ObservableValue<?> modelValue = atts().get(ModelAttributes.value);
		if(modelValue instanceof org.muis.core.rx.SettableValue && modelValue.getType().canAssignTo(String.class))
			((org.muis.core.rx.SettableValue<String>) modelValue).set(getContentModel().toString(), null);
	}

	private void modelValueChanged(Object oldValue, Object newValue) {
		if(oldValue instanceof MutableSelectableDocumentModel) {
			if(!(newValue instanceof MuisDocumentModel))
				setContentModel(null);
		}
		if(newValue instanceof MuisDocumentModel) {
			if(newValue instanceof MutableSelectableDocumentModel)
				setContentModel((MutableSelectableDocumentModel) newValue);
			else
				throw new IllegalArgumentException("Document model for a text field must be a "
					+ MutableSelectableDocumentModel.class.getName());
		} else if(newValue != null) {
			if(!theEventLock) {
				theEventLock = true;
				try {
					getContentModel().setText(getTextFor(newValue));
				} finally {
					theEventLock = false;
				}
			}
		} else
			pushToModel();
	}

	private static String getTextFor(Object value) {
		return value == null ? null : value.toString();
	}
}
