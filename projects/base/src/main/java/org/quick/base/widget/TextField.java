package org.quick.base.widget;

import java.util.Objects;

import org.observe.*;
import org.observe.util.TypeTokens;
import org.observe.util.WeakListening;
import org.qommons.*;
import org.quick.base.BaseAttributes;
import org.quick.base.BaseConstants;
import org.quick.base.model.*;
import org.quick.core.QuickTextElement;
import org.quick.core.mgr.AttributeManager2;
import org.quick.core.mgr.StateEngine.StateController;
import org.quick.core.model.*;
import org.quick.core.model.QuickDocumentModel.ContentChangeEvent;
import org.quick.core.model.QuickDocumentModel.QuickDocumentChangeEvent;
import org.quick.core.prop.QuickAttribute;
import org.quick.core.prop.QuickPropertyType;
import org.quick.core.tags.*;
import org.quick.util.QuickUtils;

import com.google.common.reflect.TypeToken;

import javafx.scene.input.KeyCode;

/**
 * A text field allows the user to edit a {@link QuickDocumentModel document}. Optionally, the content of this document can be synchronized
 * with a {@link ModelAttributes#value value} via a {@link BaseAttributes#format format}. Changes to the document not initiated by this
 * widget will be reflected in the display, but will not be propagated to the value until an appropriate user event occurs in the widget.
 *
 * The document is modified with each user input, but the value is only changed when the user presses {@link KeyCode#ENTER enter} (
 * {@link KeyBoardEvent#isControlPressed() ctrl}+{@link KeyCode#ENTER enter} for {@link QuickTextElement#multiLine multi-line} text fields)
 * or focus is lost after changes.
 */
@Template(location = "../../../../text-field.qts")
@QuickElementType(attributes = { @AcceptAttribute(declaringClass = ModelAttributes.class, field = "value", required = true),
	@AcceptAttribute(declaringClass = TextField.class, field = "charLengthAtt"),
	@AcceptAttribute(declaringClass = TextField.class, field = "charRowsAtt"),
	@AcceptAttribute(declaringClass = QuickTextElement.class, field = "multiLine"),
	@AcceptAttribute(declaringClass = BaseAttributes.class, field = "format"),
	@AcceptAttribute(declaringClass = BaseAttributes.class, field = "formatFactory"),
	@AcceptAttribute(declaringClass = BaseAttributes.class, field = "validator"),
	@AcceptAttribute(declaringClass = BaseAttributes.class, field = "document"),
	@AcceptAttribute(declaringClass = BaseAttributes.class, field = "rich")//
}, states = { //
	@State(name = BaseConstants.States.ERROR_NAME, priority = BaseConstants.States.ERROR_PRIORITY), //
	@State(name = BaseConstants.States.ENABLED_NAME, priority = BaseConstants.States.ENABLED_PRIORITY)//
})
public class TextField extends org.quick.core.QuickTemplate implements DocumentedElement {
	private final StateController theErrorController;
	private final StateController theEnabledController;

	private volatile boolean isDocDirty;
	private Object theLastParsedValue;

	private Observable<QuickDocumentChangeEvent> theDocChanges;
	/** Allows the user to set the height (in characters) of a text-editing widget */
	public static final QuickAttribute<Integer> charRowsAtt = QuickAttribute.build("rows", QuickPropertyType.integer).build();
	/** Allows the user to set the length (in characters) of a text-editing widget */
	public static final QuickAttribute<Integer> charLengthAtt = QuickAttribute.build("length", QuickPropertyType.integer).build();

