package org.muis.core.style.stateful;

/** An extension of StatefulStyle that allows setting of attribute values */
public interface MutableStatefulStyle extends StatefulStyle, org.muis.core.style.MutableConditionalStyle<StatefulStyle, StateExpression> {
}
