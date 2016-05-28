package org.quick.core.prop;

import org.observe.ObservableValue;

public class ExpressionResult<T> {
	public final ExpressionType<T> type;
	public final ObservableValue<T> value;

	public ExpressionResult(ExpressionType<T> type, ObservableValue<T> value) {
		this.type = type;
		this.value = value;
	}

	public static <T> ExpressionResult<T> of(ObservableValue<T> value) {
		return new ExpressionResult<>(ExpressionType.of(value.getType()), value);
	}

	public static <T> ExpressionResult<T> ofType(ExpressionType<T> type) {
		return new ExpressionResult<>(type, (ObservableValue<T>) null);
	}
}
