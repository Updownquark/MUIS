package org.muis.base.widget;

import static org.muis.base.layout.TextEditLayout.charLengthAtt;
import static org.muis.base.layout.TextEditLayout.charRowsAtt;
import static org.muis.core.MuisTextElement.multiLine;

import org.muis.base.BaseAttributes;
import org.muis.base.BaseConstants;
import org.muis.base.model.*;
import org.muis.core.event.FocusEvent;
import org.muis.core.event.KeyBoardEvent;
import org.muis.core.mgr.StateEngine.StateController;
import org.muis.core.model.*;
import org.muis.core.rx.ObservableValue;
import org.muis.core.rx.SettableValue;
import org.muis.core.rx.TriTuple;
import org.muis.core.tags.State;
import org.muis.core.tags.StateSupport;
import org.muis.core.tags.Template;
import org.muis.util.Transaction;

/** A simple widget that takes and displays text input from the user */
@Template(location = "../../../../text-field.muis")
@StateSupport({@State(name = BaseConstants.States.ERROR_NAME, priority = BaseConstants.States.ERROR_PRIORITY),
		@State(name = BaseConstants.States.ENABLED_NAME, priority = BaseConstants.States.ENABLED_PRIORITY)})
public class TextField extends org.muis.core.MuisTemplate implements DocumentedElement {
	private org.muis.core.model.WidgetRegistration theRegistration;

	private boolean isDocOverridden;

	private boolean isDocDirty;

	private StateController theErrorController;
	private StateController theEnabledController;

	private int theCallbackLock = 0;

