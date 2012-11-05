package org.muis.core.parser;

import java.util.ArrayList;

import org.muis.core.*;

/** Creates active MUIS content from parsed structures */
public class MuisContentCreator {
	public void fillDocument(MuisDocument doc, WidgetStructure content) throws MuisParseException {
		doc.getRoot().init(doc, doc.getEnvironment().getCoreToolkit(), content.getClassView(), null, content.getNamespace(),
			content.getTagName());
		// Add the attributes
		for(java.util.Map.Entry<String, String> att : content.getAttributes().entrySet()) {
			try {
				doc.getRoot().atts().set(att.getKey(), att.getValue());
			} catch(MuisException e) {
				doc.getRoot().msg()
					.error("Could not set attribute \"" + att.getKey() + "\"", e, "attribute", att.getKey(), "value", att.getValue());
			}
		}
		// Add the children
		ArrayList<MuisElement> elements = new ArrayList<>();
		for(MuisContent child : content.getChildren()) {
			if(child instanceof WidgetStructure)
				elements.add(createFromStructure(doc, doc.getRoot(), (WidgetStructure) child));
		}
		doc.getRoot().initChildren(elements.toArray(new MuisElement[elements.size()]));
	}

	public MuisElement createFromStructure(MuisDocument doc, MuisElement parent, WidgetStructure structure) throws MuisParseException {
		// Create the element
		MuisElement ret = createElement(doc, null, structure);
		// Add the attributes
		for(java.util.Map.Entry<String, String> att : structure.getAttributes().entrySet()) {
			try {
				ret.atts().set(att.getKey(), att.getValue());
			} catch(MuisException e) {
				ret.msg().error("Could not set attribute \"" + att.getKey() + "\"", e, "attribute", att.getKey(), "value", att.getValue());
			}
		}
		// Add the children
		ArrayList<MuisElement> children = new ArrayList<>();
		for(MuisContent childStruct : structure.getChildren()) {
			MuisElement child = getChild(ret, childStruct);
			if(child != null)
				children.add(child);
		}
		ret.initChildren(children.toArray(new MuisElement[children.size()]));
		return ret;
	}

	public MuisElement createElement(MuisDocument doc, MuisElement parent, WidgetStructure structure) throws MuisParseException {
		String ns = structure.getNamespace();
		if(ns.length() == 0)
			ns = null;
		MuisToolkit toolkit = structure.getClassView().getToolkit(ns);
		if(toolkit == null && ns == null)
			toolkit = doc.getEnvironment().getCoreToolkit();
		String nsStr = ns == null ? "default namespace" : "namespace " + ns;
		if(toolkit == null)
			throw new MuisParseException("No MUIS toolkit mapped to " + nsStr);
		String className = toolkit.getMappedClass(structure.getTagName());
		if(className == null)
			throw new MuisParseException("No tag name " + structure.getTagName() + " mapped for " + nsStr);
		Class<? extends MuisElement> muisClass;
		try {
			muisClass = toolkit.loadClass(className, MuisElement.class);
		} catch(MuisException e) {
			throw new MuisParseException("Could not load MUIS element class " + className, e);
		}
		MuisElement ret;
		try {
			ret = muisClass.newInstance();
		} catch(Throwable e) {
			throw new MuisParseException("Could not instantiate MUIS element class " + className, e);
		}
		ret.init(doc, toolkit, structure.getClassView(), parent, ns, structure.getTagName());
		return ret;
	}

	public MuisElement getChild(MuisElement parent, MuisContent child) throws MuisParseException {
		if(child instanceof WidgetStructure)
			return createFromStructure(parent.getDocument(), parent, (WidgetStructure) child);
		else if(child instanceof MuisText) {
			MuisText text = (MuisText) child;
			MuisTextElement ret = new MuisTextElement(text.getContent());
			ret.init(parent.getDocument(), parent.getDocument().getEnvironment().getCoreToolkit(), parent.getDocument().getClassView(),
				parent, null, text.isCData() ? "CDATA" : null);
			ret.initChildren(new MuisElement[0]);
			return ret;
		} else {
			throw new MuisParseException("Unrecognized " + MuisContent.class.getName() + " extension: " + child.getClass().getName());
		}
	}
}
