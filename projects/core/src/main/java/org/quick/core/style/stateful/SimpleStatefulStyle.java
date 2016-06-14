package org.quick.core.style.stateful;

import org.quick.core.mgr.QuickMessageCenter;
import org.quick.core.style.SimpleConditionalStyle;

/** Implements StatefulStyle without dependencies */
public abstract class SimpleStatefulStyle extends SimpleConditionalStyle<StatefulStyle, StateExpression> implements StatefulStyle {
	/** @see SimpleConditionalStyle#SimpleConditionalStyle(QuickMessageCenter) */
	protected SimpleStatefulStyle(QuickMessageCenter msg) {
		super(msg);
	}
}
