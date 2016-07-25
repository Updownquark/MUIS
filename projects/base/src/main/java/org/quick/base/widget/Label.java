package org.quick.base.widget;

import static org.quick.base.BaseAttributes.document;
import static org.quick.base.BaseAttributes.format;
import static org.quick.base.BaseAttributes.rich;

import org.qommons.Transaction;
import org.quick.base.BaseAttributes;
import org.quick.base.model.Formats;
import org.quick.base.model.QuickFormatter;
import org.quick.base.model.RichDocumentModel;
import org.quick.core.QuickConstants;
import org.quick.core.QuickException;
import org.quick.core.QuickTextElement;
import org.quick.core.model.*;
import org.quick.core.tags.Template;

/**
 * A label is a container intended for text-only, but this is not enforced. It differs from block only in that its default layout may be
 * different and its style sheet attributes may be different (margin and padding are typically 0)
 */
@Template(location = "../../../../label.qck")
public class Label extends org.quick.core.QuickTemplate implements org.quick.core.model.DocumentedElement {
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
						setDocumentModel(new RichDocumentModel(getDocumentBackingStyle(), msg()));
					else
						setDocumentModel(new SimpleDocumentModel(getDocumentBackingStyle(), msg()));
				});
				atts().getHolder(ModelAttributes.value).tupleV(atts().getHolder(format).mapV(Formats.defNullCatch)).act(event -> {
					if (event.getValue().getValue1() == null)
						return;
					QuickDocumentModel doc = getDocumentModel();
					if (!(doc instanceof MutableDocumentModel)) {
						msg().error("Model value specified with a non-mutable document model");
						return;
					}
					MutableDocumentModel mutableDoc = (MutableDocumentModel) doc;
					try (Transaction trans = mutableDoc.holdForWrite(event)) {
						mutableDoc.clear();
						((QuickFormatter<Object>) event.getValue().getValue2()).append(event.getValue().getValue1(), mutableDoc);
					} catch (ClassCastException e) {
						msg().error("Formatter instance " + event.getValue().getValue2() + " is incompatible with model value "
							+ event.getValue().getValue1(),
							e);
					}
				});

				/* Don't think this code is needed anymore
				if(Boolean.TRUE.equals(atts().get(BaseAttributes.rich))) {
					QuickDocumentModel doc = getWrappedModel();
					if(!(doc instanceof RichDocumentModel)) {
						String text = getText();
						setDocumentModel(new RichDocumentModel(getDocumentBackingStyle()));
						if(text != null)
							setText(text);
					}
				}*/
			}, QuickConstants.CoreStage.INITIALIZED.toString(), 1);
	}

	/**
	 * @param text The initial text for the label
	 * @param richText Whether the text is to be parsed as rich
	 */
	public Label(String text, boolean richText) {
		this();
		try {
			atts().set(BaseAttributes.rich, richText);
		} catch(QuickException e) {
			throw new IllegalStateException(e);
		}
		setText(text);
	}

	/** @return The text displayed by this label, or null if this label contains no */
	public String getText() {
		return getValue().getText();
	}

	/** @return The text element containing this label's text */
	protected QuickTextElement getValue() {
		return (QuickTextElement) getElement(getTemplate().getAttachPoint("value"));
	}

	/**
	 * Sets this label's text. This call replaces all of this widget's content with a single {@link QuickTextElement} with the given text.
	 *
	 * @param text The text to display
	 */
	public void setText(String text) {
		if(text == null)
			text = "";
		QuickDocumentModel doc = getWrappedModel();
		if(doc == null)
			getChildManager().add(0, new QuickTextElement(text));
		else if(doc instanceof RichDocumentModel) {
			RichDocumentModel richDoc = (RichDocumentModel) doc;
			richDoc.setText("");
			try {
				new org.quick.base.model.QuickRichTextParser().parse(richDoc, text, doc().getEnvironment().getPropertyParser(), this);
			} catch(QuickException e) {
				msg().error("Could not parse rich text", e);
			}
		} else
			getValue().setText(text);
	}

	@Override
	public QuickDocumentModel getDocumentModel() {
		return getValue().getDocumentModel();
	}

	/** @return The actual document model backing this label */
	public QuickDocumentModel getWrappedModel() {
		return getValue().getWrappedModel();
	}

	private org.quick.core.style.stateful.InternallyStatefulStyle getDocumentBackingStyle() {
		if(getChildren().isEmpty()) {
			QuickTextElement newText = new QuickTextElement();
			getChildManager().add(newText);
			return newText.getStyle().getSelf();
		} else {
			return getValue().getStyle().getSelf();
		}
	}

	/** @param doc The document model for this label */
	protected void setDocumentModel(QuickDocumentModel doc) {
		getValue().setDocumentModel(doc);
	}
}
