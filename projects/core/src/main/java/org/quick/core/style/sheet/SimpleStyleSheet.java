package org.quick.core.style.sheet;

import org.quick.core.mgr.QuickMessageCenter;
import org.quick.core.style.SimpleConditionalStyle;

/** A simple implementation of style sheet that does not have dependencies */
public abstract class SimpleStyleSheet extends SimpleConditionalStyle<StyleSheet, StateGroupTypeExpression<?>> implements StyleSheet {
	/** @see SimpleConditionalStyle#SimpleConditionalStyle(QuickMessageCenter) */
	protected SimpleStyleSheet(QuickMessageCenter msg) {
		super(msg);
	}
}
