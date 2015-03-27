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
import org.muis.core.tags.Template;
import org.muis.util.Transaction;
import org.observe.ObservableValue;

import prisms.lang.Type;

/**
 * A label is a container intended for text-only, but this is not enforced. It differs from block only in that its default layout may be
 * different and its style sheet attributes may be different (margin and padding are typically 0)
 */
@Template(location = "../../../../label.muis")
public class Label extends org.muis.core.MuisTemplate implements org.muis.core.model.DocumentedElement {
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
				ObservableValue
					.flatten(new Type(Object.class), atts().getHolder(ModelAttributes.value))
					.tupleV(atts().getHolder(format).mapV(Formats.defNullCatch))
					.value()
					.act(
						tuple -> {
							if(tuple.getValue1() == null)
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
							} catch(ClassCastException e) {
								msg()
									.error(
										"Formatter instance " + tuple.getValue2() + " is incompatible with model value "
											+ tuple.getValue1(), e);
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
		return getValue().getText();
	}

	/** @return The text element containing this label's text */
	protected MuisTextElement getValue() {
		return (MuisTextElement) getElement(getTemplate().getAttachPoint("value"));
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
		} else
			getValue().setText(text);
	}

	@Override
	public MuisDocumentModel getDocumentModel() {
		return getValue().getDocumentModel();
	}

	/** @return The actual document model backing this label */
	public MuisDocumentModel getWrappedModel() {
		return getValue().getWrappedModel();
	}

	private org.muis.core.style.stateful.InternallyStatefulStyle getDocumentBackingStyle() {
		if(getChildren().isEmpty()) {
			MuisTextElement newText = new MuisTextElement();
			getChildManager().add(newText);
			return newText.getStyle().getSelf();
		} else {
			return getValue().getStyle().getSelf();
		}
	}

	/** @param doc The document model for this label */
	protected void setDocumentModel(MuisDocumentModel doc) {
		getValue().setDocumentModel(doc);
	}
}
