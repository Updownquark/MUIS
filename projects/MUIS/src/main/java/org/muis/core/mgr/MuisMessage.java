/* Created by Andrew Mar 19, 2010 */
package org.muis.core.mgr;

import org.muis.core.MuisDocument;
import org.muis.core.MuisElement;

/** Represents an error, warning, or information message attached to a MUIS element */
public class MuisMessage {
	/** The types of element messages available */
	public static enum Type {
		/** Represents an information message */
		INFO,
		/** Represents a warning message */
		WARNING,
		/** Represents an error message */
		ERROR,
		/** Represents a fatal error which effectively disables an element or document */
		FATAL;
	}

	/** The MUIS document that the message is for */
	public final MuisDocument document;

	/** The MUIS element that the message is for */
	public final MuisElement element;

	/** The type of this message */
	public final Type type;

	/** The stage in the element's life cycle at which this message occurred */
	public final String stage;

	/** The text of the message, describing the problem */
	public final String text;

	/** The exception that may have caused this message */
	public final Throwable exception;

	private java.util.Map<String, Object> theParams;

	private MuisMessage(MuisDocument doc, MuisElement anElement, Type aType, String aStage, String aText, Throwable anException,
		Object... params) {
		if(doc == null && anElement == null)
			throw new NullPointerException("A MUIS message cannot be created without a document or an element");
		if(aType == null)
			throw new NullPointerException("A MUIS message cannot be created without a type");
		if(aStage == null)
			throw new NullPointerException("A MUIS message cannot be created without a creation stage");
		if(aText == null)
			throw new NullPointerException("A MUIS message cannnot be created without a description");
		document = doc;
		element = anElement;
		type = aType;
		stage = aStage;
		text = aText;
		exception = anException;
		if(params != null && params.length > 0) {
			theParams = new java.util.HashMap<String, Object>();
			if(params.length % 2 != 0)
				throw new IllegalArgumentException("message params must be in format [name, value, name, value, ...]"
					+ "--odd argument count not allowed");
			theParams = new java.util.HashMap<String, Object>();
			for(int d = 0; d < params.length; d += 2) {
				if(!(params[d] instanceof String))
					throw new IllegalArgumentException("message params must be in format [name, value, name, value, ...]"
						+ "--even indices must be strings");
				theParams.put((String) params[d], params[d + 1]);
			}
		}
	}

	MuisMessage(MuisDocument doc, Type aType, String aStage, String aText, Throwable anException, Object... params) {
		this(doc, null, aType, aStage, aText, anException, params);
	}

	MuisMessage(MuisElement anElement, Type aType, String aStage, String aText, Throwable anException, Object... params) {
		this(anElement.getDocument(), anElement, aType, aStage, aText, anException, params);
	}

	/**
	 * @param param The name of the parameter to get
	 * @return The value of the given parameter
	 */
	public Object getParam(String param) {
		if(theParams == null)
			return null;
		return theParams.get(param);
	}

	/** @return All parameter names with values in this message */
	public String [] getParams() {
		if(theParams == null)
			return new String[0];
		return theParams.keySet().toArray(new String[theParams.size()]);
	}

	@Override
	public final boolean equals(Object o) {
		return super.equals(o);
	}

	@Override
	public final int hashCode() {
		return super.hashCode();
	}

	/** Prints out a representation of the path to the problem element and the description of the error */
	@Override
	public String toString() {
		MuisElement el = element;
		String ret = "";
		while(el != null)
			ret = (el.getNamespace() != null ? el.getNamespace() + ":" : "") + el.getTagName() + "/" + ret;
		return ret + ": " + text;
	}

	/**
	 * @return A full description of the message, including the path to the problem element, the description of the error, any parameters in
	 *         the message, and the stack trace of the exception that caused the problem
	 */
	public String toFullString() {
		String ret = toString();
		if(theParams != null)
			for(java.util.Map.Entry<String, Object> param : theParams.entrySet())
				ret += "\n" + param.getKey() + "=" + param.getValue();
		if(exception != null)
			ret += "\n" + exception;
		return ret;
	}
}
