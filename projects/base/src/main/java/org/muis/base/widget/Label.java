package org.muis.base.widget;

import static org.muis.base.BaseAttributes.document;
import static org.muis.base.BaseAttributes.format;
import static org.muis.base.BaseAttributes.rich;

import org.muis.base.BaseAttributes;
import org.muis.base.model.Formats;
import org.muis.base.model.MuisFormatter;
import org.muis.base.model.RichDocumentModel;
import org.muis.core.MuisConstants;
import org.muis.core.MuisException;
import org.muis.core.MuisTextElement;
import org.muis.core.model.*;
import org.muis.core.rx.ObservableValue;
import org.muis.util.Transaction;

/**
 * A label is a container intended for text-only, but this is not enforced. It differs from block only in that its default layout may be
 * different (flow by default) and its style sheet attributes may be different (margin and padding are typically 0)
 */
public class Label extends org.muis.core.LayoutContainer implements org.muis.core.model.DocumentedElement {
	private org.muis.core.model.WidgetRegistration theRegistration;

	/** Creates the label */
	public Label() {
		life().runWhen(
			() -> {
				Object accepter = new Object();
				atts().accept(accepter, document, rich, ModelAttributes.value, format);
				atts().getHolder(document).tupleV(atts().getHolder(rich)).value().act(tuple -> {
					if(tuple.getValue1() != null) {
						if(tuple.getValue2() != null)
							msg().warn(rich.getName() + " attribute specified, but model overridden.  Ignoring.");
						setDocumentModel(tuple.getValue1());
					} else if(tuple.getValue2() == Boolean.TRUE)
						setDocumentModel(new RichDocumentModel(getDocumentBackingStyle()));
					else
						setDocumentModel(new SimpleDocumentModel(getDocumentBackingStyle()));
					});
				atts().getHolder(ModelAttributes.value).value().act(modelValue -> {
					if(theRegistration != null)
						theRegistration.unregister();
					theRegistration = null;
					if(modelValue instanceof WidgetRegister)
						theRegistration = ((WidgetRegister) modelValue).register(Label.this);
				});
				ObservableValue.flatten(null, atts().getHolder(ModelAttributes.value))
					.tupleV(atts().getHolder(format).mapV(null, Formats.defNullCatch)).value().act(tuple -> {
						if(atts().getHolder(ModelAttributes.value) == null)
							return;
						MuisDocumentModel doc = getDocumentModel();
						if(!(doc instanceof MutableDocumentModel)) {
							msg().error("Model value specified with a non-mutable document model");
							return;
						}
						MutableDocumentModel mutableDoc = (MutableDocumentModel) doc;
						try (Transaction trans = mutableDoc.holdForWrite()) {
							mutableDoc.clear();
							((MuisFormatter<Object>) tuple.getValue2()).append(tuple.getValue1(), mutableDoc);
						}
					});

				/* Don't think this code is needed anymore
				if(Boolean.TRUE.equals(atts().get(BaseAttributes.rich))) {
					MuisDocumentModel doc = getWrappedModel();
					if(!(doc instanceof RichDocumentModel)) {
						String text = getText();
						setDocumentModel(new RichDocumentModel(getDocumentBackingStyle()));
						if(text != null)
							setText(text);
					}
				}*/
			}, MuisConstants.CoreStage.INITIALIZED.toString(), 1);
	}

	/**
	 * @param text The initial text for the label
	 * @param richText Whether the text is to be parsed as rich
	 */
	public Label(String text, boolean richText) {
		this();
		try {
			atts().set(BaseAttributes.rich, richText);
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
	protected void setDocumentModel(MuisDocumentModel doc) {
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

	@Override
	protected org.muis.core.MuisLayout getDefaultLayout() {
		return new org.muis.base.layout.FlowLayout();
	}
}
