package org.quick.base.widget;

import static org.quick.base.layout.TextEditLayout.charLengthAtt;
import static org.quick.base.layout.TextEditLayout.charRowsAtt;
import static org.quick.core.QuickTextElement.multiLine;

import org.observe.ObservableValue;
import org.observe.SettableValue;
import org.qommons.BiTuple;
import org.qommons.Transaction;
import org.quick.base.BaseAttributes;
import org.quick.base.BaseConstants;
import org.quick.base.model.*;
import org.quick.core.QuickTextElement;
import org.quick.core.event.FocusEvent;
import org.quick.core.event.KeyBoardEvent;
import org.quick.core.event.KeyBoardEvent.KeyCode;
import org.quick.core.mgr.StateEngine.StateController;
import org.quick.core.model.*;
import org.quick.core.tags.*;

import com.google.common.reflect.TypeToken;

/**
 * A text field allows the user to edit a {@link QuickDocumentModel document}. Optionally, the content of this document can be synchronized
 * with a {@link ModelAttributes#value value} via a {@link BaseAttributes#format format}. Changes to the document not initiated by this
 * widget will be reflected in the display, but will not be propagated to the value until an appropriate user event occurs in the widget.
 *
 * The document is modified with each user input, but the value is only changed when the user presses {@link KeyCode#ENTER enter} (
 * {@link KeyBoardEvent#isControlPressed() ctrl}+{@link KeyCode#ENTER enter} for {@link QuickTextElement#multiLine multi-line} text fields)
 * or focus is lost after changes.
 */
@Template(location = "../../../../text-field.qck", //
	attributes = { //
		@ModelAttribute(name = "length", type = Integer.class), //
		@ModelAttribute(name = "rows", type = Integer.class), //
		@ModelAttribute(name = "multi-line", type = Boolean.class)//
	})
@StateSupport({ //
	@State(name = BaseConstants.States.ERROR_NAME, priority = BaseConstants.States.ERROR_PRIORITY), //
	@State(name = BaseConstants.States.ENABLED_NAME, priority = BaseConstants.States.ENABLED_PRIORITY)//
})
public class TextField extends org.quick.core.QuickTemplate implements DocumentedElement {
	private boolean isDocOverridden;

	private boolean isDocDirty;

	private final StateController theErrorController;
	private final StateController theEnabledController;
	private final SimpleTextEditing theTextEditing;

	private int theCallbackLock = 0;

	/** Creates a text field */
	public TextField() {
		theErrorController = state().control(BaseConstants.States.ERROR);
		theEnabledController = state().control(BaseConstants.States.ENABLED);
		theTextEditing = new SimpleTextEditing();

		life().runWhen(() -> {
			Object accepter = new Object();
			atts().accept(accepter, ModelAttributes.value, charLengthAtt, charRowsAtt, multiLine, BaseAttributes.format,
				BaseAttributes.document, BaseAttributes.rich);

			ObservableValue<BiTuple<ObservableValue<? extends Object>, QuickFormatter<?>>> duoObs = //
				atts().getHolder(ModelAttributes.value).getContainer().tupleV(//
					atts().getHolder(BaseAttributes.format));
			ObservableValue<String> enabled = ObservableValue.flatten(duoObs.mapV(tuple -> {
				if (tuple.getValue1() == null)
					return ObservableValue.constant(TypeToken.of(String.class), null); // No value to worry about
				if (!(tuple.getValue1() instanceof SettableValue))
					return ObservableValue.constant("Value is not settable"); // Value is not settable
				if (tuple.getValue2() == null){
					if(!tuple.getValue1().getType().isAssignableFrom(TypeToken.of(String.class)))
						return ObservableValue.constant("No formatter"); // No formatter
				} else{
					if (tuple.getValue2().getParseType() == null)
						return ObservableValue.constant("Formatter does not support parsing");
					if (!tuple.getValue2().getFormatType().isAssignableFrom(tuple.getValue1().getType()))
						return ObservableValue.constant("Formatter is not compatible with value");
					if(!tuple.getValue1().getType().isAssignableFrom(tuple.getValue2().getParseType()))
						return ObservableValue.constant("Formatter cannot parse value's type");
				}

				return ((SettableValue<?>) tuple.getValue1()).isEnabled();
			}));
			theEnabledController.link(enabled.mapV(e->e!=null));
			ObservableValue<String> error=ObservableValue.flatten(duoObs.mapV(tuple->{
				if(tuple.getValue1()==null && tuple.getValue2()!=null)
					return ObservableValue.constant("No value");
				if (tuple.getValue2() == null){
					if(!tuple.getValue1().getType().isAssignableFrom(TypeToken.of(String.class)))
						return ObservableValue.constant("No formatter"); // No formatter
				} else{
					if (!tuple.getValue2().getFormatType().isAssignableFrom(tuple.getValue1().getType()))
						return ObservableValue.constant("Formatter is not compatible with value");
					if(tuple.getValue2().getParseType() != null && !tuple.getValue1().getType().isAssignableFrom(tuple.getValue2().getParseType()))
						return ObservableValue.constant("Formatter cannot parse value's type");
				}
				return ObservableValue.constant(TypeToken.of(String.class), null);
			}));
			// TODO Take into account the content of the document
			theErrorController.link(error.mapV(e->e!=null));
			atts().getHolder(ModelAttributes.value).act(event -> theErrorController.set(false, event));

			ObservableValue<QuickDocumentModel> docObs = ObservableValue.flatten(atts().getHolder(BaseAttributes.document).mapV(doc -> {
				if (doc != null)
					return ObservableValue.constant(TypeToken.of(QuickDocumentModel.class), doc);
				else
					return atts().getHolder(BaseAttributes.rich).mapV(rich -> {
						return rich ? new RichDocumentModel(getStyle().getSelf(), msg())
							: new SimpleDocumentModel(getStyle().getSelf(), msg());
					});
			}));
			docObs.act(event -> getValueElement().setDocumentModel(event.getValue()));
			}, org.quick.core.QuickConstants.CoreStage.INIT_CHILDREN.toString(), 1);
		life().runWhen(() -> {
			initDocument();
			DocumentCursorOverlay cursor = (DocumentCursorOverlay) getElement(getTemplate().getAttachPoint("cursor-overlay"));
			cursor.setTextElement(getValueElement());
			cursor.setStyleAnchor(getStyle().getSelf());

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
		}, org.quick.core.QuickConstants.CoreStage.INITIALIZED.toString(), 1);
	}

