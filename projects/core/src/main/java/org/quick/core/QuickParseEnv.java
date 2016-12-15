package org.quick.core;

import org.quick.core.mgr.QuickMessageCenter;
import org.quick.core.prop.ExpressionContext;

/** An environment needed to parse properties in Quick */
public interface QuickParseEnv {
	/** @return The class view to use in parsing, if needed */
	QuickClassView cv();
	/** @return The message center to report parsing errors to */
	QuickMessageCenter msg();
	/** @return The context to use to parse property values with */
	ExpressionContext getContext();
}