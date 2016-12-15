package org.quick.core.tags;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.quick.core.QuickElement;
import org.quick.core.mgr.QuickState;
import org.quick.core.prop.QuickAttribute;

import com.google.common.reflect.TypeToken;

/** A utility to help interpret the meanings of Quick annotations */
public class QuickTagUtils {
	/**
	 * Describes the acceptance of an attribute
	 * 
	 * @param <T> The type of the attribute
	 */
	public static class AcceptedAttributeStruct<T> {
		/** The annotation describing the attribute acceptance */
		public final AcceptAttribute annotation;
		/** The attribute */
		public final QuickAttribute<T> attribute;

		/**
		 * @param annotation The annotation describing the attribute acceptance
		 * @param attribute The attribute
		 */
		public AcceptedAttributeStruct(AcceptAttribute annotation, QuickAttribute<T> attribute) {
			this.annotation = annotation;
			this.attribute = attribute;
		}
	}

	/**
	 * @param <T> The target annotation type
	 * @param <A> The root annotation type
	 * @param targetType The type to search for annotations on
	 * @param annotationType The target annotation type
	 * @param subAnnotations A function getting all sub-annotations in the super annotation
	 * @return All sub-annotations found in all super-annotations found on the type
	 */
	public static <T, A extends Annotation> List<T> getTags(Class<?> targetType, Class<A> annotationType, Function<A, T[]> subAnnotations) {
		List<T> tags = new ArrayList<>();
		while (targetType != null) {
			A annotation = targetType.getAnnotation(annotationType);
			if (annotation != null)
				tags.addAll(Arrays.asList(subAnnotations.apply(annotation)));
			targetType = targetType.getSuperclass();
		}
		return tags;
	}

	/**
	 * @param type The type of element
	 * @return All states supported by the given element type
	 */
	public static List<QuickState> getStatesFor(Class<? extends QuickElement> type) {
		List<QuickState> states = new ArrayList<>();
		for (State state : getTags(type, QuickElementType.class, qet -> qet.states())) {
			try {
				states.add(new org.quick.core.mgr.QuickState(state.name(), state.priority()));
			} catch (IllegalArgumentException e) {
			}
		}
		return states;
	}

	/**
	 * @param elementType The element type
	 * @return All attributes declared by the element
	 */
	public static List<AcceptedAttributeStruct<?>> getAcceptedAttributes(Class<? extends QuickElement> elementType) {
		List<AcceptedAttributeStruct<?>> attributes = new ArrayList<>();
		for (AcceptAttribute attAnn : getTags(elementType, QuickElementType.class, elType -> elType.attributes())) {
			attributes.add(new AcceptedAttributeStruct<>(attAnn, (QuickAttribute<Object>) getAttribute(attAnn)));
		}
		return attributes;
	}

	/**
	 * @param attAnn The attribute-acceptance annotation
	 * @return The attribute referred to by the annotation
	 */
	public static QuickAttribute<?> getAttribute(AcceptAttribute attAnn) {
		Class<?> declaring = attAnn.declaringClass();
		if (attAnn.field().length() > 0) {
			if (attAnn.method().length() > 0)
				throw new IllegalArgumentException("@" + AcceptAttribute.class.getSimpleName()
					+ " annotations must declare either the field or the method property, not both");
			Field field;
			try {
				field = declaring.getField(attAnn.field());
			} catch (NoSuchFieldException | SecurityException e) {
				throw new IllegalStateException("Could not access field " + declaring.getName() + "." + attAnn.field(), e);
			}

			TypeToken<?> fieldType = TypeToken.of(field.getGenericType());
			if ((field.getModifiers() & Modifier.PUBLIC) == 0 || (field.getModifiers() & Modifier.STATIC) == 0
				|| !TypeToken.of(QuickAttribute.class).isAssignableFrom(fieldType))
				throw new IllegalArgumentException(
					"@" + AcceptAttribute.class.getSimpleName() + " annotation's field property (" + declaring.getName() + "."
						+ attAnn.field() + ") must refer to a public static field of type " + QuickAttribute.class.getName());
			QuickAttribute<?> att;
			try {
				att = (QuickAttribute<?>) field.get(null);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new IllegalStateException("Could not get value of field " + declaring.getName() + "." + attAnn.field(), e);
			}
			if (att == null)
				throw new IllegalArgumentException("@" + AcceptAttribute.class.getSimpleName() + " annotation's field "
					+ declaring.getName() + "." + attAnn.field() + " returned null");
			return att;
		} else if (attAnn.method().length() > 0) {
			Method method;
			try {
				method = declaring.getMethod(attAnn.method());
			} catch (NoSuchMethodException | SecurityException e) {
				throw new IllegalStateException("Could not access method " + declaring.getName() + "." + attAnn.method(), e);
			}

			TypeToken<?> fieldType = TypeToken.of(method.getGenericReturnType());
			if ((method.getModifiers() & Modifier.PUBLIC) == 0 || (method.getModifiers() & Modifier.STATIC) == 0
				|| !TypeToken.of(QuickAttribute.class).isAssignableFrom(fieldType))
				throw new IllegalArgumentException(
					"@" + AcceptAttribute.class.getSimpleName() + " annotation's method property (" + declaring.getName() + "."
						+ attAnn.method() + "() ) must refer to a public static method of type " + QuickAttribute.class.getName());
			QuickAttribute<?> att;
			try {
				att = (QuickAttribute<?>) method.invoke(null);
			} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
				throw new IllegalStateException("Could not invoke method " + declaring.getName() + "." + attAnn.field() + "()", e);
			}
			if (att == null)
				throw new IllegalArgumentException("@" + AcceptAttribute.class.getSimpleName() + " annotation's method "
					+ declaring.getName() + "." + attAnn.method() + "() returned null");
			return att;
		} else
			throw new IllegalArgumentException(
				"@" + AcceptAttribute.class.getSimpleName() + " annotations must declare either the field or the method property");
	}
}