	/** @return The text element containing the text that is the value of this text field */
	protected org.quick.core.QuickTextElement getValueElement() {
		return (org.quick.core.QuickTextElement) getElement(getTemplate().getAttachPoint("value"));
	}

	/**
	 * Initializes the document for this text field. This may be overridden and used by subclasses but should never be called directly
	 * except as the super call from the extending method.
	 */
	protected void initDocument() {
		theTextEditing.install(this);
		theEnabledController.act(evt -> theTextEditing.setEnabled(evt.getValue()));
		getValueElement().getDocumentModel().onContentStyleChange(() -> {
			if(theCallbackLock == 0)
				setDocDirty();
		});
	}

	/** @return The document model that is edited directly by the user */
	@Override
	public QuickDocumentModel getDocumentModel() {
		return getValueElement().getDocumentModel();
	}
	private void setEditModel(QuickDocumentModel model) {
		getValueElement().setDocumentModel(model);
	}

	private void setValue(ObservableValue<?> value, QuickFormatter<?> formatter, org.observe.ObservableValueEvent<?> event) {
		MutableDocumentModel editModel = (MutableDocumentModel) getDocumentModel();
		boolean rich = atts().get(BaseAttributes.rich) == Boolean.TRUE;
		if(isDocOverridden || (rich && !(editModel instanceof RichDocumentModel)) || (!rich && !(editModel instanceof SimpleDocumentModel))) {
			editModel = rich ? new RichDocumentModel(getStyle().getSelf(), msg()) : new SimpleDocumentModel(getStyle().getSelf(), msg());
			setEditModel(editModel);
		}
		isDocOverridden = false;
		theCallbackLock++;
		try (Transaction t = editModel.holdForWrite(event)) {
			editModel.clear();
			if(formatter != null)
				((QuickFormatter<Object>) formatter).append(value.get(), editModel);
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

	private void setDocument(QuickDocumentModel doc) {
		isDocOverridden = true;
		setEditModel(doc);
	}

	private void validateValue(boolean commit) {
		ObservableValue<?> mv = atts().get(ModelAttributes.value);
		if(!(mv instanceof SettableValue))
			return;
		QuickFormatter<?> formatter = atts().get(BaseAttributes.format);
		Validator<?> val = atts().get(BaseAttributes.validator);
		Object value = null;
		boolean parsed = false;
		try {
			if(formatter != null)
				value = formatter.parse(getDocumentModel());
			else
				value = getDocumentModel().toString();
			parsed = true;
			if(val != null) {
				String valMsg = ((Validator<Object>) val).validate(value);
				if(valMsg != null) {
					msg().debug(valMsg);
					// TODO Do something with the message
					theErrorController.set(true, null);
					return;
				}
			}

			String error = ((SettableValue<Object>) mv).isAcceptable(value);
			if(error != null) {
				msg().debug(error);
				// TODO Do something with the message
				theErrorController.set(true, null);
				return;
			}

			theErrorController.set(false, null);
		} catch(QuickParseException e) {
			msg().message(org.quick.core.mgr.QuickMessage.Type.DEBUG, e.getMessage(), null, e);
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
		theCallbackLock++;
		try {
			validateValue(true);
		} finally {
			theCallbackLock--;
		}
		resetDocument();
	}

	private void resetDocument() {
		ObservableValue<?> mv = atts().get(ModelAttributes.value);
		if(mv == null)
			return;
		QuickDocumentModel docModel = getDocumentModel();
		if(!(docModel instanceof MutableDocumentModel))
			return;
		MutableDocumentModel editModel = (MutableDocumentModel) docModel;
		QuickFormatter<?> formatter = atts().get(BaseAttributes.format);
		theCallbackLock++;
		try (Transaction t = editModel.holdForWrite()) {
			editModel.clear();
			if(formatter != null)
				((QuickFormatter<Object>) formatter).append(mv.get(), editModel);
			else
				editModel.append("" + mv.get());
		} finally {
			theCallbackLock--;
		}
		theErrorController.set(false, null);
		// TODO Think of a way to preserve selection?
	}
}
