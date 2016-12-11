package org.quick.core.parser;

/** Represents textual data from a Quick file */
public class QuickText extends QuickContent {
	private final String theContent;

	private final boolean isCData;

	/**
	 * @param parent The parent structure of this text
	 * @param content The text content
	 * @param cdata Whether this content represents a CDATA block
	 */
	public QuickText(WidgetStructure parent, String content, boolean cdata) {
		super(parent);
		theContent = content;
		isCData = cdata;
	}

	/** @return The text content */
	public String getContent() {
		return theContent;
	}

	/** @return Whether this content represents a CDATA block */
	public boolean isCData() {
		return isCData;
	}

	@Override
	public String toString() {
		return new StringBuilder()//
			.append("<!TEXT")//
			.append(attrString()).append('>')//
			.append(org.jdom2.output.Format.escapeText(ch -> {
				if (org.jdom2.Verifier.isHighSurrogate(ch)) {
					return true; // Safer this way per http://unicode.org/faq/utf_bom.html#utf8-4
				}
				return false;
			}, "\n", theContent))//
			.append("</TEXT\u00a1>")//
			.toString();
	}
}
