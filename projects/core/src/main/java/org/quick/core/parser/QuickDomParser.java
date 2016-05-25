package org.quick.core.parser;

import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;

import org.jdom2.CDATA;
import org.jdom2.Element;
import org.jdom2.Text;
import org.observe.SimpleSettableValue;
import org.quick.core.*;
import org.quick.core.mgr.QuickMessageCenter;
import org.quick.core.model.DefaultQuickModel;
import org.quick.core.model.QuickAppModel;
import org.quick.core.model.QuickValueReferenceParser;
import org.quick.core.style.sheet.ParsedStyleSheet;
import org.quick.util.QuickUtils;

import prisms.lang.Type;

/** Parses Quick components using the JDOM library */
public class QuickDomParser implements QuickDocumentParser {
	private QuickEnvironment theEnvironment;

	private CustomStyleSheetParser theStyleSheetParser;

	/** @param env The environment for the parser to operate in */
	public QuickDomParser(QuickEnvironment env) {
		theEnvironment = env;
		theStyleSheetParser = new CustomStyleSheetParser();
	}

	@Override
	public QuickEnvironment getEnvironment() {
		return theEnvironment;
	}

	@Override
	public QuickDocumentStructure parseDocument(URL location, Reader reader) throws QuickParseException, IOException {
		return parseDocument(location, reader, null, theEnvironment.msg());
	}

	@Override
	public QuickDocumentStructure parseDocument(URL location, Reader reader, QuickClassView rootClassView, QuickMessageCenter msg)
		throws IOException, QuickParseException {
		Element rootEl;
		try {
			rootEl = new org.jdom2.input.SAXBuilder().build(reader).getRootElement();
		} catch(org.jdom2.JDOMException e) {
			throw new QuickParseException("Could not parse document XML", e);
		}

		QuickHeadSection head = new QuickHeadSection();
		QuickClassView classView = getClassView(rootClassView, rootEl, msg, location);
		if(rootEl.getTextTrim().length() > 0)
			msg.warn("Text found under root element: " + rootEl.getTextTrim());
		Element [] headEl = rootEl.getChildren("head").toArray(new Element[0]);
		if(headEl.length > 1)
			msg.error("Multiple head elements in document XML");
		if(headEl.length > 0) {
			if(headEl[0].getTextTrim().length() > 0)
				msg.warn("Text found in head section: " + headEl[0].getTextTrim());
			String title = headEl[0].getChildTextTrim("title");
			if(title != null)
				head.setTitle(title);
			for(Element styleSheetEl : headEl[0].getChildren("style-sheet")) {
				ParsedStyleSheet styleSheet = parseStyleSheet(styleSheetEl, location, classView, msg);
				if(styleSheet != null) {
					head.getStyleSheets().add(styleSheet);
					styleSheet.seal();
				}
			}
			for(Element modelEl : headEl[0].getChildren("model")) {
				String name = modelEl.getAttributeValue("name");
				if(name == null) {
					msg.error("No name specified for model", "element", modelEl);
					continue;
				}
				if(head.getModel(name) != null) {
					msg.error("Model \"" + name + "\" specified multiple times", "element", modelEl);
					continue;
				}
				QuickAppModel model = parseModel(modelEl, name, classView, msg);
				if(model != null)
					head.addModel(name, model);
			}
		}
		head.seal();
		Element [] body = rootEl.getChildren("body").toArray(new Element[0]);
		if(body.length > 1)
			msg.error("Multiple body elements in document XML");
		if(body.length == 0)
			throw new QuickParseException("No body in document XML");
		for(Element el : rootEl.getChildren()) {
			if(el.getName().equals("head") || el.getName().equals("body"))
				continue;
			msg.error("Extra element " + el.getName() + " in document XML");
		}
		WidgetStructure content = parseContent(new WidgetStructure(null, classView, rootEl.getNamespacePrefix(), rootEl.getName()),
			classView, body[0], msg, location);
		return new QuickDocumentStructure(location, head, content);
	}

	/**
	 * Creates a fully-initialized class view
	 *
	 * @param parent The parent class view
	 * @param xml The XML element to get namespaces to map
	 * @param msg The message center to report errors to
	 * @param location The location of the XML file
	 * @return The class view for the element
	 */
	protected QuickClassView getClassView(QuickClassView parent, Element xml, QuickMessageCenter msg, URL location) {
		QuickClassView ret = new QuickClassView(theEnvironment, parent, parent == null ? null : parent.getToolkitForQName(xml
			.getQualifiedName()));
		for(org.jdom2.Namespace ns : xml.getNamespacesIntroduced()) {
			QuickToolkit toolkit;
			try {
				toolkit = theEnvironment.getToolkit(QuickUtils.resolveURL(location, ns.getURI()));
			} catch(MalformedURLException e) {
				msg.error("Invalid URL \"" + ns.getURI() + "\" for toolkit at namespace " + ns.getPrefix(), e);
				continue;
			} catch(IOException e) {
				msg.error("Could not read toolkit " + ns.getPrefix() + ":" + ns.getURI(), e);
				continue;
			} catch(QuickParseException e) {
				msg.error("Could not parse toolkit " + ns.getPrefix() + ":" + ns.getURI(), e);
				continue;
			} catch(QuickException e) {
				msg.error("Could not resolve location of toolkit for namespace " + ns.getPrefix(), e);
				continue;
			}
			try {
				ret.addNamespace(ns.getPrefix(), toolkit);
			} catch(QuickException e) {
				msg.error("Could not add namespace", e);
			}
		}
		ret.seal();
		return ret;
	}

