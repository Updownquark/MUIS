package org.quick.core.style;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.observe.ObservableValue;
import org.observe.assoc.ObservableMap;
import org.observe.collect.CollectionSession;
import org.observe.collect.ObservableCollection;
import org.observe.collect.ObservableSet;
import org.qommons.Transaction;
import org.quick.core.QuickElement;
import org.quick.core.QuickTemplate;
import org.quick.core.QuickTemplate.AttachPoint;
import org.quick.core.QuickTemplate.TemplateStructure.RoleAttribute;
import org.quick.core.mgr.QuickState;

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
			ObservableSet<RoleAttribute> roles = element.atts().getAllAttributes()
				.filter(RoleAttribute.class).immutable();
			return new ObservableMap<AttachPoint<?>, StyleConditionInstance<?>>(){
				@Override
				public boolean isSafe() {
					return roles.isSafe();
				}

				@Override
				public Transaction lock(boolean write, Object cause) {
					return roles.lock(write, cause);
				}

				@Override
				public ObservableValue<CollectionSession> getSession() {
					return roles.getSession();
				}

				@Override
				public TypeToken<AttachPoint<?>> getKeyType() {
					return new TypeToken<AttachPoint<?>>(){};
				}

				@Override
				public TypeToken<StyleConditionInstance<?>> getValueType() {
					return new TypeToken<StyleConditionInstance<?>>(){};
				}

				@Override
				public ObservableSet<AttachPoint<?>> keySet() {
					return roles.mapEquivalent(att->element.atts().get(att), null);
				}

				@Override
				public ObservableValue<StyleConditionInstance<?>> observe(Object key) {
					if (!(key instanceof AttachPoint))
						return ObservableValue.constant(getValueType(), null);
					AttachPoint<?> attachPoint = (AttachPoint<?>) key;
					return element.atts().observe(attachPoint.template.role)
						.mapV(ap -> ap.equals(key) ? StyleConditionInstance.of(QuickTemplate.getTemplateFor(element, ap)) : null);
				}

				@Override
				public ObservableSet<? extends org.observe.assoc.ObservableMap.ObservableEntry<AttachPoint<?>, StyleConditionInstance<?>>> observeEntries() {
					return ObservableMap.defaultObserveEntries(this);
				}

				@Override
				public String toString() {
					return entrySet().toString();
				}
			};
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

				if (!theState.isEmpty()) {
					str.append('(').append(theState).append(')');
				}

				return str.toString();
			}
		}
	}
}
