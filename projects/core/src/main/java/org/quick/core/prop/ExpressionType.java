package org.quick.core.prop;

import com.google.common.reflect.TypeToken;

public class ExpressionType<T> {
	private static final ExpressionType<Object> NULL = new ExpressionType<>(TypeToken.of(Object.class), false, true);
	public final TypeToken<T> type;
	public final boolean isType;
	public final boolean isNull;

	private ExpressionType(TypeToken<T> t, boolean isTyp, boolean isNul) {
		type = t;
		isType = isTyp;
		isNull = isNul;
	}

	public static <T> ExpressionType<T> of(TypeToken<T> type) {
		return new ExpressionType<>(type, false, false);
	}

	public static <T> ExpressionType<T> ofType(TypeToken<T> type) {
		return new ExpressionType<>(type, true, false);
	}

	public static ExpressionType<Object> ofNull() {
		return NULL;
	}
}
