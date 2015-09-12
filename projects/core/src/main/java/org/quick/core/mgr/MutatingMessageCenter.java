package org.quick.core.mgr;

import org.quick.core.event.QuickEvent;
import org.quick.core.mgr.QuickMessage.Type;

/** Wraps another message center, passing modified messages through to it */
public class MutatingMessageCenter extends QuickMessageCenter {
	private QuickMessageCenter theWrapped;

	private String thePrepend;

	private Object [] theParams;

	/** @param wrap The message center to wrap */
	public MutatingMessageCenter(QuickMessageCenter wrap) {
		super(wrap.getEnvironment(), wrap.getDocument(), wrap.getElement());
		theWrapped = wrap;
		theParams = new Object[0];
	}

	/**
	 * @param wrap The message center to wrap
	 * @param prepend The text to prepend to messages passed through to the wrapped message center
	 * @param params Parameters to append to messages passed through to the wrapped message center
	 */
	public MutatingMessageCenter(QuickMessageCenter wrap, String prepend, Object... params) {
		this(wrap);
		thePrepend = prepend;
		if(params.length % 2 != 0)
			throw new IllegalArgumentException("message params must be in format [name, value, name, value, ...]"
				+ "--odd argument count not allowed");
		for(int p = 0; p < params.length; p += 2) {
			if(!(params[p] instanceof String))
				throw new IllegalArgumentException("message params must be in format [name, value, name, value, ...]"
					+ "--even indices must be strings. Found " + (params[p] == null ? "null" : params[p].getClass().getName()));
		}
		theParams = params;
	}

	@Override
	public void message(Type type, String text, QuickEvent cause, Throwable exception, Object... params) {
		if(thePrepend != null)
			text = thePrepend + text;
		if(theParams != null)
			params = prisms.util.ArrayUtils.addAll(params, theParams);
		theWrapped.message(type, text, cause, exception, params);
	}
}
