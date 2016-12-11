package org.quick.core.parser;

import java.util.List;

import org.quick.core.QuickClassView;

/** Represents the structure of a widget as defined in XML */
public class WidgetStructure extends QuickContent implements org.qommons.Sealable {
	private final String theNamespace;

	private final String theTagName;

	private final QuickClassView theClassView;

	private List<QuickContent> theChildren;

	/**
	 * @param parent This structure's parent
	 * @param classView The class view for this widget
	 * @param ns The namespace for this widget
	 * @param tag The tag name for this widget
	 */
	public WidgetStructure(WidgetStructure parent, QuickClassView classView, String ns, String tag) {
		super(parent);
		theNamespace = ns;
		theTagName = tag;
		theClassView = classView;
		theChildren = new java.util.ArrayList<>();
	}

	/** @return The namespace for this widget */
	public String getNamespace() {
		return theNamespace;
	}

	/** @return The tag name for this widget */
	public String getTagName() {
		return theTagName;
	}

	/** @return The class view for this widget */
	public QuickClassView getClassView() {
		return theClassView;
	}

	/** @return This widget's children */
	public List<QuickContent> getChildren() {
		return theChildren;
	}

	/** @param widget The content to add as a child of this widget */
	public void addChild(QuickContent widget) {
		if (isSealed())
			throw new SealedException(this);
		theChildren.add(widget);
	}

	@Override
	public void seal() {
		super.seal();
		theChildren = java.util.Collections.unmodifiableList(theChildren);
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		ret.append('<');
		if(theNamespace != null)
			ret.append(theNamespace).append(':');
		ret.append(theTagName);
		ret.append(attrString());
		ret.append('>');
		return ret.toString();
	}
}
