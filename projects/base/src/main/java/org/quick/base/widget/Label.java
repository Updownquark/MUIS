package org.quick.base.widget;

import static org.quick.base.BaseAttributes.document;
import static org.quick.base.BaseAttributes.format;
import static org.quick.base.BaseAttributes.rich;

import org.observe.ObservableValue;
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
@Template(location = "../../../../label.qml")
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
					QuickDocumentModel doc = getDocumentModel().get();
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
		if (getChildManager().size() != 1 || !(getChildManager().get(0) instanceof QuickTextElement)) {
			getChildManager().clear();
			getChildManager().add(new QuickTextElement(text));
		} else {
			QuickTextElement textEl = (QuickTextElement) getChildManager().get(0);
			if (textEl.getDocumentModel().get() instanceof RichDocumentModel) {
				RichDocumentModel richDoc = (RichDocumentModel) textEl.getDocumentModel().get();
				richDoc.setText("");
				try {
					new org.quick.base.model.QuickRichTextParser().parse(richDoc, text, doc().getEnvironment().getPropertyParser(), this);
				} catch (QuickException e) {
					msg().error("Could not parse rich text", e);
				}
			}
			else
				textEl.setText(text);
		}
	}

	@Override
	public ObservableValue<QuickDocumentModel> getDocumentModel() {
		// TODO This does not account for the fact that the value element could change (e.g. setText above)
		return getValue().getDocumentModel();
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
