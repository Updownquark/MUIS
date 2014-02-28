package org.muis.core.parser;

import java.util.List;
import java.util.Map;

import org.muis.core.MuisClassView;
import org.muis.core.MuisEnvironment;

/** Represents the structure of a widget as defined in XML */
public class WidgetStructure extends MuisContent implements prisms.util.Sealable {
	private final String theNamespace;

	private final String theTagName;

	private final MuisClassView theClassView;

	private Map<String, String> theAttributes;

	private List<MuisContent> theChildren;

	private boolean isSealed;

	/**
	 * @param parent This structure's parent
	 * @param env The environment this structure was parsed in
	 * @param classView The class view for this widget
	 * @param ns The namespace for this widget
	 * @param tag The tag name for this widget
	 */
	public WidgetStructure(WidgetStructure parent, MuisEnvironment env, MuisClassView classView, String ns, String tag) {
		super(parent);
		theNamespace = ns;
		theTagName = tag;
		theClassView = classView;
		theAttributes = new java.util.LinkedHashMap<>();
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
	public MuisClassView getClassView() {
		return theClassView;
	}

	/** @return The attribute map for this widget */
	public Map<String, String> getAttributes() {
		return theAttributes;
	}

	/** @return This widget's children */
	public List<MuisContent> getChildren() {
		return theChildren;
	}

	/**
	 * @param attName The name of the attribute to set in this widget
	 * @param attValue The value to set for the attribute
	 */
	public void addAttribute(String attName, String attValue) {
		if(isSealed)
			throw new SealedException(this);
		theAttributes.put(attName, attValue);
	}

	/** @param widget The content to add as a child of this widget */
	public void addChild(MuisContent widget) {
		if(isSealed)
			throw new SealedException(this);
		theChildren.add(widget);
	}

	@Override
	public boolean isSealed() {
		return isSealed;
	}

	@Override
	public void seal() {
		isSealed = true;
		theAttributes = java.util.Collections.unmodifiableMap(theAttributes);
		theChildren = java.util.Collections.unmodifiableList(theChildren);
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		ret.append('<');
		if(theNamespace != null)
			ret.append(theNamespace).append(':');
		ret.append(theTagName);
		for(Map.Entry<String, String> attr : theAttributes.entrySet())
			ret.append(' ').append(attr.getKey()).append("=\"").append(attr.getValue()).append('"');
		ret.append('>');
		return ret.toString();
	}
}
