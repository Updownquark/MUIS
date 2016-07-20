package org.quick.core.prop;

import java.util.List;

import org.observe.ObservableValue;

public interface ExpressionContext {
	ObservableValue<?> getVariable(String name);

	void getFunctions(String name, List<ExpressionFunction<?>> functions);

	Unit<?, ?> getUnit(String name);
}