	/** Creates a text field */
	public TextField() {
		life().runWhen(() -> {
			theErrorController = state().control(BaseConstants.States.ERROR);
			theEnabledController = state().control(BaseConstants.States.ENABLED);
			Object accepter = new Object();
			atts().accept(accepter, charLengthAtt, charRowsAtt, multiLine, BaseAttributes.format, BaseAttributes.document, ModelAttributes.value, BaseAttributes.rich);
			atts().getHolder(charLengthAtt).act(event -> {
				try {
					getElement(getTemplate().getAttachPoint("text")).atts().set(charLengthAtt, event.getValue());
				} catch(org.muis.core.MuisException e) {
					msg().error("Could not pass on " + charLengthAtt, e);
				}
			});
			atts().getHolder(charRowsAtt).act(event -> {
				try {
					getElement(getTemplate().getAttachPoint("text")).atts().set(charRowsAtt, event.getValue());
				} catch(org.muis.core.MuisException e) {
					msg().error("Could not pass on " + charRowsAtt, e);
				}
			});
			atts().getHolder(ModelAttributes.value)
				.tupleV(atts().getHolder(BaseAttributes.format),
					atts().getHolder(BaseAttributes.document).composeV(ObservableValue.first(), atts().getHolder(BaseAttributes.rich))).act(event -> {
					if(theRegistration != null)
						theRegistration.unregister();
					TriTuple<ObservableValue<?>, MuisFormatter<?>, MuisDocumentModel> tuple = event.getValue();
					if(tuple.getValue1() == null && tuple.getValue3() == null)
						msg().warn("No model value or document specified");
					else if(tuple.getValue1() != null) {
						if(tuple.getValue3() != null)
							msg().warn("Both model value and document specified. Using model value.");
						setValue(tuple.getValue1(), tuple.getValue2(), event);
					} else
						setDocument(tuple.getValue3());
				});
			atts().getHolder(ModelAttributes.value).act(event -> theErrorController.set(false, event));
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

			events().filterMap(FocusEvent.blur).act(event -> {
				pushChanges();
			});
			events().filterMap(KeyBoardEvent.key.press()).act(event -> {
				if(event.getKeyCode() == KeyBoardEvent.KeyCode.ENTER) {
					if(Boolean.TRUE.equals(atts().get(multiLine)) && !event.isControlPressed())
						return;
					pushChanges();
				} else if(event.getKeyCode() == KeyBoardEvent.KeyCode.ESCAPE)
					resetDocument();
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
		new org.muis.base.model.SimpleTextEditing().install(this);
		getValueElement().getDocumentModel().onContentStyleChange(() -> {
			setDocDirty();
		});
	}

	/** @return The document model that is edited directly by the user */
	@Override
	public MuisDocumentModel getDocumentModel() {
		return getValueElement().getDocumentModel();
	}
	private void setEditModel(MuisDocumentModel model) {
		getValueElement().setDocumentModel(model);
	}

	private void setValue(ObservableValue<?> value, MuisFormatter<?> formatter, org.muis.core.rx.ObservableValueEvent<?> event) {
		register(value);
		MutableDocumentModel editModel = (MutableDocumentModel) getDocumentModel();
		boolean rich = atts().get(BaseAttributes.rich) == true;
		if(isDocOverridden || (rich && !(editModel instanceof RichDocumentModel)) || (!rich && !(editModel instanceof SimpleDocumentModel))) {
			editModel=rich ? new RichDocumentModel(getStyle().getSelf()) : new SimpleDocumentModel(getStyle().getSelf());
			setEditModel(editModel);
		}
		isDocOverridden = false;
		theCallbackLock++;
		try (Transaction t = editModel.holdForWrite()) {
			editModel.clear();
			if(formatter != null)
				((MuisFormatter<Object>) formatter).append(value, editModel);
			else
				editModel.append("" + value);
		} finally {
			theCallbackLock--;
		}
		if(value instanceof SettableValue) {
			((SettableValue<?>) value).isEnabled().takeUntil(event.getObservable())
				.act(enabledEvent -> theEnabledController.set(enabledEvent.getValue(), enabledEvent));
		} else
			theEnabledController.set(false, event);
	}

	private void setDocument(MuisDocumentModel doc) {
		register(doc);
		isDocOverridden = true;
		setEditModel(doc);
	}

	private void validateValue(boolean commit) {
		ObservableValue<?> mv = atts().get(ModelAttributes.value);
		if(!(mv instanceof SettableValue))
			return;
		MuisFormatter<?> formatter = atts().get(BaseAttributes.format);
		Validator<?> val = atts().get(BaseAttributes.validator);
		Object value = null;
		boolean parsed = false;
		try {
			if(formatter != null)
				value = formatter.parse(getDocumentModel());
			else
				value = getDocumentModel().toString();
			parsed = true;
			if(BaseAttributes.validator != null) {
				String valMsg = ((Validator<Object>) val).validate(value);
				if(valMsg != null) {
					// TODO Do something with the message
					theErrorController.set(true, null);
					return;
				}
			}

			String error = ((SettableValue<Object>) mv).isAcceptable(value);
			if(error != null) {
				// TODO Do something with the message
				theErrorController.set(true, null);
				return;
			}

			theErrorController.set(false, null);
		} catch(MuisParseException e) {
			// TODO Do something with the message and the positions
			theErrorController.set(true, e);
			return;
		} catch(Exception e) {
			if(!parsed)
				msg().error("Error parsing text field value: " + getDocumentModel(), e);
			else
				msg().error("Error validating text field value: " + getDocumentModel(), e, "value", value);
			return;
		}

		if(commit)
			try {
				((SettableValue<Object>) mv).set(value, null);
				theErrorController.set(false, null);
			} catch(IllegalArgumentException e) {
				theErrorController.set(true, null);
			} catch(Exception e) {
				msg().error("Error setting model value for text field: " + getDocumentModel(), e, "value", value);
			}
	}

	private void setDocDirty() {
		isDocDirty = true;
		if(isDocOverridden || atts().get(ModelAttributes.value) == null)
			return;
		validateValue(false); // Set the error state
	}

	private void pushChanges() {
		if(!isDocDirty || theCallbackLock > 0 || isDocOverridden)
			return;
		validateValue(true);
		resetDocument();
	}

	private void resetDocument() {
		ObservableValue<?> mv = atts().get(ModelAttributes.value);
		if(mv == null)
			return;
		MuisDocumentModel docModel = getDocumentModel();
		if(!(docModel instanceof MutableDocumentModel))
			return;
		MutableDocumentModel editModel = (MutableDocumentModel) docModel;
		MuisFormatter<?> formatter = atts().get(BaseAttributes.format);
		theCallbackLock++;
		try (Transaction t = editModel.holdForWrite()) {
			editModel.clear();
			if(formatter != null)
				((MuisFormatter<Object>) formatter).append(mv.get(), editModel);
			else
				editModel.append("" + mv.get());
		} finally {
			theCallbackLock--;
		}
		theErrorController.set(false, null);
		// TODO Think of a way to preserve selection?
	}

	private void register(Object o) {
		if(o instanceof org.muis.core.model.WidgetRegister)
			theRegistration = ((org.muis.core.model.WidgetRegister) o).register(TextField.this);
	}
}
