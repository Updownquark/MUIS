package org.quick.base.widget;

import static org.quick.core.QuickTextElement.multiLine;

import org.observe.*;
import org.qommons.Transaction;
import org.qommons.TriTuple;
import org.quick.base.BaseAttributes;
import org.quick.base.BaseConstants;
import org.quick.base.layout.TextEditLayout;
import org.quick.base.model.*;
import org.quick.core.QuickTextElement;
import org.quick.core.event.FocusEvent;
import org.quick.core.event.KeyBoardEvent;
import org.quick.core.event.KeyBoardEvent.KeyCode;
import org.quick.core.mgr.StateEngine.StateController;
import org.quick.core.model.*;
import org.quick.core.model.QuickDocumentModel.ContentChangeEvent;
import org.quick.core.model.SelectableDocumentModel.SelectionChangeEvent;
import org.quick.core.prop.DefaultExpressionContext;
import org.quick.core.prop.ExpressionContext;
import org.quick.core.tags.*;
import org.quick.util.QuickUtils;

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
@Template(location = "../../../../text-field.qts")
@QuickElementType(attributes = { @AcceptAttribute(declaringClass = ModelAttributes.class, field = "value", required = true),
	@AcceptAttribute(declaringClass = TextEditLayout.class, field = "charLengthAtt"),
	@AcceptAttribute(declaringClass = TextEditLayout.class, field = "charRowsAtt"),
	@AcceptAttribute(declaringClass = QuickTextElement.class, field = "multiLine"),
	@AcceptAttribute(declaringClass = BaseAttributes.class, field = "format"),
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
	private final SimpleTextEditing theTextEditing;

	private volatile boolean isDocDirty;
	private Object theLastParsedValue;

	/** Creates a text field */
	public TextField() {
		theErrorController = state().control(BaseConstants.States.ERROR);
		theEnabledController = state().control(BaseConstants.States.ENABLED);
		theTextEditing = new SimpleTextEditing();

		life().runWhen(() -> {
			QuickDocumentModel doc = QuickDocumentModel.flatten(getValueElement().getDocumentModel());
			theTextEditing.install(this); // Installs the text editing behavior
			// Mark the document dirty when the user edits it
			doc.changes().filter(evt -> {
				if (evt instanceof ContentChangeEvent || evt instanceof SelectionChangeEvent) {
					TextEditEvent textEdit = evt.getCauseLike(c -> {
						if (c instanceof TextEditEvent && ((TextEditEvent) c).getTextField() == TextField.this)
							return (TextEditEvent) c;
						else
							return null;
					});
					return textEdit == null;
				} else
					return false;
			}).act(evt -> setDocDirty());
			// When the value changes outside of this widget, update the document
			atts().getHolder(ModelAttributes.value).noInit().act(event -> {
				if (!isDocDirty)
					resetDocument(event);
			});
			// Set up the cursor overlay
			DocumentCursorOverlay cursor = (DocumentCursorOverlay) getElement(getTemplate().getAttachPoint("cursor-overlay")).get();
			cursor.setElement(this, getValueElement());

			// When the user leaves this widget, flush--either modify the value or reset the document
			events().filterMap(FocusEvent.blur).act(event -> {
				if (theErrorController.get())
					resetDocument(event);
				else
					pushChanges(event);
			});
			// When the user presses enter (CTRL+enter is required for a multi-line text field), push the changes
			// When the user presses escape, reset the document
			events().filterMap(KeyBoardEvent.key.press()).act(event -> {
				if (event.getKeyCode() == KeyBoardEvent.KeyCode.ENTER) {
					if (atts().get(multiLine, false) && !event.isControlPressed())
						return;
					pushChanges(event);
				} else if (event.getKeyCode() == KeyBoardEvent.KeyCode.ESCAPE)
					resetDocument(event);
			});

			// Allow the specification of the document and rich attributes
			atts().getHolder(BaseAttributes.document).tupleV(atts().getHolder(BaseAttributes.rich)).act(evt -> {
				if (evt.getValue().getValue1() != null) {
					if (evt.getValue().getValue2() != null)
						msg().warn(BaseAttributes.document + " and " + BaseAttributes.rich + " both specified");
					getValueElement().setDocumentModel(evt.getValue().getValue1());
				} else if (evt.getValue().getValue2() != null) {
					getValueElement().setDocumentModel(evt.getValue().getValue2() ? //
					new RichDocumentModel(getValueElement()) : //
					new SimpleDocumentModel(getValueElement()));
				} else
					getValueElement().setDocumentModel(null);
			});
			// Set up the enabled state as a function of the value, format and validator
			ObservableValue<TriTuple<ObservableValue<? extends Object>, QuickFormatter<?>, Validator<?>>> trioObs = //
				atts().getHolder(ModelAttributes.value).getContainer().tupleV(//
					atts().getHolder(BaseAttributes.format), atts().getHolder(BaseAttributes.validator));
			ObservableValue<String> enabled = ObservableValue.flatten(trioObs.mapV(tuple -> {
				String configError = null;
				String disabled = null;
				if (tuple.getValue1() == null)
					return ObservableValue.constant(TypeToken.of(String.class), null); // No value to worry about
				if (!(tuple.getValue1() instanceof SettableValue))
					disabled = "Value is not settable"; // Value is not settable
				else if (tuple.getValue2() == null) {
					if (!tuple.getValue1().getType().isAssignableFrom(TypeToken.of(String.class)))
						configError = "No formatter"; // No formatter
					else if (tuple.getValue3() != null && !tuple.getValue3().getType().isAssignableFrom(TypeToken.of(String.class)))
						configError = "Validator not valid for Strings";
				} else {
					if (tuple.getValue2().getParseType() == null)
						disabled = "Formatter does not support parsing";
					else if (!QuickUtils.isAssignableFrom(tuple.getValue2().getFormatType(), tuple.getValue1().getType()))
						configError = "Formatter is not compatible with value";
					else if (!QuickUtils.isAssignableFrom(tuple.getValue1().getType(), tuple.getValue2().getParseType()))
						configError = "Formatter cannot parse value's type";
					else if (tuple.getValue3() != null
						&& !QuickUtils.isAssignableFrom(tuple.getValue3().getType(), tuple.getValue2().getParseType()))
						configError = "Validator is not valid for type " + tuple.getValue2().getParseType();
				}

				if (configError != null) {
					msg().error(configError);
					return ObservableValue.constant(configError);
				} else if (disabled != null)
					return ObservableValue.constant(disabled);
				return ((SettableValue<?>) tuple.getValue1()).isEnabled();
			}));
			theEnabledController.link(enabled.mapV(e -> e == null, true));
			// Set up the error state as a function of value, formatter, validator, and the document itself
			ObservableValue<String> error = ObservableValue.flatten(trioObs.mapV(tuple -> {
				String configError = null;
				if (tuple.getValue1()==null)
					configError = "No value";
				else if (tuple.getValue2() == null) {
					if (!tuple.getValue1().getType().isAssignableFrom(TypeToken.of(String.class)))
						configError = "No formatter";
				} else {
					if (!QuickUtils.isAssignableFrom(tuple.getValue2().getFormatType(), tuple.getValue1().getType()))
						configError = "Formatter for " + tuple.getValue2().getFormatType() + " is not compatible with value "
							+ tuple.getValue1() + "'s type " + tuple.getValue1().getType();
					else if (tuple.getValue2().getParseType() != null
						&& !QuickUtils.isAssignableFrom(tuple.getValue1().getType(), tuple.getValue2().getParseType()))
						configError = "Formatter parseable for " + tuple.getValue2().getParseType() + " cannot parse value "
							+ tuple.getValue1() + "'s type " + tuple.getValue1().getType();
				}
				if (configError != null) {
					msg().error(configError);
					return ObservableValue.constant(configError);
				}
				if (!(tuple.getValue1() instanceof SettableValue))
					return ObservableValue.constant(TypeToken.of(String.class), null);
				ObservableValue<String> contentError = new ObservableValue<String>() {
					private String theLastError;

					@Override
					public TypeToken<String> getType() {
						return TypeToken.of(String.class);
					}

					@Override
					public String get() {
						if (tuple.getValue2() == null)
							theLastParsedValue = getDocumentModel().get().toString();
						else {
							try {
								theLastParsedValue = tuple.getValue2().parse(getDocumentModel().get());
							} catch (QuickParseException e) {
								theLastParsedValue = null;
								msg().message(org.quick.core.mgr.QuickMessage.Type.DEBUG, e.getMessage(), null, e);
								return e.getMessage() == null ? "Parsing failed" : e.getMessage();
							}
						}
						if (tuple.getValue3() != null) {
							String error2 = ((Validator<Object>) tuple.getValue3()).validate(theLastParsedValue);
							if (error2 != null) {
								msg().debug(error2);
								theLastParsedValue = null;
								return error2;
							}
						}
						SettableValue<?> sv = (SettableValue<?>) tuple.getValue1();
						String error2 = ((SettableValue<Object>) sv).isAcceptable(theLastParsedValue);
						if (error2 != null)
							theLastParsedValue = null;
						return error2;
					}

					@Override
					public Subscription subscribe(Observer<? super ObservableValueEvent<String>> observer) {
						Subscription sub = doc.changes().act(evt -> {
							String newError = get();
							if (newError != theLastError) {
								observer.onNext(createChangeEvent(theLastError, newError, evt));
								theLastError = newError;
							}
						});
						observer.onNext(createInitialEvent(get()));
						return sub;
					}

					@Override
					public boolean isSafe() {
						return doc.changes().isSafe();
					}
				};
				return contentError;
			}));
			theErrorController.link(error.mapV(err -> err != null, true));
			resetDocument(null); // Set the document text to the value's initial value
		}, org.quick.core.QuickConstants.CoreStage.STARTUP.toString(), 1);
	}

	/** @return This text field's model value, acceptance-filtered by its validator */
	public SettableValue<Object> getValue() {
		return new SettableValue<Object>() {
			@Override
			public TypeToken<Object> getType() {
				// Not real kosher that the type of this value could technically change, but the value attribute shouldn't change
				ObservableValue<?> internal = atts().getHolder(ModelAttributes.value).getContainedObservable();
				if (internal == null)
					return TypeToken.of(Object.class);
				return (TypeToken<Object>) internal.getType();
			}

			@Override
			public Object get() {
				return atts().get(ModelAttributes.value);
			}

			@Override
			public boolean isSafe() {
				ObservableValue<?> internal = atts().getHolder(ModelAttributes.value).getContainedObservable();
				return internal == null ? false : internal.isSafe();
			}

			@Override
			public Subscription subscribe(Observer<? super ObservableValueEvent<Object>> observer) {
				return atts().observe(ModelAttributes.value).subscribe(observer);
			}

			@Override
			public <V> String isAcceptable(V value) {
				ObservableValue<?> internal = atts().getHolder(ModelAttributes.value).getContainedObservable();
				if (internal == null)
					throw new IllegalArgumentException("No value");
				else if (!(internal instanceof SettableValue))
					throw new IllegalArgumentException("Value is not settable");
				else
					return ((SettableValue<Object>) internal).isAcceptable(value);
			}

			@Override
			public <V> Object set(V value, Object cause) throws IllegalArgumentException {
				ObservableValue<?> internal = atts().getHolder(ModelAttributes.value).getContainedObservable();
				if (internal == null)
					throw new IllegalArgumentException("No value");
				else if (!(internal instanceof SettableValue))
					throw new IllegalArgumentException("Value is not settable");
				else
					return ((SettableValue<Object>) internal).set(value, cause);
			}

			@Override
			public ObservableValue<String> isEnabled() {
				return ObservableValue.flatten(atts().getHolder(ModelAttributes.value).mapV(v -> {
					if (v == null)
						return ObservableValue.constant("No value");
					else if (!(v instanceof SettableValue))
						return ObservableValue.constant("Value is not settable");
					else
						return ((SettableValue<?>) v).isEnabled();
				}));
			}
		};
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

	private void pushChanges(Object cause) {
		if (!isDocDirty || !atts().isSet(ModelAttributes.value) || theErrorController.get())
			return;
		SettableValue<?> mv = atts().getHolder(ModelAttributes.value).asSettable();
		TextEditEvent textEdit = new TextEditEvent(this, cause);
		((SettableValue<Object>) mv).set(theLastParsedValue, textEdit);
		resetDocument(textEdit);
	}

	private void resetDocument(Object cause) {
		if (!atts().isSet(ModelAttributes.value))
			return;
		ObservableValue<?> mv = atts().getHolder(ModelAttributes.value);
		QuickDocumentModel docModel = getDocumentModel().get();
		if (!(docModel instanceof MutableDocumentModel))
			return;
		MutableDocumentModel editModel = (MutableDocumentModel) docModel;
		QuickFormatter<?> formatter = atts().get(BaseAttributes.format);
		TextEditEvent textEdit = cause instanceof TextEditEvent ? (TextEditEvent) cause : new TextEditEvent(this, cause);
		try (Transaction t = editModel.holdForWrite(textEdit)) {
			if (formatter != null)
				((QuickFormatter<Object>) formatter).adjust(editModel, mv.get());
			else {
				editModel.clear();
				editModel.append("" + mv.get());
			}
		}
		theErrorController.set(false, null);
		isDocDirty = false;
		// TODO Think of a way to preserve selection?
	}

	private static class TextEditEvent implements Causable {
		private final TextField theTextField;
		private final Object theCause;

		TextEditEvent(TextField textField, Object cause) {
			theTextField = textField;
			theCause = cause;
		}

		TextField getTextField() {
			return theTextField;
		}

		@Override
		public Object getCause() {
			return theCause;
		}
	}
}
