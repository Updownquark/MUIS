package org.quick.core;

import java.util.Collection;
import java.util.Collections;

import org.observe.collect.ObservableList;
import org.quick.core.mgr.QuickMessageCenter;
import org.quick.core.parser.*;
import org.quick.core.prop.DefaultExpressionContext;
import org.quick.core.prop.ExpressionContext;
import org.quick.core.style.sheet.StyleSheet;

import com.google.common.reflect.TypeToken;

/** The environment that Quick documents operate in */
public class QuickEnvironment implements QuickParseEnv {
	/** The location of the core toolkit */
	public static final java.net.URL CORE_TOOLKIT = QuickEnvironment.class.getResource("/QuickRegistry.xml");

	private static class EnvironmentStyle extends org.quick.core.style.sheet.AbstractStyleSheet {
		EnvironmentStyle(QuickMessageCenter msg, ObservableList<StyleSheet> dependencies) {
			super(msg, dependencies);
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
	private QuickPropertyParser theAttributeParser;

	private final DefaultExpressionContext theContext;
	private final QuickMessageCenter theMessageCenter;
	private final java.util.Map<String, QuickToolkit> theToolkits;
	private final QuickCache theCache;
	private final EnvironmentStyle theStyle;

	private final Object theToolkitLock;
	private ObservableList<StyleSheet> theStyleDependencyController;

	private QuickEnvironment() {
		theContext = DefaultExpressionContext.build().build();
		theMessageCenter = new QuickMessageCenter(this, null, null);
		theToolkits = new java.util.concurrent.ConcurrentHashMap<>();
		theCache = new QuickCache();
		theStyleDependencyController = new org.observe.collect.impl.ObservableArrayList<>(TypeToken.of(StyleSheet.class));
		ObservableList<StyleSheet> styleDepends = theStyleDependencyController.immutable();
		theStyle = new EnvironmentStyle(theMessageCenter, styleDepends);
		theToolkitLock = new Object();
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

	public QuickPropertyParser getPropertyParser() {
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

	@Override
	public ExpressionContext getContext() {
		return theContext;
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
	public Collection<QuickToolkit> getToolkits() {
		return Collections.unmodifiableCollection(theToolkits.values());
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
		QuickToolkit toolkit = theToolkits.get(location.toString());
		if(toolkit != null)
			return toolkit;
		synchronized(theToolkitLock) {
			toolkit = theToolkits.get(location.toString());
			if(toolkit != null)
				return toolkit;
			toolkit = theToolkitParser.parseToolkit(location, tk -> {
				theStyleDependencyController.add(tk.getStyle());
				theToolkits.put(location.toString(), tk);
			});
		}
		return toolkit;
	}

	/** @return The toolkit containing the core Quick classes */
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
			theEnv.theToolkitParser = new org.quick.core.parser.DefaultToolkitParser(theEnv);
			theEnv.theDocumentParser = new org.quick.core.parser.QuickDomParser(theEnv);
			theEnv.theContentCreator = new QuickContentCreator();
			theEnv.theStyleParser = new DefaultStyleParser(theEnv);
			theEnv.theAttributeParser = new DefaultPropertyParser(theEnv);
			return this;
		}

		public Builder setToolkitParser(QuickToolkitParser toolkitParser) {
			if(isBuilt.get())
				throw new IllegalStateException("The builder may not be changed after the environment is built");
			theEnv.theToolkitParser = toolkitParser;
			return this;
		}

		public Builder setDocumentParser(QuickDocumentParser documentParser) {
			if(isBuilt.get())
				throw new IllegalStateException("The builder may not be changed after the environment is built");
			theEnv.theDocumentParser = documentParser;
			return this;
		}

		public Builder setContentCreator(QuickContentCreator contentCreator) {
			if(isBuilt.get())
				throw new IllegalStateException("The builder may not be changed after the environment is built");
			theEnv.theContentCreator = contentCreator;
			return this;
		}

		public Builder setStyleParser(QuickStyleParser styleParser) {
			if(isBuilt.get())
				throw new IllegalStateException("The builder may not be changed after the environment is built");
			theEnv.theStyleParser = styleParser;
			return this;
		}

		public Builder setAttributeParser(QuickPropertyParser attributeParser) {
			if(isBuilt.get())
				throw new IllegalStateException("The builder may not be changed after the environment is built");
			theEnv.theAttributeParser = attributeParser;
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
