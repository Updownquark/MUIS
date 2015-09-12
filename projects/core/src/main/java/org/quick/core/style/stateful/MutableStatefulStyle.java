package org.quick.core.style.stateful;

/** An extension of StatefulStyle that allows setting of attribute values */
public interface MutableStatefulStyle extends StatefulStyle, org.quick.core.style.MutableConditionalStyle<StatefulStyle, StateExpression> {
}
