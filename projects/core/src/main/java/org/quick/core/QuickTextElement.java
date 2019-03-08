package org.quick.core;

import org.observe.ObservableValue;
import org.observe.SimpleSettableValue;
import org.quick.core.model.MutableDocumentModel;
import org.quick.core.model.QuickDocumentModel;
import org.quick.core.model.SimpleDocumentModel;
import org.quick.core.prop.QuickAttribute;
import org.quick.core.prop.QuickPropertyType;
import org.quick.core.tags.AcceptAttribute;
import org.quick.core.tags.QuickElementType;

import com.google.common.reflect.TypeToken;

/** A Quick element that serves as a placeholder for text content which may be interspersed with element children in an element. */
@QuickElementType(attributes = { @AcceptAttribute(declaringClass = QuickTextElement.class, field = "multiLine") })
public class QuickTextElement extends QuickLeaf implements org.quick.core.model.DocumentedElement {
	/** Whether a text element's document supports multiple lines */
	public static final QuickAttribute<Boolean> multiLine = QuickAttribute.build("multi-line", QuickPropertyType.boole).build();

	private final SimpleSettableValue<QuickDocumentModel> theDocument;
	private String theInitText;

	/** Creates a Quick text element */
	public QuickTextElement() {
		this("");
	}

	/**
	 * Creates a Quick text element with text
	 *
	 * @param text The text for the element
	 */
	public QuickTextElement(String text) {
		theInitText = text;
		setFocusable(true);
		theDocument = new SimpleSettableValue<>(TypeToken.of(QuickDocumentModel.class), false);
		life().runWhen(() -> {
			if (theDocument.get() == null)
				theDocument.set(getInitDocument(theInitText), null);
			theInitText = null;
		}, QuickConstants.CoreStage.INIT_SELF.toString(), 1);
	}

	/**
	 * Creates a Quick text element with a document
	 *
	 * @param doc The document for this element
	 */
	public QuickTextElement(QuickDocumentModel doc) {
		this("");
	}

	/**
	 * @param text The initial text for the document
	 * @return The initial document for the element after initialization. Will not be called if the
	 *         {@link #QuickTextElement(QuickDocumentModel)} constructor is used or if the document is otherwise initialized previously.
	 */
	protected QuickDocumentModel getInitDocument(String text) {
		return new SimpleDocumentModel(this, text);
	}


	/**
	 * @param text The text content for this element
	 * @throws UnsupportedOperationException If this element's document is not {@link MutableDocumentModel mutable}
	 */
	public void setText(String text) {
		QuickDocumentModel doc = theDocument.get();
		if (doc instanceof MutableDocumentModel)
			((MutableDocumentModel) doc).setText(text);
		else
			throw new UnsupportedOperationException("This text element's document is not mutable");
	}

	/** @return This element's text content */
	public String getText() {
		return getDocumentModel().get().toString();
	}

	@Override
	public ObservableValue<QuickDocumentModel> getDocumentModel() {
		return theDocument.unsettable();
	}

	/** @param docModel The new document model for this text element */
	public void setDocumentModel(QuickDocumentModel docModel) {
		if (docModel == null) {
			if (theDocument.get() == null)
				docModel = new SimpleDocumentModel(this);
			else
				return;
		}
		theDocument.set(docModel, null);
	}
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		if (getTagName() != null)
			ret.append('<').append(getTagName()).append('>');
		else
			ret.append("<!TEXT>");
		ret.append(org.jdom2.output.Format.escapeText(ch -> {
			if (org.jdom2.Verifier.isHighSurrogate(ch)) {
				return true; // Safer this way per http://unicode.org/faq/utf_bom.html#utf8-4
			}
			return false;
		}, "\n", theDocument.get().toString()));
		if (getTagName() != null)
			ret.append('<').append('/').append(getTagName()).append('>');
		else
			ret.append("</TEXT\u00a1>");
		return ret.toString();
	}
}
