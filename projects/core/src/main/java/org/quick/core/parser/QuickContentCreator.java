package org.quick.core.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.observe.ObservableValue;
import org.quick.core.QuickDocument;
import org.quick.core.QuickElement;
import org.quick.core.QuickException;
import org.quick.core.QuickHeadSection;
import org.quick.core.QuickParseEnv;
import org.quick.core.QuickTextElement;
import org.quick.core.QuickToolkit;
import org.quick.core.model.DefaultQuickModel;
import org.quick.core.model.QuickAppModel;
import org.quick.core.model.QuickModelConfig;
import org.quick.core.prop.ExpressionContext;
import org.quick.core.style.ImmutableStyleSheet;

/** Creates active Quick content from parsed structures */
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
		for (java.util.Map.Entry<String, String> att : content.getAttributes().entrySet()) {
			try {
				doc.getRoot().atts().set(att.getKey(), att.getValue(), doc);
			} catch (QuickException e) {
				doc.getRoot().msg().error("Could not set attribute \"" + att.getKey() + "\"", e, "attribute", att.getKey(), "value",
					att.getValue());
			}
		}
		// Add the children
		ArrayList<QuickElement> elements = new ArrayList<>();
		for (QuickContent child : content.getChildren()) {
			if (child instanceof WidgetStructure)
				elements.add(createFromStructure(doc, doc.getRoot(), null, (WidgetStructure) child, true));
			else
				elements.add(getChild(doc.getRoot(), null, child, true));
		}
		doc.getRoot().initChildren(elements);
	}

	/**
	 * Creates an element from a structure and fills its content
	 *
	 * @param doc The document that the element is for
	 * @param parent The parent for the new element
	 * @param parentCtx May be null. Allows additional context data to be inserted into the element.
	 * @param structure The widget structure for the element
	 * @param withChildren Whether to also populate the element's descendants
	 * @return The new element
	 * @throws QuickParseException If an unrecoverable error occurs
	 */
	public QuickElement createFromStructure(QuickDocument doc, QuickElement parent, ExpressionContext parentCtx, WidgetStructure structure,
		boolean withChildren)
		throws QuickParseException {
		// Create the element
		QuickElement ret = createElement(doc, parent, structure);
		// Add the attributes
		QuickParseEnv parseEnv;
		if (parent != null) {
			if (parentCtx == null)
				parseEnv = parent;
			else
				parseEnv = new SimpleParseEnv(parent.cv(), parent.msg(), parentCtx);
		} else {
			if (parentCtx == null)
				parseEnv = doc;
			else
				parseEnv = new SimpleParseEnv(doc.cv(), doc.msg(), parentCtx);
		}
		for (java.util.Map.Entry<String, String> att : structure.getAttributes().entrySet()) {
			try {
				ret.atts().set(att.getKey(), att.getValue(), parseEnv);
			} catch (QuickException e) {
				ret.msg().error("Could not set attribute \"" + att.getKey() + "\"", e, "attribute", att.getKey(), "value", att.getValue());
			}
		}
		if (withChildren) {
			// Add the children
			ArrayList<QuickElement> children = new ArrayList<>();
			for (QuickContent childStruct : structure.getChildren()) {
				QuickElement child = getChild(ret, parentCtx, childStruct, true);
				if (child != null)
					children.add(child);
			}
			ret.initChildren(children);
		}
		return ret;
	}

	QuickElement createElement(QuickDocument doc, QuickElement parent, WidgetStructure structure)
		throws QuickParseException {
		String ns = structure.getNamespace();
		if (ns != null && ns.length() == 0)
			ns = null;
		QuickToolkit toolkit = null;
		String className = null;
		if (ns != null) {
			toolkit = structure.getClassView().getToolkit(ns);
			String nsStr = ns == null ? "default namespace" : "namespace " + ns;
			if (toolkit == null)
				throw new QuickParseException("No Quick toolkit mapped to " + nsStr);
			className = toolkit.getMappedClass(structure.getTagName());
			if (className == null)
				throw new QuickParseException("No tag name " + structure.getTagName() + " mapped for " + nsStr);
		} else {
			for (QuickToolkit tk : structure.getClassView().getScopedToolkits()) {
				className = tk.getMappedClass(structure.getTagName());
				if (className != null) {
					toolkit = tk;
					break;
				}
			}
			if (className == null)
				throw new QuickParseException("No tag name " + structure.getTagName() + " mapped in scoped namespaces");
		}
		Class<? extends QuickElement> quickClass;
		try {
			quickClass = toolkit.loadClass(className, QuickElement.class);
		} catch (QuickException e) {
			throw new QuickParseException("Could not load Quick element class " + className, e);
		}
		QuickElement ret;
		try {
			ret = quickClass.newInstance();
		} catch (Throwable e) {
			throw new QuickParseException("Could not instantiate Quick element class " + className, e);
		}
		ret.init(doc, toolkit, structure.getClassView(), parent, ns, structure.getTagName());
		return ret;
	}

	/**
	 * Creates an element from content
	 *
	 * @param parent The parent of the child to create
	 * @param parentCtx May be null. Allows additional context data to be inserted into the element.
	 * @param child The structure of the child
	 * @param withChildren Whether to populate the child's descendants, if any
	 * @return The new element
	 * @throws QuickParseException If an unrecoverable error occurs
	 */
	public QuickElement getChild(QuickElement parent, ExpressionContext parentCtx, QuickContent child, boolean withChildren)
		throws QuickParseException {
		if (child instanceof WidgetStructure)
			return createFromStructure(parent.getDocument(), parent, parentCtx, (WidgetStructure) child, withChildren);
		else if (child instanceof QuickText) {
			QuickText text = (QuickText) child;
			QuickTextElement ret = new QuickTextElement(text.getContent());
			ret.init(parent.getDocument(), parent.getDocument().getEnvironment().getCoreToolkit(), parent.getDocument().getClassView(),
				parent, null, text.isCData() ? "CDATA" : null);
			ret.initChildren(Collections.emptyList());
			return ret;
		} else {
			throw new QuickParseException("Unrecognized " + QuickContent.class.getName() + " extension: " + child.getClass().getName());
		}
	}

	/**
	 * Creates a document head section from its structure
	 *
	 * @param structure The structure representing the head section
	 * @param parser The property parser for parsing model values
	 * @param parseEnv The parse environment for parsing model values
	 * @return The new head section
	 * @throws QuickParseException If an error occurs parsing the head section
	 */
	public QuickHeadSection createHeadFromStructure(QuickHeadStructure structure, QuickPropertyParser parser, QuickParseEnv parseEnv)
		throws QuickParseException {
		QuickHeadSection.Builder builder = QuickHeadSection.build();
		builder.setTitle(structure.getTitle());
		for (ImmutableStyleSheet ss : structure.getStyleSheets())
			builder.addStyleSheet(ss);
		Map<String, QuickAppModel> models = new HashMap<>();
		QuickParseEnv modelEnv = new SimpleParseEnv(parseEnv.cv(), parseEnv.msg(), org.quick.core.prop.DefaultExpressionContext.build()//
			.withParent(parseEnv.getContext()).withValueGetter(name -> {
				if (models.containsKey(name))
					return ObservableValue.of(models.get(name));
				else
					return null;
			}).build());
		for (Map.Entry<String, QuickModelConfig> mc : structure.getModelConfigs().entrySet()) {
			QuickAppModel model = DefaultQuickModel.buildQuickModel(mc.getValue().getString("name"), mc.getValue().without("name"), parser,
				modelEnv);
			models.put(mc.getKey(), model);
			builder.addModel(mc.getKey(), model);
		}
		return builder.build();
	}
}