	/** Creates a text field */
	public TextField() {
		theErrorController = state().control(BaseConstants.States.ERROR);
		theEnabledController = state().control(BaseConstants.States.ENABLED);
		life().runWhen(() -> {
			QuickDocumentModel doc = QuickDocumentModel.flatten(getValueElement().getDocumentModel());
			theDocChanges = WeakListening.weaklyListeningObservable(doc.changes());
			// Mark the document dirty when the user edits it
			theDocChanges.filter(evt -> {
				if (evt instanceof ContentChangeEvent && evt instanceof Causable) {
					TextEditEvent textEdit = null;
					textEdit = ((Causable) evt).getCauseLike(c -> {
						if (c instanceof TextEditEvent && ((TextEditEvent) c).getTextField() == TextField.this)
							return (TextEditEvent) c;
						else
							return null;
					});
					return textEdit == null;
				} else
					return false;
			}).act(evt -> setDocDirty());
			// Instantiate the format from the format factory
			ObservableValue<BiTuple<QuickFormatter.Factory<?>, QuickDocumentModel>> formatDocObs = atts().get(BaseAttributes.formatFactory)
				.combine(BiTuple::new, getDocumentModel());
			formatDocObs.changes().act(event -> {
				if (event.getNewValue().getValue1() == null)
					return; // Don't do anything if the factory is unset
				try {
					atts().get(BaseAttributes.format).set(event.getNewValue().getValue1().create(event.getNewValue().getValue2()), event);
				} catch (IllegalArgumentException e) {
					msg().error("Could not set format from factory", e);
				}
			});
			// When the value changes outside of this widget, update the document
			atts().get(ModelAttributes.value).changes().noInit().act(event -> {
				if (!isDocDirty)
					resetDocument(event);
			});

			// Allow the specification of the document and rich attributes
			atts().get(BaseAttributes.document).combine(BiTuple::new, atts().get(BaseAttributes.rich)).changes().act(evt -> {
				if (evt.getNewValue().getValue1() != null) {
					if (evt.getNewValue().getValue2() != null)
						msg().warn(BaseAttributes.document + " and " + BaseAttributes.rich + " both specified");
					getValueElement().setDocumentModel(evt.getNewValue().getValue1());
				} else if (evt.getNewValue().getValue2() != null) {
					getValueElement().setDocumentModel(evt.getNewValue().getValue2() ? //
					new RichDocumentModel(getValueElement()) : //
					new SimpleDocumentModel(getValueElement()));
				} else
					getValueElement().setDocumentModel(null);
			});
			// Set up the enabled state as a function of the value, format and validator
			ObservableValue<TriTuple<ObservableValue<? extends Object>, QuickFormatter<?>, Validator<?>>> trioObs = //
				atts().get(ModelAttributes.value).getContainer().combine(TriTuple::new, //
					atts().get(BaseAttributes.format), atts().get(BaseAttributes.validator));
			ObservableValue<String> enabled = ObservableValue
				.flatten(trioObs.map(tuple -> isEnabledFor(tuple.getValue1(), tuple.getValue2(), tuple.getValue3())));
			enabled.changes().act(e -> theEnabledController.setActive(e.getNewValue() == null, e));
			// Set up the error state as a function of value, formatter, validator, and the document itself
			ObservableValue<String> error = ObservableValue
				.flatten(trioObs.map(tuple -> getErrorFor(tuple.getValue1(), tuple.getValue2(), tuple.getValue3(), theDocChanges)));
			error.changes().act(e -> theErrorController.setActive(e.getNewValue() != null, e));
			resetDocument(null); // Set the document text to the value's initial value
		}, org.quick.core.QuickConstants.CoreStage.STARTUP.toString(), 1);
	}

	private ObservableValue<String> isEnabledFor(ObservableValue<? extends Object> valueContainer, QuickFormatter<?> formatter,
		Validator<?> validator) {
		String configError = null;
		String disabled = null;
		if (valueContainer == null)
			return ObservableValue.of(TypeTokens.get().STRING, null); // No value to worry about
		if (!(valueContainer instanceof SettableValue))
			disabled = "Value is not settable"; // Value is not settable
		else if (formatter == null) {
			if (!valueContainer.getType().isAssignableFrom(TypeToken.of(String.class)))
				configError = "No formatter"; // No formatter
			else if (validator != null && !validator.getType().isAssignableFrom(TypeToken.of(String.class)))
				configError = "Validator not valid for Strings";
		} else {
			if (formatter.getParseType() == null)
				disabled = "Formatter does not support parsing";
			else if (!QuickUtils.isAssignableFrom(formatter.getFormatType(), deepType(valueContainer)))
				configError = "Formatter is not compatible with value";
			else if (!QuickUtils.isAssignableFrom(deepType(valueContainer), formatter.getParseType()))
				configError = "Formatter cannot parse value's type";
			else if (validator != null && !QuickUtils.isAssignableFrom(validator.getType(), formatter.getParseType()))
				configError = "Validator is not valid for type " + formatter.getParseType();
		}

		if (configError != null) {
			msg().error(configError);
			return ObservableValue.of(configError);
		} else if (disabled != null)
			return ObservableValue.of(disabled);
		return ((SettableValue<?>) valueContainer).isEnabled();
	}

