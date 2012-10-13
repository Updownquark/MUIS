package org.muis.core;

import org.muis.core.parser.MuisParser;

/** The environment that MUIS documents operate in */
public class MuisEnvironment {
	/** The location of the core toolkit */
	public static final String CORE_TOOLKIT = "/MuisRegistry.xml";

	private MuisParser theParser;

	private java.util.Map<String, MuisToolkit> theToolkits;

	private final Object theToolkitLock;

	/** Creates a MUIS environment */
	public MuisEnvironment() {
		theToolkits = new java.util.concurrent.ConcurrentHashMap<>();
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
			ret = theParser.getToolkit(location);
			theToolkits.put(location.toString(), ret);
		}
		return ret;
	}

	/** @return The toolkit containing the core MUIS classes */
	public MuisToolkit getCoreToolkit() {
		try {
			return getToolkit(MuisEnvironment.class.getResource(CORE_TOOLKIT));
		} catch(NullPointerException e) {
			throw new IllegalStateException("No such resource " + CORE_TOOLKIT + " for core toolkit");
		} catch(java.io.IOException e) {
			throw new IllegalStateException("Could not obtain core toolkit " + CORE_TOOLKIT, e);
		} catch(org.muis.core.parser.MuisParseException e) {
			throw new IllegalStateException("Could not parse core toolkit " + CORE_TOOLKIT, e);
		}
	}
}
