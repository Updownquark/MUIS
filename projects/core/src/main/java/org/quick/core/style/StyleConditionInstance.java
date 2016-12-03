package org.quick.core.style;

import java.util.*;

import org.observe.ObservableValue;
import org.observe.assoc.ObservableMap;
import org.observe.assoc.ObservableSetTree;
import org.observe.collect.ObservableCollection;
import org.observe.collect.ObservableSet;
import org.qommons.BiTuple;
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

	/**
	 * @param role The template role to get the parent for
	 * @return The condition instance that defines the given template role for this condition instance. The value may be null.
	 */
	ObservableValue<StyleConditionInstance<?>> getRoleParent(AttachPoint<?> role);

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
		ObservableMap<AttachPoint<?>, StyleConditionInstance<?>> roles = StyleConditionInstanceFunctions.getTemplateRoles(element);

		return new Builder<>((Class<T>) element.getClass())//
			.withState(states)//
			.withGroups(ObservableSet.flattenValue(groupValue))//
			.withRoles(roles)//
			.build();
	}

	/** Internal functions for use by this interface's static methods */
	static class StyleConditionInstanceFunctions {
		private static ObservableMap<AttachPoint<?>, StyleConditionInstance<?>> getTemplateRoles(QuickElement element) {
			TypeToken<AttachPoint<?>> apType = new TypeToken<AttachPoint<?>>() {};
			ObservableSet<AttachPoint<?>> attachPoints = element.atts().getAllValues()
				.filterKeysStatic(attr -> attr instanceof RoleAttribute).entrySet()
				.mapEquivalent(entry -> (AttachPoint<?>) entry.getValue(), //
					null); // The reverse function is unimportant since Objects.equals doesn't care
			java.util.function.Function<AttachPoint<?>, StyleConditionInstance<?>> groupFn = ap -> StyleConditionInstance
				.of(QuickTemplate.getTemplateFor(element, ap));
			return attachPoints.groupBy(groupFn).unique();
			ObservableSetTree<BiTuple<QuickElement, AttachPoint<?>>, AttachPoint<?>> tree = ObservableSetTree.of(//
				ObservableValue.constant(new BiTuple<>(element, null)), //
				apType, tuple -> ObservableValue.constant(apType, tuple.getValue2()), //
				tuple -> rolesFor(tuple == null ? null : tuple.getValue1())//
			);
			ObservableSet<List<AttachPoint<?>>> rawPaths = ObservableSetTree.valuePathsOf(tree, false);
			ObservableSet<List<AttachPoint<?>>> filteredPaths = rawPaths//
				.filter(path -> path.size() > 1) // Filter out the root path
				.mapEquivalent(path -> { // Remove the first element, which is null, and reverse the path
					ArrayList<AttachPoint<?>> newPath = new ArrayList<>(path.size() - 1);
					for (int i = 0; i < path.size() - 1; i++)
						newPath.add(path.get(path.size() - i - 1));
					return Collections.unmodifiableList(newPath);
				}, null); // The reverse function is unimportant since Objects.equals doesn't care
			return filteredPaths;
		}

		private static ObservableSet<BiTuple<QuickElement, AttachPoint<?>>> rolesFor(QuickElement element) {
			if (element == null)
				return ObservableSet.constant(new TypeToken<BiTuple<QuickElement, AttachPoint<?>>>() {});
			ObservableMap<QuickAttribute<?>, ?> attMap = element.atts().getAllValues()
				.filterKeysStatic(attr -> attr instanceof QuickTemplate.TemplateStructure.RoleAttribute);
			ObservableMap<QuickAttribute<?>, BiTuple<QuickElement, AttachPoint<?>>> roleMap = attMap.map(v -> {
				AttachPoint<?> ap = (AttachPoint<?>) v;
				QuickTemplate template = QuickTemplate.getTemplateFor(element, ap);
				return new BiTuple<>(template, ap);
			});
			return roleMap.entrySet().mapEquivalent(entry -> entry.getValue(), null);
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
		private ObservableMap<AttachPoint<?>, StyleConditionInstance<?>> theRoles;

		Builder(Class<T> type) {
			theType = type;
			theState = ObservableSet.constant(TypeToken.of(QuickState.class));
			theGroups = ObservableSet.constant(TypeToken.of(String.class));
			theRoles = ObservableMap.empty(new TypeToken<AttachPoint<?>>() {}, new TypeToken<StyleConditionInstance<?>>() {});
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
		 * @param roles The template roles for the condition
		 * @return This builder
		 */
		public Builder<T> withRoles(ObservableMap<AttachPoint<?>, StyleConditionInstance<?>> roles) {
			theRoles = roles;
			return this;
		}

		/** @return A new condition instance with this builder's data */
		public StyleConditionInstance<T> build() {
			return new DefaultStyleConditionInstance<>(theType, theState, theGroups, theRoles);
		}

		private static class DefaultStyleConditionInstance<T extends QuickElement> implements StyleConditionInstance<T> {
			private final Class<T> theType;
			private final ObservableSet<QuickState> theState;
			private final ObservableSet<String> theGroups;
			private final ObservableMap<AttachPoint<?>, StyleConditionInstance<?>> theRoles;

			DefaultStyleConditionInstance(Class<T> type, ObservableSet<QuickState> state, ObservableSet<String> groups,
				ObservableMap<AttachPoint<?>, StyleConditionInstance<?>> rolePaths) {
				theType = type;
				theState = state;
				theGroups = groups;
				theRoles = rolePaths;
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
			public ObservableValue<StyleConditionInstance<?>> getRoleParent(AttachPoint<?> role) {
				return theRoles.observe(role);
			}

			@Override
			public String toString() {
				StringBuilder str = new StringBuilder(theType.getSimpleName());

				if (!theRoles.isEmpty()) {
					str.append('{');
					boolean first = true;
					for (Map.Entry<AttachPoint<?>, StyleConditionInstance<?>> roleEntry : theRoles.entrySet()) {
						if (!first)
							str.append("  ");
						first = false;
						str.append(roleEntry.getValue()).append('.').append(roleEntry.getKey().name);
					}
					str.append('}');
				}

				if (!theGroups.isEmpty()) {
					str.append('[');
					boolean first = true;
					for (String group : theGroups) {
						if (!first)
							str.append(',');
						first = false;
						str.append(group);
					}

					str.append(']');
				}

				if (theState != null) {
					str.append('(').append(theState).append(')');
				}

				return str.toString();
			}
		}
	}
}