	private ObservableValue<String> getErrorFor(ObservableValue<? extends Object> valueContainer, QuickFormatter<?> formatter,
		Validator<?> validator, Observable<QuickDocumentChangeEvent> docChanges) {
		String configError = null;
		if (valueContainer == null)
			configError = "No value";
		else if (formatter == null) {
			if (!valueContainer.getType().isAssignableFrom(TypeToken.of(String.class)))
				configError = "No formatter";
		} else {
			if (!QuickUtils.isAssignableFrom(formatter.getFormatType(), deepType(valueContainer)))
				configError = "Formatter for " + formatter.getFormatType() + " is not compatible with value " + valueContainer + "'s type "
					+ valueContainer.getType();
			else if (formatter.getParseType() != null && !QuickUtils.isAssignableFrom(deepType(valueContainer), formatter.getParseType()))
				configError = "Formatter parseable for " + formatter.getParseType() + " cannot parse value " + valueContainer + "'s type "
					+ valueContainer.getType();
		}
		if (configError != null) {
			msg().error(configError);
			return ObservableValue.of(configError);
		}
		if (!(valueContainer instanceof SettableValue))
			return ObservableValue.of(TypeTokens.get().STRING, null);
		class TextFieldErrorValue implements ObservableValue<String> {
			@Override
			public TypeToken<String> getType() {
				return TypeToken.of(String.class);
			}

			@Override
			public String get() {
				if (formatter == null)
					theLastParsedValue = getDocumentModel().get().toString();
				else {
					try {
						theLastParsedValue = formatter.parse(getDocumentModel().get());
					} catch (QuickParseException e) {
						theLastParsedValue = null;
						msg().message(org.quick.core.mgr.QuickMessage.Type.DEBUG, e.getMessage(), null, e);
						return e.getMessage() == null ? "Parsing failed" : e.getMessage();
					}
				}
				if (validator != null) {
					String error2 = ((Validator<Object>) validator).validate(theLastParsedValue);
					if (error2 != null) {
						msg().debug(error2);
						theLastParsedValue = null;
						return error2;
					}
				}
				SettableValue<?> sv = (SettableValue<?>) valueContainer;
				String error2 = ((SettableValue<Object>) sv).isAcceptable(theLastParsedValue);
				if (error2 != null)
					theLastParsedValue = null;
				return error2;
			}

			@Override
			public Observable<ObservableValueEvent<String>> changes() {
				return new Changes();
			}

			@Override
			public Observable<ObservableValueEvent<String>> noInitChanges() {
				return changes().noInit();
			}

			class Changes implements Observable<ObservableValueEvent<String>> {
				private String theLastError;

				@Override
				public Subscription subscribe(Observer<? super ObservableValueEvent<String>> observer) {
					Subscription sub = docChanges.act(evt -> {
						String newError = get();
						if (!Objects.equals(newError, theLastError)) {
							ObservableValueEvent<String> errEvt = createChangeEvent(theLastError, newError, evt);
							try (Transaction t = Causable.use(errEvt)) {
								observer.onNext(errEvt);
							}
							theLastError = newError;
						}
					});
					theLastError = get();
					ObservableValueEvent<String> initEvt = createInitialEvent(theLastError, null);
					try (Transaction t = Causable.use(initEvt)) {
						observer.onNext(initEvt);
					}
					return sub;
				}

				@Override
				public boolean isLockSupported() {
					return docChanges.isLockSupported();
				}

				@Override
				public Transaction lock() {
					return docChanges.lock();
				}

				@Override
				public Transaction tryLock() {
					return docChanges.tryLock();
				}

				@Override
				public boolean isSafe() {
					return docChanges.isSafe();
				}
			}
		}
		return new TextFieldErrorValue();
	}

	private static <T> TypeToken<? extends T> deepType(ObservableValue<? extends T> value) {
		if (value instanceof ObservableValue.FlattenedObservableValue)
			return ((ObservableValue.FlattenedObservableValue<? extends T>) value).getDeepType();
		else if (value instanceof AttributeManager2.AttributeValue) {
			ObservableValue<? extends T> container = ((AttributeManager2.AttributeValue<? extends T>) value).getContainer().get();
			if (container == null)
				return value.getType();
			else
				return deepType(container);
		} else
			return value.getType();
	}

	/** @return This text field's model value, acceptance-filtered by its validator */
	public SettableValue<Object> getValue() {
		return atts().get(ModelAttributes.value);
	}

	/** @return The text element containing the text that is the value of this text field */
	protected org.quick.core.QuickTextElement getValueElement() {
		return (org.quick.core.QuickTextElement) getElement(getTemplate().getAttachPoint("value")).get();
	}

	/** @return The document model that is edited directly by the user */
	@Override
	public ObservableValue<QuickDocumentModel> getDocumentModel() {
		return getValueElement().getDocumentModel();
	}

	private void setDocDirty() {
		isDocDirty = true;
	}

	public void pushChanges(Object cause) {
		if (!isDocDirty || atts().get(ModelAttributes.value) == null || theErrorController.isActive())
			return;
		SettableValue<?> mv = atts().get(ModelAttributes.value);
		TextEditEvent textEdit = new TextEditEvent(this, cause);
		((SettableValue<Object>) mv).set(theLastParsedValue, textEdit);
		resetDocument(textEdit);
	}

	public void resetDocument(Object cause) {
		if (atts().get(ModelAttributes.value) == null)
			return;
		ObservableValue<?> mv = atts().get(ModelAttributes.value);
		QuickDocumentModel docModel = getDocumentModel().get();
		if (!(docModel instanceof MutableDocumentModel))
			return;
		MutableDocumentModel editModel = (MutableDocumentModel) docModel;
		QuickFormatter<?> formatter = atts().get(BaseAttributes.format).get();
		TextEditEvent textEdit = cause instanceof TextEditEvent ? (TextEditEvent) cause : new TextEditEvent(this, cause);
		try (Transaction tet = Causable.use(textEdit); Transaction t = editModel.holdForWrite(textEdit)) {
			if (formatter != null)
				((QuickFormatter<Object>) formatter).adjust(editModel, mv.get());
			else {
				editModel.clear();
				editModel.append("" + mv.get());
			}
		}
		isDocDirty = false;
	}

	private static class TextEditEvent extends Causable {
		private final TextField theTextField;

		TextEditEvent(TextField textField, Object cause) {
			super(cause);
			theTextField = textField;
		}

		TextField getTextField() {
			return theTextField;
		}
	}
}
