package org.quick.core;

import org.observe.collect.ObservableList;
import org.quick.core.mgr.QuickMessageCenter;
import org.quick.core.parser.*;
import org.quick.core.style.sheet.StyleSheet;

import com.google.common.reflect.TypeToken;

/** The environment that MUIS documents operate in */
public class QuickEnvironment implements QuickParseEnv {
	/** The location of the core toolkit */
	public static final java.net.URL CORE_TOOLKIT = QuickEnvironment.class.getResource("/QuickRegistry.xml");

	private static class EnvironmentStyle extends org.quick.core.style.sheet.AbstractStyleSheet {
		EnvironmentStyle(ObservableList<StyleSheet> dependencies) {
			super(dependencies);
		}

		@Override
		public String toString() {
			return "Quick Env Style";
		}
	}

	private QuickToolkitParser theToolkitParser;

	private QuickDocumentParser theDocumentParser;

	private QuickContentCreator theContentCreator;

	private QuickStyleParser theStyleParser;

	private QuickAttributeParser theAttributeParser;

	private final QuickMessageCenter theMessageCenter;
	private final java.util.Map<String, QuickToolkit> theToolkits;
	private final QuickCache theCache;
	private final EnvironmentStyle theStyle;

	private final Object theToolkitLock;
	private ObservableList<StyleSheet> theStyleDependencyController;

	private QuickEnvironment() {
		theMessageCenter = new QuickMessageCenter(this, null, null);
		theToolkits = new java.util.concurrent.ConcurrentHashMap<>();
		theCache = new QuickCache();
		theStyleDependencyController = new org.observe.collect.impl.ObservableArrayList<>(TypeToken.of(StyleSheet.class));
		ObservableList<StyleSheet> styleDepends = theStyleDependencyController.immutable();
		theStyle = new EnvironmentStyle(styleDepends);
		theToolkitLock = new Object();
	}

	public void setToolkitParser(QuickToolkitParser toolkitParser) {
		theToolkitParser = toolkitParser;
	}

	private void setDocumentParser(QuickDocumentParser documentParser) {
		theDocumentParser = documentParser;
	}

	private void setContentCreator(QuickContentCreator contentCreator) {
		theContentCreator = contentCreator;
	}

	private void setStyleParser(QuickStyleParser styleParser) {
		theStyleParser = styleParser;
	}

	private void setAttributeParser(QuickAttributeParser attributeParser) {
		theAttributeParser = attributeParser;
	}

	@Override
	public QuickClassView cv() {
		QuickClassView ret = new QuickClassView(this, null, getCoreToolkit());
		ret.seal();
		return ret;
	}

	/** @return The document parser for the environment */
	public QuickDocumentParser getDocumentParser() {
		return theDocumentParser;
	}


	/** @return The content creator for the environment */
	public QuickContentCreator getContentCreator() {
		return theContentCreator;
	}

	public QuickStyleParser getStyleParser() {
		return theStyleParser;
	}

	public QuickAttributeParser getAttributeParser() {
		return theAttributeParser;
	}

	/** @return The message center for this environment */
	public QuickMessageCenter getMessageCenter() {
		return theMessageCenter;
	}

	/**
	 * @return The message center for this environment
	 * @see #getMessageCenter()
	 */
	@Override
	public QuickMessageCenter msg() {
		return getMessageCenter();
	}

	/** @return The resource cache for this environment */
	public QuickCache getCache() {
		return theCache;
	}

	/** @return The sum of all toolkit styles in this environment */
	public StyleSheet getStyle() {
		return theStyle;
	}

	/** @return All toolkits in this environment */
	public Iterable<QuickToolkit> getToolkits() {
		return org.qommons.ArrayUtils.immutableIterable(theToolkits.values());
	}

