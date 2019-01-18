package org.quick.base.widget;

import static org.quick.base.BaseAttributes.document;
import static org.quick.base.BaseAttributes.format;
import static org.quick.base.BaseAttributes.rich;

import org.observe.ObservableValue;
import org.qommons.BiTuple;
import org.qommons.Transaction;
import org.quick.base.BaseAttributes;
import org.quick.base.model.*;
import org.quick.core.QuickConstants;
import org.quick.core.QuickException;
import org.quick.core.QuickTextElement;
import org.quick.core.model.*;
import org.quick.core.tags.AcceptAttribute;
import org.quick.core.tags.QuickElementType;
import org.quick.core.tags.Template;

/**
 * A label is a container intended for text-only, but this is not enforced. It differs from block only in that its default layout may be
 * different and its style sheet attributes may be different (margin and padding are typically 0)
 */
@Template(location = "../../../../label.qts")
@QuickElementType(//
	attributes = { //
		@AcceptAttribute(declaringClass = BaseAttributes.class, field = "document"), //
		@AcceptAttribute(declaringClass = BaseAttributes.class, field = "rich"), //
		@AcceptAttribute(declaringClass = BaseAttributes.class, field = "format"), //
		@AcceptAttribute(declaringClass = BaseAttributes.class, field = "formatFactory"),
		@AcceptAttribute(declaringClass = ModelAttributes.class, field = "value"),//
	})
public class Label extends org.quick.core.QuickTemplate implements org.quick.core.model.DocumentedElement {
	/** Creates the label */
	public Label() {
		life().runWhen(
			() -> {
				// Instantiate the format from the format factory
				ObservableValue<BiTuple<QuickFormatter.Factory<?>, QuickDocumentModel>> formatDocObs = atts()
					.get(BaseAttributes.formatFactory).combine(BiTuple::new, getDocumentModel());
				formatDocObs.changes().act(event -> {
					if (event.getNewValue().getValue1() == null)
						return; // Don't do anything if the factory is unset
					try {
						atts().get(BaseAttributes.format).set(event.getNewValue().getValue1().create(event.getNewValue().getValue2()),
							event);
					} catch (IllegalArgumentException e) {
						msg().error("Could not set format from factory", e);
					}
				});
				atts().get(document).combine(BiTuple::new, atts().get(rich)).value().act(tuple -> {
					QuickTextElement textEl = getValue();
					QuickDocumentModel docModel;
					if(tuple.getValue1() != null) {
						if(tuple.getValue2() != null)
							msg().warn(rich.getName() + " attribute specified, but model overridden.  Ignoring.");
						docModel = tuple.getValue1();
						setDocumentModel(tuple.getValue1());
					} else if (tuple.getValue2() == Boolean.TRUE) {
						docModel = textEl.getDocumentModel().get();
						if (docModel == null)
							docModel = new RichDocumentModel(textEl);
						else if (!(docModel instanceof RichDocumentModel)) {
							try {
								docModel = new QuickRichTextParser().parse(new RichDocumentModel(textEl), textEl.getText(),
									getDocument().getEnvironment().getPropertyParser(), this);
							} catch (QuickException e) {
								msg().error("Could not parse rich text", e);
							}
						}
					} else {
						docModel = textEl.getDocumentModel().get();
						if (docModel == null)
							docModel = new SimpleDocumentModel(textEl);
						else if (docModel instanceof RichDocumentModel)
							docModel = new SimpleDocumentModel(textEl).setText(textEl.getText());
					}
					setDocumentModel(docModel);
				});
				atts().get(ModelAttributes.value).combine(BiTuple::new, atts().get(format).map(Formats.defNullCatch)).changes()
					.act(event -> {
						if (event.getNewValue().getValue1() == null)
						return;
					QuickDocumentModel doc = getDocumentModel().get();
					if (!(doc instanceof MutableDocumentModel)) {
						msg().error("Model value specified with a non-mutable document model");
						return;
					}
					MutableDocumentModel mutableDoc = (MutableDocumentModel) doc;
					try (Transaction trans = mutableDoc.holdForWrite(event)) {
							QuickFormatter<Object> formatter = (QuickFormatter<Object>) event.getNewValue().getValue2();
						if (formatter == null) {
								String text = String.valueOf(event.getNewValue().getValue1());
							if (!mutableDoc.toString().equals(text)) {
								mutableDoc.clear();
								mutableDoc.append(text);
							}
						} else
								formatter.adjust(mutableDoc, event.getNewValue().getValue1());
					} catch (ClassCastException e) {
							msg().error("Formatter instance " + event.getNewValue().getValue2() + " is incompatible with model value "
								+ event.getNewValue().getValue1(), e);
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
			atts().get(BaseAttributes.rich).set(richText, null);
		} catch (IllegalArgumentException e) {
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
		return (QuickTextElement) getElement(getTemplate().getAttachPoint("value")).get();
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

	/** @param doc The document model for this label */
	protected void setDocumentModel(QuickDocumentModel doc) {
		getValue().setDocumentModel(doc);
	}
}