	/**
	 * Parses a style sheet from a style-sheet element
	 *
	 * @param styleSheetEl The element specifying the style sheet to parse
	 * @param reference The location of the file that the style sheet is referenced from
	 * @param classView The classView to
	 * @param msg The message center to report errors
	 * @return The parsed style sheet, or null if an error occurred (the error must be documented in the document before returning null)
	 */
	protected ParsedStyleSheet parseStyleSheet(Element styleSheetEl, URL reference, QuickClassView classView, QuickMessageCenter msg) {
		String ssLocStr = styleSheetEl.getAttributeValue("ref");
		if(ssLocStr != null) {
			for(org.jdom2.Content content : styleSheetEl.getContent()) {
				if(content instanceof org.jdom2.Comment)
					continue;
				else if(content instanceof Text) {
					if(((Text) content).getTextTrim().length() > 0) {
						msg.error(styleSheetEl.getName() + " elements that refer to a style sheet resource may not have text", "element",
							styleSheetEl);
						return null;
					}
				} else {
					msg.error(styleSheetEl.getName() + " elements that refer to a style sheet resoruce may not have any entities",
						"element", styleSheetEl);
					return null;
				}
			}
			URL ssLoc;
			try {
				ssLoc = QuickUtils.resolveURL(reference, ssLocStr);
			} catch(QuickException e) {
				msg.error("Could not resolve style sheet location " + ssLocStr, e, "element", styleSheetEl);
				return null;
			}
			try {
				ParsedStyleSheet ret = theStyleSheetParser.parse(new SimpleParseEnv(theEnvironment, classView, msg), ssLoc, theEnvironment);
				// TODO It might be better to not call this until the entire document is parsed and ready to render--maybe add this to the
				// EventQueue somehow
				ret.startAnimation();
				return ret;
			} catch(Exception e) {
				msg.error("Could not read or parse style sheet at " + ssLocStr, e, "element", styleSheetEl);
				return null;
			}
		}
		Element temp = styleSheetEl;
		while(temp != null) {
			for(org.jdom2.Namespace ns : temp.getNamespacesIntroduced()) {
				if(classView.getToolkit(ns.getPrefix()) != null || ns.getURI().length() == 0)
					continue;
				try {
					classView.addNamespace(ns.getPrefix(), theEnvironment.getToolkit(QuickUtils.resolveURL(reference, ns.getURI())));
				} catch(QuickException | IOException e) {
					msg.error("Toolkit URI \"" + ns.getURI() + "\" at namespace \"" + ns.getPrefix()
						+ "\" could not be resolved or parsed for style-sheet", e, "element", styleSheetEl);
					continue;
				}
			}
			temp = temp.getParentElement();
		}
		msg.error("No ref specified on style sheet element");
		return null;
		/*try {
			ParsedStyleSheet ret = theStyleSheetParser.parse(styleSheetEl, classView, msg);
			ret.startAnimation();
			ret.seal();
			return ret;
		} catch(QuickParseException e) {
			msg.error("Could not read or parse inline style sheet", e, "element", styleSheetEl);
			return null;
		}*/
	}

	/**
	 * Parses a model from XML
	 *
	 * @param modelEl The XML element defining or referring to the model
	 * @param name The name of the model (for informative messaging)
	 * @param classView The class view that the model is defined under
	 * @param msg The messaging center to relay any relevant messages to about parsing
	 * @return The parsed model, or null if the model could not be parsed (errors go to the messaging center)
	 */
	protected QuickAppModel parseModel(Element modelEl, String name, QuickClassView classView, QuickMessageCenter msg) {
		String classAtt = modelEl.getAttributeValue("class");
		if(classAtt != null) {
			Class<?> modelType = null;
			for(QuickToolkit tk : classView.getScopedToolkits()) {
				try {
					modelType = tk.loadClass(classAtt);
				} catch(ClassNotFoundException e) {
				}
				if(modelType != null)
					break;
			}
			if(modelType == null) {
				msg.error("No such class \"" + classAtt + "\" found for model \"" + name + "\"", "element", modelEl);
				return null;
			}
			final Object modelObj;
			try {
				modelObj = modelType.newInstance();
			} catch(InstantiationException | IllegalAccessException e) {
				msg.error("Could not instantiate model \"" + name + "\", type \"" + modelType.getName() + "\"", e, "element", modelEl);
				return null;
			}
			if(!(modelObj instanceof QuickAppModel))
				return new org.quick.core.model.QuickWrappingModel(new org.quick.core.model.Getter<Object>() {
					@Override
					public Class<Object> getType() {
						return (Class<Object>) modelObj.getClass();
					}

					@Override
					public Object get() throws IllegalStateException {
						return modelObj;
					}
				}, msg);
			else
				return (QuickAppModel) modelObj;
		} else {
			DefaultQuickModel model = new DefaultQuickModel();
			for(Element el : modelEl.getChildren()) {
				String subName = el.getAttributeValue("name");
				switch (el.getName()) {
				case "model":
					QuickAppModel subModel = parseModel(el, name + "." + subName, classView, msg);
					if(subModel != null)
						model.subModels().put(subName, subModel);
					break;
				case "value":
					String typeAtt = el.getAttributeValue("type");
					if(typeAtt == null) {
						msg.error("No type specified for value " + name + "." + subName, "element", modelEl);
						continue;
					}
					Class<?> type = parseType(typeAtt, classView, msg, name + "." + subName, modelEl);
					if(type == null)
						continue;
					SimpleSettableValue<?> value = new SimpleSettableValue<>(new Type(type), true);
					((SimpleSettableValue<Object>) value).set(getDefaultValue(type), null);
					model.values().put(subName, value);
					break;
				default:
					msg.error("Unrecognized model element child: " + el.getName(), "element", modelEl);
				}
			}
			model.seal();
			return model;
		}
	}