	/**
	 * @param location The location of the toolkit to get
	 * @return The toolkit at the given location
	 * @throws java.io.IOException If the toolkit resources cannot be retrieved
	 * @throws org.quick.core.parser.QuickParseException If the toolkit cannot be parsed
	 */
	public QuickToolkit getToolkit(java.net.URL location) throws java.io.IOException, org.quick.core.parser.QuickParseException {
		// Need to make sure the core toolkit is loaded first
		if(CORE_TOOLKIT != null && !CORE_TOOLKIT.equals(location))
			getCoreToolkit();
		QuickToolkit ret = theToolkits.get(location.toString());
		if(ret != null)
			return ret;
		synchronized(theToolkitLock) {
			ret = theToolkits.get(location.toString());
			if(ret != null)
				return ret;
			ret = new QuickToolkit(this, location);
			theDocumentParser.fillToolkit(ret);
			theStyleDependencyController.add(ret.getStyle());
			theToolkits.put(location.toString(), ret);
			theDocumentParser.fillToolkitStyles(ret);
			ret.seal();
		}
		return ret;
	}

	/** @return The toolkit containing the core MUIS classes */
	public QuickToolkit getCoreToolkit() {
		if(CORE_TOOLKIT == null)
			throw new IllegalStateException("No such resource " + CORE_TOOLKIT + " for core toolkit");
		try {
			return getToolkit(CORE_TOOLKIT);
		} catch(java.io.IOException e) {
			throw new IllegalStateException("Could not obtain core toolkit " + CORE_TOOLKIT, e);
		} catch(org.quick.core.parser.QuickParseException e) {
			throw new IllegalStateException("Could not parse core toolkit " + CORE_TOOLKIT, e);
		}
	}

	public static Builder build() {
		return new Builder();
	}

	public static class Builder {
		private final QuickEnvironment theEnv = new QuickEnvironment();

		private final java.util.concurrent.atomic.AtomicBoolean isBuilt = new java.util.concurrent.atomic.AtomicBoolean(false);

		public Builder withDefaults() {
			if(isBuilt.get())
				throw new IllegalStateException("The builder may not be changed after the environment is built");
			theEnv.setToolkitParser(new org.quick.core.parser.DefaultToolkitParser());
			theEnv.setDocumentParser(new org.quick.core.parser.QuickDomParser(theEnv));
			theEnv.setContentCreator(new QuickContentCreator());
			theEnv.setStyleParser(new DefaultStyleParser());
			theEnv.setAttributeParser(new DefaultAttributeParser());
			return this;
		}

		public Builder setToolkitParser(QuickToolkitParser toolkitParser) {
			if(isBuilt.get())
				throw new IllegalStateException("The builder may not be changed after the environment is built");
			theEnv.setToolkitParser(toolkitParser);
			return this;
		}

		public Builder setDocumentParser(QuickDocumentParser documentParser) {
			if(isBuilt.get())
				throw new IllegalStateException("The builder may not be changed after the environment is built");
			theEnv.setDocumentParser(documentParser);
			return this;
		}

		public Builder setContentCreator(QuickContentCreator contentCreator) {
			if(isBuilt.get())
				throw new IllegalStateException("The builder may not be changed after the environment is built");
			theEnv.setContentCreator(contentCreator);
			return this;
		}

		public Builder setStyleParser(QuickStyleParser styleParser) {
			if(isBuilt.get())
				throw new IllegalStateException("The builder may not be changed after the environment is built");
			theEnv.setStyleParser(styleParser);
			return this;
		}

		public Builder setAttributeParser(QuickAttributeParser attributeParser) {
			if(isBuilt.get())
				throw new IllegalStateException("The builder may not be changed after the environment is built");
			theEnv.setAttributeParser(attributeParser);
			return this;
		}

		public QuickEnvironment build() {
			if(theEnv.theToolkitParser == null)
				throw new IllegalStateException("No toolkit parser set");
			if(theEnv.theDocumentParser == null)
				throw new IllegalStateException("No document parser set");
			if(theEnv.theContentCreator == null)
				throw new IllegalStateException("No content creator set");
			if(theEnv.theStyleParser == null)
				throw new IllegalStateException("No style parser set");
			if(theEnv.theAttributeParser == null)
				throw new IllegalStateException("No attribute parser set");
			isBuilt.set(true);
			return theEnv;
		}
	}
}
