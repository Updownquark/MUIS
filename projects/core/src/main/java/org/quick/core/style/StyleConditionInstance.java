package org.quick.core.style;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.observe.Observable;
import org.observe.ObservableValue;
import org.observe.Observer;
import org.observe.SimpleObservable;
import org.observe.Subscription;
import org.observe.collect.ObservableCollection;
import org.observe.collect.ObservableSet;
import org.observe.util.TypeTokens;
import org.qommons.Lockable;
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
	static TypeToken<StyleConditionInstance<?>> TYPE = new TypeToken<StyleConditionInstance<?>>() {};

	/** @return The type of element that this condition is for */
	Class<T> getElementType();

	/** @return The set of states that are active in this condition */
	ObservableSet<QuickState> getState();

	/** @return The set of groups that this condition has */
	ObservableSet<String> getGroups();

	ObservableSet<AttachPoint<?>> getInterestedRoles();

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
		return of(element, null, null);
	}

	default Observable<?> changes() {
		Observable<?> stateChanges = getState().simpleChanges();
		Observable<?> groupChanges = getGroups().simpleChanges();
		Map<AttachPoint<?>, Subscription> roleSubs = new HashMap<>();
		SimpleObservable<Object> roleChanges = new SimpleObservable<>(null, false, null, null);
		Observable<?> rolledUpChanges = Observable.or(stateChanges, groupChanges, roleChanges);
		return new Observable<Object>() {
			final AtomicInteger subCount = new AtomicInteger();
			Subscription roleSub;

			@Override
			public Subscription subscribe(Observer<? super Object> observer) {
				if (subCount.getAndIncrement() == 0) {
					try (Transaction t = getInterestedRoles().lock(false, null)) {
						for (AttachPoint<?> role : getInterestedRoles()) {
							Observable<?> roleConditionChanges = StyleConditionInstanceFunctions.roleConditionChanges(getRoleParent(role));
							roleSubs.put(role, roleConditionChanges.act(v -> roleChanges.onNext(v)));
						}
						roleSub = getInterestedRoles().onChange(evt -> {
							switch (evt.getType()) {
							case add:
								Observable<?> roleConditionChanges = StyleConditionInstanceFunctions
									.roleConditionChanges(getRoleParent(evt.getNewValue()));
								roleSubs.put(evt.getNewValue(), roleConditionChanges.act(v -> roleChanges.onNext(v)));
								break;
							case remove:
								roleSubs.remove(evt.getOldValue()).unsubscribe();
								break;
							case set:
								if (evt.getOldValue() != evt.getNewValue()) {
									roleSubs.remove(evt.getOldValue()).unsubscribe();
									roleConditionChanges = StyleConditionInstanceFunctions
										.roleConditionChanges(getRoleParent(evt.getNewValue()));
									roleSubs.put(evt.getNewValue(), roleConditionChanges.act(v -> roleChanges.onNext(v)));
								}
								break;
							}
						});
					}
				}
				Subscription rolledUpSub = rolledUpChanges.subscribe(observer);
				return () -> {
					rolledUpSub.unsubscribe();
					if (subCount.decrementAndGet() == 0) {
						roleSub.unsubscribe();
						for (Subscription sub : roleSubs.values())
							sub.unsubscribe();
						roleSubs.clear();
					}
				};
			}

			@Override
			public boolean isSafe() {
				return false;
			}

			@Override
			public Transaction lock() {
				Lockable roleLock = Lockable.collapse(Lockable.lockable(getInterestedRoles(), false, false, null), //
					() -> getInterestedRoles().stream().map(r -> StyleConditionInstanceFunctions.roleConditionChanges(getRoleParent(r)))
						.collect(Collectors.toList()));
				return Lockable.lockAll(stateChanges, groupChanges, roleLock);
			}

			@Override
			public Transaction tryLock() {
				Lockable roleLock = Lockable.collapse(Lockable.lockable(getInterestedRoles(), false, false, null), //
					() -> getInterestedRoles().stream().map(r -> StyleConditionInstanceFunctions.roleConditionChanges(getRoleParent(r)))
						.collect(Collectors.toList()));
				return Lockable.tryLockAll(stateChanges, groupChanges, roleLock);
			}
		};
	}

	/**
	 * @param <T> The type of the element
	 * @param element The element
	 * @param extraStates Extra states for the condition that may or may not be present in the element's active states
	 * @param extraGroups Extra groups for the condition that may or may not be present in the element's groups
	 * @return A condition instance whose data reflects the element's, with the addition of the extra states
	 */
	public static <T extends QuickElement> StyleConditionInstance<T> of(T element, ObservableSet<QuickState> extraStates,
		ObservableSet<String> extraGroups) {
		ObservableSet<QuickState> states = element.getStateEngine().activeStates();
		if (extraStates != null)
			states = ObservableCollection.flattenCollections(TypeTokens.get().of(QuickState.class), states, extraStates).distinct()
				.collect();
		ObservableValue<ObservableSet<String>> groupValue = element.atts().get(StyleAttributes.group).map((Set<String> g) -> {
			return ObservableSet.<String> of(TypeTokens.get().STRING, g == null ? Collections.<String> emptySet() : g);
		});
		ObservableSet<String> groups = ObservableSet.flattenValue(groupValue);
		if (extraGroups != null)
			groups = ObservableCollection.flattenCollections(TypeTokens.get().STRING, groups, extraGroups).distinct().collect();
		ObservableSet<AttachPoint<?>> roles = element.atts().attributes().flow().filter(RoleAttribute.class)//
			.refreshEach(role -> element.atts().get(role).changes().noInit())//
			.mapEquivalent(AttachPoint.TYPE, ap -> element.atts().get(ap).get(), ap -> ap.template.role)
			.collect();
		Function<AttachPoint<?>, ObservableValue<StyleConditionInstance<?>>> conditions = attachPoint -> element.atts()
			.watchFor(attachPoint.template.role)//
			.map(StyleConditionInstance.TYPE, ap -> {
				if (ap == null || !ap.equals(attachPoint))
					return null;
				return StyleConditionInstance.of(QuickTemplate.getTemplateFor(element, ap), extraStates, extraGroups);
			});

		return new Builder<>((Class<T>) element.getClass())//
			.withState(states)//
			.withGroups(groups)//
			.withRoles(roles, conditions)//
			.build();
	}

	/** Internal functions for use by this interface's static methods */
	static class StyleConditionInstanceFunctions {
		private static Observable<?> roleConditionChanges(ObservableValue<StyleConditionInstance<?>> roleCondition) {
			return Observable.flatten(roleCondition.value().noInit().map(sci -> sci.changes()));
		}
	}

	public static final StyleConditionInstance<QuickElement> EMPTY = build(QuickElement.class).build();

	/**
	 * Builds ad-hoc condition values
	 *
	 * @param <T> The type of element that the condition will be for
	 */
	public static class Builder<T extends QuickElement> {
		private final Class<T> theType;
		private ObservableSet<QuickState> theState;
		private ObservableSet<String> theGroups;
		private ObservableSet<AttachPoint<?>> theRoles;
		private Function<AttachPoint<?>, ObservableValue<StyleConditionInstance<?>>> theRoleConditions;

		Builder(Class<T> type) {
			theType = type;
			theState = ObservableSet.of(TypeTokens.get().of(QuickState.class));
			theGroups = ObservableSet.of(TypeTokens.get().STRING);
			theRoles = ObservableSet.of(AttachPoint.TYPE);
			theRoleConditions = ap -> ObservableValue.of(StyleConditionInstance.TYPE, null);
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
		public Builder<T> withRoles(ObservableSet<AttachPoint<?>> roles,
			Function<AttachPoint<?>, ObservableValue<StyleConditionInstance<?>>> conditions) {
			theRoles = roles;
			theRoleConditions = conditions;
			return this;
		}

		/** @return A new condition instance with this builder's data */
		public StyleConditionInstance<T> build() {
			return new DefaultStyleConditionInstance<>(theType, theState, theGroups, theRoles, theRoleConditions);
		}

		private static class DefaultStyleConditionInstance<T extends QuickElement> implements StyleConditionInstance<T> {
			private final Class<T> theType;
			private final ObservableSet<QuickState> theState;
			private final ObservableSet<String> theGroups;
			private final ObservableSet<AttachPoint<?>> theRoles;
			private final Function<AttachPoint<?>, ObservableValue<StyleConditionInstance<?>>> theConditions;

			DefaultStyleConditionInstance(Class<T> type, ObservableSet<QuickState> state, ObservableSet<String> groups,
				ObservableSet<AttachPoint<?>> roles, Function<AttachPoint<?>, ObservableValue<StyleConditionInstance<?>>> conditions) {
				theType = type;
				theState = state;
				theGroups = groups;
				theRoles = roles;
				theConditions = conditions;
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
			public ObservableSet<AttachPoint<?>> getInterestedRoles() {
				return theRoles;
			}

			@Override
			public ObservableValue<StyleConditionInstance<?>> getRoleParent(AttachPoint<?> role) {
				return theConditions.apply(role);
			}

			@Override
			public String toString() {
				StringBuilder str = new StringBuilder(theType.getSimpleName());

				if (!theRoles.isEmpty()) {
					str.append('{');
					boolean first = true;
					for (AttachPoint<?> role : theRoles) {
						if (!first)
							str.append("  ");
						first = false;
						str.append(theConditions.apply(role)).append('.').append(role.name);
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
