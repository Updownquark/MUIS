package org.muis.core.parser;

import java.util.ArrayList;

import org.muis.core.*;

/** Creates active MUIS content from parsed structures */
public class MuisContentCreator {
	/**
	 * Fills in a document with widget structure
	 *
	 * @param doc The document to fill
	 * @param content The parsed content to fill the document with
	 * @throws MuisParseException If an unrecoverable error occurs
	 */
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
				elements.add(createFromStructure(doc, doc.getRoot(), (WidgetStructure) child, true));
		}
		doc.getRoot().initChildren(elements.toArray(new MuisElement[elements.size()]));
	}

	/**
	 * Creates an element from a structure and fills its content
	 *
	 * @param doc The document that the element is for
	 * @param parent The parent for the new element
	 * @param structure The widget structure for the element
	 * @param withChildren Whether to also populate the element's descendants
	 * @return The new element
	 * @throws MuisParseException If an unrecoverable error occurs
	 */
	public MuisElement createFromStructure(MuisDocument doc, MuisElement parent, WidgetStructure structure, boolean withChildren)
		throws MuisParseException {
		// Create the element
		MuisElement ret = createElement(doc, parent, structure);
		// Add the attributes
		for(java.util.Map.Entry<String, String> att : structure.getAttributes().entrySet()) {
			try {
				ret.atts().set(att.getKey(), att.getValue());
			} catch(MuisException e) {
				ret.msg().error("Could not set attribute \"" + att.getKey() + "\"", e, "attribute", att.getKey(), "value", att.getValue());
			}
		}
		if(withChildren) {
			// Add the children
			ArrayList<MuisElement> children = new ArrayList<>();
			for(MuisContent childStruct : structure.getChildren()) {
				MuisElement child = getChild(ret, childStruct, true);
				if(child != null)
					children.add(child);
			}
			ret.initChildren(children.toArray(new MuisElement[children.size()]));
		}
		return ret;
	}

	MuisElement createElement(MuisDocument doc, MuisElement parent, WidgetStructure structure) throws MuisParseException {
		String ns = structure.getNamespace();
		if(ns != null && ns.length() == 0)
			ns = null;
		MuisToolkit toolkit = null;
		String className = null;
		if(ns != null) {
			toolkit = structure.getClassView().getToolkit(ns);
			String nsStr = ns == null ? "default namespace" : "namespace " + ns;
			if(toolkit == null)
				throw new MuisParseException("No MUIS toolkit mapped to " + nsStr);
			className = toolkit.getMappedClass(structure.getTagName());
			if(className == null)
				throw new MuisParseException("No tag name " + structure.getTagName() + " mapped for " + nsStr);
		} else {
			for(MuisToolkit tk : structure.getClassView().getScopedToolkits()) {
				className = tk.getMappedClass(structure.getTagName());
				if(className != null) {
					toolkit = tk;
					break;
				}
			}
			if(className == null)
				throw new MuisParseException("No tag name " + structure.getTagName() + " mapped in scoped namespaces");
		}
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

	/**
	 * Creates an element from content
	 *
	 * @param parent The parent of the child to create
	 * @param child The structure of the child
	 * @param withChildren Whether to populate the child's descendants, if any
	 * @return The new element
	 * @throws MuisParseException If an unrecoverable error occurs
	 */
	public MuisElement getChild(MuisElement parent, MuisContent child, boolean withChildren) throws MuisParseException {
		if(child instanceof WidgetStructure)
			return createFromStructure(parent.getDocument(), parent, (WidgetStructure) child, withChildren);
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
