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

	public WidgetStructure(WidgetStructure parent, MuisEnvironment env, MuisClassView classView, String ns, String tag) {
		super(parent);
		theNamespace = ns;
		theTagName = tag;
		theClassView = classView;
		theAttributes = new java.util.LinkedHashMap<>();
		theChildren = new java.util.ArrayList<>();
	}

	public String getNamespace() {
		return theNamespace;
	}

	public String getTagName() {
		return theTagName;
	}

	public MuisClassView getClassView() {
		return theClassView;
	}

	public Map<String, String> getAttributes() {
		return theAttributes;
	}

	public List<MuisContent> getChildren() {
		return theChildren;
	}

	public void addAttribute(String attName, String attValue) {
		if(isSealed)
			throw new SealedException(this);
		theAttributes.put(attName, attValue);
	}

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
}
