package org.quick.core;

import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.quick.core.parser.Version;

public class QuickWidgetSet<D extends QuickDefinedDocument<?>, W extends QuickDefinedWidget<D, ?>> extends QuickLibrary {
	private final QuickToolkit theInterface;
	private Class<? extends D> theDocumentType;
	private final Map<Class<? extends QuickElement>, Class<? extends W>> theWidgetTypes;

	public QuickWidgetSet(QuickToolkit intf, URL uri, String name, String descrip, Version version, List<URL> cps,
		Map<String, String> classMap, Map<String, String> resMap, Map<Class<? extends QuickElement>, Class<? extends W>> widgetTypes,
		List<? extends QuickLibrary> depends, List<QuickPermission> perms) {
		super(uri, name, descrip, version, cps, classMap, resMap, depends, perms);
		theInterface = intf;
		theWidgetTypes = Collections.unmodifiableMap(widgetTypes);
	}

	private void setDocumentType(Class<? extends D> docType) {
		if (theDocumentType != null)
			throw new IllegalStateException("Document type is already set");
		theDocumentType = docType;
	}

	public QuickToolkit getInterface() {
		return theInterface;
	}

	public Class<? extends D> getDocumentType() {
		return theDocumentType;
	}

	public D createDocument(QuickDocument quickDoc) throws QuickException {
		D doc;
		try {
			doc = theDocumentType.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new QuickException("Unable to instantiate document", e);
		}
		((QuickDefinedDocument<W>) doc).init(quickDoc, this);
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

	public static <D extends QuickDefinedDocument<?>, W extends QuickDefinedWidget<D, ?>> Builder<D, W> build(URL location,
		QuickToolkit intf) {
		return new Builder<>(location, intf);
	}

	public static class Builder<D extends QuickDefinedDocument<?>, W extends QuickDefinedWidget<D, ?>>
		extends QuickLibrary.Builder<QuickWidgetSet<D, W>, Builder<D, W>> {
		private final QuickToolkit theInterface;
		private final Map<Class<? extends QuickElement>, Class<? extends W>> theWidgetTypes;

		public Builder(URL location, QuickToolkit intf) {
			super(location);
			theInterface = intf;
			theWidgetTypes = new LinkedHashMap<>();
		}

		@Override
		public String getLibraryType() {
			return "widget set";
		}

		public Builder<D, W> withDocType(Class<? extends D> docType) {
			getBuilt().setDocumentType(docType);
			return this;
		}

		public Builder<D, W> withWidget(Class<? extends QuickElement> elementType, Class<? extends W> widgetType) {
			theWidgetTypes.put(elementType, widgetType);
			return this;
		}

		@Override
		public QuickWidgetSet<D, W> createLibrary(URL location, String name, String description, Version version, List<URL> classPaths,
			Map<String, String> classMappings, Map<String, String> resourceLocations, List<QuickLibrary> dependencies,
			List<QuickPermission> permissions) {
			return new QuickWidgetSet<>(theInterface, location, name, description, version, classPaths, classMappings, resourceLocations,
				theWidgetTypes, dependencies, permissions);
		}
	}
}
