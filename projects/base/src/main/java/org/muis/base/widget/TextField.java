package org.muis.base.widget;

import static org.muis.base.layout.TextEditLayout.charLengthAtt;
import static org.muis.base.layout.TextEditLayout.charRowsAtt;
import static org.muis.core.MuisTextElement.multiLine;

import org.muis.base.BaseConstants;
import org.muis.base.model.MuisFormatter;
import org.muis.base.model.RichDocumentModel;
import org.muis.core.MuisAttribute;
import org.muis.core.MuisProperty;
import org.muis.core.event.FocusEvent;
import org.muis.core.event.KeyBoardEvent;
import org.muis.core.mgr.StateEngine.StateController;
import org.muis.core.model.*;
import org.muis.core.rx.ObservableValue;
import org.muis.core.rx.SettableValue;
import org.muis.core.tags.State;
import org.muis.core.tags.StateSupport;
import org.muis.core.tags.Template;
import org.muis.util.BiTuple;
import org.muis.util.Transaction;

/** A simple widget that takes and displays text input from the user */
@Template(location = "../../../../text-field.muis")
@StateSupport({@State(name = BaseConstants.States.ERROR_NAME, priority = BaseConstants.States.ERROR_PRIORITY),
		@State(name = BaseConstants.States.ENABLED_NAME, priority = BaseConstants.States.ENABLED_PRIORITY)})
public class TextField extends org.muis.core.MuisTemplate implements DocumentedElement {
	/** Allows the user to specify the model whose content is displayed in this text field */
	public static final MuisAttribute<MuisDocumentModel> document = new MuisAttribute<>("doc", new MuisProperty.MuisTypeInstanceProperty<>(
		MuisDocumentModel.class));

	/** Allows specification of the format used by the text field */
	public static final MuisAttribute<MuisFormatter<?>> format = new MuisAttribute<>("format", new MuisProperty.MuisTypeInstanceProperty<>(
		(Class<MuisFormatter<?>>) (Class<?>) MuisFormatter.class));

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
			atts().accept(accepter, charLengthAtt).act(event -> {
				try {
					getElement(getTemplate().getAttachPoint("text")).atts().set(charLengthAtt, event.getValue());
				} catch(org.muis.core.MuisException e) {
					msg().error("Could not pass on " + charLengthAtt, e);
				}
			});
			atts().accept(accepter, charRowsAtt).act(event -> {
				try {
					getElement(getTemplate().getAttachPoint("text")).atts().set(charRowsAtt, event.getValue());
				} catch(org.muis.core.MuisException e) {
					msg().error("Could not pass on " + charRowsAtt, e);
				}
			});
			atts().accept(accepter, multiLine);
				atts().accept(accepter, ModelAttributes.value).tupleV(atts().accept(accepter, format))
				.tupleV(atts().accept(accepter, document).composeV(ObservableValue.first(), atts().accept(accepter, Label.rich)))
				.act(event -> {
					if(theRegistration != null)
						theRegistration.unregister();
						BiTuple<BiTuple<ObservableValue<?>, MuisFormatter<?>>, MuisDocumentModel> tuple = event.getValue();
					if(tuple.getValue1().getValue1() == null && tuple.getValue2() == null)
						msg().warn("No model value or document specified");
					else if(tuple.getValue1().getValue1() != null) {
						if(tuple.getValue2() != null)
							msg().warn("Both model value and document specified. Using model value.");
						setValue(tuple.getValue1().getValue1(), tuple.getValue1().getValue2());
					} else {
						setDocument(tuple.getValue2());
					}
				});
				atts().getHolder(ModelAttributes.value).act(event -> theErrorController.set(false, event));
				atts().getHolder(ModelAttributes.value).error().act(ex -> theErrorController.set(true, ex));
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
		new org.muis.base.model.SimpleTextEditing().install(this);
		getValueElement().getDocumentModel().onContentStyleChange(() -> {
			isDocDirty = true;
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

	private void setValue(ObservableValue<?> value, MuisFormatter<?> formatter) {
		register(value);
		MutableDocumentModel editModel = (MutableDocumentModel) getDocumentModel();
		boolean rich = atts().get(Label.rich) == true;
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
	}

	private void setDocument(MuisDocumentModel doc) {
		register(doc);
		isDocOverridden = true;
		setEditModel(doc);
	}

	void pushChanges() {
		if(!isDocDirty || theCallbackLock > 0 || isDocOverridden)
			return;
		ObservableValue<?> mv = atts().get(ModelAttributes.value);
		if(mv == null)
			return;
		MuisFormatter<?> formatter = atts().get(format);
		if(formatter != null)
			((SettableValue<Object>) mv).set(formatter.parse(getDocumentModel()), null);
		else
			((SettableValue<String>) mv).set(getDocumentModel().toString(), null);
	}

	private void register(Object o) {
		if(o instanceof org.muis.core.model.WidgetRegister)
			theRegistration = ((org.muis.core.model.WidgetRegister) o).register(TextField.this);
	}
}
