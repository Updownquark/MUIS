package org.muis.base.widget;

import org.muis.core.MuisConstants;
import org.muis.core.MuisTextElement;
import org.muis.core.event.AttributeChangedEvent;
import org.muis.core.model.*;

/**
 * A label is a container intended for text-only, but this is not enforced. It differs from block only in that its default layout may be
 * different (flow by default) and its style sheet attributes may be different (margin and padding are typically 0)
 */
public class Label extends org.muis.core.LayoutContainer implements org.muis.core.model.DocumentedElement {
	private org.muis.core.model.WidgetRegistration theRegistration;

	private org.muis.core.model.MuisModelValueListener<Object> theValueListener;

	/** Creates the label */
	public Label() {
		theValueListener = new org.muis.core.model.MuisModelValueListener<Object>() {
			@Override
			public void valueChanged(MuisModelValueEvent<? extends Object> evt) {
				setText(getTextFor(evt.getNewValue()));
			}
		};
		life().runWhen(new Runnable() {
			@Override
			public void run() {
				atts().accept(new Object(), ModelAttributes.value);
				addListener(MuisConstants.Events.ATTRIBUTE_CHANGED, new org.muis.core.event.AttributeChangedListener<MuisModelValue<?>>(
					ModelAttributes.value) {
					@Override
					public void attributeChanged(AttributeChangedEvent<MuisModelValue<?>> event) {
						modelValueChanged(event.getOldValue(), event.getValue());
					}
				});
				modelValueChanged(null, atts().get(ModelAttributes.value));
			}
		}, MuisConstants.CoreStage.INITIALIZED.toString(), 1);
	}

	/** @param text The initial text for the label */
	public Label(String text) {
		this();
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
			getChildManager().clear();
		else if(getChildren().isEmpty())
			getChildManager().add(new MuisTextElement(text));
		else {
			if(getChildren().size() > 1 || !(getChildren().get(0) instanceof MuisTextElement))
				msg().warn("Label: Replacing content widgets with text");
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

	private void modelValueChanged(MuisModelValue<?> oldValue, MuisModelValue<?> newValue) {
		if(theRegistration != null)
			theRegistration.unregister();
		if(oldValue != null)
			oldValue.removeListener(theValueListener);
		if(newValue instanceof org.muis.core.model.WidgetRegister)
			theRegistration = ((org.muis.core.model.WidgetRegister) newValue).register(Label.this);
		if(newValue != null) {
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
