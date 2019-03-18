package org.quick.core;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.quick.core.parser.Version;

public class QuickWidgetSet<D extends QuickDefinedDocument, W extends QuickDefinedWidget<D, ?>> extends QuickLibrary {
	private final QuickToolkit theInterface;
	private final Class<? extends D> theDocumentType;
	private final Map<Class<? extends QuickElement>, Class<? extends W>> theWidgetTypes;

	public QuickWidgetSet(QuickToolkit intf, URL uri, String name, String descrip, Version version, List<URL> cps,
		Map<String, String> classMap, Map<String, String> resMap, Class<? extends D> docType,
		Map<Class<? extends QuickElement>, Class<? extends W>> widgetTypes, List<? extends QuickLibrary> depends,
		List<QuickPermission> perms) {
		super(uri, name, descrip, version, cps, classMap, resMap, depends, perms);
		theInterface = intf;
		theDocumentType = docType;
		theWidgetTypes = Collections.unmodifiableMap(widgetTypes);
	}

	public D createDocument(QuickDocument quickDoc) throws QuickException {
		D doc;
		try {
			doc = theDocumentType.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new QuickException("Unable to instantiate document", e);
		}
		doc.init(quickDoc, this);
		return doc;
	}

	public Class<? extends W> getWidgetType(Class<? extends QuickElement> elementType) {
		Class<? extends W> widgetType = theWidgetTypes.get(elementType);
		if (widgetType != null)
			return widgetType;
		for (QuickLibrary dep : getDependencies()) {
			if (dep instanceof QuickWidgetSet) {
				widgetType = ((QuickWidgetSet<?, W>) dep).getWidgetType(elementType);
				if (widgetType != null)
					return widgetType;
			}
		}
		return null;
	}

	public W createWidget(D document, QuickElement element, W parent) throws QuickException {
		Class<? extends W> widgetType = getWidgetType(element.getClass());
		if (widgetType == null)
			throw new QuickException("No widget mapped to elements of type " + element.getClass());
		W widget;
		try {
			widget = widgetType.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new QuickException("Unable to instantiate widget of type " + widgetType.getName(), e);
		}
		((QuickDefinedWidget<D, QuickElement>) widget).init(document, element, parent);
		return widget;
	}
}
