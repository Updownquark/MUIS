package org.quick.core.parser;

import java.util.ArrayList;

import org.quick.core.*;

/** Creates active MUIS content from parsed structures */
public class QuickContentCreator {
	/**
	 * Fills in a document with widget structure
	 *
	 * @param doc The document to fill
	 * @param content The parsed content to fill the document with
	 * @throws QuickParseException If an unrecoverable error occurs
	 */
	public void fillDocument(QuickDocument doc, WidgetStructure content) throws QuickParseException {
		doc.getRoot().init(doc, doc.getEnvironment().getCoreToolkit(), content.getClassView(), null, content.getNamespace(),
			content.getTagName());
		// Add the attributes
		for(java.util.Map.Entry<String, String> att : content.getAttributes().entrySet()) {
			try {
				doc.getRoot().atts().set(att.getKey(), att.getValue(), doc);
			} catch(QuickException e) {
				doc.getRoot().msg()
					.error("Could not set attribute \"" + att.getKey() + "\"", e, "attribute", att.getKey(), "value", att.getValue());
			}
		}
		// Add the children
		ArrayList<QuickElement> elements = new ArrayList<>();
		for(QuickContent child : content.getChildren()) {
			if(child instanceof WidgetStructure)
				elements.add(createFromStructure(doc, doc.getRoot(), (WidgetStructure) child, true));
		}
		doc.getRoot().initChildren(elements.toArray(new QuickElement[elements.size()]));
	}

	/**
	 * Creates an element from a structure and fills its content
	 *
	 * @param doc The document that the element is for
	 * @param parent The parent for the new element
	 * @param structure The widget structure for the element
	 * @param withChildren Whether to also populate the element's descendants
	 * @return The new element
	 * @throws QuickParseException If an unrecoverable error occurs
	 */
	public QuickElement createFromStructure(QuickDocument doc, QuickElement parent, WidgetStructure structure, boolean withChildren)
		throws QuickParseException {
		// Create the element
		QuickElement ret = createElement(doc, parent, structure);
		// Add the attributes
		for(java.util.Map.Entry<String, String> att : structure.getAttributes().entrySet()) {
			try {
				ret.atts().set(att.getKey(), att.getValue(), parent);
			} catch(QuickException e) {
				ret.msg().error("Could not set attribute \"" + att.getKey() + "\"", e, "attribute", att.getKey(), "value", att.getValue());
			}
		}
		if(withChildren) {
			// Add the children
			ArrayList<QuickElement> children = new ArrayList<>();
			for(QuickContent childStruct : structure.getChildren()) {
				QuickElement child = getChild(ret, childStruct, true);
				if(child != null)
					children.add(child);
			}
			ret.initChildren(children.toArray(new QuickElement[children.size()]));
		}
		return ret;
	}

	QuickElement createElement(QuickDocument doc, QuickElement parent, WidgetStructure structure) throws QuickParseException {
		String ns = structure.getNamespace();
		if(ns != null && ns.length() == 0)
			ns = null;
		QuickToolkit toolkit = null;
		String className = null;
		if(ns != null) {
			toolkit = structure.getClassView().getToolkit(ns);
			String nsStr = ns == null ? "default namespace" : "namespace " + ns;
			if(toolkit == null)
				throw new QuickParseException("No MUIS toolkit mapped to " + nsStr);
			className = toolkit.getMappedClass(structure.getTagName());
			if(className == null)
				throw new QuickParseException("No tag name " + structure.getTagName() + " mapped for " + nsStr);
		} else {
			for(QuickToolkit tk : structure.getClassView().getScopedToolkits()) {
				className = tk.getMappedClass(structure.getTagName());
				if(className != null) {
					toolkit = tk;
					break;
				}
			}
			if(className == null)
				throw new QuickParseException("No tag name " + structure.getTagName() + " mapped in scoped namespaces");
		}
		Class<? extends QuickElement> quickClass;
		try {
			quickClass = toolkit.loadClass(className, QuickElement.class);
		} catch(QuickException e) {
			throw new QuickParseException("Could not load MUIS element class " + className, e);
		}
		QuickElement ret;
		try {
			ret = quickClass.newInstance();
		} catch(Throwable e) {
			throw new QuickParseException("Could not instantiate MUIS element class " + className, e);
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
	 * @throws QuickParseException If an unrecoverable error occurs
	 */
	public QuickElement getChild(QuickElement parent, QuickContent child, boolean withChildren) throws QuickParseException {
		if(child instanceof WidgetStructure)
			return createFromStructure(parent.getDocument(), parent, (WidgetStructure) child, withChildren);
		else if(child instanceof QuickText) {
			QuickText text = (QuickText) child;
			QuickTextElement ret = new QuickTextElement(text.getContent());
			ret.init(parent.getDocument(), parent.getDocument().getEnvironment().getCoreToolkit(), parent.getDocument().getClassView(),
				parent, null, text.isCData() ? "CDATA" : null);
			ret.initChildren(new QuickElement[0]);
			return ret;
		} else {
			throw new QuickParseException("Unrecognized " + QuickContent.class.getName() + " extension: " + child.getClass().getName());
		}
	}
}
