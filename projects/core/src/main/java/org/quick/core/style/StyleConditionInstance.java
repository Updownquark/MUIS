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

public interface StyleConditionInstance<T extends QuickElement> {
	Class<T> getElementType();

	ObservableSet<QuickState> getState();

	ObservableSet<String> getGroups();

	ObservableSet<List<QuickTemplate.AttachPoint<?>>> getRolePaths();

	public static <T extends QuickElement> Builder<T> build(Class<T> type) {
		return new Builder<>(type);
	}

	public static <T extends QuickElement> StyleConditionInstance<T> of(T element) {
		return of(element, null);
	}

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

		return new Builder<>(element.getClass())//
			.withState(states)//
			.withGroups(ObservableSet.flattenValue(groupValue))//
			.withRolePaths(rolePaths)//
			.build();
	}

	static void addTemplatePaths(QuickElement element, Set<List<AttachPoint<?>>> rolePaths, LinkedList<QuickTemplate.AttachPoint<?>> path) {
		for (QuickAttribute<?> att : element.atts().attributes()) {
			if (att instanceof RoleAttribute) {
				QuickTemplate.AttachPoint<?> role = element.atts().get((RoleAttribute) att);
				path.add(role);
				rolePaths.add(new ArrayList<>(path));
				if (element.getParent() != null)
					addTemplatePaths(element.getParent(), rolePaths, path);
				path.removeLast();
			}
		}
	}

	public static class Builder<T extends QuickElement> {
		private final Class<T> theType;
		private ObservableSet<QuickState> theState;
		private ObservableSet<String> theGroups;
		private ObservableSet<List<QuickTemplate.AttachPoint<?>>> theRolePaths;

		public Builder(Class<T> type) {
			theType = type;
			theState = ObservableSet.constant(TypeToken.of(QuickState.class));
			theGroups = ObservableSet.constant(TypeToken.of(String.class));
			theRolePaths = ObservableSet.constant(new TypeToken<List<QuickTemplate.AttachPoint<?>>>() {});
		}

		public Builder<T> withState(ObservableSet<QuickState> state) {
			theState = state;
			return this;
		}

		public Builder<T> withGroups(ObservableSet<String> groups) {
			theGroups = groups;
			return this;
		}

		public Builder<T> withRolePaths(ObservableSet<List<QuickTemplate.AttachPoint<?>>> rolePaths) {
			theRolePaths = rolePaths;
			return this;
		}

		public StyleConditionInstance build() {
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
