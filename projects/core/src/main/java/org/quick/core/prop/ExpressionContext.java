package org.quick.core.prop;

import java.util.List;

import org.observe.ObservableValue;

import com.google.common.reflect.TypeToken;

public interface ExpressionContext {
	ObservableValue<?> getVariable(String name);

	void getFunctions(String name, List<TypeToken<?>> args, List<ExpressionFunction<?>> functions);

	Unit<?, ?> getUnit(String name);
}
