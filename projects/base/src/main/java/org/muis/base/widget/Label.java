package org.muis.base.widget;

import org.muis.core.MuisConstants;
import org.muis.core.MuisTextElement;
import org.muis.core.event.AttributeChangedEvent;
import org.muis.core.model.ModelAttributes;
import org.muis.core.model.MuisModelValue;
import org.muis.core.model.MuisModelValueEvent;

/**
 * A label is a container intended for text-only, but this is not enforced. It differs from block only in that its default layout may be
 * different (flow by default) and its style sheet attributes may be different (margin and padding are typically 0)
 */
public class Label extends org.muis.core.LayoutContainer {
	private org.muis.core.model.WidgetRegistration theRegistration;

	private org.muis.core.model.MuisModelValueListener<Object> theValueListener;

	public Label() {
		theValueListener = new org.muis.core.model.MuisModelValueListener<Object>() {
			@Override
			public void valueChanged(MuisModelValueEvent<? extends Object> evt) {
				setText(String.valueOf(evt.getNewValue()));
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

	/** @return The text displayed by this label, or null if this label contains no */
	public String getText() {
		if(getChildren().isEmpty())
			return "";
		if(getChildren().size() > 1 || !(getChildren().get(0) instanceof MuisTextElement))
			return null;
		return ((MuisTextElement) getChildren().get(0)).getText();
	}

	public void setText(String text) {
		if(getChildren().isEmpty())
			getChildManager().add(new MuisTextElement(text));
		else {
			if(getChildren().size() > 1 || !(getChildren().get(0) instanceof MuisTextElement))
				msg().warn("Label: Replacing content widgets with text");
			((MuisTextElement) getChildren().get(0)).setText(text);
		}
	}

	private void modelValueChanged(MuisModelValue<?> oldValue, MuisModelValue<?> newValue) {
		if(theRegistration != null)
			theRegistration.unregister();
		if(newValue instanceof org.muis.core.model.WidgetRegister)
			theRegistration = ((org.muis.core.model.WidgetRegister) newValue).register(Label.this);
		// TODO Auto-generated method stub

	}

	@Override
	protected org.muis.core.MuisLayout getDefaultLayout() {
		return new org.muis.base.layout.FlowLayout();
	}
}
