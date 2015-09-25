package org.quick.core;

import org.observe.collect.ObservableList;
import org.quick.core.mgr.QuickMessageCenter;
import org.quick.core.model.QuickValueReferenceParser;
import org.quick.core.parser.DefaultModelValueReferenceParser;
import org.quick.core.parser.QuickContentCreator;
import org.quick.core.parser.QuickParser;
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

	private QuickParser theParser;

	private QuickContentCreator theContentCreator;

	private QuickMessageCenter theMessageCenter;

	private java.util.Map<String, QuickToolkit> theToolkits;

	private QuickCache theCache;

	private EnvironmentStyle theStyle;

	private final Object theToolkitLock;

	private DefaultModelValueReferenceParser theMVP;

	private ObservableList<StyleSheet> theStyleDependencyController;

	/** Creates a MUIS environment */
	public QuickEnvironment() {
		theToolkits = new java.util.concurrent.ConcurrentHashMap<>();
		theMessageCenter = new QuickMessageCenter(this, null, null);
		theCache = new QuickCache();
		theStyleDependencyController = new org.observe.collect.impl.ObservableArrayList<>(TypeToken.of(StyleSheet.class));
		ObservableList<StyleSheet> styleDepends = theStyleDependencyController.immutable();
		theStyle = new EnvironmentStyle(styleDepends);
		theToolkitLock = new Object();
		theMVP = new DefaultModelValueReferenceParser(DefaultModelValueReferenceParser.BASE, null);
	}

	@Override
	public QuickClassView cv() {
		QuickClassView ret = new QuickClassView(this, null, getCoreToolkit());
		ret.seal();
		return ret;
	}

	@Override
	public QuickValueReferenceParser getValueParser() {
		return theMVP;
	}

	/** @return The parser for the environment */
	public QuickParser getParser() {
		return theParser;
	}

	/** @param parser The parser for this environment */
	public void setParser(QuickParser parser) {
		if(theParser != null)
			throw new IllegalStateException("The environment parser may not be re-set");
		theParser = parser;
	}

	/** @return The content creator for the environment */
	public QuickContentCreator getContentCreator() {
		return theContentCreator;
	}

	/** @param creator The content creator for this environment */
	public void setContentCreator(QuickContentCreator creator) {
		if(theContentCreator != null)
			throw new IllegalStateException("The environment content creator may not be re-set");
		theContentCreator = creator;
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
			theParser.fillToolkit(ret);
			theStyleDependencyController.add(ret.getStyle());
			theToolkits.put(location.toString(), ret);
			theParser.fillToolkitStyles(ret);
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
}
