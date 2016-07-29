package org.quick;

import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;
import org.quick.core.layout.Direction;
import org.quick.core.prop.ExpressionFunction;

public class PropertyTest {
	@Test
	public void testEnumFunctionBug() {
		try {
			ExpressionFunction.build(makeEnumFunction(Direction.class));
			Assert.fail("The InternalError thrown from this method must have been fixed."
				+ "  Remove the ExpressionFunction.build(TypeToken, ...) methods");
		} catch (InternalError error) {
		}
	}

	private <T extends Enum<T>> Function<String, T> makeEnumFunction(Class<T> enumType) {
		return new Function<String, T>() {
			@Override
			public T apply(String name) {
				return Enum.valueOf(enumType, name);
			}
		};
	}
}
