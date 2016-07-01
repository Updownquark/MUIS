package org.quick.core.prop;

import java.util.List;

public interface ExpressionContext {
	ExpressionResult<?> getVariable(String name);

	void getFunctions(String name, List<ExpressionResult<?>> args, List<ExpressionFunction<?>> functions);

	Unit<?, ?> getUnit(String name);
}
