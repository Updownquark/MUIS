package org.muis.core;

import org.muis.core.mgr.MuisMessageCenter;
import org.muis.core.parser.MuisParser;
import org.muis.core.style.sheet.StyleSheet;

/** The environment that MUIS documents operate in */
public class MuisEnvironment {
	/** The location of the core toolkit */
	public static final java.net.URL CORE_TOOLKIT = MuisEnvironment.class.getResource("/MuisRegistry.xml");

	private static class EnvironmentStyle extends org.muis.core.style.sheet.AbstractStyleSheet {
		@Override
		protected void addDependency(StyleSheet depend) {
			super.addDependency(depend);
		}
	}

	private MuisParser theParser;

	private MuisMessageCenter theMessageCenter;

	private java.util.Map<String, MuisToolkit> theToolkits;

	private EnvironmentStyle theStyle;

	private final Object theToolkitLock;

	/** Creates a MUIS environment */
	public MuisEnvironment() {
		theToolkits = new java.util.concurrent.ConcurrentHashMap<>();
		theMessageCenter = new MuisMessageCenter(this, null, null);
		theStyle = new EnvironmentStyle();
		theToolkitLock = new Object();
	}

	/** @return The parser for the environment */
	public MuisParser getParser() {
		return theParser;
	}

	/** @param parser The parser for this environment */
	public void setParser(MuisParser parser) {
		if(theParser != null)
			throw new IllegalStateException("The environment parser may not be re-set");
		theParser = parser;
	}

	/** @return The message center for this environment */
	public MuisMessageCenter getMessageCenter() {
		return theMessageCenter;
	}

	/**
	 * @return The message center for this environment
	 * @see #getMessageCenter()
	 */
	public MuisMessageCenter msg() {
		return getMessageCenter();
	}

	/** @return The sum of all toolkit styles in this environment */
	public StyleSheet getStyle() {
		return theStyle;
	}

	/** @return All toolkits in this environment */
	public Iterable<MuisToolkit> getToolkits() {
		return prisms.util.ArrayUtils.immutableIterable(theToolkits.values());
	}

	/**
	 * @param location The location of the toolkit to get
	 * @return The toolkit at the given location
	 * @throws java.io.IOException If the toolkit resources cannot be retrieved
	 * @throws org.muis.core.parser.MuisParseException If the toolkit cannot be parsed
	 */
	public MuisToolkit getToolkit(java.net.URL location) throws java.io.IOException, org.muis.core.parser.MuisParseException {
		MuisToolkit ret = theToolkits.get(location.toString());
		if(ret != null)
			return ret;
		synchronized(theToolkitLock) {
			ret = theToolkits.get(location.toString());
			if(ret != null)
				return ret;
			ret = new MuisToolkit(this, location);
			theParser.fillToolkit(ret);
			theStyle.addDependency(ret.getStyle());
			theToolkits.put(location.toString(), ret);
			theParser.fillToolkitStyles(ret);
			ret.seal();
		}
		return ret;
	}

	/** @return The toolkit containing the core MUIS classes */
	public MuisToolkit getCoreToolkit() {
		if(CORE_TOOLKIT == null)
			throw new IllegalStateException("No such resource " + CORE_TOOLKIT + " for core toolkit");
		try {
			return getToolkit(CORE_TOOLKIT);
		} catch(java.io.IOException e) {
			throw new IllegalStateException("Could not obtain core toolkit " + CORE_TOOLKIT, e);
		} catch(org.muis.core.parser.MuisParseException e) {
			throw new IllegalStateException("Could not parse core toolkit " + CORE_TOOLKIT, e);
		}
	}
}
