package org.quick.core.prop;

import java.util.Collection;

import org.observe.ObservableValue;

public interface ExpressionContext {
	ObservableValue<?> getVariable(String name);

	<T extends Collection<ExpressionFunction<?>>> T getFunctions(String name, T functions);

	<T extends Collection<Unit<?, ?>>> T getUnits(String name, T units);
}