	private Class<?> parseType(String typeName, QuickClassView classView, QuickMessageCenter msg, String modelName, Element modelEl) {
		if(typeName.equals("char"))
			return Character.class;
		else if(typeName.equals("boolean"))
			return Boolean.class;
		else if(typeName.equals("int"))
			return Long.class;
		else if(typeName.equals("float"))
			return Double.class;
		else if(typeName.equals("string"))
			return String.class;
		Class<?> ret = null;
		for(QuickToolkit tk : classView.getScopedToolkits()) {
			try {
				ret = tk.loadClass(typeName);
			} catch(ClassNotFoundException e) {
			}
			if(ret != null)
				break;
		}
		if(ret == null) {
			msg.error("No such type \"" + typeName + "\" found for model value \"" + modelName + "\"", "element", modelEl);
			return null;
		}
		return ret;
	}

	private Object getDefaultValue(Class<?> type) {
		if(type == Boolean.class)
			return Boolean.FALSE;
		else if(type == Character.class)
			return Character.valueOf(' ');
		else if(type == Byte.class)
			return Byte.valueOf((byte) 0);
		else if(type == Short.class)
			return Short.valueOf((short) 0);
		else if(type == Integer.class)
			return Integer.valueOf(0);
		else if(type == Long.class)
			return Long.valueOf(0);
		else if(type == Float.class)
			return Float.valueOf(0);
		else if(type == Double.class)
			return Double.valueOf(0);
		else if(Enum.class.isAssignableFrom(type) && type.getEnumConstants().length > 0)
			return type.getEnumConstants()[0];
		return null;
	}

	/**
	 * @param parent The structure parent
	 * @param xml The XML to parse widget structures from
	 * @param rootClassView The class view for the root of the structure--may be null
	 * @param msg The message center to report errors to
	 * @param location The location of the XML file
	 * @return The MUIS-formatted structure of the given element
	 */
	protected WidgetStructure parseContent(WidgetStructure parent, QuickClassView rootClassView, Element xml, QuickMessageCenter msg,
		URL location) {
		String ns = xml.getNamespacePrefix();
		if(ns.length() == 0)
			ns = null;
		QuickClassView classView = getClassView(parent == null ? rootClassView : parent.getClassView(), xml, msg, location);
		WidgetStructure ret = new WidgetStructure(parent, classView, ns, xml.getName());

		for(org.jdom2.Attribute att : xml.getAttributes())
			ret.addAttribute(att.getName(), att.getValue());

		for(org.jdom2.Content content : xml.getContent())
			if(content instanceof Element) {
				ret.addChild(parseContent(ret, null, (Element) content, msg, location));
			} else if(content instanceof Text || content instanceof CDATA) {
				String text;
				if(content instanceof Text)
					text = ((Text) content).getTextNormalize();
				else
					text = ((CDATA) content).getTextTrim().replaceAll("\r", "");
				if(text.length() == 0)
					continue;
				ret.addChild(new QuickText(ret, text, content instanceof CDATA));
			}
		return ret;
	}

	private static class SimpleParseEnv implements QuickParseEnv {
		private final QuickClassView theClassView;
		private final QuickMessageCenter theMsg;
		private final QuickValueReferenceParser theModelParser;

		SimpleParseEnv(QuickEnvironment environment, QuickClassView classView, QuickMessageCenter msg) {
			theClassView = new QuickClassView(environment, classView, null);
			theMsg = msg;
			theModelParser = new DefaultModelValueReferenceParser(environment.getValueParser(), theClassView);
		}

		@Override
		public QuickClassView cv() {
			return theClassView;
		}

		@Override
		public QuickMessageCenter msg() {
			return theMsg;
		}

		@Override
		public QuickValueReferenceParser getValueParser() {
			return theModelParser;
		}
	}
}
