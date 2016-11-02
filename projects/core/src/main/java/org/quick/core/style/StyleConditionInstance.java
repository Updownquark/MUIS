package org.quick.core.style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.observe.ObservableValue;
import org.observe.assoc.ObservableMap;
import org.observe.assoc.ObservableSetTree;
import org.observe.collect.ObservableCollection;
import org.observe.collect.ObservableSet;
import org.qommons.BiTuple;
import org.quick.core.QuickElement;
import org.quick.core.QuickTemplate;
import org.quick.core.QuickTemplate.AttachPoint;
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
	ObservableSet<List<AttachPoint<?>>> getRolePaths();

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
		ObservableSet<List<AttachPoint<?>>> rolePaths = StyleConditionInstanceFunctions.getTemplateRoles(element);

		return new Builder<>((Class<T>) element.getClass())//
			.withState(states)//
			.withGroups(ObservableSet.flattenValue(groupValue))//
			.withRolePaths(rolePaths)//
			.build();
	}

	/** Internal functions for use by this interface's static methods */
	static class StyleConditionInstanceFunctions {
		private static ObservableSet<List<AttachPoint<?>>> getTemplateRoles(QuickElement element) {
			TypeToken<AttachPoint<?>> apType = new TypeToken<AttachPoint<?>>() {};
			ObservableSetTree<BiTuple<QuickElement, AttachPoint<?>>, AttachPoint<?>> tree = ObservableSetTree.of(//
				ObservableValue.constant(new BiTuple<>(element, null)), //
				apType, tuple -> ObservableValue.constant(apType, tuple.getValue2()), //
				tuple -> rolesFor(tuple.getValue1())//
			);
			ObservableSet<List<AttachPoint<?>>> rawPaths = ObservableSetTree.valuePathsOf(tree, false);
			ObservableSet<List<AttachPoint<?>>> filteredPaths = rawPaths//
				.filter(path -> path.get(0) != null) // Filter out the root path
				.mapEquivalent(path -> { // Remove the first element, which is null, and reverse the path
					ArrayList<AttachPoint<?>> newPath = new ArrayList<>(path.size() - 1);
					for (int i = 0; i < newPath.size(); i++)
						newPath.add(path.get(path.size() - i - 1));
					return Collections.unmodifiableList(newPath);
				}, null); // The reverse function is unimportant since Objects.equals doesn't care
			return filteredPaths;
		}

		private static ObservableSet<BiTuple<QuickElement, AttachPoint<?>>> rolesFor(QuickElement element) {
			if (element == null)
				return ObservableSet.constant(new TypeToken<BiTuple<QuickElement, AttachPoint<?>>>() {});
			ObservableMap<QuickAttribute<?>, ?> attMap = element.atts().getAllValues()
				.filterKeys(attr -> attr instanceof QuickTemplate.TemplateStructure.RoleAttribute);
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
		private ObservableSet<List<AttachPoint<?>>> theRolePaths;

		Builder(Class<T> type) {
			theType = type;
			theState = ObservableSet.constant(TypeToken.of(QuickState.class));
			theGroups = ObservableSet.constant(TypeToken.of(String.class));
			theRolePaths = ObservableSet.constant(new TypeToken<List<AttachPoint<?>>>() {});
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
		public Builder<T> withRolePaths(ObservableSet<List<AttachPoint<?>>> rolePaths) {
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
			private final ObservableSet<List<AttachPoint<?>>> theRolePaths;

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
			public ObservableSet<List<AttachPoint<?>>> getRolePaths() {
				return theRolePaths;
			}
		}
	}
}
