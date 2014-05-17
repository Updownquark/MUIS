package org.muis.base.widget;

import org.muis.base.model.RichDocumentModel;
import org.muis.core.*;
import org.muis.core.event.AttributeChangedEvent;
import org.muis.core.model.*;

/**
 * A label is a container intended for text-only, but this is not enforced. It differs from block only in that its default layout may be
 * different (flow by default) and its style sheet attributes may be different (margin and padding are typically 0)
 */
public class Label extends org.muis.core.LayoutContainer implements org.muis.core.model.DocumentedElement {
	/** The attribute allowing the user to specify a label that parses rich text */
	public static final MuisAttribute<Boolean> rich = new MuisAttribute<>("rich", org.muis.core.MuisProperty.boolAttr);

	private org.muis.core.model.WidgetRegistration theRegistration;

	private org.muis.core.rx.ObservableListener<Object> theValueListener;

	/** Creates the label */
	public Label() {
		theValueListener = evt -> {
			setText(getTextFor(evt.getNewValue()));
		};
		life().runWhen(() -> {
			Object accepter = new Object();
			atts().accept(accepter, rich);
			atts().accept(accepter, ModelAttributes.value);
			events().listen(AttributeChangedEvent.att(rich), (AttributeChangedEvent<Boolean> event) -> {
				setDocumentModel(event.getValue() ? new RichDocumentModel(getDocumentBackingStyle()) : new SimpleDocumentModel(
					getDocumentBackingStyle()));
			});
			events().listen(AttributeChangedEvent.att(ModelAttributes.value), (AttributeChangedEvent<MuisModelValue<?>> event) -> {
				modelValueChanged(event.getOldValue(), event.getValue());
			});
			modelValueChanged(null, atts().get(ModelAttributes.value));

			if(Boolean.TRUE.equals(atts().get(rich))) {
				MuisDocumentModel doc = getWrappedModel();
				if(!(doc instanceof RichDocumentModel)) {
					String text = getText();
					setDocumentModel(new RichDocumentModel(getDocumentBackingStyle()));
					if(text != null)
						setText(text);
				}
			}
		}, MuisConstants.CoreStage.INITIALIZED.toString(), 1);
	}

	/**
	 * @param text The initial text for the label
	 * @param richText Whether the text is to be parsed as rich
	 */
	public Label(String text, boolean richText) {
		this();
		try {
			atts().set(rich, richText);
		} catch(MuisException e) {
			throw new IllegalStateException(e);
		}
		setText(text);
	}

	/** @return The text displayed by this label, or null if this label contains no */
	public String getText() {
		if(getChildren().isEmpty())
			return "";
		if(getChildren().size() > 1 || !(getChildren().get(0) instanceof MuisTextElement))
			return null;
		return ((MuisTextElement) getChildren().get(0)).getText();
	}

	/**
	 * Sets this label's text. This call replaces all of this widget's content with a single {@link MuisTextElement} with the given text.
	 *
	 * @param text The text to display
	 */
	public void setText(String text) {
		if(text == null)
			text = "";
		MuisDocumentModel doc = getWrappedModel();
		if(doc == null)
			getChildManager().add(0, new MuisTextElement(text));
		else if(doc instanceof RichDocumentModel) {
			RichDocumentModel richDoc = (RichDocumentModel) doc;
			richDoc.setText("");
			try {
				new org.muis.base.model.MuisRichTextParser().parse(richDoc, text, this);
			} catch(MuisException e) {
				msg().error("Could not parse rich text", e);
			}
		} else {
			if(getChildren().size() > 1 || !(getChildren().get(0) instanceof MuisTextElement)) {
				msg().warn("Label: Replacing content widgets with text");
				getChildren().clear();
				getChildren().add(new MuisTextElement(text));
			} else
				((MuisTextElement) getChildren().get(0)).setText(text);
		}
	}

	@Override
	public MuisDocumentModel getDocumentModel() {
		if(getChildren().isEmpty())
			return null;
		if(getChildren().size() > 1 || !(getChildren().get(0) instanceof MuisTextElement))
			return null;
		return ((MuisTextElement) getChildren().get(0)).getDocumentModel();
	}

	/** @return The actual document model backing this label */
	public MuisDocumentModel getWrappedModel() {
		if(getChildren().isEmpty())
			return null;
		if(getChildren().size() > 1 || !(getChildren().get(0) instanceof MuisTextElement))
			return null;
		return ((MuisTextElement) getChildren().get(0)).getWrappedModel();
	}

	private org.muis.core.style.stateful.InternallyStatefulStyle getDocumentBackingStyle() {
		if(getChildren().isEmpty()) {
			MuisTextElement newText = new MuisTextElement();
			getChildManager().add(newText);
			return newText.getStyle().getSelf();
		} else {
			if(getChildren().size() > 1 || !(getChildren().get(0) instanceof MuisTextElement)) {
				msg().warn("Label: Replacing content widgets with text");
				getChildren().clear();
				MuisTextElement newText = new MuisTextElement();
				getChildManager().add(newText);
				return newText.getStyle().getSelf();
			} else
				return ((MuisTextElement) getChildren().get(0)).getStyle().getSelf();
		}
	}

	/** @param doc The document model for this label */
	public void setDocumentModel(MuisDocumentModel doc) {
		if(getChildren().isEmpty())
			getChildManager().add(new MuisTextElement(doc));
		else {
			if(getChildren().size() > 1 || !(getChildren().get(0) instanceof MuisTextElement)) {
				msg().warn("Label: Replacing content widgets with text");
				getChildren().clear();
				getChildren().add(new MuisTextElement(doc));
			} else
				((MuisTextElement) getChildren().get(0)).setDocumentModel(doc);
		}
	}

	private void modelValueChanged(MuisModelValue<?> oldValue, MuisModelValue<?> newValue) {
		if(theRegistration != null)
			theRegistration.unregister();
		if(oldValue instanceof MuisDocumentModel) {
			if(!(newValue instanceof MuisDocumentModel))
				setDocumentModel(null);
		} else if(oldValue != null)
			oldValue.removeListener(theValueListener);
		if(newValue instanceof org.muis.core.model.WidgetRegister)
			theRegistration = ((org.muis.core.model.WidgetRegister) newValue).register(Label.this);
		if(newValue instanceof MuisDocumentModel)
			setDocumentModel((MuisDocumentModel) newValue);
		else if(newValue != null) {
			newValue.addListener(theValueListener);
			setText(getTextFor(newValue.get()));
		}
	}

	@Override
	protected org.muis.core.MuisLayout getDefaultLayout() {
		return new org.muis.base.layout.FlowLayout();
	}

	private static String getTextFor(Object value) {
		return value == null ? null : value.toString();
	}
}
