package org.quick.core.style;

import java.util.*;

import org.observe.ObservableValue;
import org.observe.collect.ObservableCollection;
import org.observe.collect.ObservableSet;
import org.quick.core.QuickElement;
import org.quick.core.QuickTemplate;
import org.quick.core.QuickTemplate.AttachPoint;
import org.quick.core.QuickTemplate.TemplateStructure.RoleAttribute;
import org.quick.core.mgr.QuickState;
import org.quick.core.prop.QuickAttribute;

import com.google.common.reflect.TypeToken;

/**
 * An instance that may or may not satisfy a particular {@link StyleCondition}
 *
 * @param <T> The type of element that this condition is for
 */
public interface StyleConditionInstance<T extends QuickElement> {
	/** @return The type of element that this condition is for */
	Class<T> getElementType();

	/** @return The set of states that are active in this condition */
	ObservableSet<QuickState> getState();

	/** @return The set of groups that this condition has */
	ObservableSet<String> getGroups();

	/** @return The set of template role paths that this condition satisfies */
	ObservableSet<List<QuickTemplate.AttachPoint<?>>> getRolePaths();

	/**
	 * Builds an ad-hoc condition instance
	 *
	 * @param <T> The compile-time type of element that the condition is for
	 * @param type The run-time type of the element that the condition is for
	 * @return A builder to build the condition instance
	 */
	public static <T extends QuickElement> Builder<T> build(Class<T> type) {
		return new Builder<>(type);
	}

	/**
	 * @param <T> The type of the element
	 * @param element The element
	 * @return A condition instance whose data reflects the element's
	 */
	public static <T extends QuickElement> StyleConditionInstance<T> of(T element) {
		return of(element, null);
	}

	/**
	 * @param <T> The type of the element
	 * @param element The element
	 * @param extraStates Extra states for the condition that may or may not be present in the element's active states
	 * @return A condition instance whose data reflects the element's, with the addition of the extra states
	 */
	public static <T extends QuickElement> StyleConditionInstance<T> of(T element, ObservableSet<QuickState> extraStates) {
		ObservableSet<QuickState> states = element.getStateEngine().activeStates();
		if (extraStates != null)
			states = ObservableSet.unique(ObservableCollection.flattenCollections(states, extraStates), Object::equals);
		ObservableValue<ObservableSet<String>> groupValue = element.atts().getHolder(StyleAttributes.group).mapV((Set<String> g) -> {
			return ObservableSet.<String> constant(TypeToken.of(String.class), g == null ? Collections.<String> emptySet() : g);
		});
		// TODO Make this observable. Probably easier when element parent is observable
		ObservableSet<List<QuickTemplate.AttachPoint<?>>> rolePaths;
		Set<List<QuickTemplate.AttachPoint<?>>> tempRolePaths = new java.util.LinkedHashSet<>();
		addTemplatePaths(element, tempRolePaths, new LinkedList<>());
		rolePaths = ObservableSet.constant(new TypeToken<List<QuickTemplate.AttachPoint<?>>>() {}, tempRolePaths);
		/*rolePaths = org.observe.util.ObservableUtils.getPathSet(element, new TypeToken<QuickTemplate.AttachPoint<?>>() {},
			StyleConditionInstance::getTemplateRoles, QuickElement::getParent);*/

		return new Builder<>((Class<T>) element.getClass())//
			.withState(states)//
			.withGroups(ObservableSet.flattenValue(groupValue))//
			.withRolePaths(rolePaths)//
			.build();
	}

	static ObservableSet<QuickTemplate.AttachPoint<?>> getTemplateRoles(QuickElement element) {
		// TODO make this observable. Probably easier when attribute manager exposes an observable collection of attributes

	}

	/** For internal use */
	@SuppressWarnings("javadoc")
	static void addTemplatePaths(QuickElement element, Set<List<AttachPoint<?>>> rolePaths, LinkedList<QuickTemplate.AttachPoint<?>> path) {
		for (QuickAttribute<?> att : element.atts().attributes()) {
			if (att instanceof RoleAttribute) {
				QuickTemplate.AttachPoint<?> role = element.atts().get((RoleAttribute) att);
				path.add(role);
				rolePaths.add(new ArrayList<>(path));
				QuickElement parent = element.getParent().get();
				if (parent != null)
					addTemplatePaths(parent, rolePaths, path);
				path.removeLast();
			}
		}
	}

	/**
	 * Builds ad-hoc condition values
	 *
	 * @param <T> The type of element that the condition will be for
	 */
	public static class Builder<T extends QuickElement> {
		private final Class<T> theType;
		private ObservableSet<QuickState> theState;
		private ObservableSet<String> theGroups;
		private ObservableSet<List<QuickTemplate.AttachPoint<?>>> theRolePaths;

		Builder(Class<T> type) {
			theType = type;
			theState = ObservableSet.constant(TypeToken.of(QuickState.class));
			theGroups = ObservableSet.constant(TypeToken.of(String.class));
			theRolePaths = ObservableSet.constant(new TypeToken<List<QuickTemplate.AttachPoint<?>>>() {});
		}

		/**
		 * @param state The states for the condition
		 * @return This builder
		 */
		public Builder<T> withState(ObservableSet<QuickState> state) {
			theState = state;
			return this;
		}

		/**
		 * @param groups The groups for the condition
		 * @return This builder
		 */
		public Builder<T> withGroups(ObservableSet<String> groups) {
			theGroups = groups;
			return this;
		}

		/**
		 * @param rolePaths The template role paths for the condition
		 * @return This builder
		 */
		public Builder<T> withRolePaths(ObservableSet<List<QuickTemplate.AttachPoint<?>>> rolePaths) {
			theRolePaths = rolePaths;
			return this;
		}

		/** @return A new condition instance with this builder's data */
		public StyleConditionInstance<T> build() {
			return new DefaultStyleConditionInstance<>(theType, theState, theGroups, theRolePaths);
		}

		private static class DefaultStyleConditionInstance<T extends QuickElement> implements StyleConditionInstance<T> {
			private final Class<T> theType;
			private final ObservableSet<QuickState> theState;
			private final ObservableSet<String> theGroups;
			private final ObservableSet<List<QuickTemplate.AttachPoint<?>>> theRolePaths;

			DefaultStyleConditionInstance(Class<T> type, ObservableSet<QuickState> state, ObservableSet<String> groups,
				ObservableSet<List<AttachPoint<?>>> rolePaths) {
				super();
				theType = type;
				theState = state;
				theGroups = groups;
				theRolePaths = rolePaths;
			}

			@Override
			public Class<T> getElementType() {
				return theType;
			}

			@Override
			public ObservableSet<QuickState> getState() {
				return theState;
			}

			@Override
			public ObservableSet<String> getGroups() {
				return theGroups;
			}

			@Override
			public ObservableSet<List<QuickTemplate.AttachPoint<?>>> getRolePaths() {
				return theRolePaths;
			}
		}
	}
}
